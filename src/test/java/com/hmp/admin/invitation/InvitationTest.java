package com.hmp.admin.invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

// Status rules of one invitation (p.113c ⑥), without Spring or a database
class InvitationTest {

	private static final Instant T0 = Instant.parse("2026-10-01T01:00:00Z");
	private static final Duration VALIDITY = Duration.ofDays(14);
	private static final String ADMIN = "admin@hmp.co.kr";
	private static final DoctorContact KIM = new DoctorContact("Dr. Kim Han-mi", "kim@clinic.co.kr", "010-1234-5678");

	private Invitation issued() {
		return Invitation.issue(KIM, T0, VALIDITY, ADMIN);
	}

	@Test
	void issueStartsPendingWithExpiryAndOneHistoryLine() {
		Invitation invitation = issued();

		assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.PENDING);
		assertThat(invitation.getExpiresAt()).isEqualTo(T0.plus(VALIDITY));
		assertThat(invitation.getReissueCount()).isZero();
		assertThat(invitation.getHistory()).extracting(InvitationHistory::getAction).containsExactly(HistoryAction.ISSUED);
	}

	@Test
	void reissueGivesAFreshLinkAndCountsIt() {
		Invitation invitation = issued();
		Instant later = T0.plus(Duration.ofDays(3));

		invitation.reissue(later, VALIDITY, ADMIN);

		assertThat(invitation.getIssuedAt()).isEqualTo(later);
		assertThat(invitation.getExpiresAt()).isEqualTo(later.plus(VALIDITY));
		assertThat(invitation.getReissueCount()).isEqualTo(1);
	}

	@Test
	void revokedAndExpiredCanBeReissuedButUsedCannot() {
		Invitation revoked = issued();
		revoked.revoke(T0, ADMIN);
		revoked.reissue(T0, VALIDITY, ADMIN);
		assertThat(revoked.getStatus()).isEqualTo(InvitationStatus.PENDING);

		Invitation used = issued();
		used.markUsed(T0);
		assertThatThrownBy(() -> used.reissue(T0, VALIDITY, ADMIN)).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void onlyPendingCanBeRevokedOrEdited() {
		Invitation invitation = issued();
		invitation.revoke(T0, ADMIN);

		assertThatThrownBy(() -> invitation.revoke(T0, ADMIN)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> invitation.edit(KIM, T0, VALIDITY, ADMIN)).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void editChangesContactAndSendsACorrectedLink() {
		Invitation invitation = issued();
		var corrected = new DoctorContact("Dr. Kim Han-mi", "kim.hanmi@clinic.co.kr", "010-9999-5678");

		invitation.edit(corrected, T0.plusSeconds(60), VALIDITY, ADMIN);

		assertThat(invitation.getEmail()).isEqualTo("kim.hanmi@clinic.co.kr");
		assertThat(invitation.getReissueCount()).isEqualTo(1);
		assertThat(invitation.getHistory()).extracting(InvitationHistory::getAction)
			.containsExactly(HistoryAction.ISSUED, HistoryAction.EDITED);
	}

	@Test
	void expiresOnlyWhenPendingAndPastExpiry() {
		Invitation invitation = issued();

		assertThat(invitation.expireIfDue(T0.plus(Duration.ofDays(13)))).isFalse();
		assertThat(invitation.expireIfDue(T0.plus(Duration.ofDays(15)))).isTrue();
		assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.EXPIRED);
		// recorded at the moment it expired, by the system (no actor)
		InvitationHistory last = invitation.getHistory().getLast();
		assertThat(last.getOccurredAt()).isEqualTo(T0.plus(VALIDITY));
		assertThat(last.getActor()).isNull();
	}

}
