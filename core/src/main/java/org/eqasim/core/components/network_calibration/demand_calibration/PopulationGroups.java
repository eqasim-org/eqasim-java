package org.eqasim.core.components.network_calibration.demand_calibration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;

/**
 * Groups persons into spatial cells based on their home location.
 *
 * The algorithm builds a quadtree starting with cells of INITIAL_CELL_SIZE.
 * Any cell that exceeds MAX_POPULATION_PER_CELL is split into 4 equal children,
 * down to a minimum cell size of MIN_CELL_SIZE. Each leaf cell becomes one group.
 */
public class PopulationGroups {

    private static final double INITIAL_CELL_SIZE = 10_000.0; // 10 km
    private static final double MIN_CELL_SIZE = 1_000.0;      // 1 km
    private static final int MAX_POPULATION_PER_CELL = 5_000;

    private final IdMap<Person, Integer> personToGroup;
    private final Map<GridIndex, Cell> topLevelCells;
    private final Map<GridIndex, Integer> outsideGridLookupCache = new HashMap<>();
    private final int size;

    private PopulationGroups(IdMap<Person, Integer> personToGroup, Map<GridIndex, Cell> topLevelCells, int size) {
        this.personToGroup = personToGroup;
        this.topLevelCells = topLevelCells;
        this.size = size;
    }

    public int getGroup(Person person) {
        Integer group = personToGroup.get(person.getId());
        if (group != null) {
            return group;
        }

        return getGroup(Tools.getHomeLocation(person));
    }

    public int getGroup(Coord coord) {
        if (topLevelCells.isEmpty()) {
            return 0;
        }

        GridIndex index = gridIndexOf(coord);
        Cell topCell = topLevelCells.get(index);
        if (topCell != null) {
            return findGroup(topCell, coord);
        }

        // Outside covered top-level cells: map to nearest populated top-level cell.
        return outsideGridLookupCache.computeIfAbsent(index, this::findNearestGroup);
    }

    public int size(){
        return size;
    }
    // -------------------------------------------------------------------------
    // Quadtree cell
    // -------------------------------------------------------------------------

    private static class Cell {
        final double minX, minY, maxX, maxY;

        int population = 0;
        int groupId = -1;

        Cell[] children = null;           // non-null when this cell has been split
        List<Coord> members = new ArrayList<>(); // only used on leaf cells

        Cell(double minX, double minY, double maxX, double maxY) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
        }

        boolean isLeaf() {
            return children == null;
        }

        double size() {
            return Math.max(maxX - minX, maxY - minY);
        }

