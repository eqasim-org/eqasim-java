'''
This file contains the functions for creating the subgraphs for a given city.
Steps:
1. Create the hexagon grid for the city
2. Create the subgraphs for each road type
3. Create the scenario networks
4. Cross check the first created scenario
5. Plot the check for the first created scenario

Folder structure for input data:
data/
├── city_boundaries/
│   └── augsburg/
│       ├── augsburg.json
│
├── simulation_data_per_city_new/
│   └── augsburg/
│       ├── augsburg_network.xml.gz
│
├── simulation_output/
│   └── output_links.csv.gz


Folder structure for output data:
data/
└── subgraph_check/
    ├── hexagon/
    │   └── augsburg/
    │       ├── plots/
    │       └── data/
    ├── centrality/
    │   └── augsburg/
    │       ├── csv/
    │       └── plots/
    └── network_files/
        └── augsburg/
            ├── subgraphs/
            │   └── augsburg_seed_13/
            │       └── networks/
            │           └── networks_0/
            └── validation/
'''

# Standard library imports
import os
import gzip
import logging
import multiprocessing as mp
from collections import Counter
from functools import reduce
from itertools import islice
from pathlib import Path
import random
import sys
from lxml import etree as ET
import re

# Third-party imports
import networkx as nx
import numpy as np
import pandas as pd
import geopandas as gpd
import osmnx as ox
import seaborn as sns
from shapely import wkt
from shapely.geometry import LineString, box
import shapely.geometry as sgeo
import matplotlib.pyplot as plt
from matplotlib.lines import Line2D
from matplotlib.patches import Patch
from matplotlib.colors import Normalize
from concurrent.futures import ProcessPoolExecutor, as_completed
from tqdm import tqdm
from typing import Dict, List, Tuple, Set, Optional
import shutil
import time
from datetime import datetime

# Local imports
import network_io as nio
from hexagon_creation_and_plot import matsim_network_input_to_gdf,clean_duplicates_based_on_modes,create_nodes_dict, multipolygon_to_polygon, modify_geodataframe, merge_edges_and_zones, generate_hexagon_grid,merge_edges_and_hexagon_grid,consolidate_road_types, check_hexagon_statistics, plot_grid_and_edges,convert_and_save_geodataframe
from betweenness_and_closeness import edge_closeness_centrality, analyze_centrality_measures,create_network_from_csv,verify_components

def setup_output_directories(base_dir, city_name, seed_number):
    """
    Create structured output directories for different purposes.
    Each city has its own structure, with subgraphs having additional seed hierarchy.
    
    Parameters:
        base_dir: Path object for the base directory
        city_name: Name of the city (e.g., 'Augsburg')
        seed_number: Seed number (e.g., 1)
    """
    # Keep the original base path
    output_base_path = base_dir / "data" / "subgraph_check"
    
    # Create city-specific seed directory name
    city_seed_dir = f"{city_name}_seed_{seed_number}"
    
    # Create specific output directories under output_base_path with city subdirectories
    output_paths = {
        'hexagon': output_base_path / "hexagon" / city_name,
        'centrality': output_base_path / "centrality" / city_name,
        'network_files': output_base_path / "network_files" / city_name,
        'networks': output_base_path / "network_files" / city_name / "subgraphs" / city_seed_dir / "networks" / "networks_0",
        'validation': output_base_path / "network_files" / city_name / "validation"
    }
    
    # Create all directories and their subdirectories
    for path in output_paths.values():
        path.mkdir(parents=True, exist_ok=True)
        
        if 'hexagon' in str(path):
            (path / "plots").mkdir(exist_ok=True)
            (path / "data").mkdir(exist_ok=True)
        elif 'centrality' in str(path):
            (path / "csv").mkdir(exist_ok=True)
            (path / "plots").mkdir(exist_ok=True)
    
    # Return complete path dictionary
    return {
        'base': output_base_path,
        'hexagon_plots': output_paths['hexagon'] / "plots",
        'hexagon_data': output_paths['hexagon'] / "data",
        'centrality_csv': output_paths['centrality'] / "csv",
        'centrality_plots': output_paths['centrality'] / "plots",
        'network_files': output_paths['network_files'],
        'networks': output_paths['networks'],
        'validation': output_paths['validation']
    }
    

