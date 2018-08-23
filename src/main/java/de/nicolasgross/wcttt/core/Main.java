package de.nicolasgross.wcttt.core;

import de.nicolasgross.wcttt.core.algorithms.Algorithm;
import de.nicolasgross.wcttt.core.algorithms.ParameterDefinition;
import de.nicolasgross.wcttt.core.algorithms.ParameterType;
import de.nicolasgross.wcttt.core.algorithms.ParameterValue;
import de.nicolasgross.wcttt.core.algorithms.tabu_based_memetic_approach.TabuBasedMemeticApproach;
import de.nicolasgross.wcttt.lib.binder.WctttBinder;
import de.nicolasgross.wcttt.lib.binder.WctttBinderException;
import de.nicolasgross.wcttt.lib.model.Semester;
import de.nicolasgross.wcttt.lib.model.Timetable;
import de.nicolasgross.wcttt.lib.model.WctttModelException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {

	public static void main(String[] args) throws WctttCoreException {
		if (args.length == 0 || args[0].startsWith("-")) {
			printHelp();
			return;
		}

		Semester semester;
		WctttBinder binder;
		try {
			binder = new WctttBinder(new File(args[0]));
			semester = binder.parse();
		} catch (WctttBinderException e) {
			throw new WctttCoreException("Error while parsing the semester", e);
		}

		// ADD NEW ALGORITHMS TO THIS LIST
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
			throw new WctttCoreFatalException("Problem with input reader", e);
		}

		if (generatedTimetable != null) {
			System.out.println("A feasible timetable was found");
			try {
				generatedTimetable.setName("wcttt-core-default-id");
				semester.addTimetable(generatedTimetable);
				setNextTimetableName(semester, generatedTimetable);
				binder.write(semester);
			} catch (WctttModelException e) {
				throw new WctttCoreException("Generated timetable was " +
						"invalid, there is a bug in the algorithm", e);
			} catch (WctttBinderException e) {
				throw new WctttCoreException("Error while writing the semester",
						e);
			}
		} else {
			System.out.println("No feasible timetable was found");
		}
	}

	private static void printHelp() {
		System.out.println("Usage: wcttt-core [SEMESTER]");
		System.out.println("Example: wcttt-core ws1819/semester.xml");
		System.out.println();
		System.out.println("Options:");
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
				throw new WctttCoreFatalException("End of input stream reached");
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
			} catch (WctttCoreException e) {
				throw new WctttCoreFatalException("Implementation error in " +
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
			} catch (WctttCoreException e) {
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
			throws WctttCoreException {
		AtomicBoolean finished = new AtomicBoolean(false);
		Thread thread = listenForAbort(selectedAlgorithm, finished, inputReader);
		System.out.println("Enter 'q' to exit the algorithm");
		Timetable timetable;
		try {
			timetable = selectedAlgorithm.generate();
		} catch (WctttCoreException e) {
			throw new WctttCoreException("A problem occurred while running " +
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
