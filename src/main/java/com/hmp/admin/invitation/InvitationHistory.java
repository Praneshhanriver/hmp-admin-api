package com.hmp.admin.invitation;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// What happened to an invitation, who did it and when. Never changed after it is written
@Entity
@Table(name = "invitation_history")
public class InvitationHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "invitation_id")
	private Invitation invitation;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private HistoryAction action;

	// The admin account, or null for system events (expiry, doctor sign-up)
	@Column(length = 100)
	private String actor;

	@Column(name = "occurred_at", nullable = false)
	private Instant occurredAt;

	protected InvitationHistory() {
	}

	InvitationHistory(Invitation invitation, HistoryAction action, String actor, Instant occurredAt) {
		this.invitation = invitation;
		this.action = action;
		this.actor = actor;
		this.occurredAt = occurredAt;
	}

	public Long getId() {
		return id;
	}

	public HistoryAction getAction() {
		return action;
	}

	public String getActor() {
		return actor;
	}

	public Instant getOccurredAt() {
		return occurredAt;
	}

}
