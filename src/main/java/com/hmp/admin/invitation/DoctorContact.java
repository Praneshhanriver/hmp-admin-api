package com.hmp.admin.invitation;

import java.util.Locale;

// Who gets invited, already cleaned up: trimmed name, lower-case email, "010-1234-5678" mobile
public record DoctorContact(String doctorName, String email, String mobile) {

	public static DoctorContact from(InvitationRequest request) {
		return new DoctorContact(
			request.doctorName().trim(),
			request.email().trim().toLowerCase(Locale.ROOT),
			MobileNumbers.normalize(request.mobile()));
	}

}
