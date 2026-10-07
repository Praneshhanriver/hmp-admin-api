package com.hmp.admin.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// One clock for the whole app, so tests can replace "now" with a fixed time
@Configuration
public class ClockConfig {

	@Bean
	public Clock clock(InvitationProperties properties) {
		return Clock.system(properties.timeZone());
	}

}
