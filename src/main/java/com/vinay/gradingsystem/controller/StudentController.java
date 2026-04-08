package com.vinay.gradingsystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.vinay.gradingsystem.model.Student;
import com.vinay.gradingsystem.service.StudentService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")   // 🔥 MUST match frontend
@CrossOrigin(origins = "*")
public class StudentController {

    @Autowired
    private StudentService service;

    // ✅ REGISTER
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Student student) {

        Student saved = service.register(student);

        Map<String, Object> res = new HashMap<>();
        res.put("token", "dummy-token");   // frontend needs this
        res.put("user", saved);

        return res;
    }

    // ✅ LOGIN
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Student student) {

        Student user = service.login(student.getEmail(), student.getPassword());

        Map<String, Object> res = new HashMap<>();
        res.put("token", "dummy-token");
        res.put("user", user);

        return res;
    }

    // ✅ PROFILE (VERY IMPORTANT FIX)
    @GetMapping("/profile")
    public Student profile(@RequestParam String email) {
        return service.getProfile(email);
    }
}
