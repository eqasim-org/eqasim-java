package org.eqasim.core.analysis.sociodemographics;

import org.matsim.api.core.v01.population.Person;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;

public class SocioDemographicsWriter {
    final private SocioDemographics socioDemographics;
    final private String delimiter;

    public SocioDemographicsWriter(SocioDemographics socioDemographics) {
        this(socioDemographics, ";");
    }

    public SocioDemographicsWriter(SocioDemographics socioDemographics, String delimiter) {
        this.socioDemographics = socioDemographics;
        this.delimiter = delimiter;
    }

    public void write(String outputPath) throws IOException{
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath)));

        writer.write(formatHeader() + "\n");
        writer.flush();

        for (Person person : socioDemographics.population) {
            writer.write(formatItem(person) + "\n");
            writer.flush();
        }

        writer.flush();
        writer.close();
    }

    private String formatHeader() {
        List<String> strings = new LinkedList<>();

        // add person id
        strings.add("person_id");

        // add all other attributes
        strings.addAll(socioDemographics.populationAttributes);

        return String.join(delimiter, strings);
    }

    private String formatItem(Person person) {

        List<String> strings = new LinkedList<>();

        // add person id
        strings.add(person.getId().toString());

        // add all remaining attributes
        // we use the getOrDefault since some attributes are not present, and we default to null
        // we then get the string value of so that the null values are correctly converted to string
        // and do not return null pointer exceptions
        for (String attribute : socioDemographics.populationAttributes) {
            strings.add(String.valueOf(person.getAttributes().getAsMap().getOrDefault(attribute, null)));
        }

        return String.join(delimiter, strings);
    }

}