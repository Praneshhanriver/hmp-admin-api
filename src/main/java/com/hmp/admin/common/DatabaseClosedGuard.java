package com.hmp.admin.common;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * The demo database lives in memory (H2). If H2 ever closes it while the app keeps running, reads still
 * answer but every write fails with 500 until someone restarts the service (seen on Render, 8 Oct).
 * When that happens this guard stops the app with exit code 1, so the host starts a fresh instance
 * with the demo data again instead of serving errors.
 */
@Component
public class DatabaseClosedGuard {

	private static final Logger log = LoggerFactory.getLogger(DatabaseClosedGuard.class);

	// H2 error code DATABASE_IS_CLOSED (h2 is a runtime-only dependency, so the number is copied here)
	static final int H2_DATABASE_IS_CLOSED = 90098;

	private final ApplicationContext context;
	private final AtomicBoolean restarting = new AtomicBoolean(false);

	public DatabaseClosedGuard(ApplicationContext context) {
		this.context = context;
	}

	static boolean isDatabaseClosed(Throwable error) {
		for (Throwable cause = error; cause != null; cause = cause.getCause()) {
			if (cause instanceof SQLException sql && sql.getErrorCode() == H2_DATABASE_IS_CLOSED) {
				return true;
			}
			if (cause.getCause() == cause) {
				break;
			}
		}
		return false;
	}

	// Called for every unexpected error; does nothing unless the database is closed
	public void check(Throwable error) {
		if (!isDatabaseClosed(error) || !restarting.compareAndSet(false, true)) {
			return;
		}
		log.error("The in-memory database was closed; stopping so the host restarts the service");
		// A new thread: the current request still gets its 500 answer before the app stops
		Thread stopper = new Thread(() -> System.exit(SpringApplication.exit(context, () -> 1)), "database-closed-exit");
		stopper.setDaemon(false);
		stopper.start();
	}

}
