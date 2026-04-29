package com.vinay.gradingsystem.repository;

import com.vinay.gradingsystem.model.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findAllByActiveTrueOrderByDueDateAsc();
    List<Assignment> findByCourseCodeIgnoreCaseAndActiveTrueOrderByDueDateAsc(String courseCode);
    Optional<Assignment> findByIdAndActiveTrue(Long id);
    List<Assignment> findByActiveFalseAndRemovedAtIsNull();
}
