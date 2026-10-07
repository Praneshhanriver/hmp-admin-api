package com.hmp.admin.invitation;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonValue;

// One line of the invitation history on the detail screen (p.113e)
public enum HistoryAction {
	ISSUED, REISSUED, EDITED, REVOKED, USED, EXPIRED;

	@JsonValue
	public String apiValue() {
		return name().toLowerCase(Locale.ROOT);
	}
}
