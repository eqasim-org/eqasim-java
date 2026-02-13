package org.eqasim.switzerland.ch_cmdp.utils.pricing.inputs;

public class Authority {
    private final String authorityId;
	private final int priority;
	private final String authorityType;
    
     public Authority(String authorityId, int priority, String authorityType) {
		this.authorityId   = authorityId;
		this.priority      = priority;
		this.authorityType = authorityType;
	}

	public String getId() {
		return authorityId;
	}

	public int getPriority() {
		return priority;
	}

	public String getAuthorityType() {
		return authorityType;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Authority authority) {
			return authorityId.equals(authority.authorityId);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return authorityId.hashCode();
	}

	@Override
	public String toString() {
		return "Authority(" + authorityId + ")";
	}
    
}
