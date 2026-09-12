package cl.previred.personas.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita el soporte de tareas programadas ({@code @Scheduled}) en toda la
 * aplicacion. Sin esta clase, el {@link cl.previred.personas.resilience.ReconciliationScheduler}
 * quedaria declarado pero nunca se ejecutaria.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
