package com.vinay.gradingsystem.controller;

import com.vinay.gradingsystem.model.Submission;
import com.vinay.gradingsystem.service.SubmissionService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;

import java.nio.file.*;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/submissions")
@CrossOrigin(origins = "*")
public class SubmissionController {

    @Autowired
    private SubmissionService service;

    // ✅ NORMAL SUBMIT
    @PostMapping
    public Submission submit(@RequestBody Submission submission) {
        return service.submit(submission);
    }

    // ✅ FILE UPLOAD
    @PostMapping("/upload")
    public Submission upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("assignmentId") Long assignmentId,
            @RequestParam("studentName") String studentName,
            @RequestParam("studentEmail") String studentEmail
    ) {
        return service.saveFile(file, assignmentId, studentName, studentEmail);
    }

    // ✅ GET BY ASSIGNMENT
    @GetMapping("/{assignmentId}")
    public List<Submission> getByAssignment(@PathVariable Long assignmentId) {
        return service.getByAssignment(assignmentId);
    }

    // ✅ GET ALL
    @GetMapping
    public List<Submission> getAll() {
        return service.getAllSubmissions();
    }

    @GetMapping("/mine")
    public List<Submission> getMine(@RequestParam("studentEmail") String studentEmail) {
        return service.getStudentSubmissions(studentEmail);
    }

    // ✅ GRADE
    @PutMapping("/grade/{id}")
    public Submission grade(@PathVariable Long id, @RequestBody Submission sub) {
        return service.gradeSubmission(id, sub.getGrade(), sub.getFeedback());
    }

    // ✅ FILE VIEW (FIXED 🔥)
    @GetMapping("/file/{fileName}")
    public ResponseEntity<Resource> getFile(@PathVariable String fileName) throws IOException {

        Path path = Paths.get("uploads").resolve(fileName).normalize();
        Resource resource = new UrlResource(path.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            throw new RuntimeException("File not found: " + fileName);
        }

        String contentType = Files.probeContentType(path);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(resource);
    }
}
