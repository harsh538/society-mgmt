package com.society.app.member.dto;

import com.society.app.member.Member;

/**
 * Public-safe projection of a {@link Member}. Never contains the password hash.
 *
 * <p>Field shape (id, fullName, phone, email, role, isActive) is consumed by the
 * frontend {@code AuthContext} — do not rename without updating the React app.</p>
 */
public class MemberDto {

    private Long id;
    private String fullName;
    private String phone;
    private String email;
    private String role;
    private boolean isActive;

    public MemberDto() {
    }

    public MemberDto(Long id, String fullName, String phone, String email,
                    String role, boolean isActive) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.role = role;
        this.isActive = isActive;
    }

    /** Map a {@link Member} entity to its public DTO form. */
    public static MemberDto from(Member m) {
        return new MemberDto(
                m.getId(),
                m.getFullName(),
                m.getPhone(),
                m.getEmail(),
                m.getRole(),
                m.isActive()
        );
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
