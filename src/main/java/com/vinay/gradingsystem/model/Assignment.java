package com.vinay.gradingsystem.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "assignment")
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;

    @Column(name = "due_date")
    @JsonProperty("dueDate") // 🔥 IMPORTANT FIX
    private String dueDate;

    @Column(name = "teacher_name")
    private String teacherName;

    @Column(name = "course_code")
    @JsonProperty("courseCode")
    private String courseCode;

    @Column(name = "course_name")
    @JsonProperty("courseName")
    private String courseName;

    @Column(name = "question_file_name")
    @JsonProperty("questionFileName")
    private String questionFileName;

    @Column(name = "question_file_original_name")
    @JsonProperty("questionFileOriginalName")
    private String questionFileOriginalName;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "removed_at")
    @JsonProperty("removedAt")
    private String removedAt;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @JsonIgnore
    @OneToMany(mappedBy = "assignment")
    private List<Submission> submissions = new ArrayList<>();

    // getters & setters

    public Long getId() { return id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public String getQuestionFileName() { return questionFileName; }
    public void setQuestionFileName(String questionFileName) { this.questionFileName = questionFileName; }

    public String getQuestionFileOriginalName() { return questionFileOriginalName; }
    public void setQuestionFileOriginalName(String questionFileOriginalName) { this.questionFileOriginalName = questionFileOriginalName; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getRemovedAt() { return removedAt; }
    public void setRemovedAt(String removedAt) { this.removedAt = removedAt; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public List<Submission> getSubmissions() { return submissions; }
    public void setSubmissions(List<Submission> submissions) { this.submissions = submissions; }

    @JsonIgnore
    @Transient
    public LocalDateTime getDeadline() {
        if (dueDate == null || dueDate.isBlank()) {
            return null;
        }

        try {
            return LocalDateTime.parse(dueDate.trim());
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}
