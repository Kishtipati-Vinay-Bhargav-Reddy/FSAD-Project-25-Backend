package com.vinay.gradingsystem.repository;

import com.vinay.gradingsystem.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findByAssignmentId(Long assignmentId);
    List<Submission> findByStudentEmailIgnoreCaseOrderByIdDesc(String studentEmail);
    Optional<Submission> findFirstByAssignmentIdAndStudentNameIgnoreCaseOrderByIdDesc(Long assignmentId, String studentName);
    Optional<Submission> findFirstByAssignmentIdAndStudentEmailIgnoreCaseOrderByIdDesc(Long assignmentId, String studentEmail);
}
