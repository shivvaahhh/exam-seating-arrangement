package com.example.examseating.repository;

import com.example.examseating.entity.Invigilator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvigilatorRepository extends JpaRepository<Invigilator, Long> {

    Optional<Invigilator> findByEmployeeId(String employeeId);

    Optional<Invigilator> findByEmail(String email);

    Optional<Invigilator> findByUser_Username(String username);

    boolean existsByEmployeeId(String employeeId);

    boolean existsByEmail(String email);

    boolean existsByEmployeeIdAndIdNot(String employeeId, Long id);

    boolean existsByEmailAndIdNot(String email, Long id);

    List<Invigilator> findByStatus(String status);

    @Query("SELECT i FROM Invigilator i WHERE " +
           "(:deptId IS NULL OR i.department.id = :deptId) AND " +
           "(:keyword IS NULL OR LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(i.employeeId) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(i.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY i.name ASC")
    List<Invigilator> findWithFilters(@Param("deptId") Long deptId, @Param("keyword") String keyword);
}
