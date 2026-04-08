package com.vinay.gradingsystem.model;

import jakarta.persistence.*;

@Entity
@Table(name = "course_catalog")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_code", nullable = false, unique = true)
    private String code;

    @Column(name = "course_name", nullable = false)
    private String name;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private String term;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "removed_at")
    private String removedAt;

    public Long getId() { return id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getTerm() { return term; }
    public void setTerm(String term) { this.term = term; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getRemovedAt() { return removedAt; }
    public void setRemovedAt(String removedAt) { this.removedAt = removedAt; }
}
