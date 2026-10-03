package com.prijilevschi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Fills the added/modified timestamps of audited entities. */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
