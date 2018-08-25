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
		// TODO double sessions
		// TODO pre-assignments
		TimetablePeriod[] randomPeriods = selectRandomPeriods(timetable);

		TimetableDay dayA =
				timetable.getDays().get(randomPeriods[0].getDay() - 1);
		TimetableDay dayB =
				timetable.getDays().get(randomPeriods[1].getDay() - 1);

		// Remove the periods:
		dayA.removePeriod(randomPeriods[0]);
		dayB.removePeriod(randomPeriods[1]);
		// TODO handle double session

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
			// TODO handle double sessions
		} catch (WctttModelException e) {
			throw new WctttCoreFatalException("Implementation error", e);
		}
	}

	private TimetablePeriod[] selectRandomPeriods(Timetable timetable) {
		Random random = new Random();
		TimetablePeriod periodA;
		TimetablePeriod periodB;
		TimetableDay dayA;
		TimetableDay dayB;

		do {
			dayA = timetable.getDays().get(
					random.nextInt(timetable.getDays().size()));
			dayB = timetable.getDays().get(
					random.nextInt(timetable.getDays().size()));
			periodA = dayA.getPeriods().get(
					random.nextInt(dayA.getPeriods().size()));
			periodB = dayB.getPeriods().get(
					random.nextInt(dayB.getPeriods().size()));
		} while (periodA == periodB || (dayA != dayB &&
				(twoCourseLecturesInDay(dayA, periodB, periodA) ||
						twoCourseLecturesInDay(dayB, periodA, periodB))));

		return new TimetablePeriod[]{periodA, periodB};
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
