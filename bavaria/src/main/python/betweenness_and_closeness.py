'''
This file contains the functions for calculating the edge betweenness and edge closeness centrality of the 
network for a given city.   
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
import xml.etree.ElementTree as ET


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



# Set up logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)


def create_network_from_csv(df) -> nx.DiGraph:
    '''
    This function creates a NetworkX directed graph from a CSV file containing link information.
    input:
        df: DataFrame containing the link information
    output:
        G: NetworkX directed graph
    '''
    try:
        print(f"\nNumber of edges in CSV: {len(df)}")
        print(f"Number of unique from_nodes: {df['from_node'].nunique()}")
        print(f"Number of unique to_nodes: {df['to_node'].nunique()}")
        print(f"Number of unique links: {df['link'].nunique()}")
        
        # Print the first few rows to see what we're working with
        logger.info("First few rows of the CSV:")
        logger.info(df.head())
        
        # Print the column names
        logger.info("\nColumn names:")
        logger.info(df.columns.tolist())
        
        # Create a directed graph
        G = nx.DiGraph()
        
        # Add edges to the graph
        for _, row in df.iterrows():
            G.add_edge(
                row['from_node'],
                row['to_node'],
                length=row['length'],  # Use length as weight
                id=row['link']
            )
        logger.info(f"Number of nodes: {G.number_of_nodes()}")
        logger.info(f"Number of edges: {G.number_of_edges()}")
        logger.info(f"Is directed: {G.is_directed()}")
        
        return G
        
    except Exception as e:
        logger.error(f"Error creating network: {e}")
        raise

def edge_closeness_centrality(G, centrality_weight='length'):
    '''
    This function calculates the edge closeness centrality of the network
    input:
        G: NetworkX directed graph
        centrality_weight: Weight of the centrality
    output:
        edge_closeness: Dictionary containing the edge closeness centrality
    '''
    edge_closeness = {}
    
    print("\n=== Edge Closeness Centrality Calculation ===")
    print(f"Total nodes: {G.number_of_nodes()}")
    print(f"Total edges: {G.number_of_edges()}")

    # Get strongly connected components
    scc = list(nx.strongly_connected_components(G))
    node_to_component = {}
    for component in scc:
        for node in component:
            node_to_component[node] = component

    # Calculate component sizes
    component_sizes = {node: len(component) for node, component in node_to_component.items()}

    print("\nCalculating edge centrality...")
    total_edges = G.number_of_edges()
    processed_edges = 0
    zero_centrality = 0
    non_zero_centrality = 0

    for u, v in G.edges():
        processed_edges += 1
        if processed_edges % 1000 == 0:
            print(f"Processed {processed_edges}/{total_edges} edges...")

        # Get component size
        component_size = component_sizes[u]
        
        # Only handle isolated nodes differently
        if component_size == 1:  # Isolated node
            edge_closeness[(u, v)] = 0.0000000
            zero_centrality += 1
            continue

        # Calculate shortest paths from source node
        dist_u = nx.single_source_dijkstra_path_length(G, u, weight=centrality_weight)
        
        # Calculate total distance and count reachable nodes
        total_distance = 0
        reachable_count = 0
        
        for node in node_to_component[u]:
            if node != u and node != v:  # Exclude source and target nodes
                distance = dist_u.get(node, 0)
                if distance > 0:  # Only count actually reachable nodes
                    total_distance += distance
                    reachable_count += 1

        # Calculate normalized centrality
        if reachable_count > 0:
            average_distance = total_distance / reachable_count
            centrality = round(1 / average_distance, 7) if average_distance > 0 else 0.0000000
        else:
            centrality = 0.0000000

        edge_closeness[(u, v)] = centrality
        
        if centrality > 0:
            non_zero_centrality += 1
        else:
            zero_centrality += 1

    print("\n=== Results Summary ===")
    print(f"Total edges processed: {processed_edges}")
    print(f"Edges with non-zero centrality: {non_zero_centrality}")
    print(f"Edges with zero centrality: {zero_centrality}")
    print(f"Percentage of edges with non-zero centrality: {(non_zero_centrality/processed_edges)*100:.2f}%")
    
    # Print centrality statistics for non-zero values
    centrality_values = [v for v in edge_closeness.values() if v > 0]
    if centrality_values:
        print("\nCentrality Statistics (non-zero values only):")
        print(f"Average centrality: {sum(centrality_values)/len(centrality_values):.7f}")
        print(f"Max centrality: {max(centrality_values):.7f}")
        print(f"Min centrality: {min(centrality_values):.7f}")
    else:
        print("\nNo edges with non-zero centrality found!")

    return edge_closeness


def plot_centrality_measures(gdf_edges_with_hex, centrality_df, output_dirs, city_name):
    ''' 
    This function plots and saves the betweenness and closeness centrality distributions
    input:
        gdf_edges_with_hex: GeoDataFrame containing the network edges
        centrality_df: DataFrame containing the centrality measures
        output_dirs: Dictionary containing the output directories
        city_name: Name of the city being processed
    '''

    # Get the centrality plots directory from output_dirs
    centrality_plots_dir = output_dirs['centrality_plots']
    centrality_plots_dir.mkdir(parents=True, exist_ok=True)

    # Create a figure with two subplots
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(15, 5))

    # Plot betweenness distribution
    sns.histplot(data=centrality_df, x='betweenness', ax=ax1, bins=50)
    ax1.set_title('Betweenness Centrality Distribution')
    ax1.set_xlabel('Betweenness Centrality')
    ax1.set_ylabel('Count')

    # Calculate closeness bins based on actual data range
    min_closeness = centrality_df['closeness'].min()
    max_closeness = centrality_df['closeness'].max()
    bin_width = 0.000005
    closeness_bins = np.arange(min_closeness, max_closeness + bin_width, bin_width)

    # Plot closeness distribution with dynamic range
    sns.histplot(data=centrality_df, x='closeness', ax=ax2, bins=closeness_bins)
    ax2.set_title('Closeness Centrality Distribution')
    ax2.set_xlabel('Closeness Centrality')
    ax2.set_ylabel('Count')
    ax2.set_xlim(min_closeness, max_closeness)

    # Add some statistics
    print("\nBetweenness Centrality Statistics:")
    print(centrality_df['betweenness'].describe())
    print("\nCloseness Centrality Statistics:")
    print(centrality_df['closeness'].describe())

    # Save the combined plot
    plt.tight_layout()
    plot_path = centrality_plots_dir / f'{city_name}_centrality_distributions.png'
    plt.savefig(plot_path, dpi=300, bbox_inches='tight')
    print(f"\nSaved centrality distributions plot to: {plot_path}")
    plt.close()  # Close the figure to free memory

    # Create separate plots for better resolution
    # Betweenness plot
    plt.figure(figsize=(10, 6))
    sns.histplot(data=centrality_df, x='betweenness', bins=50)
    plt.title('Betweenness Centrality Distribution')
    plt.xlabel('Betweenness Centrality')
    plt.ylabel('Count')
    plt.tight_layout()
    betweenness_path = centrality_plots_dir / f'{city_name}_betweenness_distribution.png'
    plt.savefig(betweenness_path, dpi=300, bbox_inches='tight')
    plt.close()

    # Closeness plot with dynamic range
    plt.figure(figsize=(10, 6))
    sns.histplot(data=centrality_df, x='closeness', bins=closeness_bins)
    plt.title('Closeness Centrality Distribution')
    plt.xlabel('Closeness Centrality')
    plt.ylabel('Count')
    plt.xlim(min_closeness, max_closeness)
    plt.tight_layout()
    closeness_path = centrality_plots_dir / f'{city_name}_closeness_distribution.png'
    plt.savefig(closeness_path, dpi=300, bbox_inches='tight')
    plt.close()

    print(f"Saved betweenness distribution plot to: {betweenness_path}")
    print(f"Saved closeness distribution plot to: {closeness_path}")

def analyze_centrality_measures(gdf_edges_with_hex, output_dirs, city_only=True):
    '''
    This function analyzes the centrality measures and saves the results to the appropriate directories
    input:
        gdf_edges_with_hex: GeoDataFrame containing the network edges
        output_dirs: Dictionary containing the output directories
        city_only: Boolean to filter only city edges
    '''
    try:
        # Filter edges if city_only is True
        if city_only:
            print("Filtering edges to include only city edges (is_in_stadt = 1)")
            gdf_filtered = gdf_edges_with_hex[gdf_edges_with_hex['is_in_stadt'] == 1].copy()
            print(f"Number of edges in city: {len(gdf_filtered)} out of {len(gdf_edges_with_hex)} total edges")
        else:
            gdf_filtered = gdf_edges_with_hex.copy()
        
        # Save the filtered GeoDataFrame to CSV temporarily
        temp_csv = output_dirs['centrality_csv'] / "temp_network.csv"
        # Save only the necessary columns
        temp_df = gdf_filtered[['link', 'from_node', 'to_node', 'length']].copy()
        temp_df.to_csv(temp_csv, index=False)
        
        # Create the network
        print("Creating network from GeoDataFrame...")
        G = create_network_from_csv(temp_df)
        
        # Remove temporary file
        temp_csv.unlink()
        
        # Compute centrality measures
        print("Computing betweenness centrality...")
        betweenness = nx.betweenness_centrality(G, weight='length')
        
        print("Computing edge closeness centrality...")
        closeness = edge_closeness_centrality(G, centrality_weight='length')
        
        # Create DataFrame for centrality measures
        centrality_df = pd.DataFrame({
            'from_node': [u for u, v in closeness.keys()],
            'to_node': [v for u, v in closeness.keys()],
            'link_id': [G[u][v].get('id', f"{u}-{v}") for u, v in closeness.keys()],
            'betweenness': [betweenness.get(u, 0) for u, _ in closeness.keys()],
            'closeness': list(closeness.values())
        })
        
        # Save results
        output_file = output_dirs['centrality_csv'] / ('city_centrality_measures.csv' if city_only else 'all_centrality_measures.csv')
        centrality_df.to_csv(output_file, index=False)
        print(f"Saved centrality measures to {output_file}")
        
        # Add centrality measures to the original GeoDataFrame
        # Initialize columns with -1
        gdf_edges_with_hex['betweenness'] = -1.0
        gdf_edges_with_hex['closeness'] = -1.0
        
        # Update values for edges that were analyzed
        for idx, row in gdf_edges_with_hex.iterrows():
            # Find matching edge in centrality_df using link_id
            mask = centrality_df['link_id'] == row['link']
            if mask.any():
                gdf_edges_with_hex.at[idx, 'betweenness'] = centrality_df.loc[mask, 'betweenness'].iloc[0]
                gdf_edges_with_hex.at[idx, 'closeness'] = centrality_df.loc[mask, 'closeness'].iloc[0]
        
        # Print summary statistics
        print("\nCentrality Measures Summary:")
        print(f"Number of edges analyzed: {len(centrality_df)}")
        print(f"Number of edges in original GeoDataFrame: {len(gdf_edges_with_hex)}")
        print("\nBetweenness Centrality Statistics:")
        print(centrality_df['betweenness'].describe())
        print("\nCloseness Centrality Statistics:")
        print(centrality_df['closeness'].describe())
        
        return centrality_df, gdf_edges_with_hex, G
        
    except Exception as e:
        print(f"Error in analyze_centrality_measures: {e}")
        raise
    
def verify_components(G):
    '''
    This function verifies the strongly connected components in the graph
    input:
        G: NetworkX directed graph
    output:
        size_counts: Dictionary containing the size counts of the components
        largest_component: List containing the largest component
    '''
    # Get strongly connected components
    scc = list(nx.strongly_connected_components(G))
    
    # Method 1: Direct count from graph
    total_nodes_in_graph = G.number_of_nodes()
    
    # Method 2: Sum of all component sizes
    total_nodes_in_components = sum(len(component) for component in scc)
    
    # Method 3: Count nodes in size distribution
    size_counts = {}
    for component in scc:
        size = len(component)
        size_counts[size] = size_counts.get(size, 0) + 1
    
    total_nodes_from_distribution = sum(size * count for size, count in size_counts.items())
    
    print("\nCross Verification:")
    print(f"Total nodes in graph: {total_nodes_in_graph}")
    print(f"Total nodes in components: {total_nodes_in_components}")
    print(f"Total nodes from size distribution: {total_nodes_from_distribution}")
    
    print("\nDetailed Component Analysis:")
    print("Size | Count | Total Nodes | Cumulative %")
    print("-" * 50)
    total_so_far = 0
    
    for size, count in sorted(size_counts.items(), reverse=True):
        total_in_this_size = size * count
        total_so_far += total_in_this_size
        percentage = (total_so_far / total_nodes_in_graph) * 100
        print(f"{size:4d} | {count:5d} | {total_in_this_size:11d} | {percentage:6.2f}%")
    
    # Verify each node appears exactly once
    all_nodes = set()
    duplicate_nodes = set()
    missing_nodes = set(G.nodes())
    
    for component in scc:
        for node in component:
            if node in all_nodes:
                duplicate_nodes.add(node)
            all_nodes.add(node)
            missing_nodes.discard(node)
    
    print("\nVerification Results:")
    if duplicate_nodes:
        print(f"Warning: Found {len(duplicate_nodes)} nodes in multiple components!")
        print("First few duplicate nodes:", list(duplicate_nodes)[:5])
    else:
        print("✓ No nodes appear in multiple components")
    
    if missing_nodes:
        print(f"Warning: Found {len(missing_nodes)} nodes not in any component!")
        print("First few missing nodes:", list(missing_nodes)[:5])
    else:
        print("✓ All nodes are assigned to components")
    
    # Verify the largest component
    largest_component = max(scc, key=len)
    print(f"\nLargest Component Analysis:")
    print(f"Size: {len(largest_component)}")
    print(f"Percentage of total nodes: {(len(largest_component)/total_nodes_in_graph)*100:.2f}%")
    
    # Verify connectivity within largest component
    subgraph = G.subgraph(largest_component)
    is_strongly_connected = nx.is_strongly_connected(subgraph)
    print(f"Is largest component strongly connected? {'Yes' if is_strongly_connected else 'No'}")
    
    return size_counts, largest_component