package org.eqasim.simulation.mode_choice.parameters;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;

public interface ParameterDefinition {
	static public void applyCommandLine(String prefix, CommandLine cmd, ParameterDefinition parameterDefinition) {
		Map<String, Double> values = new HashMap<>();

		for (String option : cmd.getAvailableOptions()) {
			if (option.startsWith(prefix + ":")) {
				try {
					values.put(option.split(":")[1], Double.parseDouble(cmd.getOptionStrict(option)));
				} catch (ConfigurationException e) {
					// Should not happen
				}
			}
		}

		applyMap(parameterDefinition, values);
	}

	static public void applyMap(ParameterDefinition parameterDefinition, Map<String, Double> values) {
		for (Map.Entry<String, Double> entry : values.entrySet()) {
			String option = entry.getKey();
			double value = entry.getValue();

			try {
				String[] parts = option.split("\\.");

				Object activeObject = parameterDefinition;

				for (int i = 0; i < parts.length - 1; i++) {
					Field field = activeObject.getClass().getField(parts[i]);
					activeObject = field.get(activeObject);
				}

				Field field = activeObject.getClass().getField(parts[parts.length - 1]);

				if (field.getType().equals(double.class)) {
					field.set(activeObject, value);
				} else {
					throw new IllegalStateException(
							String.format("Option %s does not refer to a double field", option));
				}
			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
				throw new IllegalStateException("Error while processing option " + option);
			}
		}
	}
}
