#!/bin/bash

#SBATCH -J simulation_bavaria_job

# Output and error files
#SBATCH --output=simulation_bavaria_output%j.log
#SBATCH --error=simulation_bavaria_error_%j.log

#SBATCH --nodes=1
#SBATCH --ntasks-per-node=1
#SBATCH --cpus-per-task=48
#SBATCH --mem=80GB
#SBATCH --time=10:00:00

# Account and partition
#SBATCH --account=pn39mu
#SBATCH --partition=micro

# Notification and type
#SBATCH --mail-type=END
#SBATCH --mail-user=elena.natterer@tum.de
#SBATCH --get-user-env

# Set up of execution environment
#SBATCH --export=none

#export HOME=/dss/dsshome1/06/ge49wav3
export FONTCONFIG_PATH=$HOME/test_java_font_4u/fonts
export FONTCONFIG_FILE=$HOME/test_java_font_4u/fonts/fonts.conf
export FC_FONT_PATH=$HOME/test_java_font_4u/fonts

# Run the Java program with full arguments
java -cp ile_de_france/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city bavaria --threads 48 --memory 80 > output.log 2>&1 &
