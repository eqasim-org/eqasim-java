package org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import java.util.Locale;

public class SwissPredictorUtils {
	static public boolean hasGeneralSubscription(Person person) {
		Boolean hasGeneralSubscription = (Boolean) person.getAttributes().getAttribute("ptHasGA");
		return hasGeneralSubscription != null && hasGeneralSubscription;
	}

	static public boolean hasHalbtaxSubscription(Person person) {
		Boolean hasHalbtaxSubscription = (Boolean) person.getAttributes().getAttribute("ptHasHalbtax");
		return hasHalbtaxSubscription != null && hasHalbtaxSubscription;
	}

	static public boolean hasRegionalSubscription(Person person) {
		boolean hasRegionalSubscription = false;

		Boolean ptHasVerbund = (Boolean) person.getAttributes().getAttribute("ptHasVerbund");
		if (ptHasVerbund != null && ptHasVerbund) {
			hasRegionalSubscription = true;
		}

		Boolean ptHasStrecke = (Boolean) person.getAttributes().getAttribute("ptHasStrecke");
		if (ptHasStrecke != null && ptHasStrecke) {
			hasRegionalSubscription = true;
		}

		return hasRegionalSubscription;
	}

	static public boolean hasJuniorSubscription(Person person) {
		boolean hasJuniorSubscription = false;
		Boolean ptHasJunior = (Boolean) person.getAttributes().getAttribute("ptHasJunior");
		if (ptHasJunior != null && ptHasJunior) {
			hasJuniorSubscription = true;
		}
		return hasJuniorSubscription;
	}

	static public boolean hasGleis7Subscription(Person person) {
		boolean hasGleis7Subscription = false;
		Boolean ptHasGleis7 = (Boolean) person.getAttributes().getAttribute("ptHasGleis7");
		if (ptHasGleis7 != null && ptHasGleis7) {
			hasGleis7Subscription = true;
		}
		return hasGleis7Subscription;
	}

	static public int getStatedPreferenceRegion(Person person) {
		Integer spRegion = (Integer) person.getAttributes().getAttribute("spRegion");
		return spRegion == null ? -1 : spRegion;
	}

	static public Double getIncomePerCapita(Person person) {
		try{
        	return (Double) person.getAttributes().getAttribute("incomePerCapita");
		} catch(Exception e){
			return 0.0;
		}
	}

	static public Double getCarOwnershipRatio(Person person) {
		try{
			return (Double) person.getAttributes().getAttribute("carOwnershipRatio");
		} catch(Exception e){
			return 0.0;
		}
	}

	static public Integer getCantonId(Person person) {
		Integer cantonId = ((Double) person.getAttributes().getAttribute("cantonId")).intValue();
		return cantonId;
	}

	static public Integer getSex(Person person) {
		Object sexAttr = person.getAttributes().getAttribute("sex");
		if ("m".equals(sexAttr)) {
			return 0;
		} else if ("f".equals(sexAttr)) {
			return 1;
		} else {
			return null;
		}
	}

	static public Integer hasDrivingLicense(Person person) {
		String licenseAttr = (String) person.getAttributes().getAttribute("hasLicense");
		if ("yes".equals(licenseAttr)) {
			return 1;
		} else if ("no".equals(licenseAttr)) {
			return 0;
		} else {
			return null;
		}
	}

	static public String getCantonName(Person person) {
		return (String) person.getAttributes().getAttribute("cantonName");
	}

	static public Coord getHomeLocation(Person person) {
		Double homeX = (Double) person.getAttributes().getAttribute("home_x");
		Double homeY = (Double) person.getAttributes().getAttribute("home_y");

		if (homeX == null || homeY == null) {
			homeX = 0.0;
			homeY = 0.0;

			for (PlanElement element : person.getSelectedPlan().getPlanElements()) {
				if (element instanceof Activity) {
					Activity activity = (Activity) element;

					if (activity.getType().equals("home")) {
						homeX = activity.getCoord().getX();
						homeY = activity.getCoord().getY();
					}
				}
			}

			person.getAttributes().putAttribute("home_x", homeX);
			person.getAttributes().putAttribute("home_y", homeY);
		}

		return new Coord(homeX, homeY);
	}


	public static int getCluster(Person person) {
		int cantonId = getCantonId(person);
		if (cantonId == 1 || cantonId == 12 || cantonId == 25) {
		//if (cantonId == 23 || cantonId == 21) {
			return 2;
		}
		if (cantonId == 5 || cantonId == 10 || cantonId == 18 || cantonId == 21 || cantonId == 22 || cantonId == 23 || cantonId == 24) {
		// if (cantonId == 4 || cantonId == 8 || cantonId == 17 || cantonId == 20 || cantonId == 7 || cantonId == 9 || cantonId == 11|| cantonId == 26) {
			return 1;
		}
		return 0;
	}

	static public String getOvgk(Person person) {
		String ovgk = (String) person.getAttributes().getAttribute("ovgk");
		if (ovgk != null) {
			return ovgk;
		} else {
			return "none";
		}
	}

}
