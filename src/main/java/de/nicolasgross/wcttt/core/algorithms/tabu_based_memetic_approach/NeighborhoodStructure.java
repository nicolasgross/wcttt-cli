package de.nicolasgross.wcttt.core.algorithms.tabu_based_memetic_approach;

import de.nicolasgross.wcttt.core.WctttCoreException;
import de.nicolasgross.wcttt.lib.model.Semester;
import de.nicolasgross.wcttt.lib.model.Timetable;

public interface NeighborhoodStructure {

	void apply(Timetable timetable, Semester semester)
			throws WctttCoreException;
}
