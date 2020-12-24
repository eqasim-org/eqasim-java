package org.eqasim.core.location_assignment.matsim.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.StageActivityHandling;

public class ActivityIndicesFinder {
	final private Set<String> variableActivityTypes;

	public ActivityIndicesFinder(Set<String> variableActivityTypes) {
		this.variableActivityTypes = variableActivityTypes;
	}

	public Optional<ActivityTailIndices> findRightTailIndices(List<PlanElement> planElements) {
		if (countStageActivities(planElements) > 0) {
			throw new IllegalStateException("Plans should not contain stage activities for ActivityIndicesFinder");
		}

		List<Activity> activities = TripStructureUtils.getActivities(planElements,
				StageActivityHandling.ExcludeStageActivities);
		List<Integer> activityIndices = getVariableIndices(activities);
		List<Integer> tailIndices = getRightIndexTail(activityIndices, activities.size());

		if (tailIndices.size() > 0) {
			Activity anchorActivity = activities.get(tailIndices.get(0));
			Activity startActivity = activities.get(activities.size() - 1);

			int anchorActivityIndex = planElements.indexOf(anchorActivity);
			int tailActivityIndex = planElements.indexOf(startActivity);

			return Optional.of(new ActivityTailIndices(anchorActivityIndex, tailActivityIndex));
		} else {
			return Optional.empty();
		}
	}

	public Optional<ActivityTailIndices> findLeftTailIndices(List<PlanElement> planElements) {
		if (countStageActivities(planElements) > 0) {
			throw new IllegalStateException("Plans should not contain stage activities for ActivityIndicesFinder");
		}

		List<Activity> activities = TripStructureUtils.getActivities(planElements,
				StageActivityHandling.ExcludeStageActivities);
		List<Integer> activityIndices = getVariableIndices(activities);
		List<Integer> tailIndices = getLeftIndexTail(activityIndices);

		if (tailIndices.size() > 0) {
			Activity anchorActivity = activities.get(tailIndices.get(tailIndices.size() - 1));
			Activity startActivity = activities.get(0);

			int anchorActivityIndex = planElements.indexOf(anchorActivity);
			int tailActivityIndex = planElements.indexOf(startActivity);

			return Optional.of(new ActivityTailIndices(anchorActivityIndex, tailActivityIndex));
		} else {
			return Optional.empty();
		}
	}

	public Collection<ActivityIndices> findChainIndices(List<PlanElement> planElements) {
		if (countStageActivities(planElements) > 0) {
			throw new IllegalStateException("Plans should not contain stage activities for ActivityIndicesFinder");
		}

		List<ActivityIndices> result = new LinkedList<>();

		List<Activity> activities = TripStructureUtils.getActivities(planElements,
				StageActivityHandling.ExcludeStageActivities);
		List<Integer> activityIndices = activities.stream().map(planElements::indexOf).collect(Collectors.toList());

		List<Integer> variableActivityIndices = getVariableIndices(activities);

		List<Integer> leftTailIndices = getLeftIndexTail(variableActivityIndices);
		List<Integer> rightTailIndices = getRightIndexTail(variableActivityIndices, activities.size());

		if (leftTailIndices.size() > 0) {
			int anchorIndex = activityIndices.get(leftTailIndices.get(leftTailIndices.size() - 1) + 1);
			List<Integer> elementIndices = leftTailIndices.stream().map(activityIndices::get)
					.collect(Collectors.toList());
			result.add(new ActivityIndices(Optional.empty(), Optional.of(anchorIndex), elementIndices));
		}

		if (rightTailIndices.size() > 0) {
			int anchorIndex = planElements.indexOf(activities.get(rightTailIndices.get(0) - 1));
			List<Integer> elementIndices = rightTailIndices.stream().map(activityIndices::get)
					.collect(Collectors.toList());
			result.add(new ActivityIndices(Optional.of(anchorIndex), Optional.empty(), elementIndices));
		}

		variableActivityIndices.removeAll(getLeftIndexTail(variableActivityIndices));
		variableActivityIndices.removeAll(getRightIndexTail(variableActivityIndices, activities.size()));
		List<List<Integer>> indexChains = getIndexChains(variableActivityIndices);

		for (List<Integer> chainIndices : indexChains) {
			int originActivityIndex = activityIndices.get(chainIndices.get(0) - 1);
			int destinationActivityIndex = activityIndices.get(chainIndices.get(chainIndices.size() - 1) + 1);
			List<Integer> elementIndices = chainIndices.stream().map(activityIndices::get).collect(Collectors.toList());

			result.add(new ActivityIndices(Optional.of(originActivityIndex), Optional.of(destinationActivityIndex),
					elementIndices));
		}

		return result;
	}

	private List<Integer> getRightIndexTail(List<Integer> indices, int numberOfActivities) {
		if (indices.contains(numberOfActivities - 1)) {
			int nextTailIndex = numberOfActivities - 2;

			while (indices.contains(nextTailIndex)) {
				nextTailIndex--;
			}

			return IntStream.range(nextTailIndex + 1, numberOfActivities).mapToObj(i -> i).collect(Collectors.toList());
		} else {
			return Collections.emptyList();
		}
	}

	private List<Integer> getLeftIndexTail(List<Integer> indices) {
		if (indices.contains(0)) {
			int nextTailIndex = 1;

			while (indices.contains(nextTailIndex)) {
				nextTailIndex++;
			}

			return IntStream.range(0, nextTailIndex).mapToObj(i -> i).collect(Collectors.toList());
		} else {
			return Collections.emptyList();
		}
	}

	private List<List<Integer>> getIndexChains(List<Integer> indices) {
		List<Integer> remainingIndices = new LinkedList<>(indices);
		List<List<Integer>> chains = new LinkedList<>();

		if (indices.size() > 0) {
			List<Integer> currentChain = new LinkedList<>();
			currentChain.add(remainingIndices.remove(0));

			while (remainingIndices.size() > 0) {
				if (remainingIndices.get(0) == currentChain.get(currentChain.size() - 1) + 1) {
					currentChain.add(remainingIndices.remove(0));
				} else {
					chains.add(currentChain);

					currentChain = new LinkedList<>();
					currentChain.add(remainingIndices.remove(0));
				}
			}

			chains.add(currentChain);
		}

		return chains;
	}

	private List<Integer> getVariableIndices(List<Activity> activities) {
		List<Integer> indices = new LinkedList<>();

		for (int i = 0; i < activities.size(); i++) {
			if (variableActivityTypes.contains(activities.get(i).getType())) {
				indices.add(i);
			}
		}

		return indices;
	}

	private long countStageActivities(List<PlanElement> planElements) {
		return planElements.stream().filter(Activity.class::isInstance).map(Activity.class::cast).map(Activity::getType)
				.filter(TripStructureUtils::isStageActivityType).count();
	}
}
