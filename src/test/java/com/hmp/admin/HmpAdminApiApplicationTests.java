package com.hmp.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.hmp.admin.invitation.Invitation;
import com.hmp.admin.invitation.InvitationRepository;
import com.hmp.admin.invitation.InvitationStatus;

// The app starts with the Flyway schema (validated by Hibernate) and the 18 demo invitations
@SpringBootTest
class HmpAdminApiApplicationTests {

	@Autowired
	private InvitationRepository repository;

	@Test
	void startsWithDemoDataInEveryStatus() {
		Map<InvitationStatus, Long> byStatus = repository.findAll().stream()
			.collect(Collectors.groupingBy(Invitation::getStatus, Collectors.counting()));

		assertThat(byStatus).containsOnly(
			Map.entry(InvitationStatus.PENDING, 6L),
			Map.entry(InvitationStatus.USED, 5L),
			Map.entry(InvitationStatus.EXPIRED, 4L),
			Map.entry(InvitationStatus.REVOKED, 3L));
	}

}
