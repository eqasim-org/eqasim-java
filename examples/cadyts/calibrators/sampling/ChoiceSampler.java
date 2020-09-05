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
package cadyts.calibrators.sampling;

import cadyts.demand.Plan;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 * @param <L>
 *            the link type
 */
public interface ChoiceSampler<L> {

	/**
	 * Whenever an agent is about to make a choice, draw plans according to this
	 * agent's behavioral model (i.e., this agents prior choice distribution)
	 * until this function returns an "accept". The first accepted plan can be
	 * considered a draw from the agent's behavioral posterior distribution.
	 * <em>It is of greatest importance that the
	 * agent does indeed implement the first accepted plan!</em>
	 * 
	 * @param plan
	 *            the plan under consideration, must be a draw from the
	 *            behavioral prior distribution
	 * @return if the plan is accepted
	 */
	public boolean isAccepted(final Plan<L> plan);

	/**
	 * Enforces that the next proposed plan is accepted.
	 */
	public void enforceNextAccept();

}
