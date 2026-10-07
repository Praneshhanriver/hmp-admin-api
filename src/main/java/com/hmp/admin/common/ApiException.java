package com.hmp.admin.common;

// A business rule was broken. The handler turns it into a ProblemDetail with the code's message
public class ApiException extends RuntimeException {

	private final ErrorCode code;
	private final String field; // the form field to show the message under, or null

	public ApiException(ErrorCode code) {
		this(code, null);
	}

	public ApiException(ErrorCode code, String field) {
		super(code.message());
		this.code = code;
		this.field = field;
	}

	public ErrorCode code() {
		return code;
	}

	public String field() {
		return field;
	}

}
