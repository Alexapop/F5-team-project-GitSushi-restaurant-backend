package dev.team1.mappers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import dev.team1.users.UserEntity;

class UserMapperTest {

    @Test
    void toDTO_shouldTreatMissingActiveAsActive() {
        UserEntity user = new UserEntity();
        user.setActive(null);

        assertTrue(UserMapper.toDTO(user).active());
    }

    @Test
    void toDTO_shouldKeepActiveUsersActive() {
        UserEntity user = new UserEntity();
        user.setActive(true);

        assertTrue(UserMapper.toDTO(user).active());
    }

    @Test
    void toDTO_shouldKeepInactiveUsersInactive() {
        UserEntity user = new UserEntity();
        user.setActive(false);

        assertFalse(UserMapper.toDTO(user).active());
    }
}