package dev.team1.mappers;

import dev.team1.roles.RoleEntity;
import dev.team1.users.UserEntity;
import dev.team1.users.dtos.UserRequestDTO;
import dev.team1.users.dtos.UserResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserEntity toEntity(UserRequestDTO dto) {
        UserEntity entity = new UserEntity();
        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());
        entity.setEmail(dto.getEmail());
        entity.setPassword(dto.getPassword());
        entity.setAddress(dto.getAddress());
        entity.setPostalCode(dto.getPostalCode());
        entity.setCity(dto.getCity());
        return entity;
    }

    public UserResponseDTO toDTO(UserEntity entity) {
        UserResponseDTO dto = UserResponseDTO.builder()
            .id(entity.getId())
            .email(entity.getEmail())
            .firstName(entity.getFirstName())
            .lastName(entity.getLastName())
            .postalCode(entity.getPostalCode())
            .address(entity.getAddress())
            .city(entity.getCity())
            .roles(
                entity.getRoles().stream()
                    .map(RoleEntity::getName)
                    .toList()
            )
            .build();
        return dto;
    }

}