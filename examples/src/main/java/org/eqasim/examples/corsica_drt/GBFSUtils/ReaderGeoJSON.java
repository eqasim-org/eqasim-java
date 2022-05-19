package org.eqasim.examples.corsica_drt.GBFSUtils;//package org.eqasim.examples.corsica_drt.GBFSUtils;
//import java.io.File;
//import java.io.IOException;
//import java.net.URL;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//
//import org.geotools.data.DataStore;
//import org.geotools.data.DataStoreFinder;
//import org.geotools.data.DataUtilities;
//import org.geotools.data.FileDataStore;
//import org.geotools.data.FileDataStoreFinder;
//import org.geotools.data.collection.ListFeatureCollection;
//
//import org.geotools.data.simple.SimpleFeatureCollection;
//import org.geotools.data.simple.SimpleFeatureSource;
//import org.geotools.data.simple.SimpleFeatureStore;
//import org.geotools.feature.FeatureIterator;
//import org.geotools.feature.NameImpl;
//import org.geotools.feature.simple.SimpleFeatureTypeImpl;
//import org.geotools.feature.type.GeometryDescriptorImpl;
//import org.geotools.feature.type.GeometryTypeImpl;
//import org.geotools.referencing.crs.DefaultGeographicCRS;
//import org.geotools.util.URLs;
//import org.opengis.feature.simple.SimpleFeature;
//import org.opengis.feature.simple.SimpleFeatureType;
//import org.opengis.feature.type.AttributeDescriptor;
//import org.opengis.feature.type.AttributeType;
//import org.opengis.feature.type.GeometryDescriptor;
//import org.opengis.feature.type.GeometryType;
//
//
//public class ReaderGeoJSON {
//    public static void main(String[] args) throws IOException {
//
//        // open geojson
//
//        URL url = URLs.fileToUrl(new File("/home/ian/Data/states/states.geojson"));
//        HashMap<String, Object> params = new HashMap<>();
//        params.put(GeoJSONDataStoreFactory.URLP.key, url);
//        DataStore in = DataStoreFinder.getDataStore(params);
//        if (in == null) {
//            throw new IOException("couldn't open datastore from " + url);
//        }
//        SimpleFeatureCollection features = in.getFeatureSource(in.getTypeNames()[0]).getFeatures();
//
//        // convert schema for shapefile
//        SimpleFeatureType schema = features.getSchema();
//        GeometryDescriptor geom = schema.getGeometryDescriptor();
//
//        List<AttributeDescriptor> attributes = schema.getAttributeDescriptors();
//        GeometryType geomType = null;
//        List<AttributeDescriptor> attribs = new ArrayList<>();
//        for (AttributeDescriptor attrib : attributes) {
//            AttributeType type = attrib.getType();
//            if (type instanceof GeometryType) {
//                geomType = (GeometryType) type;
//            } else {
//                attribs.add(attrib);
//            }
//        }
//
//        GeometryTypeImpl gt = new GeometryTypeImpl(new NameImpl("the_geom"), geomType.getBinding(),
//                DefaultGeographicCRS.WGS84, geomType.isIdentified(), geomType.isAbstract(), geomType.getRestrictions(),
//                geomType.getSuper(), geomType.getDescription());
//
//        GeometryDescriptor geomDesc = new GeometryDescriptorImpl(gt, new NameImpl("the_geom"), geom.getMinOccurs(),
//                geom.getMaxOccurs(), geom.isNillable(), geom.getDefaultValue());
//
//        attribs.add(0, geomDesc);
//
//        SimpleFeatureType outSchema = new SimpleFeatureTypeImpl(schema.getName(), attribs, geomDesc, schema.isAbstract(),
//                schema.getRestrictions(), schema.getSuper(), schema.getDescription());
//
//        // create output datastore
//
//        File outFile = new File("output.shp");
//        outFile.createNewFile();
//        FileDataStore ds = FileDataStoreFinder.getDataStore(outFile);
//        ds.createSchema(outSchema);
//        SimpleFeatureSource featureSource = ds.getFeatureSource();
//        if (featureSource instanceof SimpleFeatureStore) {
//            SimpleFeatureCollection collection;
//            List<SimpleFeature> feats = new ArrayList<>();
//            // retype the features
//            try (FeatureIterator<SimpleFeature> features2 = features.features()) {
//                while (features2.hasNext()) {
//                    SimpleFeature f = features2.next();
//                    SimpleFeature reType = DataUtilities.reType(outSchema, f, true);
//
//                    reType.setAttribute(outSchema.getGeometryDescriptor().getName(),
//                            f.getAttribute(schema.getGeometryDescriptor().getName()));
//
//                    feats.add(reType);
//                }
//            }
//
//            collection = new ListFeatureCollection(outSchema, feats);
//
//            SimpleFeatureStore outStore = (SimpleFeatureStore) featureSource;
//
//            outStore.addFeatures(collection);
//            ds.dispose();
//        } else {
//            System.err.println("Unable to write to " + outFile);
//        }
//        in.dispose();
//    }
//}
