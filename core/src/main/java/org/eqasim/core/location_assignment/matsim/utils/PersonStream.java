package org.eqasim.core.location_assignment.matsim.utils;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;

final public class PersonStream {
	private PersonStream() {
	}

	static public Stream<Person> get(Scenario scenario, String populationPath, boolean parallel) {
		return StreamSupport.stream(new PersonSpliterator(scenario, populationPath), parallel);
	}
}
