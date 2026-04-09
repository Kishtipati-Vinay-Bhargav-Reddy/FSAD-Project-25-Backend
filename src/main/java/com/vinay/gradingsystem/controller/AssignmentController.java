package com.vinay.gradingsystem.controller;

import com.vinay.gradingsystem.model.Assignment;
import com.vinay.gradingsystem.model.Course;
import com.vinay.gradingsystem.service.AccessControlService;
import com.vinay.gradingsystem.service.AssignmentService;
import com.vinay.gradingsystem.service.CourseService;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/assignments")
@CrossOrigin(origins = "*")
@Tag(name = "Assignments", description = "Assignment and course management APIs")
public class AssignmentController {

    @Autowired
    private AssignmentService service;

    @Autowired
    private CourseService courseService;

    @Autowired
    private AccessControlService accessControlService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create an assignment")
    public Assignment create(
            @RequestHeader(value = "X-User-Email", required = false) String requesterEmail,
            @RequestBody Assignment assignment
    ) {
        accessControlService.requireTeacherOrAdmin(requesterEmail);
        return service.createAssignment(assignment);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create an assignment with an optional question file")
    public Assignment createWithQuestionFile(
            @RequestHeader(value = "X-User-Email", required = false) String requesterEmail,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String dueDate,
            @RequestParam(required = false) String courseCode,
            @RequestParam(required = false) String courseName,
            @RequestParam(required = false) MultipartFile questionFile
    ) {
        accessControlService.requireTeacherOrAdmin(requesterEmail);

        Assignment assignment = new Assignment();
        assignment.setTitle(title);
        assignment.setDescription(description);
        assignment.setDueDate(dueDate);
        assignment.setCourseCode(courseCode);
        assignment.setCourseName(courseName);

        return service.createAssignment(assignment, questionFile);
    }

    @PutMapping("/{id}/remove")
    @Operation(summary = "Soft delete an assignment")
    public Assignment removeAssignment(
            @RequestHeader(value = "X-User-Email", required = false) String requesterEmail,
            @PathVariable Long id
    ) {
        accessControlService.requireTeacherOrAdmin(requesterEmail);
        return service.removeAssignment(id);
    }

    @GetMapping
    @Operation(summary = "Get active assignments")
    public List<Assignment> getAll(@RequestParam(required = false) String courseCode) {
        return service.getAllAssignments(courseCode);
    }

    @GetMapping("/{id}/question-file")
    @Operation(summary = "Download the question file for an assignment")
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
    @Operation(summary = "Get available courses")
    public List<Course> getCourses() {
        return courseService.getAllCourses();
    }

    @PostMapping("/courses")
    @Operation(summary = "Create a course")
    public Course createCourse(
            @RequestHeader(value = "X-User-Email", required = false) String requesterEmail,
            @RequestBody Course course
    ) {
        accessControlService.requireTeacherOrAdmin(requesterEmail);
        return courseService.createCourse(course);
    }

    @PutMapping("/courses/{code}/remove")
    @Operation(summary = "Soft delete a course")
    public Course removeCourse(
            @RequestHeader(value = "X-User-Email", required = false) String requesterEmail,
            @PathVariable String code
    ) {
        accessControlService.requireTeacherOrAdmin(requesterEmail);
        return courseService.removeCourse(code);
    }
}
