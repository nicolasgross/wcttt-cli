package de.nicolasgross.wcttt.core.de.nicolasgross.wcttt.core.algorithms;

public interface Algorithm {

	String getName();

	void readParameters();

	boolean createTimetable();

	void cancelTimetableCreation();
}
