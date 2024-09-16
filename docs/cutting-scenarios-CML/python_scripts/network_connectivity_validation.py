import networkx as nx
import geopandas as gpd
import matplotlib.pyplot as plt


network_links =  gpd.read_file('/mnt/efs/analysis/ys/matsim_cutter/te-10pc-outputs-ignoring-errors/genet-vis/network_links.geojson')

G_directed = nx.DiGraph()

print("Adding {} network links to a directed graph...".format(len(network_links)))
for _, row in network_links.iterrows():
    G_directed.add_node(row['from'], pos=(row['geometry'].coords[0][0], row['geometry'].coords[0][1]))
    G_directed.add_node(row['to'], pos=(row['geometry'].coords[-1][0], row['geometry'].coords[-1][1]))

for _, row in network_links.iterrows():
    G_directed.add_edge(row['from'], row['to'], modes=row['modes'], length=row['length'])

G_undirected = G_directed.to_undirected()
print(f"Converted directed graph to an undirected graph with {len(G_undirected.nodes())} nodes")

is_strongly_connected = nx.is_strongly_connected(G_directed)
print(f"Is the directed graph strongly connected? {is_strongly_connected}")

# Find connected components
connected_components = list(nx.connected_components(G_undirected))
print(f"Number of connected components: {len(connected_components)}")

# Visualize the components in the undirected graph
component_colors = {}
for i, component in enumerate(connected_components):
    for node in component:
        component_colors[node] = i

node_colors = [component_colors[node] for node in G_undirected.nodes()]
pos = nx.get_node_attributes(G_undirected, 'pos')

plt.figure(figsize=(12, 8))
nx.draw(G_undirected, pos, node_color=node_colors, node_size=10, edge_color='gray', cmap=plt.cm.tab20)
plt.title('Network Components Visualization')
plt.show()

isolated_nodes = [node for node, degree in G_directed.degree() if degree == 0]
print(f"Number of isolated nodes: {len(isolated_nodes)}")

dead_end_nodes = [node for node, degree in G_directed.degree() if degree == 1]
print(f"Number of dead-end nodes: {len(dead_end_nodes)}")
