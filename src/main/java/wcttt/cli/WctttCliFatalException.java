/*
 * WCT³ (WIAI Course Timetabling Tool) is a software that strives to automate
 * the timetabling process at the WIAI faculty of the University of Bamberg.
 *
 * WCT³-CLI comprises a command line interface to be able to run the algorithms
 * without using a GUI.
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

package wcttt.cli;

/**
 * Thrown if an error occurs in the CLI that is caused by a faulty
 * implementation.
 */
public class WctttCliFatalException extends RuntimeException {

	public WctttCliFatalException() {
		super();
	}

	public WctttCliFatalException(String message) {
		super(message);
	}

	public WctttCliFatalException(String message, Throwable cause) {
		super(message, cause);
	}

	public WctttCliFatalException(Throwable cause) {
		super(cause);
	}

	protected WctttCliFatalException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
