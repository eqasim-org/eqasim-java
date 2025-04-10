#!/bin/bash

#SBATCH -J simulation_bavaria_job

# Output and error files
#SBATCH --output=simulation_bavaria_output_%j.log
#SBATCH --error=simulation_bavaria_error_%j.log

#SBATCH --nodes=1
#SBATCH --ntasks-per-node=1
#SBATCH --cpus-per-task=12
#SBATCH --mem=80GB
#SBATCH --time=00:30:00

# Account and partition
#SBATCH --account=pn39mu
#SBATCH --partition=test

# Notification and type
#SBATCH --mail-type=BEGIN,END
#SBATCH --mail-user=elena.natterer@tum.de
#SBATCH --get-user-env

# Set up of execution environment
#SBATCH --export=none

#export HOME=/dss/dsshome1/06/ge49wav3
export FONTCONFIG_PATH=$HOME/test_java_font_4u/fonts
export FONTCONFIG_FILE=$HOME/test_java_font_4u/fonts/fonts.conf
export FC_FONT_PATH=$HOME/test_java_font_4u/fonts

# Run the Java program with full arguments
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city augsburg_test --seeds 1 --threads 12 --memory 80
