package de.nicolasgross.wcttt.core.de.nicolasgross.wcttt.core.algorithms;

import java.io.BufferedReader;

public interface Algorithm {

	String getName();

	void readParameters(BufferedReader inputReader);

	boolean createTimetable();

	void cancelTimetableCreation();
}
