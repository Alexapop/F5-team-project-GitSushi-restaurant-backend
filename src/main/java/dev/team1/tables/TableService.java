package dev.team1.tables;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import dev.team1.tables.dtos.TableDTOResponse;

@Service
public class TableService {

    private final TableRepository tableRepository;

    public TableService(
            TableRepository tableRepository) {
        this.tableRepository = tableRepository;
    }

    public TableDTOResponse getTableByDeviceIdentifier(String deviceIdentifier) {
        if (deviceIdentifier == null || deviceIdentifier.strip().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Device identifier cannot be empty.");
        }

        return tableRepository.findByDeviceIdentifier(deviceIdentifier.strip())
                .map(table -> new TableDTOResponse(table.getTableNumber()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No table found for the given device."));
    }
}