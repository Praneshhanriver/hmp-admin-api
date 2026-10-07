package com.hmp.admin.invitation;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.data.domain.Page;

// JSON shapes returned by the API. Dates are ISO 8601 in Korean time: "2026-09-08T10:00:00+09:00"
public final class InvitationResponses {

	private InvitationResponses() {
	}

	// One list row. The mobile is masked here, so the full number never reaches the list screen
	public record Summary(
		long id,
		String doctorName,
		String maskedMobile,
		InvitationStatus status,
		OffsetDateTime issuedAt,
		OffsetDateTime expiresAt,
		int reissueCount) {

		static Summary from(Invitation invitation, ZoneId zone) {
			return new Summary(
				invitation.getId(),
				invitation.getDoctorName(),
				MobileNumbers.mask(invitation.getMobile()),
				invitation.getStatus(),
				at(invitation.getIssuedAt(), zone),
				at(invitation.getExpiresAt(), zone),
				invitation.getReissueCount());
		}
	}

	// Detail screen (p.113e) and the edit form. Includes the full contact so the form can be prefilled
	public record Detail(
		long id,
		String doctorName,
		String email,
		String mobile,
		String maskedMobile,
		InvitationStatus status,
		OffsetDateTime issuedAt,
		OffsetDateTime expiresAt,
		int reissueCount,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt,
		List<HistoryEntry> history) {

		static Detail from(Invitation invitation, ZoneId zone) {
			return new Detail(
				invitation.getId(),
				invitation.getDoctorName(),
				invitation.getEmail(),
				invitation.getMobile(),
				MobileNumbers.mask(invitation.getMobile()),
				invitation.getStatus(),
				at(invitation.getIssuedAt(), zone),
				at(invitation.getExpiresAt(), zone),
				invitation.getReissueCount(),
				at(invitation.getCreatedAt(), zone),
				at(invitation.getUpdatedAt(), zone),
				invitation.getHistory().stream().map(entry -> HistoryEntry.from(entry, zone)).toList());
		}
	}

	// One history line, oldest first. actor is null for system events
	public record HistoryEntry(long id, HistoryAction action, String actor, OffsetDateTime occurredAt) {

		static HistoryEntry from(InvitationHistory entry, ZoneId zone) {
			return new HistoryEntry(entry.getId(), entry.getAction(), entry.getActor(), at(entry.getOccurredAt(), zone));
		}
	}

	// One page of results. page is 1-based, like the pagination on screen
	public record PageResult<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

		static <T> PageResult<T> from(Page<T> page) {
			return new PageResult<>(page.getContent(), page.getNumber() + 1, page.getSize(), page.getTotalElements(),
				page.getTotalPages());
		}
	}

	private static OffsetDateTime at(Instant instant, ZoneId zone) {
		return instant.atZone(zone).toOffsetDateTime();
	}

}
