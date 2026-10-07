package com.hmp.admin.invitation;

import java.net.URI;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hmp.admin.invitation.InvitationResponses.Detail;
import com.hmp.admin.invitation.InvitationResponses.PageResult;
import com.hmp.admin.invitation.InvitationResponses.Summary;

// Admin ADM-003 Doctor invitations. Paths follow the API proposed in the wireframe
@RestController
@RequestMapping("/api/v1/admin/doctor-invitations")
public class InvitationController {

	private final InvitationService service;

	public InvitationController(InvitationService service) {
		this.service = service;
	}

	// List (p.113c): GET ?keyword=kim&status=pending&page=1&size=8
	@GetMapping
	public PageResult<Summary> list(
			@RequestParam(defaultValue = "") @Size(max = 50, message = "Search text must be 50 characters or fewer.") String keyword,
			@RequestParam(required = false) InvitationStatus status,
			@RequestParam(defaultValue = "1") @Min(value = 1, message = "Page must be 1 or more.") int page,
			@RequestParam(defaultValue = "20") @Min(value = 1, message = "Page size must be 1 to 100.")
			@Max(value = 100, message = "Page size must be 1 to 100.") int size) {
		return service.search(keyword, status, page, size);
	}

	// Detail + history (p.113e)
	@GetMapping("/{id}")
	public Detail get(@PathVariable long id) {
		return service.get(id);
	}

	// Issue invitation (p.113b) → 201 Created with a Location header
	@PostMapping
	public ResponseEntity<Detail> issue(@Valid @RequestBody InvitationRequest request) {
		Detail created = service.issue(request);
		return ResponseEntity.created(URI.create("/api/v1/admin/doctor-invitations/" + created.id())).body(created);
	}

	// Edit a Pending invitation (training extension): updates the details only, no new link
	@PutMapping("/{id}")
	public Detail edit(@PathVariable long id, @Valid @RequestBody InvitationRequest request) {
		return service.edit(id, request);
	}

	@PostMapping("/{id}/reissue")
	public Detail reissue(@PathVariable long id) {
		return service.reissue(id);
	}

	// Delete = revoke (spec p.113c: no hard delete, the row stays as history)
	@DeleteMapping("/{id}")
	public Detail revoke(@PathVariable long id) {
		return service.revoke(id);
	}

}
