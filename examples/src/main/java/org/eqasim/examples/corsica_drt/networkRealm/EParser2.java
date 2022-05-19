package org.eqasim.examples.corsica_drt.networkRealm;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.InvalidGridGeometryException;
import org.geotools.data.DataSourceException;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.DirectPosition2D;
import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.bicycle.network.ElevationDataParser;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.opengis.referencing.operation.TransformException;

import java.awt.image.Raster;
import java.io.IOException;

public class EParser2  extends ElevationDataParser{
    private static GridCoverage2D grid;
    private static Raster gridData;
    private CoordinateTransformation ct;
    public static void main(String[] args) {

    }


    public EParser2(String tiffFile, String scenarioCRS) {
        super(tiffFile,scenarioCRS);
        this.ct = TransformationFactory.getCoordinateTransformation( "EPSG:3035",scenarioCRS);

            GeoTiffReader reader = null;
            try {
                reader = new GeoTiffReader(tiffFile);
            } catch (DataSourceException e) {
                e.printStackTrace();
            }

            try {
                grid = reader.read(null);
            } catch (IOException e) {
                e.printStackTrace();
            }
            gridData = grid.getRenderedImage().getData();
        }


        public double getElevation(double x, double y) {
            return getElevation(CoordUtils.createCoord(x, y));
        }


        public double getElevation(Coord coord) {
            GridGeometry2D gg = grid.getGridGeometry();

            Coord transformedCoord = ct.transform(coord);

            GridCoordinates2D posGrid = null;
            try {
                posGrid = gg.worldToGrid(new DirectPosition2D(transformedCoord.getX(), transformedCoord.getY()));
            } catch (InvalidGridGeometryException e) {
                e.printStackTrace();
            } catch (TransformException e) {
                e.printStackTrace();
            }

            double[] pixel = new double[1];
            double[] data = gridData.getPixel(posGrid.x, posGrid.y, pixel);
            return data[0];
        }
    }



