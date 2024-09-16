import xml.etree.ElementTree as ET
import xml.dom.minidom as minidom

# Load and parse the XML file
tree = ET.parse('/mnt/efs/analysis/ys/matsim_cutter/output_no_intermodal_20240909_10pct/TE_cutter_population.xml')
root = tree.getroot()

# List of subpopulations to remove
subpopulations_to_remove = ['hgv', 'lgv', 'lgv-ev']

for person in root.findall('person'):
    subpopulation = person.find(".//attribute[@name='subpopulation']")
    if subpopulation is not None and subpopulation.text in subpopulations_to_remove:
        root.remove(person)

def prettify(elem):
    rough_string = ET.tostring(elem, 'utf-8')
    reparsed = minidom.parseString(rough_string)
    return reparsed.toprettyxml(indent="  ")

pretty_xml_as_string = prettify(root)

with open('/mnt/efs/analysis/ys/matsim_cutter/output_no_intermodal_20240909_10pct/modified_remove_freight_TE_cutter_population_v1.xml', 'w') as f:
    f.write(pretty_xml_as_string)
