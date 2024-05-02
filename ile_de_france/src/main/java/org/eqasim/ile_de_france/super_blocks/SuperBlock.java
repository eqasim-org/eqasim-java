package org.eqasim.ile_de_france.super_blocks;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.locationtech.jts.geom.*;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Identifiable;
import org.opengis.feature.simple.SimpleFeature;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class SuperBlock implements Identifiable<SuperBlock> {
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
    private Id<SuperBlock> id;
    private Polygon polygon;
    public SuperBlock(Id<SuperBlock> id, Polygon polygon) {
        this.id = id;
        this.polygon = polygon;
    }

    @Override
    public Id<SuperBlock> getId() {
        return this.id;
    }

    public boolean containsCoord(Coord coord) {
        Coordinate coordinate = new Coordinate(coord.getX(), coord.getY());
        Point point = GEOMETRY_FACTORY.createPoint(coordinate);
        return this.polygon.contains(point);
    }

    public static IdMap<SuperBlock, SuperBlock> readFromShapefile(String filePath) throws IOException {
        DataStore dataStore = DataStoreFinder.getDataStore(Collections.singletonMap("url", new File(filePath).toURI().toURL()));
        SimpleFeatureSource featureSource = dataStore.getFeatureSource(dataStore.getTypeNames()[0]);
        SimpleFeatureCollection featureCollection = featureSource.getFeatures();
        SimpleFeatureIterator featureIterator = featureCollection.features();

        List<Polygon> polygons = new LinkedList<>();

        while (featureIterator.hasNext()) {
            SimpleFeature feature = featureIterator.next();
            Geometry geometry = (Geometry) feature.getDefaultGeometry();

            if (geometry instanceof MultiPolygon multiPolygon) {

                if (multiPolygon.getNumGeometries() != 1) {
                    throw new IllegalStateException("Extent shape is non-connected.");
                }

                polygons.add((Polygon) multiPolygon.getGeometryN(0));
            } else if (geometry instanceof Polygon) {
                polygons.add((Polygon) geometry);
            } else {
                throw new IllegalStateException("Expecting polygon geometry!");
            }
        }

        featureIterator.close();
        dataStore.dispose();

        IdMap<SuperBlock, SuperBlock> superBlocks =  new IdMap<>(SuperBlock.class);
        for(int i=0; i<polygons.size(); i++) {
            Id<SuperBlock> superBlockId = Id.create(String.valueOf(i), SuperBlock.class);
            superBlocks.put(superBlockId, new SuperBlock(superBlockId, polygons.get(i)));
        }

        return superBlocks;
    }
}
