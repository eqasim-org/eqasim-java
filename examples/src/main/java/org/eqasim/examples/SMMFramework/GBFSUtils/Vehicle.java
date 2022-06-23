package org.eqasim.examples.SMMFramework.GBFSUtils;

import org.matsim.api.core.v01.Coord;
/**
 * * Representation of Vehicle for GBFS creation
 */
public class Vehicle {

    public String id;
    public Coord coord;


    public Coord getCoord() {
        return coord;
    }

    public void setCoord(Coord coord) {
        this.coord = coord;
    }

    public Vehicle(String id ,Coord coord) {
        this.id=id;
        this.coord = coord;
    }
}