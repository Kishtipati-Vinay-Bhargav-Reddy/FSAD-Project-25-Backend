package com.vinay.gradingsystem.service;

import com.vinay.gradingsystem.model.Assignment;
import com.vinay.gradingsystem.model.Course;
import com.vinay.gradingsystem.repository.AssignmentRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
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
public class AssignmentService {

    private static final Logger log = LoggerFactory.getLogger(AssignmentService.class);
    private static final String UNASSIGNED_COURSE_CODE = "UNASSIGNED";
    private static final String UNASSIGNED_COURSE_NAME = "Course Not Assigned";
    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;
    private static final Path QUESTION_FILE_UPLOAD_PATH = Paths.get("assignment-files").toAbsolutePath().normalize();
    private static final Set<String> ALLOWED_QUESTION_FILE_EXTENSIONS = Set.of("pdf", "doc", "docx");

    @Autowired
    private AssignmentRepository repo;

    @Autowired
    private CourseService courseService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void migrateExistingAssignments() {
        dropLegacyCourseForeignKeys();
        reactivateExistingAssignmentsAfterSoftDeleteMigration();
        connectExistingAssignmentsToCourses();
    }

    public Assignment createAssignment(Assignment assignment) {
        return createAssignment(assignment, null);
    }

    public Assignment createAssignment(Assignment assignment, MultipartFile questionFile) {
        String normalizedCourseCode = normalizeCourseCode(assignment.getCourseCode());
        String normalizedDueDate = normalizeDueDate(assignment.getDueDate());

        if (normalizedDueDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignment deadline is required.");
        }

        validateDeadlineFormat(normalizedDueDate);

        assignment.setDueDate(normalizedDueDate);
        assignment.setTitle(normalizeRequiredText(assignment.getTitle(), "Assignment title is required."));
        assignment.setDescription(normalizeRequiredText(assignment.getDescription(), "Assignment description is required."));
        assignment.setTeacherName(normalizeOptionalText(assignment.getTeacherName()));
        assignment.setActive(true);
        assignment.setRemovedAt(null);

        if (normalizedCourseCode == null) {
            assignment.setCourse(null);
            assignment.setCourseCode(UNASSIGNED_COURSE_CODE);
            assignment.setCourseName(UNASSIGNED_COURSE_NAME);
        } else {
            Course selectedCourse = courseService.findActiveByCode(normalizedCourseCode)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected course does not exist or was removed."));

            assignment.setCourse(selectedCourse);
            assignment.setCourseCode(normalizedCourseCode);
            assignment.setCourseName(selectedCourse.getName());
        }

        if (questionFile != null && !questionFile.isEmpty()) {
            applyQuestionFile(assignment, questionFile);
        } else {
            assignment.setQuestionFileName(null);
            assignment.setQuestionFileOriginalName(null);
        }

        return repo.save(assignment);
    }

    public List<Assignment> getAllAssignments(String courseCode) {
        String normalizedCourseCode = normalizeCourseCode(courseCode);

        if (normalizedCourseCode == null) {
            log.info("Fetching assignments without filters");
            return repo.findAllByActiveTrueOrderByDueDateAsc();
        }

        return repo.findByCourseCodeIgnoreCaseAndActiveTrueOrderByDueDateAsc(normalizedCourseCode);
    }

    public Assignment removeAssignment(Long assignmentId) {
        Assignment assignment = repo.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found."));

        if (!assignment.isActive()) {
            return assignment;
        }

        assignment.setActive(false);
        assignment.setRemovedAt(LocalDateTime.now().toString());

        return repo.save(assignment);
    }

    public Assignment getActiveAssignment(Long assignmentId) {
        return repo.findByIdAndActiveTrue(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignment is not available."));
    }

    public Path getQuestionFilePath(Long assignmentId) {
        Assignment assignment = getActiveAssignment(assignmentId);

        if (!hasText(assignment.getQuestionFileName())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Question file not available.");
        }

        Path filePath = QUESTION_FILE_UPLOAD_PATH
                .resolve(Paths.get(assignment.getQuestionFileName()).getFileName().toString())
                .normalize();

        if (!filePath.startsWith(QUESTION_FILE_UPLOAD_PATH)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid question file path.");
        }

        if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Question file not found.");
        }

        return filePath;
    }

