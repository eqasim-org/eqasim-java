package org.eqasim.core.simulation.mode_choice;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public interface ParameterDefinition {
	final static Logger logger = Logger.getLogger(ParameterDefinition.class);

	static public void applyCommandLine(String prefix, CommandLine cmd, ParameterDefinition parameterDefinition) {
		Map<String, String> values = new HashMap<>();

		for (String option : cmd.getAvailableOptions()) {
			if (option.startsWith(prefix + ":")) {
				try {
					values.put(option.split(":")[1], cmd.getOptionStrict(option));
				} catch (ConfigurationException e) {
					// Should not happen
				}
			}
		}

		applyMap(parameterDefinition, values);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	static public void applyMap(ParameterDefinition parameterDefinition, Map<String, String> values) {
		for (Map.Entry<String, String> entry : values.entrySet()) {
			String option = entry.getKey();
			String value = entry.getValue();

			try {
				String[] parts = option.split("\\.");
				int numberOfParts = parts.length;

				Object activeObject = parameterDefinition;

				for (int i = 0; i < parts.length; i++) {
					Field field = activeObject.getClass().getField(parts[i]);

					if (i == numberOfParts - 1) {
						// We need to set the value
						if (field.getType() == Double.class || field.getType() == double.class) {
							field.setDouble(activeObject, Double.parseDouble(value));
						} else if (field.getType() == Float.class || field.getType() == float.class) {
							field.setFloat(activeObject, Float.parseFloat(value));
						} else if (field.getType() == Integer.class || field.getType() == int.class) {
							field.setInt(activeObject, Integer.parseInt(value));
						} else if (field.getType() == Long.class || field.getType() == long.class) {
							field.setLong(activeObject, Long.parseLong(value));
						} else if (field.getType() == String.class) {
							field.set(activeObject, value);
						} else if (field.getType().isEnum()) {
							Class<Enum> enumType = (Class<Enum>) field.getType();
							field.set(activeObject, Enum.valueOf(enumType, value));
						} else {
							throw new IllegalStateException(
									String.format("Cannot convert parameter %s because type %s is not supported",
											option, field.getType().toString()));
						}
					} else {
						// We need to traverse the objects
						activeObject = field.get(activeObject);
					}
				}

				logger.info(String.format("Set %s = %s", option, value));
			} catch (NoSuchFieldException e) {
				throw new IllegalStateException(String.format("Parameter %s does not exist", option));
			} catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
				logger.error("Error while processing option " + option);
				throw new RuntimeException(e);
			}
		}
	}

	static public MapType mapType = TypeFactory.defaultInstance().constructMapType(HashMap.class, String.class,
			String.class);

	static public void applyFile(File path, ParameterDefinition definition) {
		try {
			Map<String, String> values = new ObjectMapper(new YAMLFactory()).readValue(path, mapType);
			applyMap(definition, values);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
