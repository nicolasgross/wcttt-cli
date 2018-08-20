package de.nicolasgross.wcttt.core.algorithms.tabu_based_memetic_approach;

import de.nicolasgross.wcttt.core.WctttCoreException;
import de.nicolasgross.wcttt.core.WctttCoreFatalException;
import de.nicolasgross.wcttt.lib.model.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

class SaturationDegreeHeuristic {

	private Semester semester;

	SaturationDegreeHeuristic(Semester semester) {
		this.semester = semester;
	}

	List<Timetable> generateFeasibleSolutions(int count) throws WctttCoreException {
		List<Session> allSessions = new LinkedList<>();
		for (Course course : semester.getCourses()) {
			allSessions.addAll(course.getLectures());
			allSessions.addAll(course.getPracticals());
		}

		List<Timetable> generatedTimetables = new LinkedList<>();

		for (int i = 0; i < count; i++) {
			List<Session> remainingSessions = new LinkedList<>(allSessions);
			Timetable timetable = new Timetable();
			addTimetablePeriods(timetable);
			addFixedPreAssignments(timetable, remainingSessions);

			// TODO

			generatedTimetables.add(timetable);
		}

		return generatedTimetables;
	}

	private void addTimetablePeriods(Timetable timetable) {
		try {
			for (int i = 1; i <= semester.getDaysPerWeek(); i++) {
				TimetableDay day = new TimetableDay(i);
				for (int j = 1; j <= semester.getTimeSlotsPerDay(); j++) {
					TimetablePeriod period = new TimetablePeriod(i, j);
					day.addPeriod(period);
				}
				timetable.addDay(day);
			}
		} catch (WctttModelException e) {
			throw new WctttCoreFatalException("Implementation error, a " +
					"timetable day or period was created with illegal " +
					"parameters", e);
		}
	}

	private void addFixedPreAssignments(Timetable timetable,
	                                    List<Session> remainingSessions)
			throws WctttCoreException {
		List<Session> removeBecauseAssigned = new LinkedList<>();
		for (Session session : remainingSessions) {
			if (session instanceof ExternalSession) {
				// Pre-assignment must be present because it is an external
				// session
				addTimetableAssignment(session.getPreAssignment().get().getDay(),
						session.getPreAssignment().get().getTimeSlot(), session,
						((ExternalSession) session).getRoom(), timetable,
						remainingSessions);
				removeBecauseAssigned.add(session);
			} else if (session.getPreAssignment().isPresent()) {
				TimetableAssignment assignment = new TimetableAssignment();
				assignment.setSession(session);
				InternalRoom randomRoom = selectRandomSuitableRoom(
						(InternalSession) session,
						session.getPreAssignment().get(), timetable);
				addTimetableAssignment(session.getPreAssignment().get().getDay(),
						session.getPreAssignment().get().getTimeSlot(),
						session, randomRoom, timetable, remainingSessions);
				removeBecauseAssigned.add(session);
			}
		}
		remainingSessions.removeAll(removeBecauseAssigned);
	}

	private void addTimetableAssignment(int day, int timeSlot, Session session,
	                                    Room room, Timetable timetable,
	                                    List<Session> remainingSessions) {
		TimetableAssignment firstSession = new TimetableAssignment();
		firstSession.setSession(session);
		firstSession.setRoom(room);
		TimetableAssignment secondSession = null;
		if (session.isDoubleSession()) {
			secondSession = new TimetableAssignment();
			secondSession.setSession(session);
			secondSession.setRoom(room);
		}
		try {
			timetable.getDays().get(day - 1).getPeriods().get(timeSlot - 1).
					addAssignment(firstSession);
			if (session.isDoubleSession()) {
				timetable.getDays().get(day - 1).getPeriods().get(timeSlot).
						addAssignment(secondSession);
			}
		} catch (WctttModelException e) {
			throw new WctttCoreFatalException("Implementation error, " +
					"assignment could not be added to the period", e);
		}
	}

	private InternalRoom selectRandomSuitableRoom(InternalSession session,
	                                              Period period,
	                                              Timetable timetable)
			throws WctttCoreException {
		List<InternalRoom> allRooms =
				new LinkedList<>(semester.getInternalRooms());
		TimetablePeriod timetablePeriod = timetable.getDays().get(
				period.getDay() - 1).getPeriods().get(period.getTimeSlot() - 1);

		for (TimetableAssignment assgmt : timetablePeriod.getAssignments()) {
			if (assgmt.getRoom() instanceof InternalRoom) {
				allRooms.remove(assgmt.getRoom());
			}
		}
		List<InternalRoom> suitableRooms = new LinkedList<>();
		for (InternalRoom roomCandidate : allRooms) {
			if (roomCandidate.getFeatures().compareTo(
					session.getRoomRequirements()) >= 0) {
				suitableRooms.add(roomCandidate);
			}
		}

		if (suitableRooms.isEmpty()) {
			throw new WctttCoreException("No suitable room was found");
		} else {
			return suitableRooms.get(new Random().nextInt(suitableRooms.size()));
		}
	}
}
