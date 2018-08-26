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

package de.nicolasgross.wcttt.core.algorithms.tabu_based_memetic_approach;

import de.nicolasgross.wcttt.core.WctttCoreFatalException;
import de.nicolasgross.wcttt.lib.model.*;

import java.util.Random;

public class NeighborhoodStructure3 implements NeighborhoodStructure {

	/**
	 * Select two timeslots at random and simply swap all the lectures in one
	 * timeslot with all the lectures in the other timeslot.
	 *
	 * @param timetable the timetable to which the neighborhood structure should
	 *                  be applied.
	 * @param semester the semester the timetable belongs to.
	 */
	@Override
	public void apply(Timetable timetable, Semester semester) {
		TimetablePeriod[] randomPeriods = selectSuitablePeriods(timetable);

		if (randomPeriods == null) {
			// Could not find a suitable pair of periods, do nothing
			return;
		}

		TimetableDay dayA =
				timetable.getDays().get(randomPeriods[0].getDay() - 1);
		TimetableDay dayB =
				timetable.getDays().get(randomPeriods[1].getDay() - 1);

		// Remove the periods:
		dayA.removePeriod(randomPeriods[0]);
		dayB.removePeriod(randomPeriods[1]);

		// Switch periods:
		int tmpDay = randomPeriods[0].getDay();
		int tmpSlot = randomPeriods[0].getTimeSlot();
		try {
			randomPeriods[0].setDay(randomPeriods[1].getDay());
			randomPeriods[0].setTimeSlot(randomPeriods[1].getTimeSlot());
			randomPeriods[1].setDay(tmpDay);
			randomPeriods[1].setTimeSlot(tmpSlot);

			// Add periods again:
			dayA.addPeriod(randomPeriods[1]);
			dayB.addPeriod(randomPeriods[0]);
		} catch (WctttModelException e) {
			throw new WctttCoreFatalException("Implementation error", e);
		}
	}

	/**
	 * Selects two periods that can be interchanged. These periods must not
	 * contain pre-assignments or double sessions. The method tries up to 99
	 * random combinations of periods.
	 *
	 * @param timetable the timetable from which two periods should be selected.
	 * @return an array containing two timetable periods if a suitable pair was
	 * found, otherwise {@code null}.
	 */
	private TimetablePeriod[] selectSuitablePeriods(Timetable timetable) {
		Random random = new Random();
		TimetablePeriod periodA;
		TimetablePeriod periodB;
		TimetableDay dayA;
		TimetableDay dayB;

		int counter = 0;
		do {
			dayA = timetable.getDays().get(
					random.nextInt(timetable.getDays().size()));
			dayB = timetable.getDays().get(
					random.nextInt(timetable.getDays().size()));
			periodA = dayA.getPeriods().get(
					random.nextInt(dayA.getPeriods().size()));
			periodB = dayB.getPeriods().get(
					random.nextInt(dayB.getPeriods().size()));
			counter++;
		} while (counter < 100 && periodA == periodB || (dayA != dayB &&
				(twoCourseLecturesInDay(dayA, periodB, periodA) ||
						twoCourseLecturesInDay(dayB, periodA, periodB))) ||
				containsPreAssignmentOrDoubleSession(periodA) ||
				containsPreAssignmentOrDoubleSession(periodB) );

		if (counter == 100) {
			// No suitable pair of periods could be found, probably too many
			// double sessions and pre-assignments
			return null;
		} else {
			return new TimetablePeriod[]{periodA, periodB};
		}
	}

	private boolean containsPreAssignmentOrDoubleSession(TimetablePeriod period) {
		for (TimetableAssignment assgmt : period.getAssignments()) {
			if (assgmt.getSession().getPreAssignment().isPresent() ||
					assgmt.getSession().isDoubleSession()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks for the period that should be switched over whether a second
	 * lecture of the same course is introduced in the day.
	 *
	 * @param day the day that is checked.
	 * @param copyFromOtherDay the period that is copied over to the day.
	 * @param removeFromDay the period that is removed from the day.
	 * @return {@code true} if a conflict was detected, otherwise {@code false}.
	 */
	private boolean twoCourseLecturesInDay(TimetableDay day,
	                                       TimetablePeriod copyFromOtherDay,
	                                       TimetablePeriod removeFromDay) {
		for (TimetablePeriod period : day.getPeriods()) {
			for (TimetableAssignment assignment : period.getAssignments()) {
				for (TimetableAssignment newAssgmt :
						copyFromOtherDay.getAssignments()) {
					if (period == removeFromDay &&
							!newAssgmt.getSession().getPreAssignment().isPresent()) {
						// Conflicts with assignment that is gonna be removed
						// are irrelevant
						continue;
					} else if (assignment.getSession().isLecture() &&
							newAssgmt.getSession().isLecture() &&
							assignment.getSession().getCourse().equals(
									newAssgmt.getSession().getCourse())) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
