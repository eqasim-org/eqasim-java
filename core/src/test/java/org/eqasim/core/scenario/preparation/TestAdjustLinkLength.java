package org.eqasim.core.scenario.preparation;

import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.network.NetworkUtils;

import static org.junit.Assert.assertEquals;

public class TestAdjustLinkLength {
    @Test public void missingForeignElevationDoesNotCreateArtificialCliff() {
        assertEquals(22.2, adjusted(new Coord(0, 0, 0), new Coord(6.36, 0, 815.2), 6.36, 22.2), 1e-9);
    }
    @Test public void longEnoughSuppliedRoadLengthIsPreserved() {
        assertEquals(80, adjusted(new Coord(0, 0, 0), new Coord(70, 0, 447.2), 80, 16.67), 1e-9);
    }
    @Test public void shorterThanHorizontalDistanceIsStillCorrected() {
        assertEquals(50, adjusted(new Coord(0, 0, 400), new Coord(30, 40, 405), 20, 10), 1e-9);
    }
    @Test public void twoDimensionalAndMixedCoordinatesStillWork() {
        assertEquals(50, adjusted(new Coord(0, 0), new Coord(30, 40, 405), 20, 10), 1e-9);
        assertEquals(50, adjusted(new Coord(0, 0), new Coord(30, 40), 20, 10), 1e-9);
    }
    @Test public void realRoadLengthIncludingSlopeIsNotShortened() {
        assertEquals(120, adjusted(new Coord(0, 0, 400), new Coord(100, 0, 450), 120, 10), 1e-9);
    }
    private double adjusted(Coord from, Coord to, double length, double speed) {
        var n = NetworkUtils.createNetwork(); var f = n.getFactory();
        var a = f.createNode(Id.createNodeId("a"), from); var b = f.createNode(Id.createNodeId("b"), to);
        n.addNode(a); n.addNode(b);
        var link = f.createLink(Id.createLinkId("ab"), a, b);
        link.setLength(length); link.setFreespeed(speed); n.addLink(link);
        new AdjustLinkLength().run(n);
        return link.getLength();
    }
}
