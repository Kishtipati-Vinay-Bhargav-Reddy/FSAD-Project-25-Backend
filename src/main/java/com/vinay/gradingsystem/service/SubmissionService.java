package com.vinay.gradingsystem.service;

import com.vinay.gradingsystem.model.Assignment;
import com.vinay.gradingsystem.model.Submission;
import com.vinay.gradingsystem.repository.SubmissionRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class SubmissionService {

    private static final Path UPLOAD_PATH = Paths.get("uploads").toAbsolutePath().normalize();

    @Autowired
    private SubmissionRepository repo;

    @Autowired
    private AssignmentService assignmentService;

    // ✅ NORMAL SUBMIT
    public Submission submit(Submission submission) {

        if (submission.getStudentName() == null || submission.getStudentName().isEmpty()) {
            throw new RuntimeException("Student name required");
        }

        if (submission.getStudentEmail() == null || submission.getStudentEmail().isEmpty()) {
            throw new RuntimeException("Student email required");
        }

        if (submission.getAssignmentId() == null) {
            throw new RuntimeException("Assignment ID required");
        }

        assignmentService.getActiveAssignment(submission.getAssignmentId());
        submission.setStatus("PENDING");
        submission.setStudentEmail(normalizeEmail(submission.getStudentEmail()));

        return repo.save(submission);
    }

    // ✅ FILE UPLOAD (FIXED 🔥)
    public Submission saveFile(MultipartFile file, Long assignmentId, String studentName, String studentEmail) {

        try {
            if (file.isEmpty()) {
                throw new RuntimeException("File is empty");
            }

            if (assignmentId == null) {
                throw new RuntimeException("Assignment ID required");
            }

            if (studentName == null || studentName.isEmpty()) {
                throw new RuntimeException("Student name required");
            }

            if (studentEmail == null || studentEmail.isEmpty()) {
                throw new RuntimeException("Student email required");
            }

            Assignment assignment = assignmentService.getActiveAssignment(assignmentId);
            String normalizedStudentName = studentName.trim();
            String normalizedStudentEmail = normalizeEmail(studentEmail);
            String originalFileName = sanitizeFileName(file.getOriginalFilename());

            if (originalFileName == null || originalFileName.isEmpty()) {
                throw new RuntimeException("File name invalid");
            }

            String fileName = System.currentTimeMillis() + "_" + originalFileName;
            Optional<Submission> existingSubmission = repo
                    .findFirstByAssignmentIdAndStudentEmailIgnoreCaseOrderByIdDesc(assignment.getId(), normalizedStudentEmail);

            if (!Files.exists(UPLOAD_PATH)) {
                Files.createDirectories(UPLOAD_PATH);
            }

            Path filePath = UPLOAD_PATH.resolve(fileName).normalize();

            if (!filePath.startsWith(UPLOAD_PATH)) {
                throw new RuntimeException("Invalid file path");
            }

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            Submission submission = existingSubmission.orElseGet(Submission::new);
            String previousFileName = submission.getFileName();

            submission.setAssignmentId(assignment.getId());
            submission.setStudentName(normalizedStudentName);
            submission.setStudentEmail(normalizedStudentEmail);
            submission.setFileName(fileName);
            submission.setStatus("PENDING");
            submission.setGrade(null);
            submission.setFeedback(null);

            Submission savedSubmission = repo.save(submission);
            deleteStoredFile(previousFileName, fileName);

            return savedSubmission;

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("File upload failed");
        }
    }

    // ✅ GET BY ASSIGNMENT
    public List<Submission> getByAssignment(Long assignmentId) {
        return repo.findByAssignmentId(assignmentId);
    }

    // ✅ GET ALL
    public List<Submission> getAllSubmissions() {
        return repo.findAll();
    }

    public List<Submission> getStudentSubmissions(String studentEmail) {
        String normalizedStudentEmail = normalizeEmail(studentEmail);

        if (normalizedStudentEmail == null || normalizedStudentEmail.isEmpty()) {
            throw new RuntimeException("Student email required");
        }

        return repo.findByStudentEmailIgnoreCaseOrderByIdDesc(normalizedStudentEmail);
    }

    // ✅ GRADE
    public Submission gradeSubmission(Long id, Integer grade, String feedback) {
        Submission s = repo.findById(id).orElseThrow();

        s.setGrade(grade);
        s.setFeedback(feedback);
        s.setStatus("GRADED");

        return repo.save(s);
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return null;
        }

        return Paths.get(fileName)
                .getFileName()
                .toString()
                .trim()
                .replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }

        return email.trim().toLowerCase();
    }

    private void deleteStoredFile(String previousFileName, String currentFileName) {
        if (previousFileName == null || previousFileName.isBlank() || previousFileName.equals(currentFileName)) {
            return;
        }

        try {
            Path previousFilePath = UPLOAD_PATH.resolve(previousFileName).normalize();

            if (previousFilePath.startsWith(UPLOAD_PATH)) {
                Files.deleteIfExists(previousFilePath);
            }
        } catch (IOException ignored) {
        }
    }
}
