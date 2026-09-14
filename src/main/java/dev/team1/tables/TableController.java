package dev.team1.tables;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.team1.tables.dtos.TableDTOResponse;

@RestController
@RequestMapping(path = "${api-endpoint}/tables")
public class TableController {

    private final TableService tableService;

    public TableController(TableService tableService) {
        this.tableService = tableService;
    }

    @GetMapping("/by-device")
    public ResponseEntity<TableDTOResponse> getCurrentTable(
            @RequestHeader("Device-Identifier") String deviceIdentifier) {
        TableDTOResponse response = tableService.getTableByDeviceIdentifier(deviceIdentifier);
        return ResponseEntity.ok(response);
    }
}
