/*
 * Cadyts - Calibration of dynamic traffic simulations
 *
 * Copyright 2009, 2010 Gunnar Flötteröd
 * 
 *
 * This file is part of Cadyts.
 *
 * Cadyts is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Cadyts is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Cadyts.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.floetteroed@epfl.ch
 *
 */ 
package cadyts.utilities.misc;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Logger;

import cadyts.utilities.math.BasicStatistics;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class StatisticsTracker implements Serializable {

	// -------------------- CONSTANTS --------------------

	private static final long serialVersionUID = 1L;

	private final String fileName;

	// -------------------- MEMBER VARIABLES --------------------

	private double logLikelihood = 0;

	private boolean logLikelihoodRegistered = false;

	private double logLikelihoodPredErr = 0;

	private boolean logLikelihoodPredErrRegistered = false;

	private double p2pLogLikelihood = 0;

	private boolean p2pLogLikelihoodRegistered = false;

	private final BasicStatistics linkLambda = new BasicStatistics();

	private final BasicStatistics planLambda = new BasicStatistics();

	private long choices = 0;

	// -------------------- CONSTRUCTION AND INITIALIZATION --------------------

	public StatisticsTracker(final String fileName) {
		this.fileName = fileName;
		this.clear();

		if (this.fileName != null) {
			try {
				final BufferedWriter writer = new BufferedWriter(
						new FileWriter(this.fileName, false));
				writer.write("count-ll\t");
				writer.write("count-ll-pred-err\t");
				writer.write("p2p-ll\t");
				writer.write("total-ll\t");
				writer.write("link-lambda-avg\t");
				writer.write("link-lambda-stddev\t");
				writer.write("link-lambda-min\t");
				writer.write("link-lambda-max\t");
				writer.write("plan-lambda-avg\t");
				writer.write("plan-lambda-stddev\t");
				writer.write("plan-lambda-min\t");
				writer.write("plan-lambda-max\t");
				writer.write("replan-count");
				writer.newLine();
				writer.flush();
				writer.close();
			} catch (IOException e) {
				Logger.getLogger(this.getClass().getName()).warning(
						"unable to create file: " + this.fileName);
			}
		}
	}

	public void clear() {
		this.logLikelihood = 0;
		this.logLikelihoodRegistered = false;
		this.logLikelihoodPredErr = 0;
		this.logLikelihoodPredErrRegistered = false;
		this.p2pLogLikelihood = 0;
		this.p2pLogLikelihoodRegistered = false;
		this.linkLambda.clear();
		this.planLambda.clear();
		this.choices = 0;
	}

	// -------------------- SETTERS & GETTERS --------------------

	public String getFileName() {
		return this.fileName;
	}

	// -------------------- DATA REGISTRATION --------------------

	public void registerSingleLinkLL(final double val) {
		this.logLikelihood = val;
		this.logLikelihoodRegistered = true;
	}

	public void registerSingleLinkLLPredError(final double val) {
		this.logLikelihoodPredErr = val;
		this.logLikelihoodPredErrRegistered = true;
	}

	public void registerMultiLinkLL(final double val) {
		this.p2pLogLikelihood = val;
		this.p2pLogLikelihoodRegistered = true;
	}

	public void registerLinkLambda(final double val) {
		this.linkLambda.add(val);
	}

	public void registerPlanLambda(final double val) {
		this.planLambda.add(val);
	}

	public void registerChoice() {
		this.choices++;
	}

	// -------------------- FILE WRITING --------------------

	public void writeToFile() {

		if (this.fileName == null) {
			return;
		}

		try {
			final BufferedWriter writer = new BufferedWriter(new FileWriter(
					this.fileName, true));
			writer.write(this.logLikelihoodRegistered ? Double
					.toString(this.logLikelihood) : "--");
			writer.write("\t");
			writer.write(this.logLikelihoodPredErrRegistered ? Double
					.toString(this.logLikelihoodPredErr) : "--");
			writer.write("\t");
			writer.write(this.p2pLogLikelihoodRegistered ? Double
					.toString(this.p2pLogLikelihood) : "--");
			writer.write("\t");

			if (this.logLikelihoodRegistered || this.p2pLogLikelihoodRegistered) {
				double totalLL = 0;
				if (this.logLikelihoodRegistered) {
					totalLL += this.logLikelihood;
				}
				if (this.p2pLogLikelihoodRegistered) {
					totalLL += this.p2pLogLikelihood;
				}
				writer.write(Double.toString(totalLL));
			} else {
				writer.write("--");
			}
			writer.write("\t");

			if (this.linkLambda.size() > 0) {
				writer.write(Double.toString(this.linkLambda.getAvg()));
				writer.write("\t");
				writer.write(Double.toString(this.linkLambda.getStddev()));
				writer.write("\t");
				writer.write(Double.toString(this.linkLambda.getMin()));
				writer.write("\t");
				writer.write(Double.toString(this.linkLambda.getMax()));
			} else {
				writer.write("--\t--\t--\t--");
			}
			writer.write("\t");
			if (this.planLambda.size() > 0) {
				writer.write(Double.toString(this.planLambda.getAvg()));
				writer.write("\t");
				writer.write(Double.toString(this.planLambda.getStddev()));
				writer.write("\t");
				writer.write(Double.toString(this.planLambda.getMin()));
				writer.write("\t");
				writer.write(Double.toString(this.planLambda.getMax()));
			} else {
				writer.write("--\t--\t--\t--");
			}
			writer.write("\t");
			writer.write(Long.toString(this.choices));
			writer.newLine();
			writer.flush();
			writer.close();
		} catch (IOException e) {
			Logger.getLogger(this.getClass().getName()).warning(
					"unable to append data to file: " + this.fileName);
		}
	}
}