### Settings for filepath, working directory and output path #########################################################

#os.chdir(r"C:\Users\nktba\bavaria_simulations") # Set working directory
#base_dir = os.getcwd()
base_dir = Path(__file__).resolve().parent.parent.parent
#administrative_boundary_json_path = os.path.join(base_dir, "data", "output", "boundary_files", "Augsburg.json")
administrative_boundary_json_path = base_dir / "data" / "city_boundaries" / "augsburg" / "augsburg.json"
matsim_network_file_path = base_dir / "data" / "simulation_data_per_city_new" / "augsburg" / "augsburg_network.xml.gz"
#csv_dir = Path(r"C:\Users\nktba\misc\simulation_outputs")
#csv_filepath = csv_dir / f"augsburg"/f"augsburg_seed_1/output_links.csv.gz"
csv_filepath=  base_dir / "data" / "simulation_output" / 'output_links.csv.gz'
#csv_output_path = csv_dir/f"augsburg"/f"augsburg_seed_1/augsburg_output_links.csv"
#output_base_path = os.path.join(base_dir, "data", "output", "network_files")
output_base_path = base_dir / "data"/ "subgraphs"
#Define the path to the check output subgraph
#check_output_subgraph_path = os.path.join(base_dir, "data", "output", "network_files", "Augsburg", "networks", "networks_0", "network_residential_n7_s1.xml.gz")

######## Control Center for Variables #################################################################################

hexagon_size = 1000  # Size in meters for EPSG:25832 and in degrees for EPSG:4326 **********VERY IMPORTANT********** 
capacity_tuning_factor = 0.5 #This is the factor by which the capacity of the links is reduced
betweenness_centrality_cutoff = 0.8 # Take the lowest 80% of the links based on betweenness centrality
closeness_centrality_cutoff = 0.2 # Take the highest 80% of the links based on closeness centrality
target_size = 20 #total number of subgraphs to be created
distribution_mean_factor = 5
distribution_std_factor = 10 # for n denoting the number of hexagons, we create subgraphs whose length follows a normal distribution with mean (n/distribution_mean_factor and std dev (n/distribution_std_factor)
seed_number = 73 #This is the seed number for the random number generator
######## City Names #######################################################################################################
city_name = 'augsburg'
########################################################################################################################
output_dirs = setup_output_directories(base_dir, city_name, seed_number)

# For hexagon plots
output_file = output_dirs['hexagon_plots'] / f'{city_name}_network_hexagon_districts.png'

# For centrality analysis
csv_output = output_dirs['centrality_csv'] / 'centrality_measures.csv'

# For networks (with city-specific seed hierarchy)
subgraph_output = output_dirs['networks'] / f'{city_name}_network_residential_n7_s1.xml.gz'


