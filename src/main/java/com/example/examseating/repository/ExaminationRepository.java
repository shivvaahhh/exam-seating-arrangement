package com.example.examseating.repository;

import com.example.examseating.entity.Examination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExaminationRepository extends JpaRepository<Examination, Long> {
    List<Examination> findByDepartmentId(Long departmentId);
    List<Examination> findByStatus(String status);

    List<Examination> findByExaminationDateGreaterThanEqualOrderByExaminationDateAscStartTimeAsc(LocalDate date);

    @Query("SELECT e FROM Examination e WHERE " +
           "(:deptId IS NULL OR e.department.id = :deptId) AND " +
           "(:semester IS NULL OR e.semester = :semester) AND " +
           "(:status IS NULL OR e.status = :status) " +
           "ORDER BY e.examinationDate ASC, e.startTime ASC")
    List<Examination> findWithFilters(@Param("deptId") Long deptId,
                                      @Param("semester") Integer semester,
                                      @Param("status") String status);

    @Query("SELECT e FROM Examination e WHERE " +
           "e.examinationDate = :examDate AND " +
           "e.session = :session AND " +
           "e.department.id = :deptId AND " +
           "e.semester = :semester AND " +
           "(:excludeId IS NULL OR e.id <> :excludeId) AND " +
           "e.status <> 'CANCELLED'")
    List<Examination> findConflictingExams(@Param("examDate") LocalDate examDate,
                                          @Param("session") String session,
                                          @Param("deptId") Long deptId,
                                          @Param("semester") Integer semester,
                                          @Param("excludeId") Long excludeId);

    long countByExaminationDateGreaterThanEqual(LocalDate date);
}
