package org.eqasim.core.location_assignment.matsim.utils;

import java.util.Spliterators;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.StreamingPopulationReader;

public class PersonSpliterator extends Spliterators.AbstractSpliterator<Person> implements PersonAlgorithm {
	final private Scenario scenario;
	final private String populationPath;

	public PersonSpliterator(Scenario scenario, String populationPath) {
		super(Long.MAX_VALUE, 0);

		this.scenario = scenario;
		this.populationPath = populationPath;
	}

	@Override
	public boolean tryAdvance(Consumer<? super Person> action) {
		if (!isReading) {
			isReading = true;
			isDone = false;
			startReading();
		}

		try {
			if (action == null) {
				throw new NullPointerException();
			}

			Person nextPerson = null;

			while (!isDone && nextPerson == null) {
				nextPerson = queue.poll(10, TimeUnit.MILLISECONDS);
			}

			if (isDone) {
				return false;
			} else {
				action.accept(nextPerson);
				return true;
			}
		} catch (InterruptedException e) {
			return false;
		}
	}

	private boolean isReading = false;
	private boolean isDone = false;

	private ArrayBlockingQueue<Person> queue;

	private void startReading() {
		StreamingPopulationReader reader = new StreamingPopulationReader(scenario);
		reader.addAlgorithm(this);

		queue = new ArrayBlockingQueue<>(10);
		new Thread(() -> {
			reader.readFile(populationPath);
			isDone = true;
		}).start();
	}

	@Override
	public void run(Person person) {
		try {
			queue.put(person);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}
