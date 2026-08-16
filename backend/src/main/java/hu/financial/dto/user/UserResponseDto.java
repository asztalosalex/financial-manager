package hu.financial.dto.user;

import java.time.LocalDateTime;

public record UserResponseDto(

    Long id,
    String username,
    String email,
    LocalDateTime createdAt,
    LocalDateTime lastLogin
){}
