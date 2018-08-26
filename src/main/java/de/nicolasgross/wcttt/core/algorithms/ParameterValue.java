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

public class ParameterValue<T> {

	private final ParameterDefinition definition;
	private final T value;

	public ParameterValue(ParameterDefinition definition, T value) {
		if (definition == null || value == null) {
			throw new IllegalArgumentException("Parameters 'definition' and " +
					"'value' must not be null");
		}
		this.definition = definition;
		this.value = value;
	}

	public ParameterDefinition getDefinition() {
		return definition;
	}

	public T getValue() {
		return value;
	}
}
