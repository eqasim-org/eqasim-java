package org.eqasim.ile_de_france.discrete_mode_choice.conflicts;

import java.util.Objects;

public class ConflictItem {
	public int tripIndex;
	public String mode;

	ConflictItem(int tripIndex, String mode) {
		this.tripIndex = tripIndex;
		this.mode = mode;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof ConflictItem) {
			ConflictItem otherRejectionItem = (ConflictItem) other;

			if (otherRejectionItem.mode.equals(mode) && otherRejectionItem.tripIndex == tripIndex) {
				return true;
			}
		}

		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(tripIndex, mode);
	}
}
