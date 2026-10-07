package com.hmp.admin.invitation;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

/**
 * The real HTTP API against a real (in-memory) database, with "now" fixed so dates are predictable.
 * Demo data is off; each test starts from an empty table.
 */
@SpringBootTest(properties = "hmp.demo-data.enabled=false")
@AutoConfigureMockMvc
class InvitationApiIntegrationTest {

	private static final String API = "/api/v1/admin/doctor-invitations";
	private static final Instant NOW = Instant.parse("2026-10-07T01:00:00Z"); // 10:00 in Korea

	@TestConfiguration
	static class FixedClock {
		@Bean
		@Primary
		Clock fixedClock() {
			return Clock.fixed(NOW, ZoneId.of("Asia/Seoul"));
		}
	}

	@Autowired
	private MockMvc mvc;

	@Autowired
	private InvitationRepository repository;

	@Autowired
	private InvitationService service;

	@BeforeEach
	void emptyDatabase() {
		repository.deleteAll();
	}

	// ---- Main flow: create → list → detail → edit → delete (revoke) → re-issue

	@Test
	void fullLifecycle() throws Exception {
		ResultActions created = create("Dr. Kim Han-mi", "Kim.HanMi@Clinic.co.kr", "01012345678")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.email").value("kim.hanmi@clinic.co.kr"))   // lower-cased
			.andExpect(jsonPath("$.mobile").value("010-1234-5678"))           // normalised
			.andExpect(jsonPath("$.status").value("pending"))
			.andExpect(jsonPath("$.issuedAt").value("2026-10-07T10:00:00+09:00"))
			.andExpect(jsonPath("$.expiresAt").value("2026-10-21T10:00:00+09:00"));
		long id = idOf(created);
		created.andExpect(header().string("Location", API + "/" + id));

		mvc.perform(get(API))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].maskedMobile").value("010-****-5678"))
			.andExpect(jsonPath("$.content[0].mobile").doesNotExist()); // full number never in the list

