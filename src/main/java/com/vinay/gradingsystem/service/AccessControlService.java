package com.vinay.gradingsystem.service;

import com.vinay.gradingsystem.model.Student;
import com.vinay.gradingsystem.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccessControlService {

    @Autowired
    private StudentRepository studentRepository;

    public Student requireAuthenticatedUser(String requesterEmail) {
        String normalizedEmail = normalizeEmail(requesterEmail);

        if (normalizedEmail == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required.");
        }

        return studentRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found."));
    }

    public Student requireAdmin(String requesterEmail) {
        Student currentUser = requireAuthenticatedUser(requesterEmail);

        if (!"admin".equalsIgnoreCase(currentUser.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required.");
        }

        return currentUser;
    }

    public Student requireTeacherOrAdmin(String requesterEmail) {
        Student currentUser = requireAuthenticatedUser(requesterEmail);

        if (!"teacher".equalsIgnoreCase(currentUser.getRole()) && !"admin".equalsIgnoreCase(currentUser.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Teacher or admin access required.");
        }

        return currentUser;
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        return email.trim().toLowerCase();
    }
}