    public String getQuestionFileDownloadName(Long assignmentId) {
        Assignment assignment = getActiveAssignment(assignmentId);

        if (hasText(assignment.getQuestionFileOriginalName())) {
            return assignment.getQuestionFileOriginalName().trim();
        }

        return "assignment-question";
    }

    private void reactivateExistingAssignmentsAfterSoftDeleteMigration() {
        List<Assignment> legacyAssignments = repo.findByActiveFalseAndRemovedAtIsNull();

        legacyAssignments.forEach(assignment -> {
            assignment.setActive(true);
            repo.save(assignment);
        });
    }

    private void dropLegacyCourseForeignKeys() {
        List<String> legacyConstraintNames = jdbcTemplate.queryForList("""
                        select constraint_name
                        from information_schema.key_column_usage
                        where table_schema = database()
                          and table_name = 'assignment'
                          and column_name = 'course_id'
                          and referenced_table_name = 'course'
                        """,
                String.class
        );

        legacyConstraintNames.forEach(this::dropForeignKeyIfSafe);
    }

    private void connectExistingAssignmentsToCourses() {
        repo.findAll().forEach(assignment -> {
            if (assignment.getCourse() != null) {
                return;
            }

            String normalizedCourseCode = normalizeCourseCode(assignment.getCourseCode());

            if (normalizedCourseCode == null || UNASSIGNED_COURSE_CODE.equalsIgnoreCase(normalizedCourseCode)) {
                assignment.setCourse(null);
                assignment.setCourseCode(UNASSIGNED_COURSE_CODE);
                assignment.setCourseName(assignment.getCourseName() == null ? UNASSIGNED_COURSE_NAME : assignment.getCourseName());
                repo.save(assignment);
                return;
            }

            Optional<Course> course = courseService.findAnyByCode(normalizedCourseCode);
            course.ifPresent(existingCourse -> {
                assignment.setCourse(existingCourse);
                assignment.setCourseCode(existingCourse.getCode());
                assignment.setCourseName(existingCourse.getName());
                repo.save(assignment);
            });
        });
    }

    private void dropForeignKeyIfSafe(String constraintName) {
        if (!hasText(constraintName) || !constraintName.matches("[A-Za-z0-9_]+")) {
            log.warn("Skipping unsafe foreign key name while repairing assignment schema: {}", constraintName);
            return;
        }

        jdbcTemplate.execute("alter table assignment drop foreign key `" + constraintName + "`");
        log.info("Dropped legacy assignment foreign key {}", constraintName);
    }

    private void validateDeadlineFormat(String dueDate) {
        try {
            LocalDateTime.parse(dueDate);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignment deadline is invalid.");
        }
    }

    private void applyQuestionFile(Assignment assignment, MultipartFile questionFile) {
        String originalFileName = sanitizeFileName(questionFile.getOriginalFilename());

        if (!hasText(originalFileName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question file name is invalid.");
        }

        if (questionFile.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question file must be 10MB or smaller.");
        }

        String extension = getFileExtension(originalFileName);

        if (!ALLOWED_QUESTION_FILE_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF and Word files are allowed.");
        }

        try {
            Files.createDirectories(QUESTION_FILE_UPLOAD_PATH);

            String storedFileName = System.currentTimeMillis() + "_question_" + originalFileName;
            Path targetFile = QUESTION_FILE_UPLOAD_PATH.resolve(storedFileName).normalize();

            if (!targetFile.startsWith(QUESTION_FILE_UPLOAD_PATH)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid question file path.");
            }

            Files.copy(questionFile.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);

            assignment.setQuestionFileName(storedFileName);
            assignment.setQuestionFileOriginalName(originalFileName);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to save question file.");
        }
    }

    private String normalizeCourseCode(String courseCode) {
        if (!hasText(courseCode)) {
            return null;
        }

        return courseCode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeDueDate(String dueDate) {
        if (!hasText(dueDate)) {
            return null;
        }

        return dueDate.trim();
    }

    private String normalizeRequiredText(String value, String message) {
        if (!hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }

        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        if (!hasText(value)) {
            return null;
        }

        return value.trim();
    }

    private String sanitizeFileName(String fileName) {
        if (!hasText(fileName)) {
            return null;
        }

        String baseName = Paths.get(fileName).getFileName().toString().trim();

        if (!hasText(baseName)) {
            return null;
        }

        return baseName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String getFileExtension(String fileName) {
        int extensionStart = fileName.lastIndexOf('.');

        if (extensionStart < 0 || extensionStart == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(extensionStart + 1).toLowerCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
