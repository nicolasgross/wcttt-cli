package de.nicolasgross.wcttt.core;

import de.nicolasgross.wcttt.core.de.nicolasgross.wcttt.core.algorithms.Algorithm;
import de.nicolasgross.wcttt.core.de.nicolasgross.wcttt.core.algorithms.de.nicolasgross.wcttt.core.algorithms.tabu_based_memetic.TabuBasedMemeticApproach;
import de.nicolasgross.wcttt.lib.binder.WctttBinder;
import de.nicolasgross.wcttt.lib.binder.WctttBinderException;
import de.nicolasgross.wcttt.lib.model.Semester;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {

	public static void main(String args[]) throws WctttCoreException {
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

		boolean foundFeasibleTimetable;
		try (BufferedReader inputReader =
                new BufferedReader(new InputStreamReader(System.in))) {
			Algorithm selectedAlgorithm =
					showAlgorithmSelection(algorithms, inputReader);
			selectedAlgorithm.readParameters(inputReader);
			foundFeasibleTimetable =
					runAlgorithm(selectedAlgorithm, inputReader);
		} catch (IOException e) {
			throw new WctttCoreFatalException("Problem with input reader", e);
		}

		if (foundFeasibleTimetable) {
			System.out.println("A feasible timetable was found and is added " +
					"to the semester");
			try {
				binder.write(semester);
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

	private static boolean runAlgorithm(Algorithm selectedAlgorithm,
	                                    BufferedReader inputReader) {
		AtomicBoolean finished = new AtomicBoolean(false);
		Thread thread = listenForAbort(selectedAlgorithm, finished, inputReader);
		System.out.println("Enter 'q' to exit the algorithm");
		boolean foundFeasibleTimetable = selectedAlgorithm.createTimetable();
		finished.set(true);
		long start = System.currentTimeMillis();
		long remainingMillis;
		while ((remainingMillis = System.currentTimeMillis() - start) < 3000) {
			try {
				thread.join(remainingMillis);
				break;
			} catch (InterruptedException e) {
				// ignore
			}
		}
		return foundFeasibleTimetable;
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
							selectedAlgorithm.cancelTimetableCreation();
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
}