		mvc.perform(put(API + "/" + id).contentType(MediaType.APPLICATION_JSON)
				.content(body("Dr. Kim Han-mi", "kim.hanmi@clinic.co.kr", "010-9999-5678")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.mobile").value("010-9999-5678"))
			.andExpect(jsonPath("$.reissueCount").value(0)) // edit sends no new link
			.andExpect(jsonPath("$.issuedAt").value("2026-10-07T10:00:00+09:00"))
			.andExpect(jsonPath("$.history[*].action", contains("issued", "edited")));

		mvc.perform(delete(API + "/" + id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("revoked"))
			.andExpect(jsonPath("$.history[2].actor").value("admin@hmp.co.kr"));

		// Delete keeps the row as history (spec: no hard delete)
		mvc.perform(get(API + "/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("revoked"));

		mvc.perform(post(API + "/" + id + "/reissue"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("pending"))
			.andExpect(jsonPath("$.reissueCount").value(1));
	}

	// ---- Validation: same rules and messages as the frontend form

	@Test
	void emptyFieldsAreRequired() throws Exception {
		create("", " ", "")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[?(@.field=='doctorName')].message").value("Enter the doctor's name."))
			.andExpect(jsonPath("$.errors[?(@.field=='email')].message").value("Enter a valid email address, e.g. name@clinic.co.kr."))
			.andExpect(jsonPath("$.errors[?(@.field=='mobile')].message").value("Enter a Korean mobile number, e.g. 010-1234-5678."));
	}

	@Test
	void wrongFormatsAndLengthsAreRejected() throws Exception {
		create("K", "name@host", "010-123")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors", hasSize(3)))
			.andExpect(jsonPath("$.errors[?(@.field=='doctorName')].message")
				.value("The doctor's name must be 2 to 50 characters."))
			.andExpect(jsonPath("$.errors[?(@.field=='email')].message")
				.value("Enter a valid email address, e.g. name@clinic.co.kr."))
			.andExpect(jsonPath("$.errors[?(@.field=='mobile')].message")
				.value("Enter a Korean mobile number, e.g. 010-1234-5678."));

		create("Dr. " + "a".repeat(47), "a".repeat(95) + "@x.com", "010-1234-5678")
			.andExpect(jsonPath("$.errors[?(@.field=='doctorName')].message")
				.value("The doctor's name must be 2 to 50 characters."))
			.andExpect(jsonPath("$.errors[?(@.field=='email')].message")
				.value("The email address must be 100 characters or fewer."));
	}

	@Test
	void malformedJsonGivesAPlainMessage() throws Exception {
		mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON).content("{not json"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
			.andExpect(jsonPath("$.trace").doesNotExist());
	}

	// ---- Business conflicts (W-02d, W-02e) and status rules

	@Test
	void secondPendingInvitationForSameEmailIsRefused() throws Exception {
		create("Dr. Kim Han-mi", "kim@clinic.co.kr", "010-1234-5678");

		create("Dr. Kim", "KIM@clinic.co.kr", "010-1111-2222")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("INVITATION_ALREADY_PENDING"))
			.andExpect(jsonPath("$.errors[0].field").value("email"));
	}

	@Test
	void doctorWhoAlreadySignedUpCannotBeInvitedOrReissuedAgain() throws Exception {
		Invitation used = Invitation.issue(new DoctorContact("Dr. Lee", "lee@clinic.co.kr", "010-2345-1234"), NOW,
			java.time.Duration.ofDays(14), "admin@hmp.co.kr");
		used.markUsed(NOW);
		long usedId = repository.save(used).getId();

		create("Dr. Lee", "lee@clinic.co.kr", "010-2345-1234")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("DOCTOR_ALREADY_REGISTERED"));

		mvc.perform(post(API + "/" + usedId + "/reissue"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("INVITATION_NOT_REISSUABLE"));
		mvc.perform(put(API + "/" + usedId).contentType(MediaType.APPLICATION_JSON)
				.content(body("Dr. Lee", "lee@clinic.co.kr", "010-2345-1234")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("INVITATION_NOT_EDITABLE"));
		mvc.perform(delete(API + "/" + usedId))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("INVITATION_NOT_REVOCABLE"));
	}

	@Test
	void reissuingARevokedInvitationIsRefusedWhenTheEmailHasANewerPendingOne() throws Exception {
		long oldId = idOf(create("Dr. Kim", "kim@clinic.co.kr", "010-1234-5678"));
		mvc.perform(delete(API + "/" + oldId)).andExpect(status().isOk());
		create("Dr. Kim", "kim@clinic.co.kr", "010-1234-5678").andExpect(status().isCreated());

		mvc.perform(post(API + "/" + oldId + "/reissue"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("INVITATION_ALREADY_PENDING"));
	}

	@Test
	void unknownInvitationIsNotFound() throws Exception {
		mvc.perform(get(API + "/999"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("INVITATION_NOT_FOUND"))
			.andExpect(jsonPath("$.detail").value("This invitation does not exist. It may have been entered incorrectly."));
	}

	// ---- List: search, filter, paging

	@Test
	void searchMatchesNameOrOnlyTheVisibleContactDigits() throws Exception {
		create("Dr. Kim Han-mi", "kim@clinic.co.kr", "010-1234-5678");
		create("Dr. Lee Seo-jun", "lee@clinic.co.kr", "010-2345-1234");

		mvc.perform(get(API).param("keyword", "han-MI")).andExpect(jsonPath("$.content[*].doctorName", contains("Dr. Kim Han-mi")));
		mvc.perform(get(API).param("keyword", "010-5678")).andExpect(jsonPath("$.content[*].doctorName", contains("Dr. Kim Han-mi")));
		// "1234" is hidden in Kim's number (010-****-5678), so only Lee matches
		mvc.perform(get(API).param("keyword", "1234")).andExpect(jsonPath("$.content[*].doctorName", contains("Dr. Lee Seo-jun")));
		mvc.perform(get(API).param("keyword", "%")).andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	void digitsInsideANameAreNotAContactSearch() throws Exception {
		create("Dr. E2E Test", "e2e@clinic.co.kr", "010-3333-1111");
		create("Dr. Lee Seo-jun", "lee@clinic.co.kr", "010-2345-1234"); // visible digits 0101234 contain a "2"

		mvc.perform(get(API).param("keyword", "E2E")).andExpect(jsonPath("$.content[*].doctorName", contains("Dr. E2E Test")));
		mvc.perform(get(API).param("keyword", "010 1234")).andExpect(jsonPath("$.content[*].doctorName", contains("Dr. Lee Seo-jun")));
	}

	@Test
	void filtersByStatusAndPagesNewestFirst() throws Exception {
		for (int i = 1; i <= 5; i++) {
			create("Dr. Doctor " + i, "doctor" + i + "@clinic.co.kr", "010-1000-000" + i);
		}
		mvc.perform(delete(API + "/" + idOfLast()));

		mvc.perform(get(API).param("status", "pending").param("size", "2").param("page", "2"))
			.andExpect(jsonPath("$.totalElements").value(4))
			.andExpect(jsonPath("$.totalPages").value(2))
			.andExpect(jsonPath("$.page").value(2))
			.andExpect(jsonPath("$.content", hasSize(2)));
		mvc.perform(get(API).param("status", "revoked")).andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void badListParametersGiveA400() throws Exception {
		mvc.perform(get(API).param("status", "deleted")).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
		mvc.perform(get(API).param("page", "0").param("size", "101")).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors", hasSize(2)));
	}

	// ---- Expiry job

	@Test
	void overduePendingInvitationsExpire() throws Exception {
		Invitation old = Invitation.issue(new DoctorContact("Dr. Park", "park@clinic.co.kr", "010-3456-8765"),
			NOW.minus(java.time.Duration.ofDays(15)), java.time.Duration.ofDays(14), "admin@hmp.co.kr");
		long id = repository.save(old).getId();

		service.expireDueInvitations();

		mvc.perform(get(API + "/" + id))
			.andExpect(jsonPath("$.status").value("expired"))
			.andExpect(jsonPath("$.history[1].action").value("expired"))
			.andExpect(jsonPath("$.history[1].actor").doesNotExist());
	}

	// ---- helpers

	private ResultActions create(String name, String email, String mobile) throws Exception {
		return mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON).content(body(name, email, mobile)));
	}

	private static String body(String name, String email, String mobile) {
		return """
			{"doctorName":"%s","email":"%s","mobile":"%s"}""".formatted(name, email, mobile);
	}

	private static long idOf(ResultActions result) throws Exception {
		return JsonPath.<Integer>read(result.andReturn().getResponse().getContentAsString(), "$.id").longValue();
	}

	private long idOfLast() {
		return repository.findAll().stream().mapToLong(Invitation::getId).max().orElseThrow();
	}

}
