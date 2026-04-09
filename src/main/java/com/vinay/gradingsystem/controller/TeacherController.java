package com.vinay.gradingsystem.controller;

import com.vinay.gradingsystem.model.Course;
import com.vinay.gradingsystem.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/teacher")
@CrossOrigin(origins = "*")
@Tag(name = "Teacher", description = "Teacher-specific APIs")
public class TeacherController {

    @Autowired
    private CourseService courseService;

    @GetMapping("/courses")
    @Operation(summary = "Get courses for the current teacher")
    public ResponseEntity<List<Course>> getTeacherCourses() {
        return ResponseEntity.ok(courseService.getCoursesByTeacher());
    }

    @GetMapping("/courses/filter")
    @Operation(summary = "Filter courses for the current teacher without changing the default course API")
    public ResponseEntity<List<Course>> filterCourses(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String studentName
    ) {
        return ResponseEntity.ok(courseService.filterCourses(status, studentName));
    }
}
