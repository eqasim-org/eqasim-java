package org.eqasim.projects.astra16.convergence;

import java.util.HashMap;
import java.util.Map;

public class ConvergenceManager {
	private Map<String, ConvergenceCriterion> criteria = new HashMap<>();

	public void addCriterion(String name, ConvergenceCriterion criterion) {
		this.criteria.put(name, criterion);
	}

	public void addValue(String slot, double value) {
		for (ConvergenceCriterion criterion : criteria.values()) {
			if (criterion.getSlot().equals(slot)) {
				criterion.addValue(value);
			}
		}
	}

	public boolean isConverged() {
		for (ConvergenceCriterion criterion : criteria.values()) {
			if (!criterion.isConverged()) {
				return false;
			}
		}

		return true;
	}

	public Map<String, ConvergenceCriterion> getCriteria() {
		return criteria;
	}
}
