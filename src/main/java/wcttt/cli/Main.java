/*
 * WCT³ (WIAI Course Timetabling Tool) is a software that strives to automate
 * the timetabling process at the WIAI faculty of the University of Bamberg.
 *
 * WCT³-CLI comprises a command line interface to be able to run the algorithms
 * without using a GUI.
 *
 * Copyright (C) 2018 Nicolas Gross
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package wcttt.cli;

import wcttt.lib.algorithms.*;
import wcttt.lib.algorithms.tabu_based_memetic_approach.TabuBasedMemeticApproach;
import wcttt.lib.binder.WctttBinder;
import wcttt.lib.binder.WctttBinderException;
import wcttt.lib.model.Semester;
import wcttt.lib.model.Timetable;
import wcttt.lib.model.WctttModelException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {

	public static void main(String[] args) throws WctttCliException {
		if (checkArguments(args)) {
			return;
		}

		Semester semester;
		WctttBinder binder;
		try {
			binder = new WctttBinder(new File(args[0]));
			semester = binder.parse();
		} catch (WctttBinderException e) {
			throw new WctttCliException("Error while parsing the semester", e);
		}

		// |-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-|
		// |  ADD NEW ALGORITHMS TO THIS LIST  |
		// |-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-|
		List<Algorithm> algorithms = new ArrayList<>();
		algorithms.add(new TabuBasedMemeticApproach(semester));

		Timetable generatedTimetable;
		try (BufferedReader inputReader =
                new BufferedReader(new InputStreamReader(System.in))) {
			Algorithm selectedAlgorithm =
					showAlgorithmSelection(algorithms, inputReader);
			parseAlgorithmParameters(selectedAlgorithm, inputReader);
			generatedTimetable =
					runAlgorithm(selectedAlgorithm, inputReader);
		} catch (IOException e) {
			throw new WctttCliFatalException("Problem with input reader", e);
		}

		if (generatedTimetable != null) {
			System.out.println("A feasible timetable was found");
			try {
				generatedTimetable.setName("wcttt-cli-default-id");
				semester.addTimetable(generatedTimetable);
				setNextTimetableName(semester, generatedTimetable);
				binder.write(semester);
			} catch (WctttModelException e) {
				throw new WctttCliException("Generated timetable was " +
						"invalid, there is a bug in the algorithm", e);
			} catch (WctttBinderException e) {
				throw new WctttCliException("Error while writing the semester",
						e);
			}
		} else {
			System.out.println("No feasible timetable was found");
		}
	}

	private static boolean checkArguments(String[] args) {
		if (args.length == 0 || args.length > 1) {
			printHelp();
			return true;
		} else if (args[0].startsWith("-")) {
			switch (args[0]) {
				case "-v":
				case "--version":
					System.out.println("Version: " +
							Main.class.getPackage().getImplementationVersion());
					break;
				default:
					printHelp();
			}
			return true;
		}
		return false;
	}

	private static void printHelp() {
		System.out.println("Usage: wcttt-cli [SEMESTER]");
		System.out.println("Example: wcttt-cli ws1819/semester.xml");
		System.out.println();
		System.out.println("Options:");
		System.out.println("  -v, --version             display the version");
		System.out.println("  -h, --help                display this help");
	}

	private static Algorithm showAlgorithmSelection(List<Algorithm> algorithms,
	                                                BufferedReader inputReader)
			throws IOException {
		System.out.println("Choose an algorithm:");
		for (int i = 0; i < algorithms.size(); i++) {
			System.out.println((i + 1) + ". " + algorithms.get(i).getName());
		}
		System.out.println();

		int selected;
		while (true) {
			System.out.print("Enter the desired algorithm number: ");
			String input = inputReader.readLine();
			if (input == null) {
				// Should never happen theoretically
				throw new WctttCliFatalException("End of input stream reached");
			}
			try {
				selected = Integer.parseInt(input);
				if (selected < 1 || selected > algorithms.size()) {
					System.out.println("Please select a number within the list");
				} else {
					break;
				}
			} catch (NumberFormatException e) {
				System.out.println("Please select a number within the list");
			}
		}

		System.out.println(algorithms.get(selected - 1).getName() + " selected");
		System.out.println();
		return algorithms.get(selected - 1);
	}

	private static void parseAlgorithmParameters(Algorithm selectedAlgorithm,
	                                             BufferedReader inputReader)
			throws IOException {
		if (selectedAlgorithm.getParameters().isEmpty()) {
			try {
				selectedAlgorithm.setParameterValues(new LinkedList<>());
				return;
			} catch (WctttAlgorithmException e) {
				throw new WctttCliFatalException("Implementation error in " +
						"algorithm '" + selectedAlgorithm + "', no parameter " +
						"specified but empty value list was rejected", e);
			}
		}
		System.out.println("Following parameters are required:");
		List<ParameterDefinition> definitions =
				selectedAlgorithm.getParameters();
		for (int i = 0; i < definitions.size(); i++) {
			if (i > 0) {
				System.out.print(", ");
			}
			System.out.print(definitions.get(i));
		}
		System.out.println();
		while (true) {
			System.out.println();
			System.out.println("Please enter the according values in one " +
					"line, separated by commas and in the previously shown order");
			try {
				List<ParameterValue> values = new ArrayList<>(
						selectedAlgorithm.getParameters().size());
				String[] splitInput = inputReader.readLine().split(", ");
				for (int i = 0; i < splitInput.length; i++) {
					parseParameterValue(splitInput[i], i, definitions, values);
				}
				selectedAlgorithm.setParameterValues(values);
				break;
			} catch (WctttAlgorithmException e) {
				System.out.println(e.getMessage());
			} catch (NumberFormatException e) {
				System.out.println("Please adhere to the parameter types");
			}
		}

		System.out.println();
	}

	private static void parseParameterValue(String input, int index,
	                                        List<ParameterDefinition> definitions,
	                                        List<ParameterValue> values) {
		ParameterDefinition definition = definitions.get(index);
		if (definition.getType() == ParameterType.INT) {
			ParameterValue<Integer> value = new ParameterValue<>(definition,
					Integer.parseInt(input));
			values.add(value);
		} else {
			ParameterValue<Double> value = new ParameterValue<>(definition,
					Double.parseDouble(input));
			values.add(value);
		}
	}

	private static Timetable runAlgorithm(Algorithm selectedAlgorithm,
	                                      BufferedReader inputReader)
			throws WctttCliException {
		AtomicBoolean finished = new AtomicBoolean(false);
		Thread thread = listenForAbort(selectedAlgorithm, finished, inputReader);
		System.out.println("Enter 'q' to exit the algorithm");
		Timetable timetable;
		try {
			timetable = selectedAlgorithm.generate();
		} catch (WctttAlgorithmException e) {
			throw new WctttCliException("A problem occurred while running " +
					"the algorithm", e);
		} finally {
			finished.set(true);
			long start = System.currentTimeMillis();
			long remainingMillis;
			while ((remainingMillis = System.currentTimeMillis() - start) < 5000) {
				try {
					thread.join(remainingMillis);
					break;
				} catch (InterruptedException e) {
					// ignore
				}
			}
		}
		return timetable;
	}

	private static Thread listenForAbort(Algorithm selectedAlgorithm,
	                                     AtomicBoolean finished,
	                                     BufferedReader inputReader) {
		Runnable runnable = () -> {
			while (!finished.get()) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// ignore
				}
				String line;
				try {
					while (!finished.get() &&
							(line = inputReader.readLine()) != null) {
						if (line.equals("q")) {
							selectedAlgorithm.cancel();
							break;
						}
					}
				} catch (IOException e) {
					System.err.println("Error while reading from System.in");
					e.printStackTrace();
				}
			}
		};
		Thread thread = new Thread(runnable);
		thread.start();
		return thread;
	}

	private static void setNextTimetableName(Semester semester,
	                                         Timetable timetable) {
		int nextTimetablName = 0;
		while (true) {
			try {
				semester.updateTimetableName(timetable,
						"timetable" + nextTimetablName);
				nextTimetablName++;
				return;
			} catch (WctttModelException e) {
				nextTimetablName++;
			}
		}
	}
}
