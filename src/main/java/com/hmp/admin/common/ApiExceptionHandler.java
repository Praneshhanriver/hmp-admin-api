package com.hmp.admin.common;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns every error into an RFC 7807 ProblemDetail:
 * { "status": 400, "title": "Bad Request", "code": "VALIDATION_FAILED", "detail": "...",
 *   "errors": [ { "field": "email", "message": "..." } ] }
 * Raw exception text and stack traces never leave the server.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	public record FieldMessage(String field, String message) {
	}

	@ExceptionHandler(ApiException.class)
	ResponseEntity<ProblemDetail> handleApiException(ApiException ex) {
		List<FieldMessage> errors = ex.field() == null ? List.of() : List.of(new FieldMessage(ex.field(), ex.getMessage()));
		return problem(ex.code(), errors);
	}

	// ?page=abc, ?status=unknown, /invitations/abc
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		return problem(ErrorCode.INVALID_REQUEST, List.of());
	}

	// Anything unexpected: log it for us, show the admin a calm message
	@ExceptionHandler(Exception.class)
	ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
		log.error("Unexpected error", ex);
		return problem(ErrorCode.INTERNAL_ERROR, List.of());
	}

	// @Valid request body failed: one message per field. The validator reports broken rules in no fixed
	// order, so pick by priority: "required" first, then length, then format (same order as the frontend)
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, FieldError> firstPerField = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors().stream()
			.sorted(Comparator.comparingInt(ApiExceptionHandler::rulePriority))
			.forEach(error -> firstPerField.putIfAbsent(error.getField(), error));
		List<FieldMessage> errors = firstPerField.values().stream()
			.map(error -> new FieldMessage(error.getField(), error.getDefaultMessage()))
			.toList();
		return asObject(problem(ErrorCode.VALIDATION_FAILED, errors));
	}

	// @Min / @Max / @Size on query parameters (page, size, keyword)
	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<FieldMessage> errors = ex.getParameterValidationResults().stream()
			.flatMap(result -> result.getResolvableErrors().stream()
				.map(error -> new FieldMessage(result.getMethodParameter().getParameterName(), error.getDefaultMessage())))
			.toList();
		return asObject(problem(ErrorCode.VALIDATION_FAILED, errors));
	}

	// Body is not valid JSON
	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		return asObject(problem(ErrorCode.INVALID_REQUEST, List.of()));
	}

	private static ResponseEntity<ProblemDetail> problem(ErrorCode code, List<FieldMessage> errors) {
		ProblemDetail body = ProblemDetail.forStatusAndDetail(code.status(), code.message());
		body.setProperty("code", code.name());
		body.setProperty("errors", errors);
		return ResponseEntity.status(code.status()).body(body);
	}

	private static ResponseEntity<Object> asObject(ResponseEntity<ProblemDetail> response) {
		return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
	}

	private static int rulePriority(FieldError error) {
		int index = RULE_ORDER.indexOf(error.getCode());
		return index < 0 ? RULE_ORDER.size() : index;
	}

	private static final List<String> RULE_ORDER = List.of("NotBlank", "Size", "Pattern");

}
