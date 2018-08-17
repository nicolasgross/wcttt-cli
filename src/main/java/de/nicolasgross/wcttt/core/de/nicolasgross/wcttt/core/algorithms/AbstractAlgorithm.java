package de.nicolasgross.wcttt.core.de.nicolasgross.wcttt.core.algorithms;

import de.nicolasgross.wcttt.lib.model.Semester;

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
	protected abstract boolean runAlgorithm();

	@Override
	public boolean createTimetable() {
		isCancelled.set(false);
		return runAlgorithm();
	}

	@Override
	public void cancelTimetableCreation() {
		isCancelled.set(true);
	}
}
