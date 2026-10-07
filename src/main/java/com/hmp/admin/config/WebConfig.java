package com.hmp.admin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.hmp.admin.invitation.InvitationStatus;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final String[] allowedOrigins;

	public WebConfig(@Value("${hmp.cors.allowed-origins}") String[] allowedOrigins) {
		this.allowedOrigins = allowedOrigins;
	}

	// The Next.js app runs on another origin (localhost:3000 / Vercel), so the browser needs CORS
	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
			.allowedOrigins(allowedOrigins)
			.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
	}

	// ?status=pending (lower case, as the frontend sends it) → InvitationStatus.PENDING
	@Override
	public void addFormatters(FormatterRegistry registry) {
		registry.addConverter(String.class, InvitationStatus.class, InvitationStatus::fromApiValue);
	}

}
