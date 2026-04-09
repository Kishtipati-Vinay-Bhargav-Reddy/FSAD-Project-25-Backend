package com.vinay.gradingsystem.service;

import com.vinay.gradingsystem.model.Student;
import com.vinay.gradingsystem.repository.StudentRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class StudentService {

    private static final Logger log = LoggerFactory.getLogger(StudentService.class);
    private static final Set<String> MANAGEABLE_ROLES = Set.of("student", "teacher");
    private static final String ADMIN_ROLE = "admin";
    private static final String DEFAULT_ADMIN_EMAIL = "admin@grading.local";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin123";

    @Autowired
    private StudentRepository repo;

    @PostConstruct
    public void ensureDefaultAdminUser() {
        if (repo.findByEmailIgnoreCase(DEFAULT_ADMIN_EMAIL).isPresent()) {
            return;
        }

        Student admin = new Student();
        admin.setName("System Admin");
        admin.setEmail(DEFAULT_ADMIN_EMAIL);
        admin.setPassword(DEFAULT_ADMIN_PASSWORD);
        admin.setRole(ADMIN_ROLE);
        repo.save(admin);
    }

    public Student register(Student student) {
        if (student.getName() == null || student.getName().isBlank()) {
            throw new RuntimeException("Name required");
        }

        if (student.getEmail() == null || student.getEmail().isBlank()) {
            throw new RuntimeException("Email required");
        }

        if (student.getPassword() == null || student.getPassword().isBlank()) {
            throw new RuntimeException("Password required");
        }

        String normalizedEmail = normalizeEmail(student.getEmail());

        if (normalizedEmail == null) {
            throw new RuntimeException("Email required");
        }

        if (repo.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        student.setName(student.getName().trim());
        student.setEmail(normalizedEmail);
        student.setRole(resolveRegistrationRole(student.getRole()));

        return repo.save(student);
    }

    public Student login(String email, String password) {
        String normalizedEmail = normalizeEmail(email);

        if (normalizedEmail == null) {
            throw new RuntimeException("Email required");
        }

        Student user = repo.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getPassword().equals(password)) {
            throw new RuntimeException("Invalid password");
        }

        return user;
    }

    public Student getProfile(String email) {
        String normalizedEmail = normalizeEmail(email);

        if (normalizedEmail == null) {
            log.info("Fetching profile without filters");
            return null;
        }

        return repo.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public List<Student> getAllUsers() {
        return repo.findAllByOrderByRoleAscNameAsc();
    }

    public Student updateUserRole(Long userId, String role) {
        String normalizedRole = normalizeRole(role);

        if (!MANAGEABLE_ROLES.contains(normalizedRole)) {
            throw new RuntimeException("Role must be student or teacher.");
        }

        Student user = repo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (ADMIN_ROLE.equalsIgnoreCase(user.getRole())) {
            throw new RuntimeException("Admin role cannot be changed from this endpoint.");
        }

        user.setRole(normalizedRole);
        return repo.save(user);
    }

    private String resolveRegistrationRole(String role) {
        String normalizedRole = normalizeRole(role);

        if (normalizedRole == null || ADMIN_ROLE.equals(normalizedRole)) {
            return "student";
        }

        if (!MANAGEABLE_ROLES.contains(normalizedRole)) {
            throw new RuntimeException("Invalid role selected.");
        }

        return normalizedRole;
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }

        return role.trim().toLowerCase(Locale.ROOT);
    }
}
