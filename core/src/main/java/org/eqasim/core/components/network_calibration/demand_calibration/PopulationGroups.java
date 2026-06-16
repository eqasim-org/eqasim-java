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
 * The algorithm builds a quadtree starting with cells of initialCellSize.
 * Any cell that exceeds maxPopulationPerCell is split into 4 equal children,
 * down to a minimum cell size of minCellSize. Each leaf cell becomes one group.
 */
public class PopulationGroups {

    // -------------------------------------------------------------------------
    // Default resolution constants
    // -------------------------------------------------------------------------

    private static final double DEFAULT_INITIAL_CELL_SIZE   = 16_000.0; // 12 km
    private static final double DEFAULT_MIN_CELL_SIZE       =    2000.0; // 1.5 km
    private static final int    DEFAULT_MAX_POPULATION_PER_CELL = 2_000;

    // -------------------------------------------------------------------------
    // Immutable configuration (set once at build time, never change)
    // -------------------------------------------------------------------------

    private final Scenario scenario;
    private final double   sampleSize;
    private final boolean  considerSubpopulations;

    // -------------------------------------------------------------------------
    // Mutable resolution parameters (can be updated by reBuild)
    // -------------------------------------------------------------------------

    private double initialCellSize;
    private double minCellSize;
    private int    maxPopulationPerCell;   // already scaled by sampleSize

    // -------------------------------------------------------------------------
    // Derived state (rebuilt whenever resolution changes)
    // -------------------------------------------------------------------------

    private final IdMap<Person, Integer> personToGroup;
    private final Map<GridIndex, Cell>   topLevelCells;
    private final Map<GridIndex, Integer> outsideGridLookupCache;
    private int size;

    // -------------------------------------------------------------------------
    // Private constructor — only called from buildInternal
    // -------------------------------------------------------------------------

    private PopulationGroups(Scenario scenario,
                             double sampleSize,
                             boolean considerSubpopulations,
                             double initialCellSize,
                             double minCellSize,
                             int maxPopulationPerCell) {
        this.scenario               = scenario;
        this.sampleSize             = sampleSize;
        this.considerSubpopulations = considerSubpopulations;

        this.initialCellSize        = initialCellSize;
        this.minCellSize            = minCellSize;
        this.maxPopulationPerCell   = maxPopulationPerCell;

        this.personToGroup          = new IdMap<>(Person.class);
        this.topLevelCells          = new HashMap<>();
        this.outsideGridLookupCache = new HashMap<>();
        this.size                   = 0;
    }

    // =========================================================================
    // Public API
    // =========================================================================

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

        GridIndex index = gridIndexOf(coord, initialCellSize);
        Cell topCell = topLevelCells.get(index);
        if (topCell != null) {
            return findGroup(topCell, coord);
        }

