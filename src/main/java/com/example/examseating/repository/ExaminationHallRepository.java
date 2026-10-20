package com.example.examseating.repository;

import com.example.examseating.entity.ExaminationHall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExaminationHallRepository extends JpaRepository<ExaminationHall, Long> {
    Optional<ExaminationHall> findByHallName(String hallName);
    boolean existsByHallName(String hallName);
    boolean existsByHallNameAndIdNot(String hallName, Long id);

    List<ExaminationHall> findByStatus(String status);
    List<ExaminationHall> findByStatusOrderByHallNameAsc(String status);

    List<ExaminationHall> findByHallNameContainingIgnoreCaseOrBuildingNameContainingIgnoreCase(String hallName, String buildingName);

    long countByStatus(String status);

    @Query("SELECT COALESCE(SUM(h.totalCapacity), 0) FROM ExaminationHall h WHERE h.status = 'ACTIVE'")
    Long getTotalActiveCapacity();
}
