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

public class ParameterDefinition {

	private final String name;
	private final ParameterType type;

	public ParameterDefinition(String name, ParameterType type) {
		if (name == null) {
			throw new IllegalArgumentException("Parameter 'name' must not be " +
					"null");
		}
		this.name = name;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public ParameterType getType() {
		return type;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ParameterDefinition)) {
			return false;
		} else if (obj == this) {
			return true;
		}

		ParameterDefinition other = (ParameterDefinition) obj;
		return this.name.equals(other.name) && this.type == other.type;
	}

	@Override
	public String toString() {
		return name + " [" + type.name() + "]";
	}
}
