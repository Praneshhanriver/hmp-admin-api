package com.hmp.admin.invitation;

import java.util.Locale;

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

	// Name contains the text (any case), or the visible mobile digits contain the typed digits.
	// "kim" → name match; "5678" or "010-5678" → contact match; masked digits never match
	private static Specification<Invitation> matchesKeyword(String keyword) {
		return (root, query, cb) -> {
			String text = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
			if (text.isEmpty()) {
				return null;
			}
			var nameMatch = cb.like(cb.lower(root.get("doctorName")), "%" + escapeLike(text) + "%", '\\');
			String digits = MobileNumbers.digitsOnly(text);
			if (digits.isEmpty()) {
				return nameMatch;
			}
			return cb.or(nameMatch, cb.like(root.get("mobileSearchDigits"), "%" + digits + "%"));
		};
	}

	// A typed "%" or "_" is searched for literally, not as a wildcard
	private static String escapeLike(String text) {
		return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

}
