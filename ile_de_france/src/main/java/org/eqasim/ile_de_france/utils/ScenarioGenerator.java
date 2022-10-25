package org.eqasim.ile_de_france.utils;

import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;

import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScenarioGenerator {

    private final CommandLine.Builder cmdBuilder;
    private final Map<String, String> extraOptions = new HashMap<>();

    public ScenarioGenerator(String[] args) {
        this.cmdBuilder = new CommandLine.Builder(args).requireOptions("base-config-path", "output-dir-path", "naming-scheme").allowPrefixes("arg");

    }

    protected CommandLine.Builder getCmdBuilder() {
        return this.cmdBuilder;
    }

    protected ConfigGroup[] getConfigGroups() throws CommandLine.ConfigurationException {
        return new ConfigGroup[0];
    }

    protected Map<String, String> getExtraOptions() {
        return this.extraOptions;
    }

    public void generate() throws CommandLine.ConfigurationException {
        CommandLine cmd = this.cmdBuilder.build();
        this.extraOptions.clear();

        List<String> baseOptions = List.of(new String[]{"base-config-path", "output-dir-path", "naming-scheme"});

        String configPath = cmd.getOptionStrict("base-config-path");

        Path outputDirPath = Path.of(cmd.getOptionStrict("output-dir-path"));

        Pattern pattern = Pattern.compile("^arg:(.+)$");

        int totalNumberOfOptions = 0;

        Set<String> variablesOptions = new HashSet<>();
        List<String> variables = new ArrayList<>();
        List<List<String>> values = new ArrayList<>();

        for(String option: cmd.getAvailableOptions()) {
            totalNumberOfOptions++;
            Matcher matcher = pattern.matcher(option);
            if(matcher.matches()) {
                variablesOptions.add(option);
                variables.add(matcher.group(1));
                String[] variableValues = cmd.getOptionStrict(option).split(";");
                values.add(List.of(variableValues));
            } else if(!baseOptions.contains(option) && !option.startsWith("config:")) {
                this.extraOptions.put(option, cmd.getOptionStrict(option));
            }

        }
        if(variables.size() == 0) {
            return;
        }
        int[] indexes = new int[variables.size()];

        boolean finished;
        do {
            String[] currentArgs = new String[totalNumberOfOptions-variables.size()-extraOptions.size()-baseOptions.size()];
            {
                int i=0;
                for(String option: cmd.getAvailableOptions()) {
                    if(variablesOptions.contains(option) || baseOptions.contains(option) || extraOptions.containsKey(option)) {
                        continue;
                    }
                    String optionValue = cmd.getOptionStrict(option);
                    for(int j=0; j<indexes.length; j++) {
                        optionValue = optionValue.replace(String.format("$%s$", variables.get(j)), values.get(j).get(indexes[j]));
                    }
                    currentArgs[i] = String.format("--%s=%s", option, optionValue);
                    i+=1;
                }
            }
            CommandLine currentCmd = new CommandLine.Builder(currentArgs).build();

            Config config = ConfigUtils.loadConfig(configPath, this.getConfigGroups());

            currentCmd.applyConfiguration(config, List.of(this.getConfigGroups()));

            String fileName = cmd.getOptionStrict("naming-scheme");
            for(int i=0; i<indexes.length; i++) {
                fileName = fileName.replace(String.format("$%s$", variables.get(i)), values.get(i).get((indexes[i])));
            }

            ConfigUtils.writeConfig(config, outputDirPath.resolve(fileName).toAbsolutePath().toString());

            indexes[0]+=1;
            for(int i=0; i<indexes.length-1; i++) {

                if(indexes[i]==values.get(i).size()) {
                    indexes[i]=0;
                    indexes[i+1]++;
                }
            }
            finished = indexes[indexes.length-1] == values.get(indexes.length-1).size();
        }while(!finished);
        this.extraOptions.clear();
    }




    public static void main(String[] args) throws CommandLine.ConfigurationException {
        ScenarioGenerator scenarioGenerator = new ScenarioGenerator(args);
        scenarioGenerator.generate();
    }
}
