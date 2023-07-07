package pcd.assignment03.ex2.pixelart;

import java.io.Serializable;
import java.util.Arrays;

public class PixelGrid implements Serializable {
	private final int nRows;
	private final int nColumns;
	private final int[][] grid;
	
	public PixelGrid(final int nRows, final int nColumns) {
		this.nRows = nRows;
		this.nColumns = nColumns;
		grid = new int[nRows][nColumns];
	}

	public synchronized void clear() {
		for (int i = 0; i < nRows; i++) {
			Arrays.fill(grid[i], 0);
		}
	}
	
	public synchronized void set(final int x, final int y, final int color) {
		grid[y][x] = color;
	}
	
	public synchronized int get(int x, int y) {
		return grid[y][x];
	}
	
	public synchronized int getNumRows() {
		return this.nRows;
	}
	

	public synchronized int getNumColumns() {
		return this.nColumns;
	}
	
}
