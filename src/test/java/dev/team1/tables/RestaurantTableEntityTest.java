package dev.team1.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class RestaurantTableEntityTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validRestaurantTablePassesValidation() {
        RestaurantTableEntity restaurantTable = new RestaurantTableEntity();
        restaurantTable.setTableNumber(12);
        restaurantTable.setDeviceIdentifier("tablet-12");

        Set<ConstraintViolation<RestaurantTableEntity>> violations = validator.validate(restaurantTable);

        assertTrue(violations.isEmpty());
    }

    @Test
    void restaurantTableRequiresTableNumberAndDeviceIdentifier() {
        RestaurantTableEntity restaurantTable = new RestaurantTableEntity();

        Set<ConstraintViolation<RestaurantTableEntity>> violations = validator.validate(restaurantTable);

        assertEquals(2, violations.size());
        assertTrue(violations.stream().anyMatch(violation ->
                violation.getPropertyPath().toString().equals("tableNumber")));
        assertTrue(violations.stream().anyMatch(violation ->
                violation.getPropertyPath().toString().equals("deviceIdentifier")));
    }

    @Test
    void restaurantTableRejectsNonPositiveTableNumber() {
        RestaurantTableEntity restaurantTable = new RestaurantTableEntity();
        restaurantTable.setTableNumber(0);
        restaurantTable.setDeviceIdentifier("tablet-12");

        Set<ConstraintViolation<RestaurantTableEntity>> violations = validator.validate(restaurantTable);

        assertEquals(1, violations.size());
        assertEquals("tableNumber", violations.iterator().next().getPropertyPath().toString());
    }
}
