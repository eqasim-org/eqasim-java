from lxml import etree

def modify_xml(input_file, output_file):
    """
    A method to add car and bike access to all of the stop facilities for testing cutter
    """
    parser = etree.XMLParser(recover=True)
    tree = etree.parse(input_file, parser)
    root = tree.getroot()

    for stop_facility in root.findall('.//stopFacility'):
        attributes = stop_facility.find('attributes')
        if attributes is None:
            attributes = etree.SubElement(stop_facility, 'attributes')

        bike_accessible = False
        for attribute in attributes.findall('attribute'):
            if attribute.get('name') == 'bikeAccessible':
                bike_accessible = True
                break
        
        if not bike_accessible:
            etree.SubElement(attributes, 'attribute', {
                'name': 'bikeAccessible',
                'class': 'java.lang.String'
            }).text = 'True'

        car_accessible = False
        for attribute in attributes.findall('attribute'):
            if attribute.get('name') == 'carAccessible':
                car_accessible = True
                break
        
        if not car_accessible:
            etree.SubElement(attributes, 'attribute', {
                'name': 'carAccessible',
                'class': 'java.lang.String'
            }).text = 'True'

    tree.write(output_file, encoding='utf-8', xml_declaration=True, pretty_print=True)

# Specify the input and output file paths
input_file = '/mnt/efs/analysis/ys/matsim_cutter/input_files_locations_facilities_10pc_20240807/output_transitSchedule.xml'
output_file = '/mnt/efs/analysis/ys/matsim_cutter/input_files_locations_facilities_10pc_20240807/output_transitSchedule_add_true_car_bike_attribute_20240805.xml'  # Correctly update the path as needed

modify_xml(input_file, output_file)