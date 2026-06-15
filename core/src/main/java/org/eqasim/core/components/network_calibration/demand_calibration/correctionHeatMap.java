package org.eqasim.core.components.network_calibration.demand_calibration;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import static org.matsim.core.utils.io.IOUtils.getBufferedWriter;

public class correctionHeatMap {

	private static final String FILE_NAME = "CarAscsHeatMap.png";
	private static final String CSV_FILE_NAME = "CarAscsAverage.csv";
	private static final int DEFAULT_IMAGE_WIDTH = 1000;
	private static final int MIN_IMAGE_HEIGHT = 300;
	private static final int MAX_IMAGE_HEIGHT = 1400;
	private static final int PADDING = 40;
	private static final int LEGEND_WIDTH = 34;

	private correctionHeatMap() {
		// Static utility class.
	}

	public static void plotAverageCarAsc(Population population, PopulationGroups populationGroups,
										 OutputDirectoryHierarchy outputHierarchy, int iteration) {
		if (population == null || populationGroups == null || outputHierarchy == null) {
			return;
		}

		if (population.getPersons().isEmpty() || populationGroups.size() <= 0) {
			return;
		}

		GroupStats stats = buildGroupStats(population, populationGroups);
		if (!stats.hasValues) {
			return;
		}

		double widthMeters = Math.max(1.0, stats.maxX - stats.minX);
		double heightMeters = Math.max(1.0, stats.maxY - stats.minY);
		int imageWidth = DEFAULT_IMAGE_WIDTH;
		int imageHeight = clamp((int) Math.round(DEFAULT_IMAGE_WIDTH * heightMeters / widthMeters),
				MIN_IMAGE_HEIGHT, MAX_IMAGE_HEIGHT);

		int canvasWidth = imageWidth + PADDING * 2 + LEGEND_WIDTH + 10;
		int canvasHeight = imageHeight + PADDING * 2;

		BufferedImage image = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = image.createGraphics();
		try {
			g2.setColor(Color.WHITE);
			g2.fillRect(0, 0, canvasWidth, canvasHeight);

			double maxAbs = stats.maxAbsoluteAverage();
			int[][] groupsByPixel = new int[imageWidth][imageHeight];

			for (int px = 0; px < imageWidth; px++) {
				double x = stats.minX + (px + 0.5) * widthMeters / imageWidth;
				for (int py = 0; py < imageHeight; py++) {
					double y = stats.maxY - (py + 0.5) * heightMeters / imageHeight;
					int group = populationGroups.getGroup(new Coord(x, y));
					groupsByPixel[px][py] = group;
					double value = stats.averageFor(group);
					image.setRGB(PADDING + px, PADDING + py, divergingColor(value, maxAbs).getRGB());
				}
			}

			drawGroupBoundaries(image, groupsByPixel, imageWidth, imageHeight);
			drawLegend(g2, imageWidth, imageHeight, maxAbs);
			ImageIO.write(image, "png", new File(getIterationOutputFile(outputHierarchy, iteration)));
			saveAveragesCsv(stats, outputHierarchy, iteration);
		} catch (IOException e) {
			throw new RuntimeException("Could not write car ASC heat map", e);
		} finally {
			g2.dispose();
		}
	}

	private static void saveAveragesCsv(GroupStats stats, OutputDirectoryHierarchy outputHierarchy, int iteration)
			throws IOException {
		String csvPath = outputHierarchy.getIterationFilename(iteration, CSV_FILE_NAME);

		try (BufferedWriter writer = getBufferedWriter(csvPath)) {
			writer.write("group;count;averageCarAsc;sumCarAsc\n");

			for (int group = 0; group < stats.size(); group++) {
				int count = stats.countFor(group);
				double sum = stats.sumFor(group);
				double average = count > 0 ? sum / count : 0.0;

				writer.write(group + ";" + count + ";" + average + ";" + sum + "\n");
			}
		}
	}

	private static GroupStats buildGroupStats(Population population, PopulationGroups populationGroups) {
		int n = Math.max(1, populationGroups.size());
		double[] sums = new double[n];
		int[] counts = new int[n];

		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		boolean hasValues = false;

		for (Person person : population.getPersons().values()) {
			Coord home = Tools.getHomeLocation(person);
			minX = Math.min(minX, home.getX());
			minY = Math.min(minY, home.getY());
			maxX = Math.max(maxX, home.getX());
			maxY = Math.max(maxY, home.getY());

			int group = populationGroups.getGroup(person);
			if (group < 0 || group >= n) {
				continue;
			}

			sums[group] += Tools.getCarASC(person);
			counts[group] += 1;
			hasValues = true;
		}

		if (!hasValues) {
			return new GroupStats(sums, counts, 0.0, 0.0, 1.0, 1.0, false);
		}

		// Add a tiny margin so edge groups are not clipped.
		double marginX = Math.max(1.0, (maxX - minX) * 0.02);
		double marginY = Math.max(1.0, (maxY - minY) * 0.02);

		return new GroupStats(sums, counts, minX - marginX, minY - marginY, maxX + marginX, maxY + marginY, true);
	}

