package de.nicolasgross.wcttt.core.de.nicolasgross.wcttt.core.algorithms.de.nicolasgross.wcttt.core.algorithms.tabu_based_memetic;

import de.nicolasgross.wcttt.core.de.nicolasgross.wcttt.core.algorithms.AbstractAlgorithm;
import de.nicolasgross.wcttt.lib.model.Semester;

public class TabuBasedMemeticApproach extends AbstractAlgorithm {

	public static final String NAME = "Tabu-based memetic approach";

	public TabuBasedMemeticApproach(Semester semester) {
		super(semester);
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public void readParameters() {
		// TODO
	}

	@Override
	protected boolean runAlgorithm() {
		while (!isCancelled.get()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// ignore
			}
		}
		return false;
	}
}
