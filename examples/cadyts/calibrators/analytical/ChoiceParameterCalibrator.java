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
package cadyts.calibrators.analytical;

import java.util.List;
import java.util.logging.Logger;

import cadyts.demand.Plan;
import cadyts.supply.SimResults;
import cadyts.utilities.math.Cholesky;
import cadyts.utilities.math.CholeskyModified;
import cadyts.utilities.math.Matrix;
import cadyts.utilities.math.Vector;

/**
 * A Calibrator that is applicable to analytical demand simulators that
 * enumerate choice sets and provide a choice probability for every element of
 * every choice set.
 * 
 * @author Gunnar Flötteröd
 * 
 * @param <L>
 *            the network link type
 */
public class ChoiceParameterCalibrator<L> extends AnalyticalCalibrator<L> {

	// -------------------- CONSTANTS --------------------

	// MISC

	private static final long serialVersionUID = 1L;

	// DEFAULT PARAMETERS

	public static final double DEFAULT_INITIAL_STEP_SIZE = 1.0;

	public static final double DEFAULT_MSA_EXPONENT = 1.0;

	public static final boolean DEFAULT_USE_APPROXIMATE_NEWTON = true;

	public static final Vector DEFAULT_INITIAL_PARAMETERS = null;

	public static final Matrix DEFAULT_INITIAL_PARAMETER_COVARIANCE = null;

	public static final Matrix DEFAULT_INITIAL_PARAMETER_HESSIAN = null;

	// -------------------- MEMBER VARIABLES --------------------

	// PARAMETERS

	private final int parameterDimension;

	protected double initialStepSize = DEFAULT_INITIAL_STEP_SIZE;

	protected double msaExponent = DEFAULT_MSA_EXPONENT;

	protected boolean useApproximateNewton = DEFAULT_USE_APPROXIMATE_NEWTON;

	protected Vector initialParameters = DEFAULT_INITIAL_PARAMETERS;

	private Matrix initialParameterCovariance = DEFAULT_INITIAL_PARAMETER_COVARIANCE;

	protected Matrix initialParameterHessian = DEFAULT_INITIAL_PARAMETER_HESSIAN;

	// RUNTIME

	protected Vector parameters;

	protected Vector dLL_dParameters;

	protected Matrix paramCov;

	private final Matrix d2W_dbdb;

	// -------------------- CONSTRUCTION --------------------

	public ChoiceParameterCalibrator(final String logFile,
			final Long randomSeed, final int timeBinSize_s,
			final int parameterDimension) {

		super(logFile, randomSeed, timeBinSize_s);

		if (parameterDimension <= 0) {
			throw new IllegalArgumentException(
					"parameterDimension must be strictly positive");
		}
		this.parameterDimension = parameterDimension;

		Logger.getLogger(this.myName).info(
				"parameterDimension is " + this.parameterDimension);
		Logger.getLogger(this.myName).info(
				"default initialStepSize is " + this.initialStepSize);
		Logger.getLogger(this.myName).info(
				"default msaExponent is " + this.msaExponent);
		Logger.getLogger(this.myName).info(
				"default useApproximateNetwon is " + this.useApproximateNewton);
		Logger.getLogger(this.myName).info(
				"default initialParameters are " + this.initialParameters);
		Logger.getLogger(this.myName).info(
				"default initialParameterCovariance is "
						+ this.initialParameterCovariance);

		this.parameters = new Vector(parameterDimension);
		this.dLL_dParameters = new Vector(parameterDimension);
		this.paramCov = null;
		this.d2W_dbdb = new Matrix(parameterDimension, parameterDimension);
	}

	// -------------------- SETTERS & GETTERS --------------------

	public void setInitialStepSize(final double initialStepSize) {
		if (initialStepSize < 0) {
			throw new IllegalArgumentException(
					"initialStepSize must not be negative");
		} else if (initialStepSize == 0) {
			Logger.getLogger(this.myName).warning(
					"initialStepSize of zero means "
							+ "that initialParameters will be maintained");
		}
		this.initialStepSize = initialStepSize;
		Logger.getLogger(this.myName).info(
				"set initialStepSize to " + this.initialStepSize);
	}

	public double getInitialStepSize() {
		return this.initialStepSize;
	}

