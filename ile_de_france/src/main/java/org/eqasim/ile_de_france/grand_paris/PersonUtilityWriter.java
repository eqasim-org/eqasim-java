package org.eqasim.ile_de_france.grand_paris;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;

public class PersonUtilityWriter {
	private final Population population;
	private final StageActivityTypes stageActivityTypes;

	public PersonUtilityWriter(Population population, StageActivityTypes stageActivityTypes) {
		this.population = population;
		this.stageActivityTypes = stageActivityTypes;
	}

	public void writeFile(File path) throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path)));

		writer.write(String.join(";", new String[] { //
				"person_id", //
				"utility" //
		}) + "\n");

		for (Person person : population.getPersons().values()) {
			double personUtility = Double.NaN;

			for (Activity activity : TripStructureUtils.getActivities(person.getSelectedPlan(), stageActivityTypes)) {
				Double tripUtility = (Double) activity.getAttributes().getAttribute("utility");

				if (tripUtility != null) {
					if (Double.isNaN(personUtility)) {
						personUtility = 0.0;
					}

					personUtility += tripUtility;
				}
			}

			if (!Double.isNaN(personUtility)) {
				writer.write(String.join(";", new String[] { //
						person.getId().toString(), //
						String.valueOf(personUtility) //
				}) + "\n");
			}
		}

		writer.close();
	}
}