        double midX() { return (minX + maxX) * 0.5; }
        double midY() { return (minY + maxY) * 0.5; }
    }

    // -------------------------------------------------------------------------
    // Grid index for the top-level cells
    // -------------------------------------------------------------------------

    private record GridIndex(int x, int y) {}

    private static GridIndex gridIndexOf(Coord coord) {
        return new GridIndex(
                (int) Math.floor(coord.getX() / INITIAL_CELL_SIZE),
                (int) Math.floor(coord.getY() / INITIAL_CELL_SIZE)
        );
    }

    private static Cell createTopLevelCell(GridIndex index) {
        double minX = index.x() * INITIAL_CELL_SIZE;
        double minY = index.y() * INITIAL_CELL_SIZE;
        return new Cell(minX, minY, minX + INITIAL_CELL_SIZE, minY + INITIAL_CELL_SIZE);
    }

    // -------------------------------------------------------------------------
    // Quadtree operations
    // -------------------------------------------------------------------------

    /** Inserts a coordinate into the tree rooted at {@code cell}. */
    private static void insert(Cell cell, Coord coord) {
        cell.population++;

        if (!cell.isLeaf()) {
            insert(childFor(cell, coord), coord);
            return;
        }

        // Leaf node: record the member, then split if necessary.
        cell.members.add(coord);

        boolean tooPopulated = cell.population > MAX_POPULATION_PER_CELL;
        boolean canSplit     = cell.size() * 0.5 >= MIN_CELL_SIZE;

        if (tooPopulated && canSplit) {
            splitCell(cell);
        }
    }

    /** Splits a leaf cell into 4 children and moves its members down. */
    private static void splitCell(Cell cell) {
        double midX = cell.midX();
        double midY = cell.midY();

        cell.children = new Cell[] {
                new Cell(cell.minX, cell.minY, midX,      midY),      // SW
                new Cell(midX,      cell.minY, cell.maxX, midY),      // SE
                new Cell(cell.minX, midY,      midX,      cell.maxY), // NW
                new Cell(midX,      midY,      cell.maxX, cell.maxY)  // NE
        };

        // Redistribute existing members to the children.
        List<Coord> toRedistribute = cell.members;
        cell.members = null; // free memory; this cell is no longer a leaf

        for (Coord member : toRedistribute) {
            insert(childFor(cell, member), member);
        }
    }

    /** Returns the child cell that contains {@code coord}. */
    private static Cell childFor(Cell cell, Coord coord) {
        int index = 0;
        if (coord.getX() >= cell.midX()) index += 1;
        if (coord.getY() >= cell.midY()) index += 2;
        return cell.children[index];
    }

    // -------------------------------------------------------------------------
    // Group ID assignment and lookup
    // -------------------------------------------------------------------------

    /** Assigns a unique group ID to every leaf cell (depth-first order). */
    private static void assignGroupIds(Cell cell, int[] nextId) {
        if (cell.isLeaf()) {
            cell.groupId = nextId[0]++;
            return;
        }
        for (Cell child : cell.children) {
            assignGroupIds(child, nextId);
        }
    }

    /** Traverses the tree to find the group ID for the given coordinate. */
    private static int findGroup(Cell cell, Coord coord) {
        while (!cell.isLeaf()) {
            cell = childFor(cell, coord);
        }
        return cell.groupId;
    }

    // -------------------------------------------------------------------------
    // Public factory
    // -------------------------------------------------------------------------

    public static PopulationGroups build(Scenario scenario) {
        Population population = scenario.getPopulation();
        IdMap<Person, Integer> personToGroup = new IdMap<>(Person.class);
        Map<GridIndex, Cell> topLevelCells = new HashMap<>();

        if (population.getPersons().isEmpty()) {
            return new PopulationGroups(personToGroup, topLevelCells, 0);
        }

        // Pass 1 — build adaptive quadtrees, one per top-level grid cell.
        for (Person person : population.getPersons().values()) {
            Coord coord = Tools.getHomeLocation(person);
            Cell cell = topLevelCells.computeIfAbsent(
                    gridIndexOf(coord),
                    PopulationGroups::createTopLevelCell
            );
            insert(cell, coord);
        }

        // Pass 2 — assign group IDs to leaf cells, sorted for determinism.
        List<Cell> sortedTopLevel = new ArrayList<>(topLevelCells.values());
        sortedTopLevel.sort(
                Comparator.comparingDouble((Cell c) -> c.minX)
                        .thenComparingDouble(c -> c.minY)
        );

        int[] nextId = { 0 };
        for (Cell cell : sortedTopLevel) {
            assignGroupIds(cell, nextId);
        }

        // Pass 3 — map each person to their leaf cell's group ID.
        for (Person person : population.getPersons().values()) {
            Coord coord = Tools.getHomeLocation(person);
            Cell topCell = topLevelCells.get(gridIndexOf(coord));
            personToGroup.put(person.getId(), findGroup(topCell, coord));
        }

        return new PopulationGroups(personToGroup, topLevelCells, nextId[0]);
    }

    private int findNearestGroup(GridIndex targetIndex) {
        Cell nearest = null;
        double nearestDistance = Double.POSITIVE_INFINITY;

        double centerX = (targetIndex.x() + 0.5) * INITIAL_CELL_SIZE;
        double centerY = (targetIndex.y() + 0.5) * INITIAL_CELL_SIZE;

        for (Cell cell : topLevelCells.values()) {
            double cellCenterX = (cell.minX + cell.maxX) * 0.5;
            double cellCenterY = (cell.minY + cell.maxY) * 0.5;

            double dx = centerX - cellCenterX;
            double dy = centerY - cellCenterY;
            double squaredDistance = dx * dx + dy * dy;

            if (squaredDistance < nearestDistance) {
                nearestDistance = squaredDistance;
                nearest = cell;
            }
        }

        if (nearest == null) {
            return 0;
        }

        double nearestCenterX = (nearest.minX + nearest.maxX) * 0.5;
        double nearestCenterY = (nearest.minY + nearest.maxY) * 0.5;
        return findGroup(nearest, new Coord(nearestCenterX, nearestCenterY));
    }
}