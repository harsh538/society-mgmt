package com.society.app.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateMemberRequest {

    @NotBlank
    @Size(max = 150)
    private String fullName;

    @NotBlank
    @Size(min = 10, max = 15, message = "phone must be 10–15 digits")
    @Pattern(regexp = "\\d+", message = "phone must contain only digits")
    private String phone;

    @Size(max = 150)
    private String email;

    /** Optional — defaults to MEMBER in the service when null/blank. */
    @Pattern(regexp = "ADMIN|MEMBER", message = "role must be ADMIN or MEMBER")
    private String role;
}
