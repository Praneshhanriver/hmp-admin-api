package com.hmp.admin.invitation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of "Issue invitation" (POST) and "Edit" (PUT).
 * The frontend form (src/utils/invitationValidation.ts) uses exactly the same rules and messages,
 * worded as in the Hi-Fi frames 2g / 2j.
 */
public record InvitationRequest(

	@NotBlank(message = "Enter the doctor's name.")
	@Size(min = 2, max = 50, message = "The doctor's name must be 2 to 50 characters.")
	String doctorName,

	@NotBlank(message = "Enter a valid email address, e.g. name@clinic.co.kr.")
	@Size(max = 100, message = "The email address must be 100 characters or fewer.")
	@Pattern(regexp = EMAIL_PATTERN, message = "Enter a valid email address, e.g. name@clinic.co.kr.")
	String email,

	@NotBlank(message = "Enter a Korean mobile number, e.g. 010-1234-5678.")
	@Pattern(regexp = MobileNumbers.PATTERN, message = "Enter a Korean mobile number, e.g. 010-1234-5678.")
	String mobile) {

	// Something@something.something, no spaces. Stricter than @Email, which accepts "name@host"
	public static final String EMAIL_PATTERN = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";

	// Trim before validation, so " K " counts as 1 character, exactly like the frontend form
	public InvitationRequest {
		doctorName = trim(doctorName);
		email = trim(email);
		mobile = trim(mobile);
	}

	private static String trim(String value) {
		return value == null ? null : value.strip();
	}

}
