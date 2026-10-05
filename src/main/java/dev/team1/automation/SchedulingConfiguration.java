package dev.team1.automation;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Activa las tareas programadas (@Scheduled), como la subida nocturna del resumen de ventas.
@Configuration
@EnableScheduling
public class SchedulingConfiguration {
}