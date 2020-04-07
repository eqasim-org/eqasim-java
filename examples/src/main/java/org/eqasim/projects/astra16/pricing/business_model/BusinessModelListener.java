package org.eqasim.projects.astra16.pricing.business_model;

import java.util.List;

public interface BusinessModelListener {
	void handleBusinessModel(BusinessModelData model);

	static public BusinessModelListener combine(List<BusinessModelListener> listeners) {
		return new BusinessModelListener() {
			@Override
			public void handleBusinessModel(BusinessModelData model) {
				listeners.forEach(l -> l.handleBusinessModel(model));
			}
		};
	}
}
