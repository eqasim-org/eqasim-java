package org.eqasim.core.tools;

import java.util.*;

import org.apache.log4j.Logger;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.attributable.Attributes;

public class RunIsolateAgent {

	public static class PickyPopulation implements Population {

		private static final Logger log = Logger.getLogger(PickyPopulation.class);
		private final Population delegate;
		private final Collection<Id<Person>> personIds;
		private long counter;
		private long nextMessage;

		public PickyPopulation(Population delegate, Collection<Id<Person>> personIds) {
			this.delegate = delegate;
			this.personIds = personIds;
			this.counter=0;
			this.nextMessage = 1;
		}

		@Override
		public PopulationFactory getFactory() {
			return delegate.getFactory();
		}

		@Override
		public String getName() {
			return delegate.getName();
		}

		@Override
		public void setName(String name) {
			this.delegate.setName(name);
		}

		@Override
		public Map<Id<Person>, ? extends Person> getPersons() {
			return delegate.getPersons();
		}

		@Override
		public void addPerson(Person p) {
			this.counter++;
			if(this.counter % this.nextMessage == 0) {
				this.nextMessage *= 4;
				log.info(" checked person # " + this.counter);
			}
			if(personIds.contains(p.getId())) {
				this.delegate.addPerson(p);
			}
		}

		@Override
		public Person removePerson(Id<Person> personId) {
			return this.delegate.removePerson(personId);
		}

		@Override
		public Attributes getAttributes() {
			return this.delegate.getAttributes();
		}
	}
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("input-path", "output-path", "agent-id") //
				.build();

		Config config = ConfigUtils.createConfig();
		MutableScenario scenario = ScenarioUtils.createMutableScenario(config);
		scenario.setPopulation(new PickyPopulation(scenario.getPopulation(), Collections.singletonList(Id.createPersonId(cmd.getOptionStrict("agent-id")))));
		new EqasimConfigurator().configureScenario(scenario);

		// Load population
		String inputPath = cmd.getOptionStrict("input-path");
		new PopulationReader(scenario).readFile(inputPath);

		// Reduce population
		Collection<Id<Person>> allIds = new HashSet<>(scenario.getPopulation().getPersons().keySet());
		allIds.removeAll(Arrays.asList(Id.createPersonId(cmd.getOptionStrict("agent-id"))));
		allIds.forEach(scenario.getPopulation()::removePerson);

		// Write population
		String outputPath = cmd.getOptionStrict("output-path");
		new PopulationWriter(scenario.getPopulation()).write(outputPath);
	}
}
