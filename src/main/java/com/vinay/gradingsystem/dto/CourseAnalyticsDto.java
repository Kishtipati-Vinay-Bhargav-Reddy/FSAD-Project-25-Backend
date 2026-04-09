package com.vinay.gradingsystem.dto;

public class CourseAnalyticsDto {

    private String courseCode;
    private String courseName;
    private long submissionCount;
    private double averageGrade;

    public CourseAnalyticsDto(String courseCode, String courseName, long submissionCount, Double averageGrade) {
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.submissionCount = submissionCount;
        this.averageGrade = averageGrade == null ? 0.0 : averageGrade;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public long getSubmissionCount() {
        return submissionCount;
    }

    public void setSubmissionCount(long submissionCount) {
        this.submissionCount = submissionCount;
    }

    public double getAverageGrade() {
        return averageGrade;
    }

    public void setAverageGrade(double averageGrade) {
        this.averageGrade = averageGrade;
    }
}
