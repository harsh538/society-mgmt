package com.society.app.unit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LinkMemberRequest {

    @NotNull
    private Long memberId;

    @NotBlank
    private String relationship;       // OWNER | CO_OWNER | TENANT
}