	public void setMsaExponent(final double msaExponent) {
		if (msaExponent < 0) {
			throw new IllegalArgumentException(
					"msaExponent must be non-negative");
		}
		if (msaExponent != 0 && (msaExponent <= 0.5 || msaExponent > 1.0)) {
			Logger.getLogger(this.myName).warning(
					"nonzero msaExponent values outside of (0.5, 1.0] "
							+ "are not recommended");
		}
		this.msaExponent = msaExponent;
		Logger.getLogger(this.myName).info(
				"set msaExponent to " + this.msaExponent);
	}

	public double getMsaExponent() {
		return this.msaExponent;
	}

	public void setUseApproximateNetwton(final boolean useApproximateNewton) {
		this.useApproximateNewton = useApproximateNewton;
		Logger.getLogger(this.myName).info(
				"set useApproximateNewton to " + this.useApproximateNewton);
	}

	public boolean getUseApproximateNewton() {
		return this.useApproximateNewton;
	}

	public void setInitialParameters(final Vector initialParameters) {
		if (initialParameters != null
				&& initialParameters.size() != this.parameterDimension) {
			throw new IllegalArgumentException("dimension of "
					+ initialParameters + " is not " + this.parameterDimension);
		}
		this.initialParameters = initialParameters;
		Logger.getLogger(this.myName).info(
				"set initialParameters to " + this.initialParameters);

		if (initialParameters == null) {
			this.parameters.clear();
		} else {
			this.parameters.copy(initialParameters);
		}
		Logger.getLogger(this.myName).info(
				"set current parameter estimate to " + this.parameters);
	}

	public Vector getInitialParameters() {
		return this.initialParameters;
	}

	public void setInitialParameterCovariance(
			final Matrix initialParameterCovariance) {
		if (initialParameterCovariance != null
				&& (initialParameterCovariance.rowSize() != this.parameterDimension || initialParameterCovariance
						.columnSize() != this.parameterDimension)) {
			throw new IllegalArgumentException("dimension of "
					+ initialParameterCovariance.toSingleLineString()
					+ " is not " + this.parameterDimension + " x "
					+ this.parameterDimension);
		}
		this.initialParameterCovariance = initialParameterCovariance;
		Logger.getLogger(this.myName).info(
				"set initialParameterCovariance to "
						+ (this.initialParameterCovariance == null ? null
								: this.initialParameterCovariance
										.toSingleLineString()));

		if (initialParameterCovariance == null) {
			this.initialParameterHessian = null;
		} else {
			final Cholesky c = new Cholesky();
			if (!c.invert(this.initialParameterCovariance)) {
				throw new IllegalArgumentException(
						"could not invert initialParameterCovariance");
			}
			this.initialParameterHessian = c.getResult();
			this.initialParameterHessian.mult(-1.0);
		}
	}

	public Matrix getInitialParameterCovariance() {
		return this.initialParameterCovariance.newImmutableView();
	}

	public void setInitialParameterVariances(
			final Vector initialParameterVariances) {
		if (initialParameterVariances != null) {
			this.setInitialParameterCovariance(Matrix
					.newDiagonal(initialParameterVariances));
		} else {
			this.setInitialParameterCovariance(null);
		}
	}

	public Vector getParameters() {
		return this.parameters.newImmutableView();
	}

	public Vector get_dLL_dParam() {
		return this.dLL_dParameters.newImmutableView();
	}

	public Matrix getParameterCovariance() {
		return (this.paramCov == null ? null : this.paramCov.newImmutableView());
	}

	// ---------- OVERRIDING AND -LOADING OF AnalyticalCalibrator ----------

	@Override
	public int selectPlan(final List<? extends Plan<L>> plans,
			final Vector choiceProbs) {
		throw new UnsupportedOperationException("for parameter calibration, "
				+ "use selectPlan(Integer, List, Vector, Matrix, "
				+ "List<? extends Matrix>) instead");
	}

