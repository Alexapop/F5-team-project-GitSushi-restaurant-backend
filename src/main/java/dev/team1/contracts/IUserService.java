package dev.team1.contracts;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import dev.team1.users.dtos.UserPatchRequestDTO;
import dev.team1.users.dtos.UserRequestDTO;
import dev.team1.users.dtos.UserResponseDTO;

public interface IUserService {
    
    public UserResponseDTO store(UserRequestDTO requestDTO);
    
    public Page<UserResponseDTO> getAll(Pageable pageable);

    public UserResponseDTO update(UUID id, UserPatchRequestDTO requestDTO);

    public void delete(UUID id);
}