	private static String getIterationOutputFile(OutputDirectoryHierarchy outputHierarchy, int iteration) {
		return outputHierarchy.getIterationFilename(iteration, FILE_NAME);
	}

	private static void drawLegend(Graphics2D g2, int imageWidth, int imageHeight, double maxAbs) {
		int legendX = PADDING + imageWidth + 10;
		int legendY = PADDING;

		for (int py = 0; py < imageHeight; py++) {
			double ratio = 1.0 - py / (double) Math.max(1, imageHeight - 1);
			double value = (2.0 * ratio - 1.0) * maxAbs;
			g2.setColor(divergingColor(value, maxAbs));
			g2.drawLine(legendX, legendY + py, legendX + LEGEND_WIDTH, legendY + py);
		}

		g2.setColor(Color.DARK_GRAY);
		g2.drawRect(legendX, legendY, LEGEND_WIDTH, imageHeight);

		g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
		g2.drawString(String.format("+%.2f", maxAbs), legendX + LEGEND_WIDTH + 4, legendY + 12);
		g2.drawString("0.00", legendX + LEGEND_WIDTH + 4, legendY + imageHeight / 2 + 4);
		g2.drawString(String.format("%.2f", -maxAbs), legendX + LEGEND_WIDTH + 4, legendY + imageHeight - 4);
	}

	private static Color divergingColor(double value, double maxAbs) {
		double normalized = value / Math.max(1.0e-9, maxAbs);
		normalized = Math.max(-1.0, Math.min(1.0, normalized));

		if (normalized >= 0.0) {
			return interpolate(Color.WHITE, new Color(178, 24, 43), normalized);
		}

		return interpolate(Color.WHITE, new Color(33, 102, 172), -normalized);
	}

	private static Color interpolate(Color from, Color to, double t) {
		int r = (int) Math.round(from.getRed() + (to.getRed() - from.getRed()) * t);
		int g = (int) Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * t);
		int b = (int) Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * t);
		return new Color(clamp(r, 0, 255), clamp(g, 0, 255), clamp(b, 0, 255));
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static void drawGroupBoundaries(BufferedImage image, int[][] groupsByPixel, int imageWidth, int imageHeight) {
		int boundaryColor = new Color(220, 30, 30, 180).getRGB();

		for (int px = 0; px < imageWidth; px++) {
			for (int py = 0; py < imageHeight; py++) {
				int group = groupsByPixel[px][py];
				boolean isBoundary = (px + 1 < imageWidth && groupsByPixel[px + 1][py] != group)
						|| (py + 1 < imageHeight && groupsByPixel[px][py + 1] != group);

				if (isBoundary) {
					image.setRGB(PADDING + px, PADDING + py, boundaryColor);
				}
			}
		}
	}

	private static final class GroupStats {
		private final double[] sums;
		private final int[] counts;
		private final double minX;
		private final double minY;
		private final double maxX;
		private final double maxY;
		private final boolean hasValues;

		private GroupStats(double[] sums, int[] counts, double minX, double minY, double maxX, double maxY,
						   boolean hasValues) {
			this.sums = sums;
			this.counts = counts;
			this.minX = minX;
			this.minY = minY;
			this.maxX = maxX;
			this.maxY = maxY;
			this.hasValues = hasValues;
		}

		private double averageFor(int group) {
			if (group < 0 || group >= counts.length || counts[group] == 0) {
				return 0.0;
			}
			return sums[group] / counts[group];
		}

		private int size() {
			return counts.length;
		}

		private int countFor(int group) {
			if (group < 0 || group >= counts.length) {
				return 0;
			}
			return counts[group];
		}

		private double sumFor(int group) {
			if (group < 0 || group >= sums.length) {
				return 0.0;
			}
			return sums[group];
		}

		private double maxAbsoluteAverage() {
			double maxAbs = 0.0;
			for (int group = 0; group < counts.length; group++) {
				if (counts[group] == 0) {
					continue;
				}
				maxAbs = Math.max(maxAbs, Math.abs(sums[group] / counts[group]));
			}
			return Math.max(maxAbs, 1.0e-3);
		}
	}
}

