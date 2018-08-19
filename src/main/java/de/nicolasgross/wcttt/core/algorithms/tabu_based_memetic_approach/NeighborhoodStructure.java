package de.nicolasgross.wcttt.core.algorithms.tabu_based_memetic_approach;

import de.nicolasgross.wcttt.lib.model.Timetable;

public interface NeighborhoodStructure {

	void apply(Timetable timetable);
}