	/**
	 * @param overrideChoice
	 *            the index of the plan chosen by the simulation; if it is null,
	 *            the calibrator makes a choice according to the (non-null)
	 *            choiceProbs
	 * @param plans
	 *            the plan choice set
	 * @param choiceProbs
	 *            the probabilities of the plan choice set; may be null if
	 *            overrideChoice is not null
	 * @param dChoiceProb_dParam
	 *            the Jacobian of the choice probability vector with respect to
	 *            the parameters; may be null in order to indicate an all-zero
	 *            matrix
	 * @param d2ChoiceProb_dParam2
	 *            the Hessians of all choice probabilities with respect to the
	 *            parameter vector; may be null in order to indicate zero or
	 *            unavailable curvature information
	 * 
	 * @return the index of the selected plan
	 */
	public int selectPlan(final Integer overrideChoice,
			final List<? extends Plan<L>> plans, final Vector choiceProbs,
			final Matrix dChoiceProb_dParam,
			final List<? extends Matrix> d2ChoiceProb_dParam2) {

		this.selectPlan(overrideChoice, plans, choiceProbs);

		if (this.getIteration() >= this.getPreparatoryIterations()
				&& choiceProbs != null && dChoiceProb_dParam != null) {

			for (int i = 0; i < plans.size(); i++) {

				final double priorP = Math.max(choiceProbs.get(i), MIN_PROB);
				final double postP = Math.max(this.getLastChoiceProb(i),
						MIN_PROB);

				this.dLL_dParameters.add(dChoiceProb_dParam.getRow(i), postP
						/ priorP);

				if (d2ChoiceProb_dParam2 != null) {
					this.d2W_dbdb.add(d2ChoiceProb_dParam2.get(i), postP
							/ priorP);
					this.d2W_dbdb.addOuterProduct(dChoiceProb_dParam.getRow(i),
							dChoiceProb_dParam.getRow(i), (-1.0) * postP
									/ priorP / priorP);
				}
			}
		}

		return this.getLastChoiceIndex();
	}

	/**
	 * @param plans
	 *            the plan choice set
	 * @param choiceProbs
	 *            the probabilities of the plan choice set; may be null if
	 *            overrideChoice is not null
	 * @param dChoiceProb_dParam
	 *            the Jacobian of the choice probability vector with respect to
	 *            the parameters; may be null in order to indicate an all-zero
	 *            matrix
	 * @param d2ChoiceProb_dParam2
	 *            the Hessians of all choice probabilities with respect to the
	 *            parameter vector; may be null in order to indicate zero or
	 *            unavailable curvature information
	 * 
	 * @return the index of the selected plan
	 */
	public int selectPlan(final List<? extends Plan<L>> plans,
			final Vector choiceProbs, final Matrix dChoiceProb_dParam,
			final List<? extends Matrix> d2ChoiceProb_dParam2) {
		return this.selectPlan(null, plans, choiceProbs, dChoiceProb_dParam,
				d2ChoiceProb_dParam2);
	}

	@Override
	public void afterNetworkLoading(final SimResults<L> simResults) {

		/*
		 * (0) call superclass for standard update, possibly continue
		 */
		super.afterNetworkLoading(simResults);
		if (this.getIteration() <= this.getPreparatoryIterations()) {
			return;
		}

		/*
		 * (1) invert negative Hessian of posterior entropy function
		 */
		if (this.initialParameterHessian != null) {
			this.d2W_dbdb.add(this.initialParameterHessian, 1.0);
		}
		this.d2W_dbdb.mult(-1.0);
		final CholeskyModified c = new CholeskyModified(16);
		if (c.invert(this.d2W_dbdb)) {
			this.paramCov = c.getResult();
			Logger.getLogger(this.getClass().getName()).info(
					"modified Cholesky factorization succeeded after "
							+ c.getTrials() + " trials");
		} else {
			this.paramCov = null;
			Logger.getLogger(this.getClass().getName()).warning(
					"modified Cholesky factorization failed after "
							+ c.getTrials() + " trials");
		}
		this.d2W_dbdb.clear();

		/*
		 * (2) update parameters
		 */
		final Vector grad = this.get_dLL_dParam();
		Vector dir;
		if (this.useApproximateNewton && this.paramCov != null) {
			dir = this.paramCov.timesVectorFromRight(grad);
			if (dir.innerProd(grad) <= 0) {
				Logger.getLogger(this.getClass().getName()).warning(
						"approximate Newton step is no ascent direction");
			}
		} else {
			dir = grad;
		}
		this.parameters.add(dir, this.initialStepSize
				* Math.pow(this.getIteration()
						- this.getPreparatoryIterations(), -this.msaExponent));

		/*
		 * (3) re-initialize log-likelihood gradient
		 */
		if (this.initialParameters != null
				&& this.initialParameterHessian != null) {
			final Vector deviation = this.parameters.copy();
			deviation.add(this.initialParameters, -1.0);
			this.dLL_dParameters = this.initialParameterHessian
					.timesVectorFromRight(deviation);
		} else {
			this.dLL_dParameters.clear();
		}
	}
}
