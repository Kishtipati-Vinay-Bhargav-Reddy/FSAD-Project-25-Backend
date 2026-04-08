package com.vinay.gradingsystem.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.vinay.gradingsystem.model.Student;
import com.vinay.gradingsystem.repository.StudentRepository;

@Service
public class StudentService {

    @Autowired
    private StudentRepository repo;

    // ✅ REGISTER
    public Student register(Student student) {

        if (student.getName() == null || student.getName().isEmpty())
            throw new RuntimeException("Name required");

        if (student.getEmail() == null || student.getEmail().isEmpty())
            throw new RuntimeException("Email required");

        if (student.getPassword() == null || student.getPassword().isEmpty())
            throw new RuntimeException("Password required");

        String normalizedEmail = normalizeEmail(student.getEmail());

        if (normalizedEmail == null)
            throw new RuntimeException("Email required");

        if (repo.findByEmailIgnoreCase(normalizedEmail).isPresent())
            throw new RuntimeException("Email already exists");

        if (student.getRole() == null)
            student.setRole("student");

        student.setEmail(normalizedEmail);
        return repo.save(student);
    }

    // ✅ LOGIN
    public Student login(String email, String password) {
        String normalizedEmail = normalizeEmail(email);

        if (normalizedEmail == null)
            throw new RuntimeException("Email required");

        Student user = repo.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getPassword().equals(password))
            throw new RuntimeException("Invalid password");

        return user;
    }

    // ✅ PROFILE
    public Student getProfile(String email) {
        String normalizedEmail = normalizeEmail(email);

        if (normalizedEmail == null)
            throw new RuntimeException("Email required");

        return repo.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        return email.trim().toLowerCase();
    }
}
