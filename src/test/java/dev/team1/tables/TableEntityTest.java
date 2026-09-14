package dev.team1.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class TableEntityTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validTablePassesValidation() {
        TableEntity table = new TableEntity();
        table.setTableNumber(12);
        table.setDeviceIdentifier("tablet-12");

        Set<ConstraintViolation<TableEntity>> violations = validator.validate(table);

        assertTrue(violations.isEmpty());
    }

    @Test
    void tableRequiresTableNumberAndDeviceIdentifier() {
        TableEntity table = new TableEntity();

        Set<ConstraintViolation<TableEntity>> violations = validator.validate(table);

        assertEquals(2, violations.size());
        assertTrue(violations.stream().anyMatch(violation ->
                violation.getPropertyPath().toString().equals("tableNumber")));
        assertTrue(violations.stream().anyMatch(violation ->
                violation.getPropertyPath().toString().equals("deviceIdentifier")));
    }

    @Test
    void tableRejectsNonPositiveTableNumber() {
        TableEntity table = new TableEntity();
        table.setTableNumber(0);
        table.setDeviceIdentifier("tablet-12");

        Set<ConstraintViolation<TableEntity>> violations = validator.validate(table);

        assertEquals(1, violations.size());
        assertEquals("tableNumber", violations.iterator().next().getPropertyPath().toString());
    }
}
