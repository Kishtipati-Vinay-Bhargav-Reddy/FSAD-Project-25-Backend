package com.vinay.gradingsystem.repository;

import com.vinay.gradingsystem.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {
    boolean existsByCodeIgnoreCase(String code);
    Optional<Course> findByCodeIgnoreCase(String code);
    Optional<Course> findByCodeIgnoreCaseAndActiveTrue(String code);
    List<Course> findAllByOrderByActiveDescCodeAsc();
    List<Course> findByActiveFalseAndRemovedAtIsNull();
}
