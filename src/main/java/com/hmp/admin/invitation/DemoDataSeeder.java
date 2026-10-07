package com.hmp.admin.invitation;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.hmp.admin.config.InvitationProperties;

/**
 * Fills an empty database with the 18 invitations used in Homework 1 (rows 1–8 are the spec rows of p.113c).
 * Dates are relative to start-up, so Pending rows are always still valid and the demo looks the same
 * every time the server restarts. Each row is built through the domain methods, so its history is real.
 * Turn off with hmp.demo-data.enabled=false.
 */
@Component
@ConditionalOnProperty(name = "hmp.demo-data.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

	// created = how long ago it was first issued; reissues happen one day apart after that
	private record Seed(String name, String email, String mobile, InvitationStatus status, int reissues,
			Duration created) {
	}

	private static final List<Seed> SEEDS = List.of(
		seed("Dr. Kim Han-mi", "kim.hanmi@clinic.co.kr", "010-1234-5678", InvitationStatus.PENDING, 0, 2),
		seed("Dr. Lee Seo-jun", "lee.seojun@clinic.co.kr", "010-2345-1234", InvitationStatus.USED, 0, 5),
		seed("Dr. Park Ji-ho", "park.jiho@clinic.co.kr", "010-3456-8765", InvitationStatus.EXPIRED, 1, 25),
		seed("Dr. Choi Yun-a", "choi.yuna@clinic.co.kr", "010-4567-2468", InvitationStatus.REVOKED, 0, 8),
		seed("Dr. Jung Min-seok", "jung.minseok@clinic.co.kr", "010-5678-1357", InvitationStatus.PENDING, 2, 6),
		seed("Dr. Han Do-kyung", "han.dokyung@clinic.co.kr", "010-6789-9753", InvitationStatus.EXPIRED, 0, 20),
		seed("Dr. Oh Se-ra", "oh.sera@clinic.co.kr", "010-7890-4680", InvitationStatus.USED, 1, 12),
		seed(null, null, null, InvitationStatus.REVOKED, 3, 40),
		seed("Dr. Seo Ha-eun", "seo.haeun@clinic.co.kr", "010-1111-4321", InvitationStatus.PENDING, 0, 1),
		seed("Dr. Kang Bo-ra", "kang.bora@clinic.co.kr", "010-2222-3344", InvitationStatus.USED, 0, 15),
		seed("Dr. Yoon Tae-ho", "yoon.taeho@clinic.co.kr", "010-3333-5566", InvitationStatus.PENDING, 1, 4),
		seed("Dr. Lim Ji-won", "lim.jiwon@clinic.co.kr", "010-4444-7788", InvitationStatus.EXPIRED, 2, 35),
		seed("Dr. Shin Eun-ji", "shin.eunji@clinic.co.kr", "010-5555-9900", InvitationStatus.USED, 0, 10),
		seed("Dr. Hwang Min-ho", "hwang.minho@clinic.co.kr", "010-6666-1122", InvitationStatus.REVOKED, 1, 9),
		seed("Dr. Song Ye-jin", "song.yejin@clinic.co.kr", "010-7777-3344", InvitationStatus.PENDING, 0, 3),
		seed("Dr. Jang Woo-sung", "jang.woosung@clinic.co.kr", "010-8888-5566", InvitationStatus.USED, 0, 18),
		seed("Dr. Bae Su-ji", "bae.suji@clinic.co.kr", "010-9999-7788", InvitationStatus.EXPIRED, 0, 45),
		seed("Dr. Moon Hye-rin", "moon.hyerin@clinic.co.kr", "010-1212-9090", InvitationStatus.PENDING, 0, 0));

	private static final Duration DAY = Duration.ofDays(1);
	private static final Duration SEED_HOUR_OFFSET = Duration.ofHours(3); // so "0 days ago" is still in the past

	private final InvitationRepository repository;
	private final InvitationProperties properties;
	private final Clock clock;

	public DemoDataSeeder(InvitationRepository repository, InvitationProperties properties, Clock clock) {
		this.repository = repository;
		this.properties = properties;
		this.clock = clock;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (repository.count() > 0) {
			return;
		}
		Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
		SEEDS.forEach(seed -> repository.save(build(seed, now)));
	}

	private Invitation build(Seed seed, Instant now) {
		String admin = properties.adminActor();
		Duration validity = properties.validity();
		Instant at = now.minus(seed.created()).minus(SEED_HOUR_OFFSET);

		Invitation invitation = seed.name() == null
			? Invitation.issueWithoutContact(at, validity, admin)
			: Invitation.issue(new DoctorContact(seed.name(), seed.email(), seed.mobile()), at, validity, admin);
		for (int i = 0; i < seed.reissues(); i++) {
			at = at.plus(DAY);
			invitation.reissue(at, validity, admin);
		}
		switch (seed.status()) {
			case USED -> invitation.markUsed(at.plus(DAY));
			case REVOKED -> invitation.revoke(at.plus(DAY), admin);
			case EXPIRED -> invitation.expireIfDue(now);
			case PENDING -> {
				// stays Pending
			}
		}
		return invitation;
	}

	private static Seed seed(String name, String email, String mobile, InvitationStatus status, int reissues,
			int createdDaysAgo) {
		return new Seed(name, email, mobile, status, reissues, Duration.ofDays(createdDaysAgo));
	}

}
