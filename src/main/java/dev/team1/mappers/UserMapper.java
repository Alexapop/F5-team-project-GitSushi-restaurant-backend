package dev.team1.mappers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dev.team1.roles.RoleEntity;
import dev.team1.users.UserEntity;
import dev.team1.users.dtos.UserPatchRequestDTO;
import dev.team1.users.dtos.UserRequestDTO;
import dev.team1.users.dtos.UserResponseDTO;

public class UserMapper {

    private UserMapper() {}
    
    public static UserEntity toEntity(UserRequestDTO dto) {
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

    public static UserResponseDTO toDTO(UserEntity entity) {
        UserResponseDTO dto = UserResponseDTO.builder()
            .id(entity.getId())
            .email(entity.getEmail())
            .firstName(entity.getFirstName())
            .lastName(entity.getLastName())
            .postalCode(entity.getPostalCode())
            .address(entity.getAddress())
            .city(entity.getCity())
            // Si "active" viene vacío (usuarios antiguos) se considera activo,
            // igual que el valor por defecto de UserEntity.
            .active(!Boolean.FALSE.equals(entity.getActive()))
            .roles(
                entity.getRoles().stream()
                    .map(RoleEntity::getName)
                    .toList()
            )
            .build();
        return dto;
    }

    public static String rolesToString(List<String> rolesList) {
        return String.join(", ", rolesList);
    }

    public static UserEntity updateEntity(UserEntity entity, UserPatchRequestDTO dto, RoleEntity role) {

        if (dto.firstName() != null) entity.setFirstName(dto.firstName());
        if (dto.lastName() != null) entity.setLastName(dto.lastName());
        if (dto.email() != null) entity.setEmail(dto.email());
        if (dto.address() != null) entity.setAddress(dto.address());
        if (dto.postalCode() != null) entity.setPostalCode(dto.postalCode());
        if (dto.city() != null) entity.setCity(dto.city());
        if (dto.active() != null) entity.setActive(dto.active());
        
        if (role != null) {
            Set<RoleEntity> roles = new HashSet<>();
            roles.add(role);
            entity.setRoles(roles);
        }

        return entity;
    }

}