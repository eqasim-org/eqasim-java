package org.eqasim.core.scenario.location_assignment.listener;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.eqasim.core.location_assignment.algorithms.ThresholdObjective;
import org.eqasim.core.location_assignment.assignment.distance.FeasibleDistanceResult;
import org.eqasim.core.location_assignment.assignment.relaxation.RelaxedLocationResult;
import org.eqasim.core.location_assignment.matsim.solver.MATSimSolverResult;

public class StatisticsListener implements LocationAssignmentListener, Closeable {
	private final BufferedWriter problemWriter;
	private final BufferedWriter errorWriter;

	private int problemIndex = 0;
	private long startTime = 0;

	public StatisticsListener(File path) throws IOException {
		startTime = System.nanoTime();

		if (path.isDirectory()) {
			errorWriter = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(new File(path, "errors.csv"))));
			problemWriter = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(new File(path, "problems.csv"))));

			errorWriter.write(String.join(";", new String[] { //
					"problem_index", //
					"mode", //
					"target_distance", //
					"absolute_error", //
					"excess_error" //
			}) + "\n");

			problemWriter.write(String.join(";", new String[] { //
					"problem_index", //
					"runtime", //
					"is_tail_problem", //
					"number_of_trips", //
					"distance_converged", //
					"relaxation_converged", //
					"discretization_converged", //
					"number_of_trip_discretizations_converged", //
					"crowfly_distance", "origin_x", "origin_y", "destination_x", "destination_y", //
					"assignmentIterations", //
					"relaxationIterations", //
					"distanceIterations" //
			}) + "\n");
		} else {
			throw new IllegalStateException("Expected existing directory: " + path);
		}
	}

	@Override
	public synchronized void process(Collection<MATSimSolverResult> results) {
		long currentTime = System.nanoTime();
		long runtime = (long) Math.floor((currentTime - startTime) / 1e9);

		try {
			for (MATSimSolverResult result : results) {
				long numberOfTrips = result.getProblem().getChainActivities().size() + 1;

				FeasibleDistanceResult feasibleDistanceResult = result.getResult().getFeasibleDistanceResult();
				boolean distanceConverged = feasibleDistanceResult.isConverged();

				RelaxedLocationResult relaxedLocationResult = result.getResult().getRelaxedLocationResult();
				boolean relaxationConverged = relaxedLocationResult.isConverged();

				ThresholdObjective objectiveResult = (ThresholdObjective) result.getResult().getObjective();
				boolean discretizationConverged = objectiveResult.isConverged();

				double crowflyDistance = Double.NaN;

				if (!result.getProblem().isTailProblem()) {
					Vector2D originLocation = result.getProblem().getOriginLocation().get();
					Vector2D destinationLocation = result.getProblem().getDestinationLocation().get();

					crowflyDistance = originLocation.distance(destinationLocation);
				}

				double originX = Double.NaN;
				double originY = Double.NaN;
				double destinationX = Double.NaN;
				double destinationY = Double.NaN;

				if (result.getProblem().getOriginLocation().isPresent()) {
					Vector2D originLocation = result.getProblem().getOriginLocation().get();

					originX = originLocation.getX();
					originY = originLocation.getY();
				}

				if (result.getProblem().getDestinationLocation().isPresent()) {
					Vector2D destinationLocation = result.getProblem().getDestinationLocation().get();

					destinationX = destinationLocation.getX();
					destinationY = destinationLocation.getY();
				}

				long numberOfConvergedTripDiscretizations = 0;

				for (int i = 0; i < result.getProblem().getChainLegs().size(); i++) {
					String mode = result.getProblem().getChainLegs().get(i).getMode();

					double targetDistance = feasibleDistanceResult.getTargetDistances().get(i);
					double absoluteError = objectiveResult.getAbsoluteErrors().get(i);
					double excessError = objectiveResult.getExcessErrors().get(i);

					errorWriter.write(String.join(";", new String[] { //
							String.valueOf(problemIndex), //
							mode, //
							String.valueOf(targetDistance), //
							String.valueOf(absoluteError), //
							String.valueOf(excessError) //
					}) + "\n");

					if (excessError == 0.0) {
						numberOfConvergedTripDiscretizations++;
					}
				}

				problemWriter.write(String.join(";", new String[] { //
						String.valueOf(problemIndex), //
						String.valueOf(runtime), //
						String.valueOf(result.getProblem().isTailProblem()), //
						String.valueOf(numberOfTrips), //
						String.valueOf(distanceConverged), //
						String.valueOf(relaxationConverged), //
						String.valueOf(discretizationConverged), //
						String.valueOf(numberOfConvergedTripDiscretizations), //
						String.valueOf(crowflyDistance), //
						String.valueOf(originX), String.valueOf(originY), //
						String.valueOf(destinationX), String.valueOf(destinationY), //
						String.valueOf(result.getResult().getAssignmentIterations()), //
						String.valueOf(result.getResult().getRelaxationIterations()), //
						String.valueOf(result.getResult().getDistanceIterations()) //
				}) + "\n");

				problemIndex++;
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void update() {
		try {
			errorWriter.flush();
			problemWriter.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() throws IOException {
		errorWriter.flush();
		problemWriter.flush();
	}
}
