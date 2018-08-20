package de.nicolasgross.wcttt.core.algorithms;

import de.nicolasgross.wcttt.core.WctttCoreException;
import de.nicolasgross.wcttt.lib.model.Timetable;

import java.util.List;

public interface Algorithm {

	String getName();

	List<ParameterDefinition> getParameters();

	void setParameterValues(List<ParameterValue> parameterValues)
			throws WctttCoreException;

	Timetable createTimetable() throws WctttCoreException;

	void cancelTimetableCreation();
}
