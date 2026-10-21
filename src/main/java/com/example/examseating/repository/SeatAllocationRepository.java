package com.example.examseating.repository;

import com.example.examseating.entity.SeatAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeatAllocationRepository extends JpaRepository<SeatAllocation, Long> {

    List<SeatAllocation> findByExaminationId(Long examinationId);

    List<SeatAllocation> findByExaminationIdOrderByExaminationHall_HallNameAscRowNumberAscColumnNumberAsc(Long examinationId);

    List<SeatAllocation> findByExaminationIdAndExaminationHallIdOrderByRowNumberAscColumnNumberAsc(Long examinationId, Long hallId);

    List<SeatAllocation> findByStudentId(Long studentId);

    List<SeatAllocation> findByStudent_User_UsernameOrderByExamination_ExaminationDateAscExamination_StartTimeAsc(String username);

    Optional<SeatAllocation> findByExaminationIdAndStudentId(Long examinationId, Long studentId);

    boolean existsByExaminationId(Long examinationId);

    long countByExaminationId(Long examinationId);

    long countDistinctStudentByExaminationId(Long examinationId);

    @Query("SELECT COUNT(DISTINCT sa.student.id) FROM SeatAllocation sa")
    long countTotalAllocatedStudents();

    @Query("SELECT COUNT(DISTINCT sa.examinationHall.id) FROM SeatAllocation sa WHERE sa.examination.id = :examinationId")
    long countHallsUsedForExamination(@Param("examinationId") Long examinationId);

    @Modifying
    @Query("DELETE FROM SeatAllocation sa WHERE sa.examination.id = :examinationId")
    void deleteByExaminationId(@Param("examinationId") Long examinationId);

    @Query("SELECT sa.examinationHall.id, sa.examinationHall.hallName, COUNT(sa), sa.examinationHall.totalCapacity " +
           "FROM SeatAllocation sa WHERE sa.examination.id = :examinationId " +
           "GROUP BY sa.examinationHall.id, sa.examinationHall.hallName, sa.examinationHall.totalCapacity")
    List<Object[]> getHallUtilizationByExamination(@Param("examinationId") Long examinationId);

    @Query("SELECT sa FROM SeatAllocation sa WHERE " +
           "(:examId IS NULL OR sa.examination.id = :examId) AND " +
           "(:hallId IS NULL OR sa.examinationHall.id = :hallId) AND " +
           "(:deptId IS NULL OR sa.student.department.id = :deptId) AND " +
           "(:keyword IS NULL OR LOWER(sa.student.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(sa.student.registerNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(sa.seatNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY sa.examinationHall.hallName ASC, sa.rowNumber ASC, sa.columnNumber ASC")
    List<SeatAllocation> findWithFilters(@Param("examId") Long examId,
                                         @Param("hallId") Long hallId,
                                         @Param("deptId") Long deptId,
                                         @Param("keyword") String keyword);

    @Query("SELECT sa FROM SeatAllocation sa WHERE sa.student.id = :studentId " +
           "AND sa.examination.examinationDate = :examDate " +
           "AND sa.examination.session = :session " +
           "AND sa.examination.id <> :examId")
    List<SeatAllocation> findClashingStudentAllocations(@Param("studentId") Long studentId,
                                                       @Param("examDate") LocalDate examDate,
                                                       @Param("session") String session,
                                                       @Param("examId") Long examId);

    @Query("SELECT sa FROM SeatAllocation sa WHERE sa.examinationHall.id = :hallId " +
           "AND sa.seatNumber = :seatNumber " +
           "AND sa.examination.examinationDate = :examDate " +
           "AND sa.examination.session = :session " +
           "AND sa.examination.id <> :examId")
    List<SeatAllocation> findClashingSeatAllocations(@Param("hallId") Long hallId,
                                                    @Param("seatNumber") String seatNumber,
                                                    @Param("examDate") LocalDate examDate,
                                                    @Param("session") String session,
                                                    @Param("examId") Long examId);
}
