package com.hmp.admin.invitation;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hmp.admin.common.ApiException;
import com.hmp.admin.common.ErrorCode;
import com.hmp.admin.config.InvitationProperties;
import com.hmp.admin.invitation.InvitationResponses.Detail;
import com.hmp.admin.invitation.InvitationResponses.PageResult;
import com.hmp.admin.invitation.InvitationResponses.Summary;

/**
 * Doctor invitation use cases. The controller only maps HTTP; the rules for which status allows
 * which action live in Invitation / InvitationStatus; this class checks duplicates and loads/saves.
 */
@Service
@Transactional
public class InvitationService {

	// Newest invitation first; id breaks ties so paging is stable
	private static final Sort LIST_ORDER = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

	private final InvitationRepository repository;
	private final InvitationProperties properties;
	private final Clock clock;

	public InvitationService(InvitationRepository repository, InvitationProperties properties, Clock clock) {
		this.repository = repository;
		this.properties = properties;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public PageResult<Summary> search(String keyword, InvitationStatus status, int page, int size) {
		var pageRequest = PageRequest.of(page - 1, size, LIST_ORDER);
		var result = repository.findAll(InvitationSpecifications.matching(keyword, status), pageRequest)
			.map(invitation -> Summary.from(invitation, properties.timeZone()));
		return PageResult.from(result);
	}

	@Transactional(readOnly = true)
	public Detail get(long id) {
		return Detail.from(findWithHistory(id), properties.timeZone());
	}

	public Detail issue(InvitationRequest request) {
		DoctorContact contact = DoctorContact.from(request);
		checkNotAlreadyInvited(contact.email(), null);
		Invitation invitation = Invitation.issue(contact, now(), properties.validity(), properties.adminActor());
		return Detail.from(repository.save(invitation), properties.timeZone());
	}

	public Detail edit(long id, InvitationRequest request) {
		Invitation invitation = findWithHistory(id);
		if (!invitation.getStatus().canEdit()) {
			throw new ApiException(ErrorCode.INVITATION_NOT_EDITABLE);
		}
		DoctorContact contact = DoctorContact.from(request);
		checkNotAlreadyInvited(contact.email(), id);
		invitation.edit(contact, now(), properties.adminActor());
		return Detail.from(repository.saveAndFlush(invitation), properties.timeZone());
	}

	public Detail reissue(long id) {
		Invitation invitation = findWithHistory(id);
		if (!invitation.getStatus().canReissue()) {
			throw new ApiException(ErrorCode.INVITATION_NOT_REISSUABLE);
		}
		// A Revoked/Expired link coming back to life must not create a second live invitation for the same email
		if (invitation.getEmail() != null) {
			checkNotAlreadyInvited(invitation.getEmail(), id);
		}
		invitation.reissue(now(), properties.validity(), properties.adminActor());
		return Detail.from(repository.saveAndFlush(invitation), properties.timeZone());
	}

	// DELETE in the API = revoke: the link stops working, the row stays in the list as history
	public Detail revoke(long id) {
		Invitation invitation = findWithHistory(id);
		if (!invitation.getStatus().canRevoke()) {
			throw new ApiException(ErrorCode.INVITATION_NOT_REVOCABLE);
		}
		invitation.revoke(now(), properties.adminActor());
		return Detail.from(repository.saveAndFlush(invitation), properties.timeZone());
	}

	// Called by the expiry job. Returns how many invitations expired
	public int expireDueInvitations() {
		var due = repository.findByStatusAndExpiresAtLessThanEqual(InvitationStatus.PENDING, now());
		due.forEach(invitation -> invitation.expireIfDue(now()));
		return due.size();
	}

	// Same email already Pending → re-issue instead; already Used → the doctor has signed up
	private void checkNotAlreadyInvited(String email, Long excludeId) {
		if (exists(email, InvitationStatus.PENDING, excludeId)) {
			throw new ApiException(ErrorCode.INVITATION_ALREADY_PENDING, "email");
		}
		if (exists(email, InvitationStatus.USED, excludeId)) {
			throw new ApiException(ErrorCode.DOCTOR_ALREADY_REGISTERED, "email");
		}
	}

	private boolean exists(String email, InvitationStatus status, Long excludeId) {
		return excludeId == null
			? repository.existsByEmailAndStatus(email, status)
			: repository.existsByEmailAndStatusAndIdNot(email, status, excludeId);
	}

	private Invitation findWithHistory(long id) {
		return repository.findWithHistoryById(id).orElseThrow(() -> new ApiException(ErrorCode.INVITATION_NOT_FOUND));
	}

	// Whole seconds: the screens never show anything smaller
	private Instant now() {
		return clock.instant().truncatedTo(ChronoUnit.SECONDS);
	}

}
