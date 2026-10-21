package com.example.examseating.repository;

import com.example.examseating.entity.InvigilatorAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvigilatorAssignmentRepository extends JpaRepository<InvigilatorAssignment, Long> {

    List<InvigilatorAssignment> findByExaminationId(Long examinationId);

    List<InvigilatorAssignment> findByExaminationIdAndExaminationHallId(Long examinationId, Long hallId);

    List<InvigilatorAssignment> findByInvigilatorId(Long invigilatorId);

    Optional<InvigilatorAssignment> findByExaminationIdAndInvigilatorId(Long examinationId, Long invigilatorId);

    boolean existsByExaminationIdAndInvigilatorId(Long examinationId, Long invigilatorId);

    long countByInvigilatorId(Long invigilatorId);

    void deleteByExaminationId(Long examinationId);

    @Query("SELECT ia FROM InvigilatorAssignment ia " +
           "WHERE ia.invigilator.id = :invigilatorId " +
           "AND ia.examination.examinationDate = :examDate " +
           "AND ia.examination.session = :session")
    List<InvigilatorAssignment> findByInvigilatorAndDateAndSession(
            @Param("invigilatorId") Long invigilatorId,
            @Param("examDate") LocalDate examDate,
            @Param("session") String session
    );

    @Query("SELECT ia FROM InvigilatorAssignment ia " +
           "WHERE ia.invigilator.id = :invigilatorId " +
           "ORDER BY ia.examination.examinationDate ASC, ia.examination.startTime ASC")
    List<InvigilatorAssignment> findUpcomingByInvigilatorId(@Param("invigilatorId") Long invigilatorId);
}
