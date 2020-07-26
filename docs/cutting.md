# Cutting a scenario

In order to cut a scenario, it is possible to call the respective script from
Eclipse/IntelliJ, or from the command line. In the latter case, please follow
the instructions from the section above to package a jar file.

The relevant run script is then `org.eqasim.scenario.cutter.RunScenarioCutter`.
Note that either the `switzerland` or `ile_de_france`
jar can be used. Since the script is located in the `core` package, there is no
scenario-specific functionality in the cutter. We simply don't create an additional
jar for the `core` package since all the functionality is automatically included in
the scenario-specific jars.

The script expects a number of arguments:

```bash
java -Xmx100G -cp /path/to/switzerland-1.0.0.jar org.eqasim.core.scenario.cutter.RunScenarioCutter
--config-path /path/to/ile_de_france_config.xml
--output-path /path/to/output/paris_10pct
--extent-path /path/to/paris_shapefile.shp
--config:plans.inputPlansFile /path/to/output_plans.xml.gz
--prefix paris_
--threads 24
```

First, the path to the reference scenario must be given by providing its config
file. Second, an output path needs to be specified. Third, the path to a shape file
must be provided. This shape file determines the extent of the cut region. Make
sure that it is a compact shape without holes. The shape file should only contain
a single polygon feature. Finally, a prefix can be specified that defines according
to which pattern files will be written to the output folder. In this case, we would
have a `paris_population.xml.gz`, `paris_facilities.xml.gz`, etc.

The generated scenario can then be run. For that, you can use the same scripts
as for the reference sceanrio. If you cut a Zurich scenario from Switzerland,
the `switzerland` module can still be used to run the scenario. The same goes
for the Paris / Ile-de-France case.

To get started, the `gis/` folder in this repository contains a number of common
shape files:
- `zurich_city.shp` The 12 districts of Zurich city
- `zurich.shp` Zurich city and several high density areas in the surroundings
- `zurich_20km.shp` Zurich city and surroundings, plus a 20km buffer
- `paris.shp` City area of Paris (20 arrondissements)
- `paris_20km.shp` City area of Paris, plus a 20km buffer

Note that the shape files must be given in the correct projection. For Switzerland
this is `EPSG:2056` and for France it is `EPSG:2154`.

![Scenario Shapes](gis/shapes.png "Scenario Shapes")
