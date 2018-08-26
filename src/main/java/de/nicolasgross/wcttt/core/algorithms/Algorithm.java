/*
 * WCT³ (WIAI Course Timetabling Tool) is a software that strives to automate
 * the timetabling process at the WIAI faculty of the University of Bamberg.
 *
 * WCT³ Core comprises the implementations of the algorithms as well as a
 * command line interface to be able to run them without using a GUI.
 *
 * Copyright (C) 2018 Nicolas Gross
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

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
