package org.eqasim.examples.SMMFramework.GBFSUtils;

import org.matsim.api.core.v01.Coord;

public class Station {
    public int numVeh;
    public int numdocks;
    public Coord coord;

    public int getNumVeh() {
        return numVeh;
    }

    public void setNumVeh(int numVeh) {
        this.numVeh = numVeh;
    }

    public int getNumdocks() {
        return numdocks;
    }

    public void setNumdocks(int numdocks) {
        this.numdocks = numdocks;
    }

    public Coord getCoord() {
        return coord;
    }

    public void setCoord(Coord coord) {
        this.coord = coord;
    }

    public Station(int numVeh, int numdocks, Coord coord) {
        this.numVeh = numVeh;
        this.numdocks = numdocks;
        this.coord = coord;
    }
}
