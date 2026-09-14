package dev.team1.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import dev.team1.tables.dtos.TableDTOResponse;

@ExtendWith(MockitoExtension.class)
class TableServiceTest {

    @Mock
    private TableRepository tableRepository;

    @Test
    void getTableByDeviceIdentifierReturnsAssociatedTable() {
        TableEntity table = new TableEntity();
        table.setTableNumber(12);
        table.setDeviceIdentifier("tablet-12");
        when(tableRepository.findByDeviceIdentifier("tablet-12"))
                .thenReturn(Optional.of(table));

        TableService service = new TableService(tableRepository);

        TableDTOResponse response = service.getTableByDeviceIdentifier(" tablet-12 ");

        assertEquals(12, response.tableNumber());
        verify(tableRepository).findByDeviceIdentifier("tablet-12");
    }

    @Test
    void getTableByDeviceIdentifierReturnsNotFoundWhenTableDoesNotExist() {
        when(tableRepository.findByDeviceIdentifier("unknown-device"))
                .thenReturn(Optional.empty());

        TableService service = new TableService(tableRepository);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.getTableByDeviceIdentifier("unknown-device"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }
}
