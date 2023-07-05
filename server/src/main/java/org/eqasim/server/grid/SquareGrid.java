package org.eqasim.server.grid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import org.eqasim.server.grid.GridSource.Bounds;

public class SquareGrid<T> {
	private final double cellSize;

	private final int minU;
	private final int maxU;
	private final int minV;
	private final int maxV;

	private final int sizeU;
	private final int sizeV;

	private final ArrayList<T> cells;

	public SquareGrid(GridFactory<T> factory, double cellSize, GridSource source) {
		this.cellSize = cellSize;

		Bounds bounds = source.getBounds();
		this.minU = (int) Math.floor(bounds.minX / cellSize);
		this.maxU = (int) Math.floor(bounds.maxX / cellSize);
		this.minV = (int) Math.floor(bounds.minY / cellSize);
		this.maxV = (int) Math.floor(bounds.maxY / cellSize);

		this.sizeU = maxU - minU;
		this.sizeV = maxV - minV;

		this.cells = new ArrayList<>(sizeU * sizeV);

		for (int v = minV; v < maxV; v++) {
			for (int u = minU; u < maxU; u++) {
				cells.add(factory.createCell(u, v, u * cellSize, v * cellSize));
			}
		}
	}

	public T getCell(double x, double y) {
		int u = (int) Math.floor(x / cellSize);
		int v = (int) Math.floor(y / cellSize);

		u = Math.max(Math.min(u, maxU - 1), minU) - minU;
		v = Math.max(Math.min(v, maxV - 1), minV) - minV;

		return cells.get(v * sizeU + u);
	}

	public Collection<T> getCells(double x, double y, double radius) {
		int ul = (int) Math.floor((x - radius) / cellSize);
		int vl = (int) Math.floor((y - radius) / cellSize);

		int uu = (int) Math.floor((x + radius) / cellSize);
		int vu = (int) Math.floor((y + radius) / cellSize);

		double radius2 = radius * radius;
		List<T> selection = new LinkedList<>();

		for (int u = ul; u <= uu; u++) {
			for (int v = vl; v <= vu; v++) {
				double cellX = u * cellSize;
				double cellY = v * cellSize;

				if (Math.pow(cellX - x, 2.0) + Math.pow(cellY - y, 2.0) < radius2) {
					int ux = Math.max(Math.min(u, maxU - 1), minU) - minU;
					int vx = Math.max(Math.min(v, maxV - 1), minV) - minV;

					selection.add(cells.get(vx * sizeU + ux));
				}
			}
		}

		return selection;
	}

	public Stream<T> stream() {
		return cells.stream();
	}
}
