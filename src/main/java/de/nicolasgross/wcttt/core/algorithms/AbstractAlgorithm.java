package de.nicolasgross.wcttt.core.algorithms;

import de.nicolasgross.wcttt.core.WctttCoreException;
import de.nicolasgross.wcttt.lib.model.Semester;
import de.nicolasgross.wcttt.lib.model.Timetable;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Abstract class that can be used as a starting point to implement an algorithm.
 * It already provides basic functionality for the cancellation of the algorithm.
 */
public abstract class AbstractAlgorithm implements Algorithm {

	private final Semester semester;
	private final AtomicBoolean isCancelled = new AtomicBoolean(false);

	/**
	 * Initializion of the class.
	 *
	 * @param semester the semester that should be used to create a new timetable.
	 */
	public AbstractAlgorithm(Semester semester) {
		if (semester == null) {
			throw new IllegalArgumentException("Parameter 'semester' must not" +
					" be null");
		}
		this.semester = semester;
	}

	/**
	 * Getter for the assigned semester.
	 *
	 * @return the semester that is used to create new timetables.
	 */
	protected Semester getSemester() {
		return semester;
	}

	/**
	 * Runs the actual algorithm.
	 *
	 * @param isCancelled indicates whether the algorithm was cancelled and
	 *                       should periodically be checked by the algorithm.
	 * @return the newly created timetable.
	 * @throws WctttCoreException if an error occurred in the algorithm.
	 */
	protected abstract Timetable runAlgorithm(AtomicBoolean isCancelled)
			throws WctttCoreException;

	@Override
	public Timetable generate() throws WctttCoreException {
		isCancelled.set(false);
		return runAlgorithm(isCancelled);
	}

	@Override
	public void cancel() {
		isCancelled.set(true);
	}

	@Override
	public String toString() {
		return getName();
	}
}
