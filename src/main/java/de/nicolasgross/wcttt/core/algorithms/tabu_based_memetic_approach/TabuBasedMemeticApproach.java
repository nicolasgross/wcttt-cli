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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementation of the tabu-based memetic approach, which was proposed by
 * Salwani Abdullah and Hamza Turabieh in 'On the use of multi neighbourhood
 * structures within a Tabu-based memetic approach to university timetabling
 * problems', 2012.
 */
public class TabuBasedMemeticApproach extends AbstractAlgorithm {

	private static final String NAME = "Tabu-based memetic approach";
	private static final List<ParameterDefinition> PARAMETERS = Arrays.asList(
			new ParameterDefinition("Population size", ParameterType.INT),
			new ParameterDefinition("Crossover rate", ParameterType.DOUBLE),
			new ParameterDefinition("Mutation rate", ParameterType.DOUBLE),
			new ParameterDefinition("Tabu list size", ParameterType.INT));
	private static final int POPULATION_SIZE_MIN = 2;
	private static final double CROSSOVER_RATE_MIN = 0.0;
	private static final double CROSSOVER_RATE_MAX = 1.0;
	private static final double MUTATION_RATE_MIN = 0.0;
	private static final double MUTATION_RATE_MAX = 1.0;
	private static final int TABU_LIST_SIZE_MIN = 1;
	private static final List<NeighborhoodStructure> NBS_LIST = Arrays.asList(
			new NeighborhoodStructure1()
			// TODO add more neighborhood structures
			);

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
	protected Timetable runAlgorithm(AtomicBoolean isCancelled)
			throws WctttCoreException {
		// Generate random initial population of feasible solutions:
		SaturationDegreeHeuristic satDegHeuristic =
				new SaturationDegreeHeuristic(getSemester());
		List<Timetable> population = satDegHeuristic.generateFeasibleSolutions(
				populationSize, isCancelled);

		// If initialization of population was cancelled before a feasible
		// solution was found, then return no feasible solution:
		if (population.isEmpty()) {
			return null;
		}

		// Find best solution:
		ConstraintViolationsCalculator constrCalc =
				new ConstraintViolationsCalculator(getSemester());
		population.forEach(t -> t.setSoftConstraintPenalty(
				constrCalc.calcTimetablePenalty(t)));
		Timetable bestSolution = chooseBestSolution(population);

		Queue<NeighborhoodStructure> tabuList = new LinkedList<>();
		boolean chooseNewNbs = true; // Nbs == neighborhood structure
		NeighborhoodStructure selectedNbs = null;

		while (bestSolution.getSoftConstraintPenalty() != 0 &&
				!isCancelled.get()) {
			// Genetic operators:
			Timetable[] parents = rouletteWheelSelectParents(population);
			Timetable[] offspring = crossoverOperator(parents);
			mutationOperator(offspring[0]);
			mutationOperator(offspring[1]);

			// Local search:
			if (chooseNewNbs) {
				selectedNbs = selectNbsRandomly(tabuList);
				// TODO other selection strategies as proposed in the paper
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

	/**
	 * Selects two distinct parents using a roulette wheel selection, where the
	 * selection probability p of a solution is defined by the following formula:
	 *
	 * p = x / y,
	 *
	 * where x is the fitness value of the solution and y is the sum of all
	 * solutions' fitness values.
	 *
	 * @param population the population.
	 * @return the selected parents.
	 */
	private Timetable[] rouletteWheelSelectParents(List<Timetable> population) {
		Timetable[] parents = new Timetable[2];
		double[] fitnessValues = new double[population.size()];
		double fitnessSum = 0.0;
		double highestPenalty =
				chooseWorstSolution(population).getSoftConstraintPenalty();

		int i = 0;
		for (Timetable timetable : population) {
			double fitness =
					highestPenalty - timetable.getSoftConstraintPenalty();
			fitnessValues[i] = fitness;
			fitnessSum += fitness;
			i++;
		}

		while (parents[0] == null || parents[1] == null) {
			double selection = new Random().nextDouble() * fitnessSum;
			for (int j = 0; j < fitnessValues.length; j++) {
				if (fitnessValues[j] - selection <= 0) {
					if (parents[0] == null) {
						parents[0] = population.get(j);
					} else if (!parents[0].equals(population.get(j))) {
						parents[1] = population.get(j);
					}
					break;
				}
			}
		}

		return parents;
	}

	private Timetable chooseWorstSolution(List<Timetable> solutions) {
		Timetable worstSolution = solutions.get(0);
		for (Timetable solution : solutions) {
			if (solution.getSoftConstraintPenalty() >
					worstSolution.getSoftConstraintPenalty()) {
				worstSolution = solution;
			}
		}
		return worstSolution;
	}

	private Timetable[] crossoverOperator(Timetable[] parents) {
		Timetable[] offspring = new Timetable[2];
		// TODO
		return offspring;
	}

	private void mutationOperator(Timetable timetable) {
		// TODO
	}

	private NeighborhoodStructure selectNbsRandomly(
			Queue<NeighborhoodStructure> tabuList) {
		return NBS_LIST.get(new Random().nextInt(NBS_LIST.size()));
	}

	private void localSearch(Timetable timetable,
	                         NeighborhoodStructure selectedNbs) {
		// TODO
	}

	/**
	 * Removes the worst solution in the population and and adds another
	 * solution to the population. If the other solution is worse than the worst
	 * solution of the population, nothing happens.
	 *
	 * @param population the current population.
	 * @param newSolution the new solution that should be added to the population.
	 */
	private void updatePopulation(List<Timetable> population,
	                              Timetable newSolution) {
		Timetable worstSolution = population.get(0);
		for (Timetable timetable : population) {
			if (timetable.getSoftConstraintPenalty() >
					worstSolution.getSoftConstraintPenalty()) {
				worstSolution = timetable;
			}
		}
		if (newSolution.getSoftConstraintPenalty() <
				worstSolution.getSoftConstraintPenalty()) {
			population.remove(worstSolution);
			population.add(newSolution);
		}
	}
}
