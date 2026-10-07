package com.hmp.admin.invitation;

import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.data.jpa.domain.Specification;

// WHERE clauses for the list search: status AND (name OR visible contact digits)
public final class InvitationSpecifications {

	private InvitationSpecifications() {
	}

	public static Specification<Invitation> matching(String keyword, InvitationStatus status) {
		return Specification.allOf(hasStatus(status), matchesKeyword(keyword));
	}

	private static Specification<Invitation> hasStatus(InvitationStatus status) {
		return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
	}

	// Text that looks like (part of) a phone number: digits, spaces and hyphens only
	private static final Pattern PHONE_LIKE = Pattern.compile("^[\\d\\s-]*\\d[\\d\\s-]*$");

	// Name contains the text (any case), or — only when the text looks like a phone number — the visible
	// mobile digits contain the typed digits. "kim" → name; "5678" or "010-5678" → contact;
	// "E2E" → name only (its "2" must not match contacts); masked digits never match
	private static Specification<Invitation> matchesKeyword(String keyword) {
		return (root, query, cb) -> {
			String text = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
			if (text.isEmpty()) {
				return null;
			}
			var nameMatch = cb.like(cb.lower(root.get("doctorName")), "%" + escapeLike(text) + "%", '\\');
			if (!PHONE_LIKE.matcher(text).matches()) {
				return nameMatch;
			}
			String digits = MobileNumbers.digitsOnly(text);
			return cb.or(nameMatch, cb.like(root.get("mobileSearchDigits"), "%" + digits + "%"));
		};
	}

	// A typed "%" or "_" is searched for literally, not as a wildcard
	private static String escapeLike(String text) {
		return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

}
