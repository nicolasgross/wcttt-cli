package de.nicolasgross.wcttt.core.de.nicolasgross.wcttt.core.algorithms;

import de.nicolasgross.wcttt.core.WctttCoreException;

import java.util.List;

public interface Algorithm {

	String getName();

	List<ParameterDefinition> getParameters();

	void setParameterValues(List<ParameterValue> parameterValues)
			throws WctttCoreException;

	boolean createTimetable();

	void cancelTimetableCreation();
}
