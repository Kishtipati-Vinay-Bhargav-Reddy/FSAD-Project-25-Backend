package com.vinay.gradingsystem.service;

import com.vinay.gradingsystem.model.Course;
import com.vinay.gradingsystem.repository.CourseRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class CourseService {

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

    @PostConstruct
    public void ensureDefaultCourses() {
        reactivateExistingCoursesAfterSoftDeleteMigration();
        DEFAULT_COURSES.forEach(this::saveDefaultCourseIfMissing);
    }

    public List<Course> getAllCourses() {
        return repo.findAllByOrderByActiveDescCodeAsc();
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

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