########################################################################################################################
def generate_road_type_specific_subsets(gdf_edges_with_hex, city_name, seed_number, target_size, 
                                        distribution_mean_factor=distribution_mean_factor, 
                                        distribution_std_factor=distribution_std_factor,
                                        betweenness_centrality_cutoff=betweenness_centrality_cutoff,
                                        closeness_centrality_cutoff=closeness_centrality_cutoff):
    """
    Generate unique subsets of hexagon IDs for each road type, where the total number of subsets
    is target_size, distributed evenly across road types.
    
    Parameters:
    -----------
    gdf_edges_with_hex : GeoDataFrame
        GeoDataFrame containing road network data with hexagon assignments and highway types
    city_name : str
        Name of the city (e.g., 'augsburg')
    seed_number : int
        Seed number from city_seed_X directory structure
    target_size : int
        Total number of subsets to generate (will be divided among road types)
    distribution_mean_factor : int, optional
        Factor to calculate mean subset size (default: 5)
    distribution_std_factor : int, optional
        Factor to calculate standard deviation of subset size (default: 10)
    
    Returns:
    --------
    dict : Dictionary mapping each road type to its list of hexagon ID subsets
    """
    # Set the seed for reproducibility using the seed_number from directory structure
    np.random.seed(seed_number)
    random.seed(seed_number)
    gdf_filtered = gdf_edges_with_hex[gdf_edges_with_hex['is_in_stadt'] == 1].copy()
    closeness_cutoff = gdf_filtered['closeness'].quantile(closeness_centrality_cutoff)
    betweenness_cutoff = gdf_filtered['betweenness'].quantile(betweenness_centrality_cutoff)
    print(f"Betweenness centrality cutoff (80th percentile): {betweenness_cutoff}")
    print(f"Closeness centrality cutoff (80th percentile): {closeness_cutoff}")
    
    # Analyze hexagons per road type for edges meeting centrality criteria
    print("\nAnalyzing hexagons per road type for edges meeting centrality criteria:")
    centrality_mask = (gdf_filtered['betweenness'] < betweenness_cutoff) & (gdf_filtered['closeness'] > closeness_cutoff)
    edges_meeting_criteria = gdf_filtered[centrality_mask]
    
    # Create a dictionary to store hexagon counts per road type
    hexagons_per_road_type = {}
    
    target_road_types = ['primary', 'secondary', 'tertiary', 'residential']
    road_types = [rt for rt in target_road_types if rt in edges_meeting_criteria['consolidated_road_type'].unique()]
    
    # First, collect all hexagons for each road type
    road_type_hexagons = {}
    for road_type in road_types:
        # Get edges of this road type
        road_type_edges = edges_meeting_criteria[edges_meeting_criteria['consolidated_road_type'] == road_type]
        
        # Get all unique hexagons for this road type
        hexagons = set()
        for hex_list in road_type_edges['hexagon']:
            if isinstance(hex_list, list):
                hexagons.update(hex_list)
        
        # Store the hexagons for this road type
        road_type_hexagons[road_type] = sorted(list(hexagons))
        
        # Store the count
        hexagons_per_road_type[road_type] = len(hexagons)
        
        # Print the results
        print(f"Road type: {road_type}")
        print(f"Number of edges meeting criteria: {len(road_type_edges)}")
        print(f"Number of unique hexagons: {len(hexagons)}")
        print(f'Unique hexagons: {hexagons}')
        print("-" * 50)
    
    print(f"\nGenerating subsets for {city_name} with seed {seed_number}")
    
    # Calculate number of subsets per road type
    subsets_per_type = target_size // len(road_types)
    
    # Get all unique hexagons across all road types
    unique_hexagons = set()
    for hex_list in road_type_hexagons.values():
        unique_hexagons.update(hex_list)
    unique_hexagons = sorted(list(unique_hexagons))
    
    # Calculate target mean and std dev based on all unique hexagons
    target_mean = len(unique_hexagons) / distribution_mean_factor
    std_dev = len(unique_hexagons) / distribution_std_factor
    
    # Dictionary to store subsets for each road type
    road_type_subsets = {}
    
    for road_type in road_types:
        print(f"Generating subsets for road type: {road_type}")
        # Get hexagons that contain this road type
        road_type_hexagons_list = road_type_hexagons[road_type]
        if not road_type_hexagons_list:
            print(f"Warning: No hexagons found for road type {road_type}")
            continue
            
        # Generate subsets for this road type
        unique_subsets = set()
        
        while len(unique_subsets) < subsets_per_type:
            # Sample subset size from normal distribution using combined target mean and std dev
            subset_size = max(1, int(np.random.normal(target_mean, std_dev)))
            subset_size = min(subset_size, len(road_type_hexagons_list))
            
            # Randomly choose subset_size hexagons from this road type's hexagons
            subset = tuple(sorted(random.sample(road_type_hexagons_list, subset_size)))
            unique_subsets.add(subset)
        
        road_type_subsets[road_type] = list(unique_subsets)
        print(f"Generated {len(road_type_subsets[road_type])} subsets for {road_type}")
    
    # Calculate overall statistics
    all_subsets = [subset for subsets in road_type_subsets.values() for subset in subsets]
    overall_mean = np.mean([len(s) for s in all_subsets])
    
    print(f"\nSubset Generation Summary for {city_name}:")
    print(f"Total number of unique hexagons: {len(unique_hexagons)}")
    print(f"Target mean based on unique hexagons: {target_mean:.2f}")
    print(f"Target std dev based on unique hexagons: {std_dev:.2f}")
    print(f"Total number of subsets: {len(all_subsets)}")
    print(f"Overall actual mean subset length: {overall_mean:.2f}")
    print(f"Number of road types: {len(road_types)}")
    print(f"Subsets per road type: {subsets_per_type}")
    print(f"City: {city_name}")
    print(f"Seed number: {seed_number}")
    
    return road_type_subsets


