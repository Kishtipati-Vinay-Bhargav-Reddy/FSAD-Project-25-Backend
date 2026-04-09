package com.vinay.gradingsystem.controller;

import com.vinay.gradingsystem.dto.AdminUserRoleUpdateRequest;
import com.vinay.gradingsystem.dto.AdminUserSummaryDto;
import com.vinay.gradingsystem.model.Course;
import com.vinay.gradingsystem.model.Student;
import com.vinay.gradingsystem.service.AccessControlService;
import com.vinay.gradingsystem.service.CourseService;
import com.vinay.gradingsystem.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "*")
@Tag(name = "Admin", description = "Admin-only management APIs")
public class AdminController {

    @Autowired
    private AccessControlService accessControlService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private CourseService courseService;

    @GetMapping("/users")
    @Operation(summary = "Get all registered users")
    public List<AdminUserSummaryDto> getAllUsers(
            @RequestHeader(value = "X-User-Email", required = false) String requesterEmail
    ) {
        accessControlService.requireAdmin(requesterEmail);
        return studentService.getAllUsers().stream()
                .map(AdminUserSummaryDto::from)
                .toList();
    }

    @PutMapping("/users/{userId}/role")
    @Operation(summary = "Change a user role between student and teacher")
    public AdminUserSummaryDto updateUserRole(
            @RequestHeader(value = "X-User-Email", required = false) String requesterEmail,
            @PathVariable Long userId,
            @RequestBody AdminUserRoleUpdateRequest request
    ) {
        accessControlService.requireAdmin(requesterEmail);
        Student updatedUser = studentService.updateUserRole(userId, request.getRole());
        return AdminUserSummaryDto.from(updatedUser);
    }

    @DeleteMapping("/courses/{courseId:\\d+}")
    @Operation(summary = "Delete a course from active use by course id")
    public Course deleteCourseById(
            @RequestHeader(value = "X-User-Email", required = false) String requesterEmail,
            @PathVariable Long courseId
    ) {
        accessControlService.requireAdmin(requesterEmail);
        return courseService.removeCourseById(courseId);
    }

    @DeleteMapping("/courses/{courseCode:[A-Za-z].*}")
    @Operation(summary = "Delete a course from active use")
    public Course deleteCourse(
            @RequestHeader(value = "X-User-Email", required = false) String requesterEmail,
            @PathVariable String courseCode
    ) {
        accessControlService.requireAdmin(requesterEmail);
        return courseService.removeCourse(courseCode);
    }
}
