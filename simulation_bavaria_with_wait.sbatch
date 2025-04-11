#!/bin/bash

#SBATCH -J simulation_bavaria_job

# Output and error files
#SBATCH --output=simulation_bavaria_output_%j.log
#SBATCH --error=simulation_bavaria_error_%j.log

#SBATCH --nodes=2
#SBATCH --ntasks-per-node=48
#SBATCH --cpus-per-task=1
#SBATCH --mem=80GB
#SBATCH --time=02:30:00

# Account and partition
#SBATCH --account=pn39mu
#SBATCH --partition=micro

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
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 1 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 2 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 3 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 4 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 5 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 6 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 7 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 8 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 9 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 10 --threads 1 --memory 1 &

java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 11 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 12 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 13 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 14 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 15 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 16 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 17 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 18 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 19 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 20 --threads 1 --memory 1 &

java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 21 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 22 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 23 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 24 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 25 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 26 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 27 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 28 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 29 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 30 --threads 1 --memory 1 &

java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 31 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 32 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 33 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 34 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 35 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 36 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 37 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 38 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 39 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 40 --threads 1 --memory 1 &

java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 41 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 42 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 43 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 44 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 45 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 46 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 47 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 48 --threads 1 --memory 1 &

java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 49 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 50 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 51 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 52 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 53 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 54 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 55 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 56 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 57 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 58 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 59 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 60 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 61 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 62 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 63 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 64 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 65 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 66 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 67 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 68 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 69 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 70 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 71 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 72 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 73 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 74 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 75 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 76 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 77 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 78 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 79 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 80 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 81 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 82 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 83 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 84 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 85 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 86 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 87 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 88 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 89 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 90 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 91 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 92 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 93 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 94 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 95 --threads 1 --memory 1 &
java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city ingolstadt --seeds 96 --threads 1 --memory 1 &

wait

echo "All simulations completed"
