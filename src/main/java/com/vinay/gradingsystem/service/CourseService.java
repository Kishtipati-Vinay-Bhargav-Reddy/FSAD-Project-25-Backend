package com.vinay.gradingsystem.service;

import com.vinay.gradingsystem.model.Course;
import com.vinay.gradingsystem.model.Student;
import com.vinay.gradingsystem.repository.CourseRepository;
import com.vinay.gradingsystem.repository.StudentRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class CourseService {

    private static final Logger log = LoggerFactory.getLogger(CourseService.class);
    private static final List<String[]> DEFAULT_COURSES = List.of(
            new String[]{"FSAD301", "Full Stack Application Development", "CSE", "Semester 6"},
            new String[]{"DBMS302", "Database Management Systems", "CSE", "Semester 5"},
            new String[]{"DSA201", "Data Structures and Algorithms", "CSE", "Semester 4"},
            new String[]{"OS303", "Operating Systems", "CSE", "Semester 5"},
            new String[]{"CN304", "Computer Networks", "CSE", "Semester 5"},
            new String[]{"SE305", "Software Engineering", "CSE", "Semester 6"}
    );

    @Autowired
    private CourseRepository repo;

    @Autowired
    private StudentRepository studentRepository;

    @PostConstruct
    public void ensureDefaultCourses() {
        reactivateExistingCoursesAfterSoftDeleteMigration();
        DEFAULT_COURSES.forEach(this::saveDefaultCourseIfMissing);
    }

    public List<Course> getAllCourses() {
        log.info("Fetching courses without filters");
        return repo.findAllByOrderByActiveDescCodeAsc();
    }

    public List<Course> getCoursesByTeacher() {
        Long teacherId = getCurrentUserId();
        log.info("Fetching courses for teacherId: {}", teacherId);

        if (teacherId == null) {
            return repo.findAllByOrderByActiveDescCodeAsc();
        }

        return claimLegacyCoursesIfNeeded(teacherId);
    }

    public List<Course> filterCourses(String status, String studentName) {
        Long teacherId = getCurrentUserId();
        Boolean active = normalizeActiveStatus(status);
        String normalizedStudentName = normalizeText(studentName);

        if (teacherId != null) {
            claimLegacyCoursesIfNeeded(teacherId);
        }

        if (active == null && normalizedStudentName == null) {
            log.info("Fetching teacher courses without filters");
            return getCoursesByTeacher();
        }

        log.info(
                "Filtering teacher courses for teacherId: {} with status: {} and studentName: {}",
                teacherId,
                active,
                normalizedStudentName
        );

        return repo.customFilter(teacherId, active, normalizedStudentName);
    }

    public Course createCourse(Course course) {
        String code = normalizeCode(course.getCode());
        String name = normalizeText(course.getName());
        String department = normalizeText(course.getDepartment());
        String term = normalizeText(course.getTerm());

        if (code == null || name == null || department == null || term == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "All course fields are required.");
        }

        if (repo.existsByCodeIgnoreCase(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Course code already exists.");
        }

        Course newCourse = new Course();
        newCourse.setCode(code);
        newCourse.setName(name);
        newCourse.setDepartment(department);
        newCourse.setTerm(term);
        newCourse.setTeacherId(getCurrentUserId());
        newCourse.setActive(true);
        newCourse.setRemovedAt(null);

        return repo.save(newCourse);
    }

    public Optional<Course> findActiveByCode(String code) {
        String normalizedCode = normalizeCode(code);
        if (normalizedCode == null) {
            return Optional.empty();
        }

        return repo.findByCodeIgnoreCaseAndActiveTrue(normalizedCode);
    }

    public Optional<Course> findAnyByCode(String code) {
        String normalizedCode = normalizeCode(code);
        if (normalizedCode == null) {
            return Optional.empty();
        }

        return repo.findByCodeIgnoreCase(normalizedCode);
    }

    public Course removeCourse(String code) {
        String normalizedCode = normalizeCode(code);

        if (normalizedCode == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Course code is required.");
        }

        Course course = repo.findByCodeIgnoreCase(normalizedCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found."));

        if (!course.isActive()) {
            return course;
        }

        course.setActive(false);
        course.setRemovedAt(LocalDateTime.now().toString());

        return repo.save(course);
    }

    public Course removeCourseById(Long courseId) {
        if (courseId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Course id is required.");
        }

        Course course = repo.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found."));

        if (!course.isActive()) {
            return course;
        }

        course.setActive(false);
        course.setRemovedAt(LocalDateTime.now().toString());

        return repo.save(course);
    }

    private void reactivateExistingCoursesAfterSoftDeleteMigration() {
        List<Course> legacyCourses = repo.findByActiveFalseAndRemovedAtIsNull();

        legacyCourses.forEach(course -> {
            course.setActive(true);
            repo.save(course);
        });
    }

    private void saveDefaultCourseIfMissing(String[] values) {
        String code = normalizeCode(values[0]);
        if (code == null || repo.existsByCodeIgnoreCase(code)) {
            return;
        }

        Course course = new Course();
        course.setCode(code);
        course.setName(values[1]);
        course.setDepartment(values[2]);
        course.setTerm(values[3]);
        course.setActive(true);
        course.setRemovedAt(null);
        repo.save(course);
    }

    private String normalizeCode(String value) {
        if (!hasText(value)) {
            return null;
        }

        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        if (!hasText(value)) {
            return null;
        }

        return value.trim();
    }

    private Boolean normalizeActiveStatus(String status) {
        if (!hasText(status)) {
            return null;
        }

        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "ACTIVE", "TRUE", "ENABLED" -> true;
            case "INACTIVE", "FALSE", "DISABLED" -> false;
            default -> null;
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private List<Course> claimLegacyCoursesIfNeeded(Long teacherId) {
        List<Course> teacherCourses = repo.findByTeacherIdOrderByActiveDescCodeAsc(teacherId);

        if (!teacherCourses.isEmpty()) {
            return teacherCourses;
        }

        List<Course> legacyCourses = repo.findByTeacherIdIsNullOrderByActiveDescCodeAsc();

        if (legacyCourses.isEmpty()) {
            return teacherCourses;
        }

        legacyCourses.forEach(course -> {
            course.setTeacherId(teacherId);
            repo.save(course);
        });

        return repo.findByTeacherIdOrderByActiveDescCodeAsc(teacherId);
    }

    private Long getCurrentUserId() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            return null;
        }

        String requesterEmail = attributes.getRequest().getHeader("X-User-Email");

        if (!hasText(requesterEmail)) {
            return null;
        }

        return studentRepository.findByEmailIgnoreCase(requesterEmail.trim().toLowerCase(Locale.ROOT))
                .map(Student::getId)
                .orElse(null);
    }
}
