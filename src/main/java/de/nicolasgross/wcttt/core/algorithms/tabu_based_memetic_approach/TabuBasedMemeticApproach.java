package de.nicolasgross.wcttt.core.algorithms.tabu_based_memetic_approach;

import de.nicolasgross.wcttt.core.WctttCoreException;
import de.nicolasgross.wcttt.core.WctttCoreFatalException;
import de.nicolasgross.wcttt.core.algorithms.AbstractAlgorithm;
import de.nicolasgross.wcttt.core.algorithms.ParameterDefinition;
import de.nicolasgross.wcttt.core.algorithms.ParameterType;
import de.nicolasgross.wcttt.core.algorithms.ParameterValue;
import de.nicolasgross.wcttt.lib.model.Semester;
import de.nicolasgross.wcttt.lib.model.Timetable;
import de.nicolasgross.wcttt.lib.util.ConstraintViolationsCalculator;

import java.util.*;

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
	private static final List<NeighborhoodStructure> NBS_LIST = Arrays.asList(
			new NeighborhoodStructure1());

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
	protected Timetable runAlgorithm() throws WctttCoreException {
		// Generate random initial population of feasible solutions:
		SaturationDegreeHeuristic satDegHeuristic =
				new SaturationDegreeHeuristic(semester);
		List<Timetable> population = satDegHeuristic.generateFeasibleSolutions(
				populationSize, isCancelled);

		// If initialization of population was cancelled before a feasible
		// solution was found, then return no feasible solution:
		if (population.isEmpty()) {
			return null;
		}

		// Find best solution:
		ConstraintViolationsCalculator constrCalc =
				new ConstraintViolationsCalculator(semester);
		population.forEach(t -> t.setSoftConstraintPenalty(
				constrCalc.calcTimetablePenalty(t)));
		Timetable bestSolution = chooseBestSolution(population);

		Queue<NeighborhoodStructure> tabuList = new LinkedList<>();
		boolean chooseNewNbs = true; // Nbs == neighborhood structure
		NeighborhoodStructure selectedNbs;

		while (false && bestSolution.getSoftConstraintPenalty() != 0 &&
				!isCancelled.get()) {
			// Genetic operators:
			Timetable[] parents = rouletteWheelSelectParents(population);
			Timetable[] offspring = crossoverOperator(parents);
			mutationOperator(offspring[0]);
			mutationOperator(offspring[1]);

			// Local search:
			if (chooseNewNbs) {
				selectedNbs = selectNbs(tabuList);
			}
			Timetable[] improvedOffspring = {new Timetable(offspring[0]),
					new Timetable(offspring[1])};
			localSearch(improvedOffspring[0], selectedNbs);
			localSearch(improvedOffspring[1], selectedNbs);

			// Calculate constraint violations of new solutions:
			List<Timetable> allNewSolutions = Arrays.asList(offspring[0],
					offspring[1], improvedOffspring[0], improvedOffspring[1]);
			allNewSolutions.forEach(t -> t.setSoftConstraintPenalty(
					constrCalc.calcTimetablePenalty(t)));
			Timetable bestNewSolution = chooseBestSolution(allNewSolutions);

			// Update best solution and selected neighborhood structure:
			if (bestNewSolution.getSoftConstraintPenalty() <
					bestSolution.getSoftConstraintPenalty()) {
				bestSolution = bestNewSolution;
				chooseNewNbs = false;
			} else {
				tabuList.add(selectedNbs);
				if (tabuList.size() > tabuListSize) {
					tabuList.remove();
				}
				chooseNewNbs = true;
			}

			updatePopulation(population, bestNewSolution);
		}

		return bestSolution;
	}

	private Timetable chooseBestSolution(List<Timetable> solutions) {
		Timetable bestSolution = solutions.get(0);
		for (Timetable solution : solutions) {
			if (solution.getSoftConstraintPenalty() <
					bestSolution.getSoftConstraintPenalty()) {
				bestSolution = solution;
			}
		}
		return bestSolution;
	}

	private Timetable[] rouletteWheelSelectParents(List<Timetable> population) {
		Timetable[] parents = new Timetable[2];
		// TODO
		return parents;
	}

	private Timetable[] crossoverOperator(Timetable[] parents) {
		Timetable[] offspring = new Timetable[2];
		// TODO
		return offspring;
	}

	private void mutationOperator(Timetable timetable) {
		// TODO
	}

	private NeighborhoodStructure selectNbs(Queue<NeighborhoodStructure> tabuList) {
		// TODO
		return null;
	}

	private void localSearch(Timetable timetable,
	                         NeighborhoodStructure selectedNbs) {
		// TODO
	}

	private void updatePopulation(List<Timetable> population,
	                              Timetable bestNewSolution) {
		Timetable worstSolution = population.get(0);
		for (Timetable timetable : population) {
			if (timetable.getSoftConstraintPenalty() >
					worstSolution.getSoftConstraintPenalty()) {
				worstSolution = timetable;
			}
		}
		if (bestNewSolution.getSoftConstraintPenalty() <
				worstSolution.getSoftConstraintPenalty()) {
			population.remove(worstSolution);
			population.add(bestNewSolution);
		}
	}
}
