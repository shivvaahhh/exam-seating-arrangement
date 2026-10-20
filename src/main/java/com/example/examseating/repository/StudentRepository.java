package com.example.examseating.repository;

import com.example.examseating.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByRegisterNumber(String registerNumber);
    Optional<Student> findByEmail(String email);
    Optional<Student> findByUser_Username(String username);

    boolean existsByRegisterNumber(String registerNumber);
    boolean existsByRegisterNumberAndIdNot(String registerNumber, Long id);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);

    List<Student> findByDepartmentId(Long departmentId);
    List<Student> findByDepartmentIdAndSemester(Long departmentId, Integer semester);
    List<Student> findBySemester(Integer semester);

    @Query("SELECT s FROM Student s WHERE " +
           "(:deptId IS NULL OR s.department.id = :deptId) AND " +
           "(:semester IS NULL OR s.semester = :semester) AND " +
           "(:keyword IS NULL OR LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(s.registerNumber) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Student> findWithFilters(@Param("deptId") Long deptId,
                                  @Param("semester") Integer semester,
                                  @Param("keyword") String keyword);

    @Query("SELECT s.department.departmentName, COUNT(s) FROM Student s GROUP BY s.department.departmentName")
    List<Object[]> countStudentsByDepartment();

    long countByStatus(String status);
}
