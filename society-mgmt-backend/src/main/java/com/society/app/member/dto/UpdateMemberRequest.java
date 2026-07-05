package com.society.app.member.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Partial-update payload. Only non-null fields are applied.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMemberRequest {

    private String fullName;
    private String phone;
    private String email;
    private String role;
}
