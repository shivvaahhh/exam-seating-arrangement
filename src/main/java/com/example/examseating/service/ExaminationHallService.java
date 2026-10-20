package com.example.examseating.service;

import com.example.examseating.dto.ExaminationHallDto;
import com.example.examseating.entity.ExaminationHall;
import com.example.examseating.exception.DuplicateResourceException;
import com.example.examseating.exception.ResourceNotFoundException;
import com.example.examseating.repository.ExaminationHallRepository;
import com.example.examseating.repository.SeatAllocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExaminationHallService {

    private final ExaminationHallRepository hallRepository;
    private final SeatAllocationRepository allocationRepository;

    public ExaminationHallService(ExaminationHallRepository hallRepository,
                                  SeatAllocationRepository allocationRepository) {
        this.hallRepository = hallRepository;
        this.allocationRepository = allocationRepository;
    }

    public List<ExaminationHallDto> findAll() {
        return hallRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<ExaminationHall> findAllEntities() {
        return hallRepository.findAll();
    }

    public List<ExaminationHall> findActiveHalls() {
        return hallRepository.findByStatusOrderByHallNameAsc("ACTIVE");
    }

    public ExaminationHall findEntityById(Long id) {
        return hallRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Examination Hall not found with ID: " + id));
    }

    public ExaminationHallDto findById(Long id) {
        return toDto(findEntityById(id));
    }

    @Transactional
    public ExaminationHallDto create(ExaminationHallDto dto) {
        String hallName = dto.getHallName().trim();
        if (hallRepository.existsByHallName(hallName)) {
            throw new DuplicateResourceException("Hall with name '" + hallName + "' already exists.");
        }

        validateHallDimensions(dto);

        ExaminationHall hall = new ExaminationHall(
                hallName,
                dto.getBuildingName().trim(),
                dto.getFloor() != null ? dto.getFloor().trim() : null,
                dto.getRoomNumber().trim(),
                dto.getTotalCapacity(),
                dto.getNumberOfRows(),
                dto.getNumberOfColumns()
        );
        hall.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        hall.setStatus(dto.getStatus() != null ? dto.getStatus() : "ACTIVE");

        ExaminationHall saved = hallRepository.save(hall);
        return toDto(saved);
    }

    @Transactional
    public ExaminationHallDto update(Long id, ExaminationHallDto dto) {
        ExaminationHall hall = findEntityById(id);
        String hallName = dto.getHallName().trim();

        if (hallRepository.existsByHallNameAndIdNot(hallName, id)) {
            throw new DuplicateResourceException("Hall with name '" + hallName + "' already exists for another hall.");
        }

        validateHallDimensions(dto);

        hall.setHallName(hallName);
        hall.setBuildingName(dto.getBuildingName().trim());
        hall.setFloor(dto.getFloor() != null ? dto.getFloor().trim() : null);
        hall.setRoomNumber(dto.getRoomNumber().trim());
        hall.setTotalCapacity(dto.getTotalCapacity());
        hall.setNumberOfRows(dto.getNumberOfRows());
        hall.setNumberOfColumns(dto.getNumberOfColumns());
        hall.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        if (dto.getStatus() != null) {
            hall.setStatus(dto.getStatus());
        }

        ExaminationHall updated = hallRepository.save(hall);
        return toDto(updated);
    }

    @Transactional
    public void toggleStatus(Long id) {
        ExaminationHall hall = findEntityById(id);
        if ("ACTIVE".equalsIgnoreCase(hall.getStatus())) {
            hall.setStatus("INACTIVE");
        } else {
            hall.setStatus("ACTIVE");
        }
        hallRepository.save(hall);
    }

    @Transactional
    public void delete(Long id) {
        ExaminationHall hall = findEntityById(id);

        long allocCount = allocationRepository.findAll().stream()
                .filter(alloc -> alloc.getExaminationHall().getId().equals(id))
                .count();

        if (allocCount > 0) {
            throw new IllegalStateException("Cannot delete hall '" + hall.getHallName() +
                    "' because it has " + allocCount + " seat allocation records associated with it.");
        }

        hallRepository.delete(hall);
    }

    public List<ExaminationHallDto> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }
        return hallRepository.findByHallNameContainingIgnoreCaseOrBuildingNameContainingIgnoreCase(keyword.trim(), keyword.trim())
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private void validateHallDimensions(ExaminationHallDto dto) {
        if (dto.getTotalCapacity() == null || dto.getTotalCapacity() <= 0) {
            throw new IllegalArgumentException("Total capacity must be a positive number.");
        }
        if (dto.getNumberOfRows() == null || dto.getNumberOfRows() <= 0) {
            throw new IllegalArgumentException("Number of rows must be a positive number.");
        }
        if (dto.getNumberOfColumns() == null || dto.getNumberOfColumns() <= 0) {
            throw new IllegalArgumentException("Number of columns must be a positive number.");
        }
    }

    public ExaminationHallDto toDto(ExaminationHall hall) {
        ExaminationHallDto dto = new ExaminationHallDto();
        dto.setId(hall.getId());
        dto.setHallName(hall.getHallName());
        dto.setBuildingName(hall.getBuildingName());
        dto.setFloor(hall.getFloor());
        dto.setRoomNumber(hall.getRoomNumber());
        dto.setTotalCapacity(hall.getTotalCapacity());
        dto.setNumberOfRows(hall.getNumberOfRows());
        dto.setNumberOfColumns(hall.getNumberOfColumns());
        dto.setStatus(hall.getStatus());
        dto.setDescription(hall.getDescription());
        return dto;
    }
}
