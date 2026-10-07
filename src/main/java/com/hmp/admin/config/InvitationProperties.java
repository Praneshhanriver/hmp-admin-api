package com.hmp.admin.config;

import java.time.Duration;
import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Invitation rules from application.properties (prefix "hmp.invitation").
 *
 * @param validity            how long a link works; the spec still marks this TBC (S12 > INVITATION)
 * @param adminActor          who is written into the history; the homework has no login, so one fixed admin
 * @param expiryCheckInterval how often the expiry job looks for overdue Pending invitations
 * @param timeZone            the zone dates are returned in (Korean time)
 */
@ConfigurationProperties(prefix = "hmp.invitation")
public record InvitationProperties(Duration validity, String adminActor, Duration expiryCheckInterval, ZoneId timeZone) {
}
