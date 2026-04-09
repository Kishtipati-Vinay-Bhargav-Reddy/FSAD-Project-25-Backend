package com.vinay.gradingsystem.service;

import com.vinay.gradingsystem.dto.AnalyticsSummaryDto;
import com.vinay.gradingsystem.dto.CourseAnalyticsDto;
import com.vinay.gradingsystem.model.Assignment;
import com.vinay.gradingsystem.model.Student;
import com.vinay.gradingsystem.model.Submission;
import com.vinay.gradingsystem.repository.StudentRepository;
import com.vinay.gradingsystem.repository.SubmissionRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class SubmissionService {

    private static final Logger log = LoggerFactory.getLogger(SubmissionService.class);
    private static final Path UPLOAD_PATH = Paths.get("uploads").toAbsolutePath().normalize();
    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_FILE_EXTENSIONS = Set.of("pdf", "doc", "docx");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    @Autowired
    private SubmissionRepository repo;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private StudentRepository studentRepository;

    @PostConstruct
    public void backfillSubmissionRelations() {
        repo.findAll().forEach(submission -> {
            boolean updated = false;

            if (submission.getStudentId() == null && submission.getStudentEmail() != null) {
                studentRepository.findByEmailIgnoreCase(submission.getStudentEmail())
                        .ifPresent(student -> submission.setStudentId(student.getId()));
                updated = submission.getStudentId() != null;
            }

            if (submission.getSubmittedAt() == null) {
                submission.setSubmittedAt(LocalDateTime.now());
                updated = true;
            }

            if (updated) {
                repo.save(submission);
            }
        });
    }

    public Submission submit(Submission submission) {
        if (submission.getStudentName() == null || submission.getStudentName().isBlank()) {
            throw new RuntimeException("Student name required");
        }

        if (submission.getStudentEmail() == null || submission.getStudentEmail().isBlank()) {
            throw new RuntimeException("Student email required");
        }

        if (submission.getAssignmentId() == null) {
            throw new RuntimeException("Assignment ID required");
        }

        Assignment assignment = assignmentService.getActiveAssignment(submission.getAssignmentId());
        validateDeadline(assignment);

        Student student = resolveStudent(submission.getStudentEmail());
        Submission target = repo.findByAssignmentIdAndStudentId(assignment.getId(), student.getId())
                .orElseGet(Submission::new);

        target.setAssignmentId(assignment.getId());
        target.setStudentId(student.getId());
        target.setStudentName(submission.getStudentName().trim());
        target.setStudentEmail(student.getEmail());
        target.setStatus("PENDING");
        target.setGrade(null);
        target.setFeedback(null);
        target.setGradedAt(null);
        target.setSubmittedAt(LocalDateTime.now());

        if (submission.getFileName() != null && !submission.getFileName().isBlank()) {
            target.setFileName(sanitizeFileName(submission.getFileName()));
        }

        return repo.save(target);
    }

    public Submission saveFile(MultipartFile file, Long assignmentId, String studentName, String studentEmail) {
        SubmissionUploadContext context = prepareUploadContext(file, assignmentId, studentName, studentEmail);
        Submission submission = repo.findByAssignmentIdAndStudentId(
                        context.assignment().getId(),
                        context.student().getId()
                )
                .orElseGet(Submission::new);
        String previousFileName = submission.getFileName();
        String storedFileName = storeUploadedFile(context.file(), context.originalFileName());

        submission.setAssignmentId(context.assignment().getId());
        submission.setStudentId(context.student().getId());
        submission.setStudentName(context.studentName());
        submission.setStudentEmail(context.student().getEmail());
        submission.setFileName(storedFileName);
        submission.setStatus("PENDING");
        submission.setGrade(null);
        submission.setFeedback(null);
        submission.setGradedAt(null);
        submission.setSubmittedAt(LocalDateTime.now());

        Submission savedSubmission = repo.save(submission);
        deleteStoredFile(previousFileName, storedFileName);

        return savedSubmission;
    }

    public Submission saveFileOnce(MultipartFile file, Long assignmentId, String studentName, String studentEmail) {
        SubmissionUploadContext context = prepareUploadContext(file, assignmentId, studentName, studentEmail);

        if (repo.findByStudentIdAndAssignmentId(context.student().getId(), context.assignment().getId()).isPresent()) {
            throw new RuntimeException("Already submitted");
        }

        String storedFileName = storeUploadedFile(context.file(), context.originalFileName());
        Submission submission = new Submission();

        submission.setAssignmentId(context.assignment().getId());
        submission.setStudentId(context.student().getId());
        submission.setStudentName(context.studentName());
        submission.setStudentEmail(context.student().getEmail());
        submission.setFileName(storedFileName);
        submission.setStatus("PENDING");
        submission.setGrade(null);
        submission.setFeedback(null);
        submission.setGradedAt(null);
        submission.setSubmittedAt(LocalDateTime.now());

        return repo.save(submission);
    }

    public List<Submission> getByAssignment(Long assignmentId) {
        if (assignmentId == null) {
            throw new RuntimeException("Assignment ID required");
        }

        return repo.findByAssignmentIdOrderBySubmittedAtDescIdDesc(assignmentId);
    }

    public List<Submission> getAllSubmissions(Long assignmentId, String studentEmail, String studentName, String status, String courseCode) {
        String normalizedStudentEmail = normalizeEmail(studentEmail);
        String normalizedStudentName = normalizeText(studentName);
        String normalizedStatus = normalizeStatus(status);
        String normalizedCourseCode = normalizeCourseCode(courseCode);

        if (assignmentId == null
                && normalizedStudentEmail == null
                && normalizedStudentName == null
                && normalizedStatus == null
                && normalizedCourseCode == null) {
            log.info("Fetching submissions without filters");
        }

        return repo.search(
                assignmentId,
                normalizedStudentEmail,
                normalizedStudentName,
                normalizedStatus,
                normalizedCourseCode
        );
    }

    public List<Submission> getStudentSubmissions(String studentEmail, String status, String courseCode) {
        String normalizedStudentEmail = normalizeEmail(studentEmail);
        String normalizedStatus = normalizeStatus(status);
        String normalizedCourseCode = normalizeCourseCode(courseCode);

        if (normalizedStudentEmail == null) {
            log.info("Fetching submissions without studentEmail filter");
        }

        return repo.search(
                null,
                normalizedStudentEmail,
                null,
                normalizedStatus,
                normalizedCourseCode
        );
    }

    public Submission gradeSubmission(Long id, Integer grade, String feedback) {
        if (grade == null || grade < 0 || grade > 10) {
            throw new RuntimeException("Grade must be between 0 and 10.");
        }

        Submission submission = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Submission not found."));

        submission.setGrade(grade);
        submission.setFeedback(feedback == null ? "" : feedback.trim());
        submission.setStatus("GRADED");
        submission.setGradedAt(LocalDateTime.now());

        return repo.save(submission);
    }

    public Path getStoredFilePath(String fileName) {
        String sanitizedFileName = sanitizeFileName(fileName);

        if (sanitizedFileName == null) {
            throw new RuntimeException("File not found");
        }

        Path path = UPLOAD_PATH.resolve(sanitizedFileName).normalize();

        if (!path.startsWith(UPLOAD_PATH) || !Files.exists(path) || !Files.isReadable(path)) {
            throw new RuntimeException("File not found: " + sanitizedFileName);
        }

        return path;
    }

    public List<CourseAnalyticsDto> getCourseAnalytics() {
        return repo.fetchCourseAnalytics();
    }

    public AnalyticsSummaryDto getAnalyticsSummary() {
        Double averageGrade = repo.findAverageGrade();
        return new AnalyticsSummaryDto(repo.count(), averageGrade == null ? 0.0 : averageGrade);
    }

    private void validateDeadline(Assignment assignment) {
        if (assignment.getDeadline() == null) {
            throw new RuntimeException("Assignment deadline is invalid");
        }

        if (LocalDateTime.now().isAfter(assignment.getDeadline())) {
            throw new RuntimeException("Deadline passed");
        }
    }

    private void validateUploadedFile(MultipartFile file) {
        String fileName = sanitizeFileName(file.getOriginalFilename());

        if (fileName == null) {
            throw new RuntimeException("File name invalid");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new RuntimeException("File too large");
        }

        String extension = getFileExtension(fileName);
        String contentType = file.getContentType();

        if (!ALLOWED_FILE_EXTENSIONS.contains(extension)) {
            throw new RuntimeException("Invalid file type");
        }

        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new RuntimeException("Invalid file type");
        }
    }

    private Student resolveStudent(String studentEmail) {
        String normalizedStudentEmail = normalizeEmail(studentEmail);

        if (normalizedStudentEmail == null) {
            throw new RuntimeException("Student email required");
        }

        return studentRepository.findByEmailIgnoreCase(normalizedStudentEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student account not found."));
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

    private String getFileExtension(String fileName) {
        int extensionStart = fileName.lastIndexOf('.');

        if (extensionStart < 0 || extensionStart == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(extensionStart + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        return status.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeCourseCode(String courseCode) {
        if (courseCode == null || courseCode.isBlank()) {
            return null;
        }

        return courseCode.trim().toUpperCase(Locale.ROOT);
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

    private SubmissionUploadContext prepareUploadContext(
            MultipartFile file,
            Long assignmentId,
            String studentName,
            String studentEmail
    ) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        if (assignmentId == null) {
            throw new RuntimeException("Assignment ID required");
        }

        if (studentName == null || studentName.isBlank()) {
            throw new RuntimeException("Student name required");
        }

        if (studentEmail == null || studentEmail.isBlank()) {
            throw new RuntimeException("Student email required");
        }

        validateUploadedFile(file);

        Assignment assignment = assignmentService.getActiveAssignment(assignmentId);
        validateDeadline(assignment);

        Student student = resolveStudent(studentEmail);
        String normalizedStudentName = studentName.trim();
        String originalFileName = sanitizeFileName(file.getOriginalFilename());

        if (originalFileName == null || originalFileName.isBlank()) {
            throw new RuntimeException("File name invalid");
        }

        return new SubmissionUploadContext(file, assignment, student, normalizedStudentName, originalFileName);
    }

    private String storeUploadedFile(MultipartFile file, String originalFileName) {
        String storedFileName = System.currentTimeMillis() + "_" + originalFileName;

        try {
            Files.createDirectories(UPLOAD_PATH);

            Path filePath = UPLOAD_PATH.resolve(storedFileName).normalize();

            if (!filePath.startsWith(UPLOAD_PATH)) {
                throw new RuntimeException("Invalid file path");
            }

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new RuntimeException("File upload failed");
        }

        return storedFileName;
    }

    private record SubmissionUploadContext(
            MultipartFile file,
            Assignment assignment,
            Student student,
            String studentName,
            String originalFileName
    ) {
    }
}
