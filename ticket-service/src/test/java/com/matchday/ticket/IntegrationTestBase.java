package com.matchday.ticket;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

/**
 * Shared setup for every integration test in this module.
 *
 * <p>Identical configuration across tests means Spring reuses one cached application
 * context, which means Testcontainers starts one PostgreSQL container for the whole run
 * instead of one per test class.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTestBase {
}
