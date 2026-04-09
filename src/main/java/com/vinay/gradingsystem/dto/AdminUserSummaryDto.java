package com.vinay.gradingsystem.dto;

import com.vinay.gradingsystem.model.Student;

public class AdminUserSummaryDto {

    private Long id;
    private String name;
    private String email;
    private String role;

    public static AdminUserSummaryDto from(Student student) {
        AdminUserSummaryDto dto = new AdminUserSummaryDto();
        dto.setId(student.getId());
        dto.setName(student.getName());
        dto.setEmail(student.getEmail());
        dto.setRole(student.getRole());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