def generate_scenario_labels(road_type_subsets):
    """
    Generate meaningful labels for scenario combinations based on active road types.
    
    Parameters:
    -----------
    road_type_subsets : dict
        Dictionary mapping each road type to its list of hexagon ID subsets
    
    Returns:
    --------
    dict : Dictionary mapping scenario indices to their labels
    """
    # Get all road types
    road_types = list(road_type_subsets.keys())
    
    # Create a mapping for scenario labels
    scenario_labels = {}
    
    # For each road type
    for road_type in road_types:
        # For each subset in this road type
        for i, subset in enumerate(road_type_subsets[road_type]):
            # Create a label that includes:
            # 1. City name  
            # 2. Road type
            # 3. Number of hexagons in the subset
            # 4. Scenario number
            label = f"{city_name}_{road_type}_n{len(subset)}_s{i+1}"
            scenario_labels[(road_type, i)] = label
    
    return scenario_labels


def create_scenario_networks(matsim_network_file_path,gdf_edges_with_hex, road_type_subsets, scenario_labels, 
                              city_name, seed_number, output_dirs, nodes_dict, network_attrs, link_attrs,
                              capacity_tuning_factor=capacity_tuning_factor,
                              betweenness_centrality_cutoff=betweenness_centrality_cutoff,
                              closeness_centrality_cutoff=closeness_centrality_cutoff):
    """
    Create network.xml.gz files for each scenario by modifying only the capacity of specific links
    in the existing MATSim network file.
    """
    # Get the networks directory from output_dirs
    networks_base = output_dirs['networks']
    
    # Create seed-specific directory
    seed_dir = networks_base / f"seed_{seed_number}"
    seed_dir.mkdir(parents=True, exist_ok=True)
    
    print(f"\nCreating scenario networks for {city_name} (seed {seed_number})")
    print(f"Output directory: {seed_dir}")
    
    # Calculate the 80th percentile for closeness centrality
    gdf_filtered = gdf_edges_with_hex[gdf_edges_with_hex['is_in_stadt'] == 1].copy()
    closeness_cutoff = gdf_filtered['closeness'].quantile(closeness_centrality_cutoff)
    betweenness_cutoff = gdf_filtered['betweenness'].quantile(betweenness_centrality_cutoff)
    print(f"Betweenness centrality cutoff (80th percentile): {betweenness_cutoff}")
    print(f"Closeness centrality cutoff (80th percentile): {closeness_cutoff}")
    
    # Counter for total scenarios
    total_scenarios = 0
    first_scenario_path = None
    
    parser = ET.XMLParser(remove_blank_text=True)
    
    # Process each road type and its scenarios
    for road_type, subsets in road_type_subsets.items():
        print(f"\nProcessing road type: {road_type}")
        
        # First, get all edges of this road type in the STADT AREA that meet centrality conditions
        road_type_mask = (
            (gdf_filtered['consolidated_road_type'] == road_type) &
            (gdf_filtered['betweenness'] < betweenness_cutoff) &
            (gdf_filtered['closeness'] > closeness_cutoff)
        )
        road_type_edges = gdf_filtered[road_type_mask].copy() #all the edges in the stadt area & road type that meet the conditions
        
        for i, subset in enumerate(subsets):
            # Get the scenario label
            label = scenario_labels[(road_type, i)]
            
            # Calculate which folder this scenario belongs to (1000 files per folder)
            folder_number = (total_scenarios // 1000) * 1000
            folder_name = f"networks_{folder_number}"
            folder_path = seed_dir / folder_name
            folder_path.mkdir(exist_ok=True)
            
            # Create the network file name with seed number
            network_filename = f"network_seed{seed_number}_{label}.xml.gz"
            network_path = folder_path / network_filename
            
            # Store the path of the first scenario
            if first_scenario_path is None:
                first_scenario_path = network_path
            
            # Get edges in scenario hexagons for this road type, that are in the subset and meet the conditions
            scenario_mask = road_type_edges['hexagon'].apply(
                lambda x: any(h in subset for h in x) if isinstance(x, list) else False
            )
            
            scenario_edges = road_type_edges[scenario_mask] #all the edges in the subset that meet the conditions
            
            all_road_type_edges_in_scenario_hexagons = gdf_filtered[    
                (gdf_filtered['hexagon'].apply(
                    lambda x: any(h in subset for h in x) if isinstance(x, list) else False
                ))
            ]
            
            # Get edges in hexagons for current road type
            current_road_type_edges_in_scenario_hexagons = gdf_filtered[ 
                (gdf_filtered['hexagon'].apply(
                    lambda x: any(h in subset for h in x) if isinstance(x, list) else False
                )) & 
                (gdf_filtered['consolidated_road_type'] == road_type)
            ]
            
            edges_with_reduced_capacity = len(scenario_edges)
            
            print(f"\nScenario {label} statistics:")
            print(f"Edge count in scenario hexagons (all road types): {len(all_road_type_edges_in_scenario_hexagons)}")
            print(f"Edge count in scenario hexagons (current road type): {len(current_road_type_edges_in_scenario_hexagons)}")
            print(f"Edges with reduced capacity (current road type): {edges_with_reduced_capacity}")
            
            # Only calculate percentage if there are edges of this road type in the hexagons
            if len(current_road_type_edges_in_scenario_hexagons) > 0:
                print(f"Percentage of edges with reduced capacity: {(edges_with_reduced_capacity/len(current_road_type_edges_in_scenario_hexagons))*100:.2f}%")
            else:
                print("No edges of this road type in the selected hexagons")
            
            # Read the original network file
            with gzip.open(matsim_network_file_path, 'rt', encoding='utf-8') as f:
                xml_str = f.read()
            
            # Create a set of tuples (link_id, from_node, to_node) that need capacity reduction
            links_to_modify = set(zip(scenario_edges['link'].values, 
                                     scenario_edges['from_node'].values, 
                                     scenario_edges['to_node'].values))
            
            # Modify only the capacity of the specified links
            for link_id, from_node, to_node in links_to_modify:
            # Match the specific link line
                pattern = (
                rf'(<link[^>]*\bid="{link_id}"[^>]*\bfrom="{from_node}"[^>]*\bto="{to_node}"[^>]*capacity=")([^"]+)(")'
                )
    
            def replace_capacity(match):
                original = float(match.group(2))
                new_capacity = original * capacity_tuning_factor
                return f'{match.group(1)}{new_capacity}{match.group(3)}'
    
            xml_str = re.sub(pattern, replace_capacity, xml_str)
            
            # Write the modified XML to a new gzipped file
            with gzip.open(network_path, 'wt', encoding='utf-8') as f:
                f.write(xml_str)
            
            # Verify the file was created
            if not network_path.exists():
                print(f"Warning: Failed to create file {network_path}")
            else:
                print(f"Created scenario file: {network_path}")
            
            total_scenarios += 1
            
            # Print progress more frequently
            if total_scenarios % 5 == 0:  # Print progress every 5 scenarios
                print(f"Created {total_scenarios} network files...")
    
    print(f"\nFinished creating {total_scenarios} network files for {city_name} (seed {seed_number})")
    print(f"Files are organized in folders under: {seed_dir}")
    
    return first_scenario_path


def plot_check_for_created_networks(check_output_subgraph_path, zones_gdf, hexagon_grid_all, 
                                    gdf_edges_with_hex, scenario_labels, road_type_subsets, output_dirs=None):
    """
    Plot and verify a single created network file by visualizing its scenario edges, road types, and hexagons.
    This is a validation function to check if the network creation process worked correctly.
    
    Parameters:
    -----------
    check_output_subgraph_path : Path or str
        Path to the specific network file to check
    zones_gdf : GeoDataFrame
        GeoDataFrame containing zone boundaries
    hexagon_grid_all : GeoDataFrame
        GeoDataFrame containing all hexagons
    gdf_edges_with_hex : GeoDataFrame
        Original network data with hexagon assignments
    scenario_labels : dict
        Dictionary mapping scenario indices to their labels
    road_type_subsets : dict
        Dictionary mapping road types to their hexagon subsets
    output_dirs : dict, optional
        Dictionary containing output directory paths. If provided, will save the plot.
    """
    # Load the network file
    matsim_network, nodes_subgraph, edges_subgraph,network_attrs, link_attrs = matsim_network_input_to_gdf(check_output_subgraph_path)
    
    # Match the highway_consolidated column from gdf_edges_with_hex to matsim_network
    highway_mapping = dict(zip(gdf_edges_with_hex['link'], gdf_edges_with_hex['consolidated_road_type']))
    matsim_network['consolidated_road_type'] = matsim_network['id'].map(highway_mapping)
    
    fig, ax = plt.subplots(figsize=(15, 15))
    
    # Plot the districts
    zones_gdf.plot(ax=ax, 
                      column='zone_id',
                      cmap='YlGnBu',
                      alpha=0.3,
                      legend=False,
                      legend_kwds={'label': 'District Area (m²)'},
                      label='Districts')
    
    # Plot hexagons
    hexagon_grid_all.plot(ax=ax, color='none', edgecolor='green', alpha=0.3, label='Hexagons')
    
    # Get scenario label from filename (handling both old and new filename formats)
    filename = Path(check_output_subgraph_path).name
    if 'seed' in filename:
        # New format: network_seedX_label.xml.gz
        scenario_label = filename.split('_', 2)[2].split('.xml.gz')[0]
    else:
        # Old format: network_label.xml.gz
        scenario_label = filename.replace('network_', '').split('.xml.gz')[0]
    
    key = next((k for k, v in scenario_labels.items() if v == scenario_label), None)
    if key is None:
        print(f"Warning: Could not find matching scenario label for {scenario_label}")
        return matsim_network
        
    hex_ids = road_type_subsets[key[0]][key[1]]
    
    # Create mask for scenario edges
    scenario_mask = gdf_edges_with_hex['hexagon'].apply(
        lambda x: any(h in hex_ids for h in x) if isinstance(x, list) else False
    )
    
    # Plot parent network edges (not in scenario)
    parent_edges = matsim_network[~matsim_network['id'].isin(gdf_edges_with_hex[scenario_mask]['link'])]
    parent_edges.plot(ax=ax, 
                     color='gray',
                     linewidth=0.5,
                     alpha=0.5,
                     label='Parent Network')
    
    # Plot all scenario edges
    scenario_edges = matsim_network[matsim_network['id'].isin(gdf_edges_with_hex[scenario_mask]['link'])]
    scenario_edges.plot(ax=ax, 
                       color='red',
                       linewidth=0.5,
                       alpha=0.8,
                       label='Scenario Edges')
    
    # Plot scenario edges that match the road type from scenario label
    road_type = key[0]  # Get the road type from the key
    matching_edges = scenario_edges[scenario_edges['consolidated_road_type'] == road_type]
    matching_edges.plot(ax=ax,
                       color='blue',
                       linewidth=0.5,
                       alpha=0.8,
                       label=f'Road Type: {road_type}')
    
    legend_elements = [
        Patch(facecolor='none', edgecolor='green', alpha=0.3, label='Hexagons'),
        Line2D([0], [0], color='gray', linewidth=0.5, alpha=0.5, label='Parent Network'),
        Line2D([0], [0], color='red', linewidth=0.5, alpha=0.8, label='Scenario Edges'),
        Line2D([0], [0], color='blue', linewidth=2, alpha=0.8, label=f'Road Type: {road_type}')
    ]

    ax.legend(handles=legend_elements)
    plt.title(f'MATSim Network with Scenario Edges\n{scenario_label}')
    plt.axis('equal')
    
    # Save the plot if output_dirs is provided
    if output_dirs is not None:
        plot_path = output_dirs['validation'] / f'network_check_{scenario_label}.png'
        plt.savefig(plot_path, dpi=300, bbox_inches='tight')
        print(f"Validation plot saved to: {plot_path}")
    else:
        plt.show()
    
    plt.close()
    
    # Print some validation statistics
    print(f"\nValidation Statistics for {scenario_label}:")
    print(f"Total edges in network: {len(matsim_network)}")
    print(f"Edges in scenario hexagons: {len(scenario_edges)}")
    print(f"Edges matching road type '{road_type}': {len(matching_edges)}")
    print(f"Number of hexagons in subset: {len(hex_ids)}")
    
    return matsim_network

def cross_check_for_created_networks(check_output_subgraph_path, gdf_edges_with_hex, road_type_subsets, 
                                     scenario_labels, seed_number=None):
    """
    Cross check the created network files to verify:
    1. Which edges are in the selected hexagons
    2. Which edges match the road type
    3. How capacities have been modified
    
    Parameters:
    -----------
    check_output_subgraph_path : Path or str
        Path to the specific network file to check
    gdf_edges_with_hex : GeoDataFrame
        Original network data with hexagon assignments
    road_type_subsets : dict
        Dictionary mapping road types to their hexagon subsets
    scenario_labels : dict
        Dictionary mapping scenario indices to their labels
    seed_number : int, optional
        Seed number from city_seed_X directory structure
    """
    # Load the network file
    matsim_network, nodes_subgraph, edges_subgraph,network_attrs, link_attrs = matsim_network_input_to_gdf(check_output_subgraph_path)
    
    # Get scenario information (handling both old and new filename formats)
    filename = Path(check_output_subgraph_path).name
    if 'seed' in filename:
        # New format: network_seedX_label.xml.gz
        scenario_label = filename.split('_', 2)[2].split('.xml.gz')[0]
    else:
        # Old format: network_label.xml.gz
        scenario_label = filename.replace('network_', '').split('.xml.gz')[0]
    
    key = next((k for k, v in scenario_labels.items() if v == scenario_label), None)
    if key is None:
        print(f"Warning: Could not find matching scenario label for {scenario_label}")
        return None, None, None
        
    hex_ids = road_type_subsets[key[0]][key[1]]
    
    # Get edges in selected hexagons
    mask = gdf_edges_with_hex['hexagon'].apply(
        lambda x: any(hex_id in x for hex_id in hex_ids) if isinstance(x, list) else False
    )
    edges_in_selected_hexagon = gdf_edges_with_hex[mask]
    
    # Get edges that match both hexagon and road type
    edges_in_selected_hexagon_and_road_type = edges_in_selected_hexagon[
        edges_in_selected_hexagon['consolidated_road_type'] == key[0]
    ]
    
    # Create a comparison DataFrame for the capacity changes
    comparison_df = pd.DataFrame({
        'edge_id': edges_in_selected_hexagon['link'],
        'road_type': edges_in_selected_hexagon['consolidated_road_type'],
        'original_capacity': edges_in_selected_hexagon['capacity'],
        'modified_capacity': edges_in_selected_hexagon['link'].map(
            lambda x: matsim_network.loc[matsim_network['id'] == x, 'capacity'].iloc[0] 
            if x in matsim_network['id'].values else None
        ),
        'capacity_reduced': edges_in_selected_hexagon['consolidated_road_type'] == key[0]
    })
    
    # Remove rows where modified_capacity is None (edges that don't exist in the modified network)
    comparison_df = comparison_df.dropna(subset=['modified_capacity'])
    
    # Print summary statistics
    print(f"\nCross-check Summary for {scenario_label}:")
    print(f"Total edges in selected hexagons: {len(edges_in_selected_hexagon)}")
    print(f"Edges matching road type '{key[0]}': {len(edges_in_selected_hexagon_and_road_type)}")
    print(f"Number of hexagons in subset: {len(hex_ids)}")
    if seed_number is not None:
        print(f"Seed number: {seed_number}")
    
    return edges_in_selected_hexagon_and_road_type, edges_in_selected_hexagon, comparison_df


def main():
    #### Hexagon Creation ################################################################################
    matsim_network, nodes, df_edges, network_attrs, link_attrs = matsim_network_input_to_gdf(matsim_network_file_path)
    cleaned_network = clean_duplicates_based_on_modes(csv_filepath)
    cleaned_network['geometry'] = cleaned_network['geometry'].apply(wkt.loads)
    
    # Create a lookup dictionary for matsim_network attributes
    matsim_attrs = {row['id']: row for _, row in matsim_network.iterrows()}
    
    # Add MATSim network attributes to cleaned_network
    cleaned_network['permlanes'] = cleaned_network['link'].map(lambda x: matsim_attrs[x]['permlanes'])
    cleaned_network['oneway'] = cleaned_network['link'].map(lambda x: matsim_attrs[x]['oneway'])
    
    # Create GeoDataFrame with all attributes
    cleaned_network = gpd.GeoDataFrame(cleaned_network, geometry='geometry', crs='EPSG:25832')
    nodes_dict = create_nodes_dict(cleaned_network)
    
    gdf = gpd.read_file(administrative_boundary_json_path)
    #modify the zones geodataframe
    zones_gdf = modify_geodataframe(gdf)
    #merge the edges with the zones
    gdf_edges_with_zones = merge_edges_and_zones(cleaned_network, zones_gdf)
    #generate the hexagon grid for the polygon
    gdf_edges_with_hex,hexagon_grid_all = merge_edges_and_hexagon_grid(zones_gdf, hexagon_size ,
                                                                      gdf_edges_with_zones ,
                                                                              projection='EPSG:25832')
    #consolidate the road types
    gdf_edges_with_hex['consolidated_road_type'] = gdf_edges_with_hex['osm:way:highway'].apply(consolidate_road_types)
    #check the hexagon statistics
    check_hexagon_statistics(gdf_edges_with_hex, hexagon_grid_all)
    #plot the grid and the edges
    plot_grid_and_edges(gdf_edges_with_hex, hexagon_grid_all,zones_gdf,output_dirs,city_name)
    # Save the GeoDataFrame using the new function
    convert_and_save_geodataframe(gdf_edges_with_hex, output_dirs['hexagon_data'] / f'{city_name}_hexagon_edges.geojson')
    
    #calculate the betweenness and closeness centrality####################################################
    
    centrality_df, gdf_edges_with_hex, G = analyze_centrality_measures(gdf_edges_with_hex, output_dirs, city_only=True)
    size_counts, largest_component= verify_components(G) 
       
    #### Subgraph Creation ###############################################################################
    
    #generate the road type specific subsets
    road_type_subsets = generate_road_type_specific_subsets(gdf_edges_with_hex, city_name, seed_number, target_size,
                                                            distribution_mean_factor=distribution_mean_factor, 
                                                            distribution_std_factor=distribution_std_factor,
                                                            betweenness_centrality_cutoff=betweenness_centrality_cutoff,
                                                            closeness_centrality_cutoff=closeness_centrality_cutoff)
    #generate the scenario labels
    scenario_labels = generate_scenario_labels(road_type_subsets)
    #create the scenario networks and get the first scenario path
    first_scenario = create_scenario_networks(matsim_network_file_path, gdf_edges_with_hex, road_type_subsets, scenario_labels, 
                                                    city_name=city_name, seed_number=seed_number, 
                                                    output_dirs=output_dirs, nodes_dict=nodes_dict, network_attrs=network_attrs,
                                                    link_attrs=link_attrs, capacity_tuning_factor=capacity_tuning_factor,
                                                    betweenness_centrality_cutoff=betweenness_centrality_cutoff,
                                                    closeness_centrality_cutoff=closeness_centrality_cutoff)
    
    #### Check the created networks #######################################################################
    print(f"\nChecking first created scenario: {first_scenario.name}")
    
    #plot the check for the created networks
    matsim_network = plot_check_for_created_networks(
        check_output_subgraph_path=first_scenario,
        zones_gdf=zones_gdf,
        hexagon_grid_all=hexagon_grid_all,
        gdf_edges_with_hex=gdf_edges_with_hex,
        scenario_labels=scenario_labels,
        road_type_subsets=road_type_subsets,
        output_dirs=output_dirs
    )   
    
    #cross check the created networks
    edges_with_road_type, edges_in_hexagons, capacity_changes = cross_check_for_created_networks(
        check_output_subgraph_path=first_scenario,
        gdf_edges_with_hex=gdf_edges_with_hex,
        road_type_subsets=road_type_subsets,
        scenario_labels=scenario_labels,
        seed_number=seed_number
    )  
    
    print("\nDetailed Cross-Check Results:")
    print("\nEdges with matching road type:")
    print(edges_with_road_type)
    print("\nEdges in selected hexagons:")
    print(edges_in_hexagons)
    print("\nCapacity changes:")
    print(capacity_changes)
    
if __name__ == "__main__":
    main()

