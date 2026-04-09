package com.vinay.gradingsystem.controller;

import com.vinay.gradingsystem.model.Student;
import com.vinay.gradingsystem.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@Tag(name = "Authentication", description = "Registration, login, and profile APIs")
public class StudentController {

    @Autowired
    private StudentService service;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public Map<String, Object> register(@RequestBody Student student) {
        Student saved = service.register(student);

        Map<String, Object> response = new HashMap<>();
        response.put("token", "dummy-token");
        response.put("user", saved);

        return response;
    }

    @PostMapping("/login")
    @Operation(summary = "Login an existing user")
    public Map<String, Object> login(@RequestBody Student student) {
        Student user = service.login(student.getEmail(), student.getPassword());

        Map<String, Object> response = new HashMap<>();
        response.put("token", "dummy-token");
        response.put("user", user);

        return response;
    }

    @GetMapping("/profile")
    @Operation(summary = "Get a user profile by email")
    public Student profile(@RequestParam(required = false) String email) {
        return service.getProfile(email);
    }
}
