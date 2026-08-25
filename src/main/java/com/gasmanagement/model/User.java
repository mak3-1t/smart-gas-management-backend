package com.gasmanagement.model;

import com.gasmanagement.model.enums.AccountStatus;
import com.gasmanagement.model.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    @Indexed(unique = true)
    private String email;

    @Indexed
    private String phone;

    private String passwordHash;

    private UserRole role;

    @Builder.Default
    private AccountStatus status = AccountStatus.ACTIVE;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;
}
