package com.hmp.admin.invitation;

// Korean mobile numbers: accepted with or without hyphens, stored as "010-1234-5678"
public final class MobileNumbers {

	// 010 / 011 / 016-019, then 3 or 4 digits, then 4 digits; hyphens optional
	public static final String PATTERN = "^01[016789]-?\\d{3,4}-?\\d{4}$";

	private MobileNumbers() {
	}

	// "01012345678" or "010-1234-5678" → "010-1234-5678" (call only after validation)
	public static String normalize(String mobile) {
		String digits = digitsOnly(mobile);
		int middleEnd = digits.length() - 4;
		return digits.substring(0, 3) + "-" + digits.substring(3, middleEnd) + "-" + digits.substring(middleEnd);
	}

	// The digits an admin can see on screen in "010-****-5678": first 3 + last 4.
	// Search matches only these, so searching can never reveal the masked middle digits
	public static String visibleDigits(String mobile) {
		if (mobile == null) {
			return null;
		}
		String digits = digitsOnly(mobile);
		return digits.substring(0, 3) + digits.substring(digits.length() - 4);
	}

	// "010-1234-5678" → "010-****-5678"; the full number never leaves the server
	public static String mask(String mobile) {
		if (mobile == null) {
			return null;
		}
		String visible = visibleDigits(mobile);
		return visible.substring(0, 3) + "-****-" + visible.substring(3);
	}

	public static String digitsOnly(String value) {
		return value.replaceAll("\\D", "");
	}

}
