package com.example.examseating.repository;

import com.example.examseating.entity.ExaminationRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExaminationRegistrationRepository extends JpaRepository<ExaminationRegistration, Long> {
    List<ExaminationRegistration> findByExaminationId(Long examinationId);
    List<ExaminationRegistration> findByExaminationIdOrderByStudent_RegisterNumberAsc(Long examinationId);
    List<ExaminationRegistration> findByStudentId(Long studentId);

    Optional<ExaminationRegistration> findByExaminationIdAndStudentId(Long examinationId, Long studentId);
    boolean existsByExaminationIdAndStudentId(Long examinationId, Long studentId);

    long countByExaminationId(Long examinationId);
    long countByExaminationIdAndIsAllocated(Long examinationId, boolean isAllocated);

    @Modifying
    @Query("DELETE FROM ExaminationRegistration er WHERE er.examination.id = :examinationId")
    void deleteByExaminationId(@Param("examinationId") Long examinationId);

    @Modifying
    @Query("UPDATE ExaminationRegistration er SET er.isAllocated = :allocated WHERE er.examination.id = :examinationId")
    void updateAllocationStatusByExaminationId(@Param("examinationId") Long examinationId, @Param("allocated") boolean allocated);
}