        return outsideGridLookupCache.computeIfAbsent(index, this::findNearestGroup);
    }

    public int size() {
        return size;
    }

    /** Returns the initial cell size currently in use. */
    public double getInitialCellSize() { return initialCellSize; }

    /** Returns the minimum cell size currently in use. */
    public double getMinCellSize() { return minCellSize; }

    /**
     * Returns the effective max-population-per-cell currently in use
     * (already scaled by the sample size that was supplied at build time).
     */
    public int getMaxPopulationPerCell() { return maxPopulationPerCell; }

    /** Returns the sample size that was fixed at build time. */
    public double getSampleSize() { return sampleSize; }

    /** Returns whether subpopulation filtering was fixed at build time. */
    public boolean isConsiderSubpopulations() { return considerSubpopulations; }

    // =========================================================================
    // Factory methods
    // =========================================================================

    /** Build with default resolution parameters. */
    public static PopulationGroups build(Scenario scenario,
                                         double sampleSize,
                                         boolean considerSubpopulations) {
        return build(scenario, sampleSize, considerSubpopulations,
                DEFAULT_INITIAL_CELL_SIZE,
                DEFAULT_MIN_CELL_SIZE,
                DEFAULT_MAX_POPULATION_PER_CELL);
    }

    /** Build with explicit resolution parameters. */
    public static PopulationGroups build(Scenario scenario,
                                         double sampleSize,
                                         boolean considerSubpopulations,
                                         double initialCellSize,
                                         double minCellSize,
                                         int maxPopulationPerCell) {
        assert sampleSize > 0 && sampleSize <= 1 : "Sample size must be in (0, 1]";

        // Scale the population threshold to the sample size once, at construction.
        int scaledMax = (int) Math.ceil(maxPopulationPerCell * sampleSize);

        PopulationGroups pg = new PopulationGroups(
                scenario, sampleSize, considerSubpopulations,
                initialCellSize, minCellSize, scaledMax);

        pg.buildInternal();
        return pg;
    }

    // =========================================================================
    // Rebuild (in-place) — only resolution parameters may change
    // =========================================================================

    /**
     * Rebuilds the grouping in-place using new resolution parameters.
     * The immutable fields ({@code scenario}, {@code sampleSize},
     * {@code considerSubpopulations}) are preserved automatically.
     *
     * @param newInitialCellSize      new top-level cell size (e.g. 6_000.0 for 6 km)
     * @param newMinCellSize          new minimum leaf cell size (e.g. 375.0)
     * @param newMaxPopulationPerCell new max agents per cell <em>before</em> sample scaling
     */
    public void reBuild(double newInitialCellSize,
                        double newMinCellSize,
                        int    newMaxPopulationPerCell) {
        this.initialCellSize      = newInitialCellSize;
        this.minCellSize          = newMinCellSize;
        this.maxPopulationPerCell = (int) Math.ceil(newMaxPopulationPerCell * sampleSize);

        // Clear all derived state before rebuilding.
        personToGroup.clear();
        topLevelCells.clear();
        outsideGridLookupCache.clear();
        size = 0;

        buildInternal();
    }

    // =========================================================================
    // Core build logic (shared by build and reBuild)
    // =========================================================================

    /**
     * Populates {@code personToGroup}, {@code topLevelCells}, and {@code size}
     * using the resolution parameters currently stored on the instance.
     * Assumes all three collections are empty when called.
     */
    private void buildInternal() {
        Population population = scenario.getPopulation();

        if (population.getPersons().isEmpty()) {
            return;
        }

        // --- Phase 1: insert every qualifying person into the quadtree --------
        for (Person person : population.getPersons().values()) {
            if (qualifies(person)) {
                Coord coord = Tools.getHomeLocation(person);
                Cell cell = topLevelCells.computeIfAbsent(
                        gridIndexOf(coord, initialCellSize),
                        idx -> createTopLevelCell(idx, initialCellSize)
                );
                insert(cell, coord, maxPopulationPerCell, minCellSize);
            }
        }

        // --- Phase 2: assign group IDs in a deterministic spatial order -------
        List<Cell> sortedTopLevel = new ArrayList<>(topLevelCells.values());
        sortedTopLevel.sort(
                Comparator.comparingDouble((Cell c) -> c.minX)
                        .thenComparingDouble(c -> c.minY)
        );

        int[] nextId = { 0 };
        for (Cell cell : sortedTopLevel) {
            assignGroupIds(cell, nextId);
        }
        size = nextId[0];

        // --- Phase 3: map every person to their group ID ----------------------
        for (Person person : population.getPersons().values()) {
            if (qualifies(person)) {
                Coord coord    = Tools.getHomeLocation(person);
                Cell  topCell  = topLevelCells.get(gridIndexOf(coord, initialCellSize));
                personToGroup.put(person.getId(), findGroup(topCell, coord));
            } else {
                personToGroup.put(person.getId(), -1);
            }
        }
    }

    /** Returns true if this person should be assigned a real group. */
    private boolean qualifies(Person person) {
        boolean isSubpopulation = Tools.isInSubPopulation(person);
        boolean carAvailable    = Tools.isCarAvailable(person);
        return (considerSubpopulations || !isSubpopulation) && carAvailable;
    }

    // =========================================================================
    // Quadtree cell
    // =========================================================================

    private static class Cell {
        final double minX, minY, maxX, maxY;

        int population = 0;
        int groupId    = -1;

        Cell[]      children = null;
        List<Coord> members  = new ArrayList<>();

        Cell(double minX, double minY, double maxX, double maxY) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
        }

        boolean isLeaf() { return children == null; }

        double size()  { return Math.max(maxX - minX, maxY - minY); }
        double midX()  { return (minX + maxX) * 0.5; }
        double midY()  { return (minY + maxY) * 0.5; }
    }

    // =========================================================================
    // Grid index for the top-level cells
    // =========================================================================

    private record GridIndex(int x, int y) {}

    private static GridIndex gridIndexOf(Coord coord, double cellSize) {
        return new GridIndex(
                (int) Math.floor(coord.getX() / cellSize),
                (int) Math.floor(coord.getY() / cellSize)
        );
    }

    private static Cell createTopLevelCell(GridIndex index, double cellSize) {
        double minX = index.x() * cellSize;
        double minY = index.y() * cellSize;
        return new Cell(minX, minY, minX + cellSize, minY + cellSize);
    }

    // =========================================================================
    // Quadtree operations (stateless static helpers — algorithm unchanged)
    // =========================================================================

    private static void insert(Cell cell, Coord coord,
                               int maxPopulationPerCell, double minCellSize) {
        cell.population++;

        if (!cell.isLeaf()) {
            insert(childFor(cell, coord), coord, maxPopulationPerCell, minCellSize);
            return;
        }

        cell.members.add(coord);

        boolean tooPopulated = cell.population > maxPopulationPerCell;
        boolean canSplit     = cell.size() > minCellSize;

        if (tooPopulated && canSplit) {
            splitCell(cell, maxPopulationPerCell, minCellSize);
        }
    }

    private static void splitCell(Cell cell, int maxPopulationPerCell, double minCellSize) {
        double midX = cell.midX();
        double midY = cell.midY();

        cell.children = new Cell[] {
                new Cell(cell.minX, cell.minY, midX,      midY),
                new Cell(midX,      cell.minY, cell.maxX, midY),
                new Cell(cell.minX, midY,      midX,      cell.maxY),
                new Cell(midX,      midY,      cell.maxX, cell.maxY)
        };

        List<Coord> toRedistribute = cell.members;
        cell.members = null;

        for (Coord member : toRedistribute) {
            insert(childFor(cell, member), member, maxPopulationPerCell, minCellSize);
        }
    }

    private static Cell childFor(Cell cell, Coord coord) {
        int index = 0;
        if (coord.getX() >= cell.midX()) index += 1;
        if (coord.getY() >= cell.midY()) index += 2;
        return cell.children[index];
    }

    private static void assignGroupIds(Cell cell, int[] nextId) {
        if (cell.isLeaf()) {
            cell.groupId = nextId[0]++;
            return;
        }
        for (Cell child : cell.children) {
            assignGroupIds(child, nextId);
        }
    }

    private static int findGroup(Cell cell, Coord coord) {
        while (!cell.isLeaf()) {
            cell = childFor(cell, coord);
        }
        return cell.groupId;
    }

    // =========================================================================
    // Nearest-group fallback for coordinates outside the populated grid
    // =========================================================================

    private int findNearestGroup(GridIndex targetIndex) {
        Cell   nearest         = null;
        double nearestDistance = Double.POSITIVE_INFINITY;

        double centerX = (targetIndex.x() + 0.5) * initialCellSize;
        double centerY = (targetIndex.y() + 0.5) * initialCellSize;

        for (Cell cell : topLevelCells.values()) {
            double cellCenterX = (cell.minX + cell.maxX) * 0.5;
            double cellCenterY = (cell.minY + cell.maxY) * 0.5;

            double dx = centerX - cellCenterX;
            double dy = centerY - cellCenterY;
            double squaredDistance = dx * dx + dy * dy;

            if (squaredDistance < nearestDistance) {
                nearestDistance = squaredDistance;
                nearest         = cell;
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