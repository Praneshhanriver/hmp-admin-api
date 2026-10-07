package com.hmp.admin.invitation;

import java.util.Arrays;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonValue;

// The four token states from the spec (S08 ②, p.113c ⑤)
public enum InvitationStatus {
	PENDING, USED, EXPIRED, REVOKED;

	// JSON and query parameters use lower case: "pending"
	@JsonValue
	public String apiValue() {
		return name().toLowerCase(Locale.ROOT);
	}

	public static InvitationStatus fromApiValue(String value) {
		return Arrays.stream(values())
			.filter(status -> status.apiValue().equals(value))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Unknown status: " + value));
	}

	// Which actions each status allows (p.113c ⑥); the frontend mirrors this in invitationRules.ts
	public boolean canReissue() {
		return this != USED;
	}

	public boolean canRevoke() {
		return this == PENDING;
	}

	public boolean canEdit() {
		return this == PENDING;
	}
}
