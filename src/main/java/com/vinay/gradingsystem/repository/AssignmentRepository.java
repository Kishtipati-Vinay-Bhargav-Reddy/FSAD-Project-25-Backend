package com.vinay.gradingsystem.repository;

import com.vinay.gradingsystem.model.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findAllByActiveTrueOrderByDueDateAsc();
    List<Assignment> findByCourseCodeIgnoreCaseAndActiveTrueOrderByDueDateAsc(String courseCode);
    Optional<Assignment> findByIdAndActiveTrue(Long id);
    List<Assignment> findByActiveFalseAndRemovedAtIsNull();
}
