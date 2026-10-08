package com.hmp.admin.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;

class DatabaseClosedGuardTest {

	@Test
	void findsTheClosedDatabaseDeepInTheCauseChain() {
		// Same shape as the Render log: Spring -> Hibernate -> H2 check constraint -> "database has been closed"
		SQLException closed = new SQLException("The database has been closed", "90098",
			DatabaseClosedGuard.H2_DATABASE_IS_CLOSED);
		SQLException constraint = new SQLException("Check constraint invalid", "23514", 23514, closed);
		RuntimeException wrapped = new RuntimeException("could not execute statement", constraint);

		assertThat(DatabaseClosedGuard.isDatabaseClosed(wrapped)).isTrue();
	}

	@Test
	void ignoresOtherDatabaseErrors() {
		SQLException constraint = new SQLException("Check constraint invalid", "23514", 23514);

		assertThat(DatabaseClosedGuard.isDatabaseClosed(new RuntimeException(constraint))).isFalse();
		assertThat(DatabaseClosedGuard.isDatabaseClosed(new IllegalStateException("boom"))).isFalse();
	}

}
