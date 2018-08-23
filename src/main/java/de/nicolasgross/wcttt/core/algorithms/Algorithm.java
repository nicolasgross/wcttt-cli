package de.nicolasgross.wcttt.core.algorithms;

import de.nicolasgross.wcttt.core.WctttCoreException;
import de.nicolasgross.wcttt.lib.model.Timetable;

import java.util.List;

/**
 * Defines the interface of an algorithm for usage with WCTTT.
 */
public interface Algorithm {

	/**
	 * Getter for the name of an algorithm.
	 *
	 * @return the name of the algorithm.
	 */
	String getName();

	/**
	 * Getter for the parameters of an algorithm.
	 *
	 * @return the list of parameters defined for the algorithm.
	 */
	List<ParameterDefinition> getParameters();

	/**
	 * Setter for the parameter values.
	 *
	 * @param parameterValues A list containing all parameter values.
	 * @throws WctttCoreException if a parameter was invalid.
	 */
	void setParameterValues(List<ParameterValue> parameterValues)
			throws WctttCoreException;

	/**
	 * Generates a new Timetable. If the process is cancelled, the best solution
	 * found so far should be returned.
	 *
	 * @return a new feasible timetable or {@code null} if no feasible timetable
	 * was found.
	 * @throws WctttCoreException if an error occurred, e.g. unrealizable room
	 * requirements were found.
	 */
	Timetable generate() throws WctttCoreException;

	/**
	 * Cancels the algorithm.
	 */
	void cancel();
}
