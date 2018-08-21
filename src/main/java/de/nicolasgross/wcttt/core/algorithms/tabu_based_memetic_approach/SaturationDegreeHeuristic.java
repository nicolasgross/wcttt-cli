package de.nicolasgross.wcttt.core.algorithms.tabu_based_memetic_approach;

import de.nicolasgross.wcttt.core.WctttCoreException;
import de.nicolasgross.wcttt.core.WctttCoreFatalException;
import de.nicolasgross.wcttt.lib.model.*;
import de.nicolasgross.wcttt.lib.util.ConflictMatrixCalculator;
import de.nicolasgross.wcttt.lib.util.SessionRoomConflict;
import de.nicolasgross.wcttt.lib.util.SessionSessionConflict;
import de.nicolasgross.wcttt.lib.util.TeacherPeriodConflict;

import java.util.*;

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

		ConflictMatrixCalculator matrixCalculator =
				new ConflictMatrixCalculator(semester);
		Map<Session, Map<Session, SessionSessionConflict>> sessionSessionConflicts =
				matrixCalculator.calcSessionSessionConflicts();
		Map<InternalSession, Map<InternalRoom, SessionRoomConflict>> sessionRoomConflicts =
				matrixCalculator.calcSessionRoomConflicts();
		Map<Teacher, Map<Period, TeacherPeriodConflict>> teacherPeriodConflicts =
				matrixCalculator.calcTeacherPeriodConflicts();

		sortSessionsByMaxConflicts(allSessions, sessionSessionConflicts,
				sessionRoomConflicts, teacherPeriodConflicts);

		List<Timetable> generatedTimetables = new LinkedList<>();

		for (int i = 0; i < count; i++) {
			List<Session> remainingSessions = new LinkedList<>(allSessions);
			Timetable timetable = new Timetable();
			addTimetablePeriods(timetable);
			addFixedPreAssignments(timetable, remainingSessions);



			generatedTimetables.add(timetable);
		}

		return generatedTimetables;
	}

	private void sortSessionsByMaxConflicts(List<Session> allSessions,
			Map<Session, Map<Session, SessionSessionConflict>> sessionSessionConflicts,
			Map<InternalSession, Map<InternalRoom, SessionRoomConflict>> sessionRoomConflicts,
			Map<Teacher, Map<Period, TeacherPeriodConflict>> teacherPeriodConflicts) {
			Map<Session, Integer>  conflictNumbers = new HashMap<>();
			for (Session session : allSessions) {
				conflictNumbers.put(session, calcConflictsForSession(
						session, sessionSessionConflicts, sessionRoomConflicts,
						teacherPeriodConflicts));
			}
			allSessions.sort(Comparator.comparingInt(conflictNumbers::get));
	}

	private int calcConflictsForSession(Session session,
			Map<Session, Map<Session, SessionSessionConflict>> sessionSessionConflicts,
			Map<InternalSession, Map<InternalRoom, SessionRoomConflict>> sessionRoomConflicts,
			Map<Teacher, Map<Period, TeacherPeriodConflict>> teacherPeriodConflicts) {
		int counter = 0;
		Map<Session, SessionSessionConflict> sessionConflicts =
				sessionSessionConflicts.get(session);
		for (SessionSessionConflict conflict : sessionConflicts.values()) {
			if (conflict != null) {
				counter += conflict.getCurricula().size();
				if (conflict.isSessionConflict()) {
					counter++;
				}
				if (conflict.isTeacherConflict()) {
					counter++;
				}
			}
		}
		if (session instanceof InternalSession) {
			Map<InternalRoom, SessionRoomConflict> roomConflicts =
					sessionRoomConflicts.get(session);
			for (SessionRoomConflict conflict : roomConflicts.values()) {
				if (!conflict.fullfillsFeatures()) {
					counter++;
				}
			}
		}
		Map<Period, TeacherPeriodConflict> periodConflicts =
				teacherPeriodConflicts.get(session.getTeacher());
		for (TeacherPeriodConflict conflict : periodConflicts.values()) {
			if (conflict.isUnavailable()) {
				counter++;
			}
		}
		return counter;
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
						((ExternalSession) session).getRoom(), timetable
				);
				removeBecauseAssigned.add(session);
			} else if (session.getPreAssignment().isPresent()) {
				TimetableAssignment assignment = new TimetableAssignment();
				assignment.setSession(session);
				InternalRoom randomRoom = selectRandomSuitableRoom(
						(InternalSession) session,
						session.getPreAssignment().get(), timetable);
				addTimetableAssignment(session.getPreAssignment().get().getDay(),
						session.getPreAssignment().get().getTimeSlot(),
						session, randomRoom, timetable);
				removeBecauseAssigned.add(session);
			}
		}
		remainingSessions.removeAll(removeBecauseAssigned);
	}

	private void addTimetableAssignment(int day, int timeSlot, Session session,
	                                    Room room, Timetable timetable) {
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
