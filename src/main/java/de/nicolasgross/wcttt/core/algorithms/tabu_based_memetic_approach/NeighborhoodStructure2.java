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

import de.nicolasgross.wcttt.core.WctttCoreException;
import de.nicolasgross.wcttt.core.WctttCoreFatalException;
import de.nicolasgross.wcttt.lib.model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class NeighborhoodStructure2 implements NeighborhoodStructure {

	/**
	 * Chooses a single lecture at random and moves it to a new random feasible
	 * timeslot.
	 *
	 * @param timetable the timetable to which the neighborhood structure should
	 *                  be applied.
	 * @param semester the semester the timetable belongs to.
	 * @throws WctttCoreException if an error occurred.
	 */
	@Override
	public void apply(Timetable timetable, Semester semester)
			throws WctttCoreException {
		TimetableAssignment randomAssgmt;
		do {
			randomAssgmt = selectRandomAssignment(timetable);
		} while (randomAssgmt.getSession().getPreAssignment().isPresent());

		removeSessionAssignments(timetable, randomAssgmt);

		// Because external sessions must have a pre-assignment:
		assert randomAssgmt.getSession() instanceof InternalSession;
		assert randomAssgmt.getRoom() instanceof InternalRoom;

		List<Period> periods = Util.createPeriodList(semester);
		Collections.shuffle(periods);

		if (!Util.assignSessionRandomly(
				(InternalSession) randomAssgmt.getSession(), timetable,
				semester, periods, null, null, null, null)) {
			throw new WctttCoreFatalException("Implementation error, could " +
					"not find suitable room and period for new assignment, at" +
					" least the period and room of the previous assignment " +
					"should have been found");
		}
	}

	private TimetableAssignment selectRandomAssignment(Timetable timetable) {
		Random random = new Random();
		TimetableDay randomDay;
		TimetablePeriod randomPeriod;
		do {
			randomDay = timetable.getDays().get(
					random.nextInt(timetable.getDays().size()));
			randomPeriod = randomDay.getPeriods().get(
					random.nextInt(randomDay.getPeriods().size()));
		} while (randomPeriod.getAssignments().isEmpty());
		return randomPeriod.getAssignments().get(
				random.nextInt(randomPeriod.getAssignments().size()));
	}

	private void removeSessionAssignments(Timetable timetable,
	                                      TimetableAssignment randomAssgmt) {
		List<TimetablePeriod> removePeriods = new ArrayList<>(2);
		List<TimetableAssignment> removeAssgmts = new ArrayList<>(2);
		int removed = 0;
		outerloop:
		for (TimetableDay day : timetable.getDays()) {
			for (TimetablePeriod period : day.getPeriods()) {
				for (TimetableAssignment assgmt : period.getAssignments()) {
					if (assgmt.getSession().equals(randomAssgmt.getSession())) {
						removePeriods.add(period);
						removeAssgmts.add(assgmt);
						removed++;
						if ((!randomAssgmt.getSession().isDoubleSession() &&
								removed == 1) ||
								(randomAssgmt.getSession().isDoubleSession() &&
										removed == 2)) {
							break outerloop;
						}
					}
				}
			}
		}
		for (int i = 0; i < removePeriods.size(); i++) {
			removePeriods.get(i).removeAssignment(removeAssgmts.get(i));
		}
	}
}
