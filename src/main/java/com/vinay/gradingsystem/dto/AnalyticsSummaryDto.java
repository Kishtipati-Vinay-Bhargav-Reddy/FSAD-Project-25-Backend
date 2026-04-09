package com.vinay.gradingsystem.dto;

public class AnalyticsSummaryDto {

    private final long totalSubmissions;
    private final double averageGrade;

    public AnalyticsSummaryDto(long totalSubmissions, double averageGrade) {
        this.totalSubmissions = totalSubmissions;
        this.averageGrade = averageGrade;
    }

    public long getTotalSubmissions() {
        return totalSubmissions;
    }

    public double getAverageGrade() {
        return averageGrade;
    }
}
