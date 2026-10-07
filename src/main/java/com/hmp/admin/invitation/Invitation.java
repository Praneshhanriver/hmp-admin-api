package com.hmp.admin.invitation;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * One doctor invitation (Admin ADM-003). Every state change goes through a method here,
 * so the status rule and the history line are always applied together.
 */
@Entity
@Table(name = "invitation")
public class Invitation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Nullable: old invitations can be missing details (shown as "No information" / "-")
	@Column(name = "doctor_name", length = 50)
	private String doctorName;

	@Column(length = 100)
	private String email;

	@Column(length = 13)
	private String mobile;

	// First 3 + last 4 digits of the mobile, the only digits search may match
	@Column(name = "mobile_search_digits", length = 7)
	private String mobileSearchDigits;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private InvitationStatus status;

	@Column(name = "issued_at", nullable = false)
	private Instant issuedAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "reissue_count", nullable = false)
	private int reissueCount;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@OneToMany(mappedBy = "invitation", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("occurredAt ASC, id ASC")
	private List<InvitationHistory> history = new ArrayList<>();

	protected Invitation() {
	}

	// Issue invitation (p.113b): a new Pending link
	public static Invitation issue(DoctorContact contact, Instant now, Duration validity, String actor) {
		Invitation invitation = new Invitation();
		invitation.setContact(contact);
		invitation.status = InvitationStatus.PENDING;
		invitation.issuedAt = now;
		invitation.expiresAt = now.plus(validity);
		invitation.reissueCount = 0;
		invitation.createdAt = now;
		invitation.updatedAt = now;
		invitation.record(HistoryAction.ISSUED, actor, now);
		return invitation;
	}

	// Demo data only: an old invitation whose name and contact were never recorded (p.113c row 8)
	public static Invitation issueWithoutContact(Instant now, Duration validity, String actor) {
		return issue(new DoctorContact(null, null, null), now, validity, actor);
	}

	// Re-issue: a new link, the old one stops working, back to Pending with a fresh expiry
	public void reissue(Instant now, Duration validity, String actor) {
		if (!status.canReissue()) {
			throw new IllegalStateException("A used invitation cannot be re-issued");
		}
		startNewLink(now, validity);
		record(HistoryAction.REISSUED, actor, now);
	}

	// Edit (training extension, Hi-Fi 1d): correct the recipient details of a Pending invitation.
	// It does not send a new link — the link, its dates and the re-issue count stay the same (Re-issue does that)
	public void edit(DoctorContact contact, Instant now, String actor) {
		if (!status.canEdit()) {
			throw new IllegalStateException("Only a pending invitation can be edited");
		}
		setContact(contact);
		updatedAt = now;
		record(HistoryAction.EDITED, actor, now);
	}

	// Revoke = delete in this feature: the link stops working, the row stays as history
	public void revoke(Instant now, String actor) {
		if (!status.canRevoke()) {
			throw new IllegalStateException("Only a pending invitation can be revoked");
		}
		changeStatus(InvitationStatus.REVOKED, HistoryAction.REVOKED, actor, now);
	}

	// System event: the doctor signed up with the link. Used only by the demo data
	public void markUsed(Instant now) {
		if (status != InvitationStatus.PENDING) {
			throw new IllegalStateException("Only a pending invitation can be used");
		}
		changeStatus(InvitationStatus.USED, HistoryAction.USED, null, now);
	}

	// System event: a Pending link past its expiry stops working, recorded at the moment it expired
	public boolean expireIfDue(Instant now) {
		if (status != InvitationStatus.PENDING || expiresAt.isAfter(now)) {
			return false;
		}
		changeStatus(InvitationStatus.EXPIRED, HistoryAction.EXPIRED, null, expiresAt);
		return true;
	}

	private void startNewLink(Instant now, Duration validity) {
		status = InvitationStatus.PENDING;
		issuedAt = now;
		expiresAt = now.plus(validity);
		reissueCount++;
		updatedAt = now;
	}

	private void changeStatus(InvitationStatus newStatus, HistoryAction action, String actor, Instant at) {
		status = newStatus;
		updatedAt = at;
		record(action, actor, at);
	}

	private void setContact(DoctorContact contact) {
		doctorName = contact.doctorName();
		email = contact.email();
		mobile = contact.mobile();
		mobileSearchDigits = MobileNumbers.visibleDigits(contact.mobile());
	}

	private void record(HistoryAction action, String actor, Instant at) {
		history.add(new InvitationHistory(this, action, actor, at));
	}

	public Long getId() {
		return id;
	}

	public String getDoctorName() {
		return doctorName;
	}

	public String getEmail() {
		return email;
	}

	public String getMobile() {
		return mobile;
	}

	public InvitationStatus getStatus() {
		return status;
	}

	public Instant getIssuedAt() {
		return issuedAt;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public int getReissueCount() {
		return reissueCount;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public List<InvitationHistory> getHistory() {
		return Collections.unmodifiableList(history);
	}

}
