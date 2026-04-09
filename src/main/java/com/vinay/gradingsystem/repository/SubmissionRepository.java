package com.vinay.gradingsystem.repository;

import com.vinay.gradingsystem.dto.CourseAnalyticsDto;
import com.vinay.gradingsystem.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findByAssignmentIdOrderBySubmittedAtDescIdDesc(Long assignmentId);
    List<Submission> findByStudentEmailIgnoreCaseOrderBySubmittedAtDescIdDesc(String studentEmail);
    Optional<Submission> findFirstByAssignmentIdAndStudentNameIgnoreCaseOrderByIdDesc(Long assignmentId, String studentName);
    Optional<Submission> findFirstByAssignmentIdAndStudentEmailIgnoreCaseOrderByIdDesc(Long assignmentId, String studentEmail);
    Optional<Submission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);
    Optional<Submission> findByStudentIdAndAssignmentId(Long studentId, Long assignmentId);

    @Query("""
            select s from Submission s
            left join s.assignment a
            where (:assignmentId is null or s.assignmentId = :assignmentId)
              and (:studentEmail is null or lower(s.studentEmail) = lower(:studentEmail))
              and (:studentName is null or lower(s.studentName) like lower(concat('%', :studentName, '%')))
              and (:status is null or upper(s.status) = upper(:status))
              and (:courseCode is null or upper(a.courseCode) = upper(:courseCode))
            order by s.submittedAt desc, s.id desc
            """)
    List<Submission> search(
            @Param("assignmentId") Long assignmentId,
            @Param("studentEmail") String studentEmail,
            @Param("studentName") String studentName,
            @Param("status") String status,
            @Param("courseCode") String courseCode
    );

    @Query("""
            select new com.vinay.gradingsystem.dto.CourseAnalyticsDto(
                coalesce(a.courseCode, 'UNASSIGNED'),
                coalesce(a.courseName, 'Course Not Assigned'),
                count(s.id),
                avg(s.grade)
            )
            from Assignment a
            left join a.submissions s
            where a.active = true
            group by a.courseCode, a.courseName
            order by a.courseCode asc
            """)
    List<CourseAnalyticsDto> fetchCourseAnalytics();

    @Query("select avg(s.grade) from Submission s where s.grade is not null")
    Double findAverageGrade();
}
