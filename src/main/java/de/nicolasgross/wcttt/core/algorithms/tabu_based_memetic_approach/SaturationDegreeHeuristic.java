package de.nicolasgross.wcttt.core.algorithms.tabu_based_memetic_approach;

import de.nicolasgross.wcttt.core.WctttCoreException;
import de.nicolasgross.wcttt.core.WctttCoreFatalException;
import de.nicolasgross.wcttt.lib.model.*;
import de.nicolasgross.wcttt.lib.util.ConflictMatrixCalculator;
import de.nicolasgross.wcttt.lib.util.SessionRoomConflict;
import de.nicolasgross.wcttt.lib.util.SessionSessionConflict;
import de.nicolasgross.wcttt.lib.util.TeacherPeriodConflict;

import java.util.*;
import java.util.stream.Stream;

/**
 *
 * Period == Color
 * Session == Vertex
 */
class SaturationDegreeHeuristic {

	private Semester semester;
	private Map<Session, Map<Session, SessionSessionConflict>> sessionSessionConflicts;
	private Map<InternalSession, Map<InternalRoom, SessionRoomConflict>> sessionRoomConflicts;
	private Map<Teacher, Map<Period, TeacherPeriodConflict>> teacherPeriodConflicts;

	SaturationDegreeHeuristic(Semester semester) {
		this.semester = semester;
		ConflictMatrixCalculator matrixCalculator =
				new ConflictMatrixCalculator(semester);
		sessionSessionConflicts = matrixCalculator.calcSessionSessionConflicts();
		sessionRoomConflicts = matrixCalculator.calcSessionRoomConflicts();
		teacherPeriodConflicts = matrixCalculator.calcTeacherPeriodConflicts();
	}

	List<Timetable> generateFeasibleSolutions(int count) throws WctttCoreException {
		List<InternalSession> internalSessions = new LinkedList<>();
		List<ExternalSession> externalSessions = new LinkedList<>();
		for (Course course : semester.getCourses()) {
			for (Session lecture : course.getLectures()) {
				if (lecture instanceof InternalSession) {
					internalSessions.add((InternalSession) lecture);
				} else {
					externalSessions.add((ExternalSession) lecture);
				}
			}
			for (Session practical : course.getPracticals()) {
				if (practical instanceof InternalSession) {
					internalSessions.add((InternalSession) practical);
				} else {
					externalSessions.add((ExternalSession) practical);
				}
			}
		}
		List<Period> periods = createPeriodList();

		List<Timetable> generatedTimetables = new LinkedList<>();

		for (int i = 0; i < count; i++) {
			List<InternalSession> unassignedSessions =
					new LinkedList<>(internalSessions);

			Map<Period, Integer> periodUsages = new HashMap<>();
			periods.forEach(period -> periodUsages.put(period, 0));

			Map<InternalRoom, List<Period>> unassignedPeriods = new HashMap<>();
			semester.getInternalRooms().forEach(
					room -> unassignedPeriods.put(room, new LinkedList<>(periods)));

			Map<Session, TimetableAssignment> assignmentMap = new HashMap<>();
			Stream.concat(internalSessions.stream(), externalSessions.stream()).
					forEach(session -> assignmentMap.put(session, null));

			Timetable timetable = new Timetable();
			addTimetablePeriods(timetable);

			addPreAssignments(timetable, externalSessions, unassignedSessions,
					periodUsages, unassignedPeriods, assignmentMap);

			while (!unassignedPeriods.isEmpty()) {
				Session next;
				List<InternalSession> maxSatDegrees =
						maxSaturationDegrees(unassignedSessions, assignmentMap);
				if (maxSatDegrees.size() > 1) {
					next = maxConflictedSession(maxSatDegrees);
				} else {
					next = maxSatDegrees.get(0);
				}

				// TODO assignSession(next, unassignedPeriods);
				// smallest color == color that was used before but the least times
			}

			generatedTimetables.add(timetable);
		}

		return generatedTimetables;
	}

	// TODO hashcode methods
	// TODO period, internalroom, session, internalsession, teacher

	private List<InternalSession> maxSaturationDegrees(
			List<InternalSession> unassignedSessions,
			Map<Session, TimetableAssignment> assignmentMap) {
		List<InternalSession> maxSatDegrees = new LinkedList<>();

		Map<InternalSession, Integer> saturationDegrees = new HashMap<>();
		int max = 0;
		for (InternalSession session : unassignedSessions) {
			int counter = 0;
			for (Map.Entry<Session, SessionSessionConflict> entry :
					sessionSessionConflicts.get(session).entrySet()) {
				if (!entry.getKey().equals(session) &&
						(!entry.getValue().getCurricula().isEmpty() ||
								entry.getValue().isSessionConflict() ||
								entry.getValue().isTeacherConflict())) {
					if (assignmentMap.get(entry.getKey()) != null) {
						counter++;
					}
				}
			}
			if (counter > max) {
				max = counter;
			}
			saturationDegrees.put(session, counter);
		}

		for (Map.Entry<InternalSession, Integer> entry : saturationDegrees.entrySet()) {
			if (entry.getValue() == max) {
				maxSatDegrees.add(entry.getKey());
			}
		}

		return maxSatDegrees;
	}

