mvn clean package -Pstandalone --projects ile_de_france --also-make
scp ile_de_france/target/ile_de_france-1.3.1.jar bullx:/scratch/sebastian.horl/explo_idf/ile_de_france-1.3.1.jar
