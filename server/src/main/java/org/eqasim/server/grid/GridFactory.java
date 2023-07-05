package org.eqasim.server.grid;

public interface GridFactory<T> {
	T createCell(int u, int v, double x, double y);
}
