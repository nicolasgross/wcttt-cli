package de.nicolasgross.wcttt.core.de.nicolasgross.wcttt.core.algorithms.de.nicolasgross.wcttt.core.algorithms.tabu_based_memetic;

import de.nicolasgross.wcttt.core.de.nicolasgross.wcttt.core.algorithms.AbstractAlgorithm;
import de.nicolasgross.wcttt.lib.model.Semester;

import java.io.BufferedReader;

public class TabuBasedMemeticApproach extends AbstractAlgorithm {

	private static final String NAME = "Tabu-based memetic approach";

	public TabuBasedMemeticApproach(Semester semester) {
		super(semester);
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public void readParameters(BufferedReader inputReader) {
		// TODO
	}

	@Override
	protected boolean runAlgorithm() {
		boolean foundFeasibleSolution = false;
		while (!isCancelled.get()) {
			// TODO
		}
		return foundFeasibleSolution;
	}
}
