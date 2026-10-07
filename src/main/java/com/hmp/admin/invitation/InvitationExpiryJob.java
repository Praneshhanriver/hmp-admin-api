package com.hmp.admin.invitation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Moves Pending invitations past their expiry to Expired, once at start-up and then every minute
@Component
public class InvitationExpiryJob {

	private static final Logger log = LoggerFactory.getLogger(InvitationExpiryJob.class);

	private final InvitationService service;

	public InvitationExpiryJob(InvitationService service) {
		this.service = service;
	}

	@EventListener(ApplicationReadyEvent.class)
	@Scheduled(fixedDelayString = "${hmp.invitation.expiry-check-interval}",
		initialDelayString = "${hmp.invitation.expiry-check-interval}")
	public void expireDueInvitations() {
		int expired = service.expireDueInvitations();
		if (expired > 0) {
			log.info("Expired {} invitation(s)", expired);
		}
	}

}
