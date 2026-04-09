package com.vinay.gradingsystem.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vinay.gradingsystem.model.Student;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByEmail(String email);
    Optional<Student> findByEmailIgnoreCase(String email);
    List<Student> findAllByOrderByRoleAscNameAsc();
    long countByRoleIgnoreCase(String role);
}
