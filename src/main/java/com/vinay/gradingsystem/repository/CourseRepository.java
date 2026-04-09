package com.vinay.gradingsystem.repository;

import com.vinay.gradingsystem.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {
    boolean existsByCodeIgnoreCase(String code);
    Optional<Course> findByCodeIgnoreCase(String code);
    Optional<Course> findByCodeIgnoreCaseAndActiveTrue(String code);
    List<Course> findAllByOrderByActiveDescCodeAsc();
    List<Course> findByTeacherIdOrderByActiveDescCodeAsc(Long teacherId);
    List<Course> findByTeacherIdIsNullOrderByActiveDescCodeAsc();
    List<Course> findByActiveFalseAndRemovedAtIsNull();

    @Query("""
            select distinct c from Course c
            left join c.assignments a
            left join a.submissions s
            where (:teacherId is null or c.teacherId = :teacherId)
              and (:active is null or c.active = :active)
              and (:studentName is null or lower(s.studentName) like lower(concat('%', :studentName, '%')))
            order by c.active desc, c.code asc
            """)
    List<Course> customFilter(
            @Param("teacherId") Long teacherId,
            @Param("active") Boolean active,
            @Param("studentName") String studentName
    );
}
