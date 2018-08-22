package de.nicolasgross.wcttt.core.algorithms.tabu_based_memetic_approach;

import de.nicolasgross.wcttt.core.WctttCoreException;
import de.nicolasgross.wcttt.core.WctttCoreFatalException;
import de.nicolasgross.wcttt.lib.model.*;
import de.nicolasgross.wcttt.lib.util.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * Implementation of the saturation degree heuristic, which was proposed by
 * Daniel Brélaz in 'New methods to color the vertices of a graph', 1979.
 *
 * Session == Vertex
 * Period == Color
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

	List<Timetable> generateFeasibleSolutions(int count, AtomicBoolean isCancelled)
			throws WctttCoreException {
		List<InternalSession> internalSessions = new LinkedList<>();
		List<ExternalSession> externalSessions = new LinkedList<>();
		fillSessionLists(internalSessions, externalSessions);
		List<Period> periods = createPeriodList();

		List<Timetable> generatedTimetables = new LinkedList<>();

		for (int i = 0; i < count && !isCancelled.get(); i++) {
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

			addPreAssignments(externalSessions, unassignedSessions, timetable,
					periodUsages, unassignedPeriods, assignmentMap);

			boolean couldFindAssignment = true;
			while (couldFindAssignment && !unassignedSessions.isEmpty() &&
					!isCancelled.get()) {
				InternalSession next;
				List<InternalSession> maxSatDegrees =
						maxSaturationDegrees(unassignedSessions, assignmentMap);
				if (maxSatDegrees.size() > 1) {
					next = maxConflictedSession(maxSatDegrees);
				} else {
					next = maxSatDegrees.get(0);
				}

				couldFindAssignment = assignSessionRandomly(next, timetable,
						unassignedSessions, periodUsages, unassignedPeriods,
						assignmentMap);
			}

			if (couldFindAssignment) {
				generatedTimetables.add(timetable);
			} else {
				i--;
			}
		}

		return generatedTimetables;
	}

	private void fillSessionLists(List<InternalSession> internalSessions,
	                              List<ExternalSession> externalSessions) {
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
			List<ExternalSession> externalSessions,
			List<InternalSession> unassignedSessions, Timetable timetable,
			Map<Period, Integer> periodUsages,
			Map<InternalRoom, List<Period>> unassignedPeriods,
			Map<Session, TimetableAssignment> assignmentMap) throws WctttCoreException {
		for (ExternalSession session : externalSessions) {
			// Pre-assignment must be present because it is an external
			// session
			Period period = session.getPreAssignment().get();
			assignSession(session, period, session.getRoom(), timetable,
					periodUsages, unassignedPeriods, assignmentMap);
		}
		List<InternalSession> removeList = new LinkedList<>();
		for (InternalSession session : unassignedSessions) {
			if (session.getPreAssignment().isPresent()) {
				TimetableAssignment assignment = new TimetableAssignment();
				assignment.setSession(session);
				Period period = session.getPreAssignment().get();
				InternalRoom randomRoom = selectRandomSuitableRoom(session,
						period, unassignedPeriods);
				assignSession(session, period, randomRoom, timetable,
						periodUsages, unassignedPeriods, assignmentMap);
				removeList.add(session);
			}
		}
		unassignedSessions.removeAll(removeList);
	}

	private void assignSession(Session session, Period period, Room room,
	                           Timetable timetable, Map<Period, Integer> periodUsages,
	                           Map<InternalRoom, List<Period>> unassignedPeriods,
	                           Map<Session, TimetableAssignment> assignmentMap)
			throws WctttCoreException {
		ConstraintViolationsCalculator constraintCalc =
				new ConstraintViolationsCalculator(semester);
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
			TimetablePeriod firstTimetablePeriod = timetable.getDays().get(
					period.getDay() - 1).getPeriods().get(period.getTimeSlot() - 1);
			List<ConstraintType> hardConstraintViolations = constraintCalc.
					calcAssignmentHardViolations(firstTimetablePeriod, firstSession);
			if (!hardConstraintViolations.isEmpty()) {
				throw new WctttCoreException("Assignment of session '" +
						session + "' to period '" + period + "' and room '" +
						room + "' violates the following hard constraints: " +
						hardConstraintViolations);
			}
			firstTimetablePeriod.addAssignment(firstSession);
			periodUsages.put(period, periodUsages.get(period) + 1);
			if (room instanceof InternalRoom) {
				unassignedPeriods.get(room).remove(period);
			}
			assignmentMap.put(session, firstSession);
			if (session.isDoubleSession()) {
				TimetablePeriod secondTimetablePeriod = timetable.getDays().get(
						period.getDay() - 1).getPeriods().get(period.getTimeSlot());
				hardConstraintViolations = constraintCalc.calcAssignmentHardViolations(
						secondTimetablePeriod, secondSession);
				if (!hardConstraintViolations.isEmpty()) {
					throw new WctttCoreException("Assignment of session '" +
							session + "' to period '" + period + "' and " +
							"room '" + room + "' violates the following hard " +
							"constraints: " + hardConstraintViolations);
				}
				secondTimetablePeriod.addAssignment(secondSession);
				Period secondPeriod =
						new Period(period.getDay(), period.getTimeSlot() + 1);
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

	private List<InternalRoom> findSuitableRooms(InternalSession session)
			throws WctttCoreException {
		List<InternalRoom> suitableRooms = new LinkedList<>();
		semester.getInternalRooms().forEach(room -> {
			if (room.getFeatures().compareTo(session.getRoomRequirements()) >= 0) {
				suitableRooms.add(room);
			}
		});

		if (suitableRooms.isEmpty()) {
			throw new WctttCoreException("No suitable room was found for " +
					"session '" + session + "'");
		} else {
			return suitableRooms;
		}
	}

	private InternalRoom selectRandomSuitableRoom(InternalSession session, Period period,
	                                              Map<InternalRoom, List<Period>> unassignedPeriods)
			throws WctttCoreException {
		List<InternalRoom> suitableRooms = new LinkedList<>();
		findSuitableRooms(session).forEach(room -> {
			if (unassignedPeriods.get(room).contains(period)) {
				suitableRooms.add(room);
			}
		});

		if (suitableRooms.isEmpty()) {
			throw new WctttCoreException("No suitable room was found for " +
					"session '" + session + "' in period '" + period + "'");
		} else {
			return suitableRooms.get(new Random().nextInt(suitableRooms.size()));
		}
	}

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

	private boolean assignSessionRandomly(InternalSession session, Timetable timetable,
	                                      List<InternalSession> unassignedSessions,
	                                      Map<Period, Integer> periodUsages,
	                                      Map<InternalRoom, List<Period>> unassignedPeriods,
	                                      Map<Session, TimetableAssignment> assignmentMap)
			throws WctttCoreException {
		List<Period> orderedPeriods = getPeriodsOrderedByLowestNumber(periodUsages);
		List<InternalRoom> suitableRooms = findSuitableRooms(session);
		Collections.shuffle(suitableRooms);

		for (Period period : orderedPeriods) {
			for (InternalRoom room : suitableRooms) {
				if (unassignedPeriods.get(room).contains(period)) {
					try {
						assignSession(session, period, room, timetable,
								periodUsages, unassignedPeriods, assignmentMap);
						unassignedSessions.remove(session);
						return true;
					} catch (WctttCoreException e) {
						// ignore hard constraint violation and search on
					}
				}
			}
		}
		return false;
	}

	/**
	 * Generates a list of all periods, ordered by their number of usages for
	 * assignments from low to high, except for usages of 0, which resembles the
	 * highest number.
	 *
	 * @param periodUsages a mapping of periods to their number of usages.
	 * @return the ordered list of periods.
	 */
	private List<Period> getPeriodsOrderedByLowestNumber(Map<Period, Integer> periodUsages) {
		LinkedList<Period> alreadyUsedPeriods = new LinkedList<>();
		LinkedList<Period> unusedPeriods = new LinkedList<>();
		periodUsages.forEach((key, value) -> {
			if (value > 0) {
				alreadyUsedPeriods.add(key);
			} else {
				unusedPeriods.add(key);
			}
		});
		Collections.shuffle(alreadyUsedPeriods);
		Collections.shuffle(unusedPeriods);

		List<Period> orderedList = new LinkedList<>(alreadyUsedPeriods);
		orderedList.sort((o1, o2) -> {
			int o1Usages = periodUsages.get(o1);
			int o2Usages = periodUsages.get(o2);
			if (o1Usages == o2Usages) {
				return 0;
			} else if (o1Usages == 0 || o1Usages > o2Usages) {
				return 1;
			} else {
				return -1;
			}
		});

		Collections.shuffle(unusedPeriods);
		orderedList.addAll(unusedPeriods);

		return orderedList;
	}
}
