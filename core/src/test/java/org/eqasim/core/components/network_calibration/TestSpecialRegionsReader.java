package org.eqasim.core.components.network_calibration;

import org.junit.Test;
import org.matsim.api.core.v01.Coord;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestSpecialRegionsReader {
    @Test
    public void testPolygonContainsPoints() throws IOException {
        Path geoJson = writeTempGeoJson("""
                {
                  "type": "FeatureCollection",
                  "features": [
                    {
                      "type": "Feature",
                      "geometry": {
                        "type": "Polygon",
                        "coordinates": [
                          [[0,0], [10,0], [10,10], [0,10], [0,0]]
                        ]
                      },
                      "properties": {}
                    }
                  ]
                }
                """);
//        Path geoJson = Path.of("C:/Users/dabdelkader/Desktop/work/codes/zurich_scenario/region.json");
        SpecialRegionsReader reader = new SpecialRegionsReader(geoJson.toString());

        assertTrue(reader.isInSpecialRegion(new Coord(5.0, 5.0)));
        assertTrue(reader.isInSpecialRegion(new Coord(0.0, 5.0)));
        assertFalse(reader.isInSpecialRegion(new Coord(12.0, 5.0)));
    }

    @Test
    public void testMultiPolygonContainsPoints() throws IOException {
        Path geoJson = writeTempGeoJson("""
                {
                  "type": "FeatureCollection",
                  "features": [
                    {
                      "type": "Feature",
                      "geometry": {
                        "type": "MultiPolygon",
                        "coordinates": [
                          [[[0,0], [2,0], [2,2], [0,2], [0,0]]],
                          [[[10,10], [12,10], [12,12], [10,12], [10,10]]]
                        ]
                      },
                      "properties": {}
                    }
                  ]
                }
                """);

        SpecialRegionsReader reader = new SpecialRegionsReader(geoJson.toString());

        assertTrue(reader.isInSpecialRegion(new Coord(1.0, 1.0)));
        assertTrue(reader.isInSpecialRegion(new Coord(11.0, 11.0)));
        assertFalse(reader.isInSpecialRegion(new Coord(5.0, 5.0)));
    }

    @Test(expected = RuntimeException.class)
    public void testMissingFileThrows() {
        new SpecialRegionsReader("does-not-exist.geojson");
    }

    private static Path writeTempGeoJson(String content) throws IOException {
        Path path = Files.createTempFile("special-regions-", ".geojson");
        Files.writeString(path, content);
        path.toFile().deleteOnExit();
        return path;
    }
}

