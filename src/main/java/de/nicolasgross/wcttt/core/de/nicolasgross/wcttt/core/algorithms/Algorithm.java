package de.nicolasgross.wcttt.core.de.nicolasgross.wcttt.core.algorithms;

import java.io.BufferedReader;
import java.io.IOException;

public interface Algorithm {

	String getName();

	void readParameters(BufferedReader inputReader) throws IOException;

	boolean createTimetable();

	void cancelTimetableCreation();
}
