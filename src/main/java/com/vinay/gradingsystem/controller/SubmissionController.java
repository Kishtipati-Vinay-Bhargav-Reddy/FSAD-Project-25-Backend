package com.vinay.gradingsystem.controller;

import com.vinay.gradingsystem.dto.CourseAnalyticsDto;
import com.vinay.gradingsystem.model.Assignment;
import com.vinay.gradingsystem.model.Submission;
import com.vinay.gradingsystem.service.AccessControlService;
import com.vinay.gradingsystem.service.AssignmentService;
import com.vinay.gradingsystem.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/submissions")
@CrossOrigin(origins = "*")
@Tag(name = "Submissions", description = "Submission, grading, analytics, and download APIs")
public class SubmissionController {

    @Autowired
    private SubmissionService service;

    @Autowired
    private AccessControlService accessControlService;

    @Autowired
    private AssignmentService assignmentService;

    @PostMapping
    @Operation(summary = "Create or update a submission record")
    public Submission submit(
            @RequestHeader(value = "X-User-Email", required = false) String requesterEmail,
            @RequestBody Submission submission
    ) {
        accessControlService.requireAuthenticatedUser(requesterEmail);
        return service.submit(submission);
    }

    @PostMapping("/upload")
    @Operation(summary = "Upload or replace a submission file")
    public Submission upload(
            @RequestHeader(value = "X-User-Email", required = false) String requesterEmail,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "assignmentId", required = false) Long assignmentId,
            @RequestParam(value = "studentName", required = false) String studentName,
            @RequestParam(value = "studentEmail", required = false) String studentEmail
    ) {
        accessControlService.requireAuthenticatedUser(requesterEmail);
        return service.saveFile(file, assignmentId, studentName, studentEmail);
    }

    @PostMapping("/upload-once")
    @Operation(summary = "Upload a submission file and block duplicate submissions")
    public Submission uploadOnce(
            @RequestHeader(value = "X-User-Email", required = false) String requesterEmail,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "assignmentId", required = false) Long assignmentId,
            @RequestParam(value = "studentName", required = false) String studentName,
            @RequestParam(value = "studentEmail", required = false) String studentEmail
    ) {
        accessControlService.requireAuthenticatedUser(requesterEmail);
        return service.saveFileOnce(file, assignmentId, studentName, studentEmail);
    }

    @GetMapping("/{assignmentId}")
    @Operation(summary = "Get submissions by assignment")
    public List<Submission> getByAssignment(
            @RequestHeader(value = "X-User-Email", required = false) String requesterEmail,
            @PathVariable Long assignmentId
    ) {
        accessControlService.requireTeacherOrAdmin(requesterEmail);
        return service.getByAssignment(assignmentId);
    }

    @GetMapping
    @Operation(summary = "Search submissions with optional filters")
    public List<Submission> getAll(
            @RequestHeader(value = "X-User-Email", required = false) String requesterEmail,
            @RequestParam(required = false) Long assignmentId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String courseCode,
            @RequestParam(required = false) String studentName,
            @RequestParam(required = false) String studentEmail
    ) {
        accessControlService.requireTeacherOrAdmin(requesterEmail);
        return service.getAllSubmissions(assignmentId, studentEmail, studentName, status, courseCode);
    }

    @GetMapping("/mine")
    @Operation(summary = "Get submissions for a single student")
    public List<Submission> getMine(
            @RequestHeader(value = "X-User-Email", required = false) String requesterEmail,
            @RequestParam(value = "studentEmail", required = false) String studentEmail,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String courseCode
    ) {
        accessControlService.requireAuthenticatedUser(requesterEmail);
        return service.getStudentSubmissions(studentEmail, status, courseCode);
    }

    @PutMapping("/grade/{id}")
    @Operation(summary = "Grade a submission and add feedback")
    public Submission grade(
            @RequestHeader(value = "X-User-Email", required = false) String requesterEmail,
            @PathVariable Long id,
            @RequestBody Submission sub
    ) {
        accessControlService.requireTeacherOrAdmin(requesterEmail);
        return service.gradeSubmission(id, sub.getGrade(), sub.getFeedback());
    }

    @GetMapping("/analytics")
    @Operation(summary = "Get analytics by course")
    public List<CourseAnalyticsDto> getAnalytics(
            @RequestHeader(value = "X-User-Email", required = false) String requesterEmail
    ) {
        accessControlService.requireTeacherOrAdmin(requesterEmail);
        return service.getCourseAnalytics();
    }

    @GetMapping("/assignment/{assignmentId}/download-zip")
    @Operation(summary = "Download all assignment submissions as a ZIP file")
    public ResponseEntity<byte[]> downloadAssignmentSubmissionsZip(
            @RequestHeader(value = "X-User-Email", required = false) String requesterEmail,
            @PathVariable Long assignmentId
    ) throws IOException {
        accessControlService.requireTeacherOrAdmin(requesterEmail);

        Assignment assignment = assignmentService.getActiveAssignment(assignmentId);
        List<Submission> submissions = service.getByAssignment(assignmentId);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
            for (Submission submission : submissions) {
                if (submission.getFileName() == null || submission.getFileName().isBlank()) {
                    continue;
                }

                Path path = service.getStoredFilePath(submission.getFileName());
                String zipEntryName = submission.getStudentName().replaceAll("[^a-zA-Z0-9._-]", "_")
                        + "_"
                        + path.getFileName();

                zipOutputStream.putNextEntry(new ZipEntry(zipEntryName));
                Files.copy(path, zipOutputStream);
                zipOutputStream.closeEntry();
            }
        }

        String safeTitle = assignment.getTitle().replaceAll("[^a-zA-Z0-9._-]", "_");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeTitle + "-submissions.zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(byteArrayOutputStream.toByteArray());
    }

    @GetMapping("/file/{fileName}")
    @Operation(summary = "Download a stored submission file")
    public ResponseEntity<Resource> getFile(@PathVariable String fileName) throws IOException {
        Path path = service.getStoredFilePath(fileName);
        Resource resource = new UrlResource(path.toUri());
        String contentType = Files.probeContentType(path);

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + path.getFileName() + "\"")
                .body(resource);
    }
}
