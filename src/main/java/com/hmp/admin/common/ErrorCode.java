package com.hmp.admin.common;

import org.springframework.http.HttpStatus;

/**
 * Every error the API can return, with its HTTP status and the plain-language message shown to the admin.
 * The same list (Page | Scenario | EN) is in docs/error-messages.md, following the WM Error Message List layout.
 */
public enum ErrorCode {

	VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Some details need fixing. Check the message under each field."),
	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "The request could not be understood. Refresh the page and try again."),
	INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND, "This invitation does not exist. It may have been entered incorrectly."),
	INVITATION_ALREADY_PENDING(HttpStatus.CONFLICT,
		"An invitation for this email is already waiting to be used. Re-issue it from the list instead."),
	DOCTOR_ALREADY_REGISTERED(HttpStatus.CONFLICT,
		"A doctor with this email has already signed up. No new invitation is needed."),
	INVITATION_NOT_EDITABLE(HttpStatus.CONFLICT, "Only a pending invitation can be edited."),
	INVITATION_NOT_REISSUABLE(HttpStatus.CONFLICT, "A used invitation cannot be re-issued."),
	INVITATION_NOT_REVOCABLE(HttpStatus.CONFLICT, "Only a pending invitation can be revoked."),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong on our side. Please try again in a moment.");

	private final HttpStatus status;
	private final String message;

	ErrorCode(HttpStatus status, String message) {
		this.status = status;
		this.message = message;
	}

	public HttpStatus status() {
		return status;
	}

	public String message() {
		return message;
	}

}
