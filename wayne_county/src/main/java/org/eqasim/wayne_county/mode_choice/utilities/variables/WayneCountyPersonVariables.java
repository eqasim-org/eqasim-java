package org.eqasim.wayne_county.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;

public class WayneCountyPersonVariables implements BaseVariables {
    public final int hhlIncomeClass;
	public WayneCountyPersonVariables(int hhlIncomeClass) {

		this.hhlIncomeClass = hhlIncomeClass;
	}
}
