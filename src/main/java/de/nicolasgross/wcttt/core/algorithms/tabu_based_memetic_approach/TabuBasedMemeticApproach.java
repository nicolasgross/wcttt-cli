package de.nicolasgross.wcttt.core.algorithms.tabu_based_memetic_approach;

import de.nicolasgross.wcttt.core.WctttCoreException;
import de.nicolasgross.wcttt.core.WctttCoreFatalException;
import de.nicolasgross.wcttt.core.algorithms.AbstractAlgorithm;
import de.nicolasgross.wcttt.core.algorithms.ParameterDefinition;
import de.nicolasgross.wcttt.core.algorithms.ParameterType;
import de.nicolasgross.wcttt.core.algorithms.ParameterValue;
import de.nicolasgross.wcttt.lib.model.Semester;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TabuBasedMemeticApproach extends AbstractAlgorithm {

	private static final String NAME = "Tabu-based memetic approach";
	private static final List<ParameterDefinition> PARAMETERS = Arrays.asList(
			new ParameterDefinition("Population size", ParameterType.INT),
			new ParameterDefinition("Crossover rate", ParameterType.DOUBLE),
			new ParameterDefinition("Mutation rate", ParameterType.DOUBLE),
			new ParameterDefinition("Tabu list size", ParameterType.INT));
	private static final int POPULATION_SIZE_MIN = 1;
	private static final double CROSSOVER_RATE_MIN = 0.0;
	private static final double CROSSOVER_RATE_MAX = 1.0;
	private static final double MUTATION_RATE_MIN = 0.0;
	private static final double MUTATION_RATE_MAX = 1.0;
	private static final int TABU_LIST_SIZE_MIN = 1;

	private int populationSize;
	private double crossoverRate;
	private double mutationRate;
	private int tabuListSize;

	public TabuBasedMemeticApproach(Semester semester) {
		super(semester);
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public List<ParameterDefinition> getParameters() {
		return PARAMETERS;
	}

	@Override
	public void setParameterValues(List<ParameterValue> parameterValues)
			throws WctttCoreException {
		List<ParameterDefinition> expected = new ArrayList<>(PARAMETERS);
		for (ParameterValue value : parameterValues) {
			for (int i = 0; i < PARAMETERS.size(); i++) {
				if (value.getDefinition().equals(PARAMETERS.get(i))) {
					expected.remove(PARAMETERS.get(i));
					switch (i) {
						case 0:
							populationSize = (Integer) value.getValue();
							validatePopulationSize();
							break;
						case 1:
							crossoverRate = (Double) value.getValue();
							validateCrossoverRate();
							break;
						case 2:
							mutationRate = (Double) value.getValue();
							validateMutationRate();
							break;
						case 3:
							tabuListSize = (Integer) value.getValue();
							validateTabuListSize();
							break;
					}
				}
			}
		}
		if (!expected.isEmpty()) {
			throw new WctttCoreFatalException("List of parameter values was " +
					"not complete");
		}
	}

	private void validatePopulationSize() throws WctttCoreException {
		if (populationSize < POPULATION_SIZE_MIN) {
			throw new WctttCoreException("Population size must be >= " +
					POPULATION_SIZE_MIN);
		}
	}

	private void validateCrossoverRate() throws WctttCoreException {
		if (crossoverRate < CROSSOVER_RATE_MIN ||
				crossoverRate > CROSSOVER_RATE_MAX) {
			throw new WctttCoreException("Crossover rate must be >= " +
					CROSSOVER_RATE_MIN + " and <= " + CROSSOVER_RATE_MAX);
		}
	}

	private void validateMutationRate() throws WctttCoreException {
		if (mutationRate < MUTATION_RATE_MIN ||
				mutationRate > MUTATION_RATE_MAX) {
			throw new WctttCoreException("Mutation rate must be >= " +
					MUTATION_RATE_MIN + " and <= " + MUTATION_RATE_MAX);
		}
	}

	private void validateTabuListSize() throws WctttCoreException {
		if (tabuListSize < TABU_LIST_SIZE_MIN) {
			throw new WctttCoreException("Tabu list size must be >= " +
					TABU_LIST_SIZE_MIN);
		}
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
