'''
This file contains the functions for creating the hexagon grid and plotting the network for a given city
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

# Local imports
import network_io as nio

import gzip
import xml.etree.ElementTree as ET
import geopandas as gpd

def matsim_network_input_to_gdf(network_file):
    """
    Convert MATSim network XML to GeoDataFrame using network_io and extract network attributes
    
    Parameters:
    -----------
    network_file : str or Path
        Path to the MATSim network XML file
        
    Returns:
    --------
    tuple : (GeoDataFrame, nodes_dict, df_edges, network_attrs, link_attrs)
        - GeoDataFrame containing the network edges
        - Dictionary of node coordinates
        - DataFrame of edges
        - Dictionary of network attributes (e.g. coordinateReferenceSystem, capperiod, etc.)
        - Dictionary of link-level attributes keyed by link id, where each value is a dictionary of attributes
    """
    # Parse nodes and edges using nio (assumed to be imported and available)
    nodes_dict = nio.parse_nodes(network_file)
    df_edges = nio.parse_edges(network_file, nodes_dict)
    
    # Create GeoDataFrame from df_edges and ensure correct CRS
    gdf = gpd.GeoDataFrame(df_edges, geometry='geometry', crs='EPSG:25832')
    if gdf.crs != "EPSG:25832":
        gdf = gdf.to_crs(epsg=25832)
    
    # Parse network attributes and link-level attributes from XML
    network_attrs = {}
    link_attrs = {}  # dictionary to store attributes for each link (keyed by link id)
    
    with gzip.open(network_file, 'rb') as f:
        tree = ET.parse(f)
        root = tree.getroot()
        
        # Parse network-level attributes from <attributes>
        attributes = root.find('attributes')
        if attributes is not None:
            for attribute in attributes.findall('attribute'):
                if attribute.get('name') == 'coordinateReferenceSystem':
                    network_attrs['coordinateReferenceSystem'] = attribute.text
        
        # Get the <links> element and its attributes
        links_elem = root.find('links')
        if links_elem is not None:
            for attr in ['capperiod', 'effectivecellsize', 'effectivelanewidth']:
                if attr in links_elem.attrib:
                    network_attrs[attr] = links_elem.attrib[attr]
            
            # Iterate over each <link> element to extract link-level attributes
            for link in links_elem.findall('link'):
                link_id = link.get('id')
                attr_dict = {}  # to store this link's additional attributes
                attributes_sub = link.find('attributes')
                if attributes_sub is not None:
                    for attribute in attributes_sub.findall('attribute'):
                        attr_name = attribute.get('name')
                        # Optionally, you can also get the attribute's class via attribute.get('class')
                        attr_dict[attr_name] = attribute.text
                link_attrs[link_id] = attr_dict
    
    return gdf, nodes_dict, df_edges, network_attrs, link_attrs


def clean_duplicates_based_on_modes(file_path):
    """
    Cleans the network by:
    1. First removing full duplicates (same from_node, to_node, geometry, modes)
    2. For remaining duplicates with same node pairs but different modes:
       - Keep the entry with modes that contain both "car" and "car_passenger"
       - If multiple or none meet this criteria, keep the one with the longest modes string
    
    Args: 
        file_path: Path to the CSV file
        
    Returns:
        The cleaned DataFrame   
    """
    # Load the CSV file
    print(f"Loading file: {file_path}")
    df = pd.read_csv(file_path, delimiter=";", low_memory=False)
    
    print(f"Total edges in original file: {len(df)}")
    
    
    # Step 1: First identify duplicates by the key columns
    key_columns = ["from_node", "to_node", "geometry", "modes"]
    full_duplicates_mask = df.duplicated(subset=key_columns, keep=False)
    full_duplicates = df[full_duplicates_mask]
    
    print(f"\nFull duplicates (same from_node, to_node, geometry, modes): {len(full_duplicates)}")
    
    # Now process these duplicates - keep the highest vol_car
    df_cleaned = df[~full_duplicates_mask].copy()
    
    for _, group in full_duplicates.groupby(key_columns):
        # Keep the row with the highest vol_car
        max_idx = group['vol_car'].idxmax()
        row_to_keep = group.loc[max_idx]
        df_cleaned = pd.concat([df_cleaned, pd.DataFrame([row_to_keep])])
    
    print(f"Edges after removing full duplicates (based on higher car volume): {len(df_cleaned)}")
    print(f"Number of full duplicates removed: {len(df) - len(df_cleaned)}")
    
    # Step 2: Now look for edges with the same from_node and to_node (leftover duplicates)
    node_columns = ["from_node", "to_node"]
    leftover_duplicates_mask = df_cleaned.duplicated(subset=node_columns, keep=False)
    leftover_duplicates = df_cleaned[leftover_duplicates_mask]
    
    print(f"\nLeftover duplicates (same from_node, to_node only): {len(leftover_duplicates)}")
    print(f"Number of unique node pairs with leftover duplicates: {len(leftover_duplicates.groupby(node_columns))}")
    
    # Step 3: Process leftover duplicates based on modes criteria
    df_final = df_cleaned[~leftover_duplicates_mask].copy()
    
    # Track selection statistics
    selection_stats = {
        'total_groups': 0,
        'selected_by_car_modes': 0,
        'selected_by_length': 0,
        'selected_by_vol_car': 0
    }
    
    for (from_node, to_node), group in leftover_duplicates.groupby(node_columns):
        selection_stats['total_groups'] += 1
        
        # Convert modes to string and check for both "car" and "car_passenger"
        # (ensuring robust string comparison)
        group['has_both_car_modes'] = group['modes'].apply(
            lambda x: ('"car"' in str(x) or "'car'" in str(x) or ",car," in str(x) or "[car" in str(x)) and 
                      ('"car_passenger"' in str(x) or "'car_passenger'" in str(x) or 
                       ",car_passenger," in str(x) or "[car_passenger" in str(x))
        )
        
        # Check if any entry has both car modes
        if group['has_both_car_modes'].any():
            # Filter to entries with both car modes
            car_mode_entries = group[group['has_both_car_modes']]
            
            # If multiple entries have both car modes, select by longest modes string
            if len(car_mode_entries) > 1:
                mode_lengths = car_mode_entries['modes'].apply(lambda x: len(str(x)))
                
                # If there's a tie in length, use vol_car
                if mode_lengths.nunique() == 1:
                    row_to_keep = car_mode_entries.loc[car_mode_entries['vol_car'].idxmax()]
                    selection_stats['selected_by_vol_car'] += 1
                else:
                    row_to_keep = car_mode_entries.loc[mode_lengths.idxmax()]
                    selection_stats['selected_by_length'] += 1
            else:
                # Only one entry has both car modes - keep it
                row_to_keep = car_mode_entries.iloc[0]
                selection_stats['selected_by_car_modes'] += 1
        else:
            # No entry has both car modes - select by longest modes string
            mode_lengths = group['modes'].apply(lambda x: len(str(x)))
            
            # If there's a tie in length, use vol_car
            if mode_lengths.nunique() == 1:
                row_to_keep = group.loc[group['vol_car'].idxmax()]
                selection_stats['selected_by_vol_car'] += 1
            else:
                row_to_keep = group.loc[mode_lengths.idxmax()]
                selection_stats['selected_by_length'] += 1
        
        # Add the selected row to the final DataFrame
        df_final = pd.concat([df_final, pd.DataFrame([row_to_keep.drop('has_both_car_modes')])])
    
    print(f"\nFinal edges after processing all duplicates: {len(df_final)}")
    print(f"Total edges removed: {len(df) - len(df_final)}")
    
    # Print selection statistics
    print("\nSelection criteria statistics:")
    print(f"Total duplicate groups processed: {selection_stats['total_groups']}")
    print(f"Selected by having both car modes: {selection_stats['selected_by_car_modes']} ({selection_stats['selected_by_car_modes']/selection_stats['total_groups']*100:.1f}%)")
    print(f"Selected by longest modes string: {selection_stats['selected_by_length']} ({selection_stats['selected_by_length']/selection_stats['total_groups']*100:.1f}%)")
    print(f"Selected by highest vol_car (tiebreaker): {selection_stats['selected_by_vol_car']} ({selection_stats['selected_by_vol_car']/selection_stats['total_groups']*100:.1f}%)")
    
    # Step 4: Verify no duplicates remain
    final_duplicates_mask = df_final.duplicated(subset=node_columns, keep=False)
    final_duplicates = df_final[final_duplicates_mask]
    
    if len(final_duplicates) > 0:
        print(f"\n⚠️ WARNING: {len(final_duplicates)} duplicate edges remain!")
        print(f"Number of unique node pairs with remaining duplicates: {len(final_duplicates.groupby(node_columns))}")
        
        # Analyze remaining duplicates
        print("\n=== REMAINING DUPLICATES ===")
        for i, ((from_node, to_node), group) in enumerate(final_duplicates.groupby(node_columns)):
            print(f"\nRemaining Duplicate {i+1}: from_node={from_node}, to_node={to_node}")
            print(f"Number of edges: {len(group)}")
            
            # Display the group
            important_cols = ['modes', 'vol_car', 'geometry']
            if 'link' in group.columns:
                important_cols.insert(0, 'link')
            
            print(group[important_cols].to_string())
            
            # Only show a few examples
            if i >= 4:
                remaining = len(final_duplicates.groupby(node_columns)) - (i+1)
                print(f"\n... and {remaining} more duplicate pairs (not shown) ...")
                break
    else:
        print("\n✅ SUCCESS: No duplicates remain in the final network!")
    
    # Save the cleaned network
    #output_path = file_path.replace('.csv', '_wo_duplicates.csv')
    #df_final.to_csv(output_path, index=False, sep=';')
    #print(f"\nCleaned network saved to: {output_path}")
    df_final=df_final[df_final['from_node']!=df_final['to_node']]
    return df_final

def create_nodes_dict(cleaned_network):
    '''
    This function creates a dictionary of nodes and their coordinates from a cleaned network.
    For each edge:
    - from_node gets the coordinates of the start point of the LineString
    - to_node gets the coordinates of the end point of the LineString
    '''
    nodes_dict = {}
    for index, row in cleaned_network.iterrows():
        # Get the coordinates of the start and end points of the LineString
        coords = list(row['geometry'].coords)
        # Store start point coordinates for from_node
        nodes_dict[row['from_node']] = coords[0]
        # Store end point coordinates for to_node
        nodes_dict[row['to_node']] = coords[-1]
    print(f"Created nodes_dict with {len(nodes_dict)} unique nodes")
    return nodes_dict

def read_compressed_network_csv(csv_filepath, csv_output_path, crs='EPSG:25832'):
    '''
    This function reads a compressed CSV file containing network data and converts it to a GeoDataFrame
    '''
    print("Unzipping file...")
    with gzip.open(csv_filepath, 'rb') as f_in:
        with open(csv_output_path, 'wb') as f_out:
            f_out.write(f_in.read())
    print(f"File unzipped to: {csv_output_path}")
        
    # Read CSV
    print("Reading CSV...")
    df = pd.read_csv(csv_output_path, delimiter=';')
    gdf_csv = gpd.GeoDataFrame(
        df,  # Your original DataFrame
        geometry=df['geometry'].apply(wkt.loads),  # Convert WKT strings to geometries
        crs=crs  # Set the coordinate reference system
    )
    return gdf_csv
    
def consolidate_road_types(highway_type):
    '''
    This function consolidates road types into broader categories
    '''
    if highway_type is None:
        return None

    # Mapping of road types to categories
    road_type_mapping = {
        'motorway': 'primary',
        'motorway_link': 'primary',
        'trunk': 'primary',
        'trunk_link': 'primary',
        'primary': 'primary',
        'primary_link': 'primary',
        'secondary': 'secondary',
        'secondary_link': 'secondary',
        'tertiary': 'tertiary',
        'tertiary_link': 'tertiary',
        'residential': 'residential',
        'unclassified': 'unclassified',
        'living_street': 'living_street',
        'pedestrian': 'other',
        'service': 'other',
        'track': 'other',
        'path': 'other',
        'cycleway': 'other',
        'construction': 'other',
        'busway': 'other'
    }
    
    return road_type_mapping.get(highway_type, 'other')


def modify_geodataframe(gdf):
    '''
    This function modifies the zones geodataframe to ensure it is in the correct CRS 
    and has the correct columns. 
    Also it applies the 'multipolygon_to_polygon' function to all the geometries in the geodataframe so that 
    the geometries are all Polygons and not MultiPolygons.
    '''
    if (gdf.geometry.apply(lambda x: x.geom_type == "MultiPolygon")).any():
        gdf["geometry"] = gdf.geometry.apply(multipolygon_to_polygon)
    gdf["area"] = gdf.geometry.area
    gdf["perimetre"] = gdf.geometry.length
    gdf["zone_id"] = range(1, len(gdf)+1) #zone id
    zones_gdf = gdf[["zone_id", "area", "perimetre", "geometry"]]
    # Ensure the data is in the correct CRS (EPSG:25832) **********VERY IMPORTANT**********
    if zones_gdf.crs != "EPSG:25832": #should match with the CRS of the Network Geodataframe
        zones_gdf = zones_gdf.to_crs(epsg=25832)
    return zones_gdf


def multipolygon_to_polygon(geom):
    '''
    This function converts a MultiPolygon to a Polygon with the largest connected area.
    A MultiPolygon with 2 Polygons inside will return the Polygon with the largest area.(z.B. Stadt Bamberg had 2 disconnected polygons, we only consider the largest one)
    '''
    return max(geom.geoms, key=lambda p: p.area)


def merge_edges_and_zones(gdf_csv, zones_gdf):
    '''
    This function merges network edges with zones using spatial join.
    input:
        gdf_csv: GeoDataFrame containing network edges with your specific columns
        zones_gdf: GeoDataFrame containing zone polygons
    output:
        GeoDataFrame with edges and their intersecting zones
    '''
    # Perform spatial join
    gdf_edges_with_zones = gpd.sjoin(gdf_csv, zones_gdf, how='left', predicate='intersects')
    
    # Group by edge ID and aggregate attributes
    gdf_edges_with_zones = gdf_edges_with_zones.groupby('link').agg({
        'from_node': 'first',
        'to_node': 'first',
        'length': 'first',
        'freespeed': 'first',
        'capacity': 'first',
        'permlanes': 'first',
        'modes': 'first',
        'oneway': 'first',
        'vol_car': 'first',
        'osm:way:highway': 'first',
        'geometry': 'first',
        'zone_id': lambda x: [int(i) for i in x.dropna()]
    }).reset_index()

    # Convert numeric columns
    numeric_columns = ['freespeed', 'capacity', 'permlanes', 'vol_car']
    for col in numeric_columns:
        if col in gdf_edges_with_zones.columns:
            gdf_edges_with_zones[col] = pd.to_numeric(gdf_edges_with_zones[col], errors='coerce')

    # Ensure it's a GeoDataFrame
    gdf_edges_with_zones = gpd.GeoDataFrame(
        gdf_edges_with_zones, 
        geometry='geometry', 
        crs='EPSG:25832'
    )
    
    return gdf_edges_with_zones


def generate_hexagon_grid(polygon, hexagon_size, projection='EPSG:25832'):
    '''
    This function generates a hexagonal grid that fits within a given polygon
    input:
        polygon: Polygon to clip the grid to
        hexagon_size: Distance from the hexagon's center to any vertex
        projection: Coordinate reference system for the polygon and grid
    output:
        GeoDataFrame with hexagons clipped to the input polygon
    '''
    # Create a GeoDataFrame from the input polygon using the given projection.
    poly_gdf = gpd.GeoDataFrame({'geometry': [polygon]}, crs=projection)
    
    # Obtain the bounding box of the polygon.
    xmin, ymin, xmax, ymax = poly_gdf.total_bounds

    # Compute the vertical scaling factor using sin(60°) (~0.866).
    # This factor is used to correctly space the hexagon vertices vertically.
    a = np.sin(np.pi / 3) 

    # Define the x positions (columns) for hexagon grid centers.
    # Here, the horizontal spacing between potential hexagon positions is 3 * hexagon_size.
    cols = np.arange(np.floor(xmin), np.ceil(xmax), 3 * hexagon_size)
    
    # Define the y positions (rows) for hexagon grid centers.
    # The y positions are scaled by the factor 'a' to account for the vertical distance between rows.
    rows = np.arange(np.floor(ymin) / a, np.ceil(ymax) / a, hexagon_size)

    # Generate hexagon geometries for each grid position.
    hexagons = []
    for x in cols:
        for i, y in enumerate(rows):
            # Offset every other row horizontally to create a staggered hexagon grid.
            if i % 2 == 0:
                x0 = x
            else:
                x0 = x + 1.5 * hexagon_size

            # Create a hexagon by specifying its six vertices.
            # The vertices are calculated relative to (x0, y) and scaled vertically by 'a'.
            hexagon = sgeo.Polygon([
                (x0, y * a),
                (x0 + hexagon_size, y * a),
                (x0 + 1.5 * hexagon_size, (y + hexagon_size) * a),
                (x0 + hexagon_size, (y + 2 * hexagon_size) * a),
                (x0, (y + 2 * hexagon_size) * a),
                (x0 - 0.5 * hexagon_size, (y + hexagon_size) * a),
            ])
            hexagons.append(hexagon)
    
    # Convert the list of hexagons into a GeoDataFrame with the specified projection.
    grid = gpd.GeoDataFrame({'geometry': hexagons}, crs=projection)
    
    # Clip the hexagon grid to the input polygon so that only hexagons (or portions thereof)
    # that fall within the polygon are retained.
    grid_clipped = gpd.clip(grid, poly_gdf)
    
    # Reset the index and assign a unique grid_id to each hexagon.
    grid_clipped = grid_clipped.reset_index(drop=True)
    grid_clipped['grid_id'] = grid_clipped.index
    
    return grid_clipped


def merge_edges_and_hexagon_grid(zones_gdf, hexagon_size, gdf_edges_with_zones, 
                                        projection='EPSG:25832'):
    '''
    This function generates a hexagon grid for each zone and assigns each edge to the hexagon(s) 
    it falls into
    input:
        zones_gdf: GeoDataFrame containing zone polygons
        hexagon_size: Distance from the hexagon's center to any vertex
        gdf_edges_with_zones: GeoDataFrame containing network edges with their intersecting zones
        projection: Coordinate reference system for the polygon and grid
    output:
        GeoDataFrame with hexagons clipped to the input polygon
    '''
    # Get the boundary of zone_id 1
    zone_1_boundary = zones_gdf[zones_gdf['zone_id'] == 1].geometry.values[0]

    # Create a single continuous hexagon grid for zone 1
    hexagon_grid_all = generate_hexagon_grid(zone_1_boundary, hexagon_size, projection='EPSG:25832')
    
    # Add zone information to each hexagon
    def get_intersecting_zones(hex_geom):
        """
        This function takes a hexagon geometry and returns a list of zone IDs that intersect with that hexagon.
        """
        intersecting_zones = []
        for idx, row in zones_gdf.iterrows():
            if hex_geom.intersects(row['geometry']):
                zone_id = row.get('zone_id', idx+1)
                intersecting_zones.append(zone_id)
        return intersecting_zones

    hexagon_grid_all['hex_zone_id'] = hexagon_grid_all['geometry'].apply(get_intersecting_zones)
    print(f"Total number of hexagons created: {len(hexagon_grid_all)}")
    print(f"Number of hexagons in multiple zones: {len(hexagon_grid_all[hexagon_grid_all['hex_zone_id'].apply(lambda x: len(x) >= 2)])}")

    # Spatial join to assign each edge the hexagon(s) it falls into
    gdf_edges_with_hex = gpd.sjoin(gdf_edges_with_zones, hexagon_grid_all[['geometry', 'hex_zone_id', 'grid_id']], 
                                how='left', predicate='intersects')

    # Group by edge 'link' and aggregate the hexagon IDs into a list
    gdf_edges_with_hex = gdf_edges_with_hex.groupby('link').agg({
        'from_node': 'first',
        'to_node': 'first',
        'length': 'first',
        'freespeed': 'first',
        'capacity': 'first',
        'permlanes': 'first',
        'modes': 'first',
        'oneway': 'first',
        'vol_car': 'first',
        'osm:way:highway': 'first',
        'geometry': 'first',
        'grid_id': lambda x: list(x.dropna()),  # Aggregate hexagon IDs
        'zone_id': lambda x: list(set([d for dist_list in x.dropna() if isinstance(dist_list, list) for d in dist_list])),  # Aggregate district IDs
        'hex_zone_id': lambda x: list(set([d for dist_list in x.dropna() for d in dist_list if isinstance(dist_list, list)])),  # Aggregate unique districts
    }).reset_index()
    
    # Convert back to GeoDataFrame
    gdf_edges_with_hex = gpd.GeoDataFrame(
        gdf_edges_with_hex,
        geometry='geometry',
        crs=gdf_edges_with_zones.crs  # Preserve the original CRS
    )

    # Rename the aggregated columns
    gdf_edges_with_hex.rename(columns={'grid_id': 'hexagon'}, inplace=True)

    # Convert numeric columns
    numeric_columns = ['freespeed', 'capacity', 'lanes', 'vol_car']
    for col in numeric_columns:
        if col in gdf_edges_with_hex.columns:
            gdf_edges_with_hex[col] = pd.to_numeric(gdf_edges_with_hex[col], errors='coerce')

    # Create is_in_stadt column, which helps us identify the edges that are in the city. Because, policies will be applied only for the edges that are in the city.
    gdf_edges_with_hex['is_in_stadt'] = gdf_edges_with_hex['hex_zone_id'].apply(lambda x: 1 if 1 in x else 0)

    # Hexagon Statistics
    print("\nEdge Statistics:")
    print(f"Total number of edges: {len(gdf_edges_with_hex)}")
    print('Edges in stadt: ', len(gdf_edges_with_hex[gdf_edges_with_hex['hex_zone_id'].apply(lambda x: 1 in x)]))
    print('Edges not in stadt: ', len(gdf_edges_with_hex[gdf_edges_with_hex['hex_zone_id'].apply(lambda x: 1 not in x)]))
   

    return gdf_edges_with_hex, hexagon_grid_all

def check_hexagon_statistics(gdf_edges_with_hex, hexagon_grid_all):
    '''
    This function checks the statistics of the hexagon grid
    input:
        gdf_edges_with_hex: GeoDataFrame containing network edges with their intersecting hexagons
        hexagon_grid_all: GeoDataFrame containing all hexagons
    '''
    unique_values = set(item for sublist in gdf_edges_with_hex['hexagon'] for item in sublist)
    print(unique_values)
    print('Number of Hexagons containing edges: ', len (unique_values))
    print('Total number of Hexagons created: ', len(hexagon_grid_all))

def plot_grid_and_edges(gdf_edges_with_hex, hexagon_grid_all, zones_gdf, output_dirs,city_name):
    '''
    This function plots and saves the network with the hexagon grid and zones
    input:
        gdf_edges_with_hex: GeoDataFrame containing network edges with their intersecting hexagons
        hexagon_grid_all: GeoDataFrame containing all hexagons
        zones_gdf: GeoDataFrame containing zones    
        output_dirs: Dictionary containing output directory paths
        city_name: Name of the city
    '''
    # Create the figure and axis
    fig, ax = plt.subplots(figsize=(15, 15))

    def get_edge_color(zones):
        if not isinstance(zones, list):
            return 'gray'
        if 1 in zones and 2 in zones:
            return 'green'
        elif 1 in zones:
            return 'blue'
        elif 2 in zones:
            return 'gray'
        else:
            return 'gray'
            
    # Plot roads in gray
    gdf_edges_with_hex['edge_color'] = gdf_edges_with_hex['zone_id'].apply(get_edge_color)

    # Plot all edges at once, grouped by color
    for color in ['blue', 'gray', 'green']:
        edges = gdf_edges_with_hex[gdf_edges_with_hex['edge_color'] == color]
        if not edges.empty:
            edges.plot(
                ax=ax,
                color=color,
                linewidth=0.5,
                label=None
            )

    # Plot zones in yellow (uniform)
    zones_gdf.plot(
        ax=ax, 
        column='zone_id',
        cmap='PuBuGn',
        alpha=0.1,
        edgecolor='black',
        linewidth=0.5,
        legend=False,
        label='Zones'
    )

    # Plot hexagons in red (edges only)
    hexagon_grid_all.plot(
        ax=ax, 
        color='none', 
        edgecolor='red',
        alpha=0.7,
        linewidth=0.6,
        label='Hexagons'
    )

    # Create custom legend
    legend_elements = [
        Line2D([0], [0], color='blue', linewidth=0.7, label='Zone 1'),
        Line2D([0], [0], color='gray', linewidth=0.7, label='Zone 2'),
        Line2D([0], [0], color='green', linewidth=0.7, label='Zones 1 & 2'),
        Patch(facecolor='yellow', edgecolor='black', alpha=0.2, label='Zones'),
        Line2D([0], [0], color='red', linewidth=0.8, label='Hexagons')
    ]

    ax.legend(handles=legend_elements)
    plt.title('Network with Hexagon Grid and Zones')
    plt.axis('equal')
    plt.tight_layout()
    
    # Save the plot
    output_file = output_dirs['hexagon_plots'] / f'{city_name}_network_hexagon_zones.png'
    plt.savefig(output_file, dpi=300, bbox_inches='tight')
    print(f"Plot saved to: {output_file}")
    
    # Close the plot to free memory
    plt.close()
    
def convert_and_save_geodataframe(gdf, output_path):
    """
    Convert a GeoDataFrame to GeoJSON format, handling list columns by converting them to strings.
    
    Parameters:
    -----------
    gdf : GeoDataFrame
        The GeoDataFrame to convert and save
    output_path : Path or str
        Path where to save the GeoJSON file
    """
    # Create a copy to avoid modifying the original
    gdf_save = gdf.copy()
    
    # Convert list columns to strings
    for col in gdf_save.columns:
        if gdf_save[col].dtype == object and isinstance(gdf_save[col].iloc[0], list):
            gdf_save[col] = gdf_save[col].apply(lambda x: str(x))
    
    # Save to GeoJSON
    gdf_save.to_file(output_path, driver='GeoJSON')
    print(f"Saved GeoJSON file to: {output_path}")