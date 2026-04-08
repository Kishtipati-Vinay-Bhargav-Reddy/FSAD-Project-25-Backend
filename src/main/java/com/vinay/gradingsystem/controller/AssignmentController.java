package com.vinay.gradingsystem.controller;

import com.vinay.gradingsystem.model.Assignment;
import com.vinay.gradingsystem.model.Course;
import com.vinay.gradingsystem.service.AssignmentService;
import com.vinay.gradingsystem.service.CourseService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/assignments")
@CrossOrigin(origins = "*")
public class AssignmentController {

    @Autowired
    private AssignmentService service;

    @Autowired
    private CourseService courseService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Assignment create(@RequestBody Assignment assignment) {
        return service.createAssignment(assignment);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Assignment createWithQuestionFile(
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam String dueDate,
            @RequestParam(required = false) String courseCode,
            @RequestParam(required = false) String courseName,
            @RequestParam(required = false) MultipartFile questionFile
    ) {
        Assignment assignment = new Assignment();
        assignment.setTitle(title);
        assignment.setDescription(description);
        assignment.setDueDate(dueDate);
        assignment.setCourseCode(courseCode);
        assignment.setCourseName(courseName);

        return service.createAssignment(assignment, questionFile);
    }

    @PutMapping("/{id}/remove")
    public Assignment removeAssignment(@PathVariable Long id) {
        return service.removeAssignment(id);
    }

    @GetMapping
    public List<Assignment> getAll(@RequestParam(required = false) String courseCode) {
        return service.getAllAssignments(courseCode);
    }

    @GetMapping("/{id}/question-file")
    public ResponseEntity<Resource> getQuestionFile(@PathVariable Long id) throws IOException {
        Path filePath = service.getQuestionFilePath(id);
        Resource resource = new UrlResource(filePath.toUri());
        String contentType = Files.probeContentType(filePath);

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + service.getQuestionFileDownloadName(id) + "\""
                )
                .body(resource);
    }

    @GetMapping("/courses")
    public List<Course> getCourses() {
        return courseService.getAllCourses();
    }

    @PostMapping("/courses")
    public Course createCourse(@RequestBody Course course) {
        return courseService.createCourse(course);
    }

    @PutMapping("/courses/{code}/remove")
    public Course removeCourse(@PathVariable String code) {
        return courseService.removeCourse(code);
    }
}
