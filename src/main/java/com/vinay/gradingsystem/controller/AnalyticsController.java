package com.vinay.gradingsystem.controller;

import com.vinay.gradingsystem.dto.AnalyticsSummaryDto;
import com.vinay.gradingsystem.service.AccessControlService;
import com.vinay.gradingsystem.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analytics")
@CrossOrigin(origins = "*")
@Tag(name = "Analytics", description = "Standalone analytics APIs")
public class AnalyticsController {

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private AccessControlService accessControlService;

    @GetMapping("/summary")
    @Operation(summary = "Get platform analytics summary")
    public AnalyticsSummaryDto getSummary(
            @RequestHeader(value = "X-User-Email", required = false) String requesterEmail
    ) {
        accessControlService.requireTeacherOrAdmin(requesterEmail);
        return submissionService.getAnalyticsSummary();
    }
}
