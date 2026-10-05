package dev.team1.automation.dtos;

import java.time.Instant;

// Estado de la subida automática que muestra la tarjeta del panel de administración.
// status: "ONLINE" si la última subida fue bien, "ERROR" si falló o no hay nube configurada.
public record CronStatusDTOResponse(
    String status,
    Instant lastSyncAt,
    String lastError
) {
}