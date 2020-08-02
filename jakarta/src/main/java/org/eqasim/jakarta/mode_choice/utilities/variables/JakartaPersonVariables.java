package org.eqasim.jakarta.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;

public class JakartaPersonVariables implements BaseVariables {
//	public final boolean hasSubscription;
	public double hhlIncome;
	public int age;
	public String sex;
//	public boolean cityTrip;

	public JakartaPersonVariables(double hhlIncome, int age, String sex) {
//		this.hasSubscription = hasSubscription;
//		this.cityTrip = cityTrip;
		this.hhlIncome = hhlIncome;
		this.age = age;
		this.sex = sex;
	}
}
