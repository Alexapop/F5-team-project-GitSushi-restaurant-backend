package dev.team1.reports;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Reloj en hora de España. Se inyecta para que los tests puedan fijar la fecha.
@Configuration
public class ReportsConfiguration {

    @Bean
    Clock reportsClock() {
        return Clock.system(ReportPeriod.ZONE);
    }
}