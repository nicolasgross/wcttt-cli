package de.nicolasgross.wcttt.core.algorithms;

import de.nicolasgross.wcttt.lib.model.Semester;
import de.nicolasgross.wcttt.lib.model.Timetable;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractAlgorithm implements Algorithm {

	protected final Semester semester;
	protected final AtomicBoolean isCancelled = new AtomicBoolean(false);

	public AbstractAlgorithm(Semester semester) {
		if (semester == null) {
			throw new IllegalArgumentException("Parameter 'semester' must not" +
					" be null");
		}
		this.semester = semester;
	}

	// don't use system.in
	protected abstract Timetable runAlgorithm();

	@Override
	public Timetable createTimetable() {
		isCancelled.set(false);
		return runAlgorithm();
	}

	@Override
	public void cancelTimetableCreation() {
		isCancelled.set(true);
	}

	@Override
	public String toString() {
		return getName();
	}
}