	private InternalSession maxConflictedSession(List<InternalSession> sessions) {
		InternalSession maxSession = sessions.get(0);
		int maxConflicts = calcNumberOfConflicts(maxSession);
		int tmp;
		for (InternalSession session : sessions) {
			tmp = calcNumberOfConflicts(session);
			if (tmp > maxConflicts) {
				maxConflicts = tmp;
				maxSession = session;
			}
		}
		return maxSession;
	}

	private int calcNumberOfConflicts(Session session) {
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

	private List<Period> createPeriodList() {
		List<Period> periods = new LinkedList<>();
		for (int i = 1; i <= semester.getDaysPerWeek(); i++) {
			for (int j = 1; j <= semester.getTimeSlotsPerDay(); j++) {
				Period period;
				try {
					period = new Period(i, j);
				} catch (WctttModelException e) {
					throw new WctttCoreFatalException("Implementation error, " +
							"a period was created with illegal parameters", e);
				}
				periods.add(period);
			}
		}
		return periods;
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

	private void addPreAssignments(
			Timetable timetable, List<ExternalSession> externalSessions,
			List<InternalSession> unassignedSessions, Map<Period, Integer> periodUsages,
			Map<InternalRoom, List<Period>> unassignedPeriods,
			Map<Session, TimetableAssignment> assignmentMap) throws WctttCoreException {
		for (ExternalSession session : externalSessions) {
			int day, timeSlot;
			// Pre-assignment must be present because it is an external
			// session
			day = session.getPreAssignment().get().getDay();
			timeSlot = session.getPreAssignment().get().getTimeSlot();
			addTimetableAssignment(day, timeSlot, session, session.getRoom(),
					timetable, periodUsages, unassignedPeriods, assignmentMap);
		}
		List<InternalSession> removeList = new LinkedList<>();
		for (InternalSession session : unassignedSessions) {
			if (session.getPreAssignment().isPresent()) {
				TimetableAssignment assignment = new TimetableAssignment();
				assignment.setSession(session);
				InternalRoom randomRoom = selectRandomSuitableRoom(session,
						session.getPreAssignment().get(), unassignedPeriods);
				addTimetableAssignment(session.getPreAssignment().get().getDay(),
						session.getPreAssignment().get().getTimeSlot(), session,
						randomRoom, timetable, periodUsages, unassignedPeriods,
						assignmentMap);
				removeList.add(session);
			}
		}
		unassignedSessions.removeAll(removeList);
	}

	private void addTimetableAssignment(
			int day, int timeSlot, Session session, Room room,
			Timetable timetable, Map<Period, Integer> periodUsages,
			Map<InternalRoom, List<Period>> unassignedPeriods,
			Map<Session, TimetableAssignment> assignmentMap) {
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
			Period firstPeriod = new Period(day, timeSlot);
			periodUsages.put(firstPeriod, periodUsages.get(firstPeriod) + 1);
			if (room instanceof InternalRoom) {
				unassignedPeriods.get(room).remove(firstPeriod);
			}
			assignmentMap.put(session, firstSession);
			if (session.isDoubleSession()) {
				timetable.getDays().get(day - 1).getPeriods().get(timeSlot).
						addAssignment(secondSession);
				Period secondPeriod = new Period(day, timeSlot + 1);
				periodUsages.put(secondPeriod, periodUsages.get(secondPeriod) + 1);
				if (room instanceof InternalRoom) {
					unassignedPeriods.get(room).remove(secondPeriod);
				}
				assignmentMap.put(session, secondSession);
			}
		} catch (WctttModelException e) {
			throw new WctttCoreFatalException("Implementation error, problem " +
					"while adding an assignment to the timetable", e);
		}
	}

	private InternalRoom selectRandomSuitableRoom(InternalSession session, Period period,
	                                              Map<InternalRoom, List<Period>> unassignedPeriods)
			throws WctttCoreException {
		List<InternalRoom> suitableRooms = new LinkedList<>();
		semester.getInternalRooms().forEach(room -> {
			if (unassignedPeriods.get(room).contains(period) &&
					room.getFeatures().compareTo(session.getRoomRequirements()) >= 0) {
				suitableRooms.add(room);
			}
		});

		if (suitableRooms.isEmpty()) {
			throw new WctttCoreException("No suitable room was found");
		} else {
			return suitableRooms.get(new Random().nextInt(suitableRooms.size()));
		}
	}
}
