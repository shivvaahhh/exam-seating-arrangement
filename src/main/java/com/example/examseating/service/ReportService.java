package com.example.examseating.service;

import com.example.examseating.entity.Examination;
import com.example.examseating.entity.ExaminationHall;
import com.example.examseating.entity.ExaminationRegistration;
import com.example.examseating.entity.SeatAllocation;
import com.example.examseating.exception.ResourceNotFoundException;
import com.example.examseating.repository.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final ExaminationRepository examinationRepository;
    private final SeatAllocationRepository allocationRepository;
    private final ExaminationRegistrationRepository registrationRepository;
    private final ExaminationHallRepository hallRepository;

    @Value("${app.institution.name:GOVERNMENT ENGINEERING COLLEGE}")
    private String institutionName;

    @Value("${app.institution.tagline:Controller of Examinations - Autonomous System}")
    private String institutionTagline;

    @Value("${app.institution.academic-year:2026-2027}")
    private String academicYear;

    public ReportService(ExaminationRepository examinationRepository,
                         SeatAllocationRepository allocationRepository,
                         ExaminationRegistrationRepository registrationRepository,
                         ExaminationHallRepository hallRepository) {
        this.examinationRepository = examinationRepository;
        this.allocationRepository = allocationRepository;
        this.registrationRepository = registrationRepository;
        this.hallRepository = hallRepository;
    }

    public String getInstitutionName() { return institutionName; }
    public String getInstitutionTagline() { return institutionTagline; }
    public String getAcademicYear() { return academicYear; }

    public List<SeatAllocation> getExaminationAllocations(Long examId) {
        return allocationRepository.findByExaminationIdOrderByExaminationHall_HallNameAscRowNumberAscColumnNumberAsc(examId);
    }

    public List<SeatAllocation> getHallAllocationsForExam(Long examId, Long hallId) {
        return allocationRepository.findByExaminationIdAndExaminationHallIdOrderByRowNumberAscColumnNumberAsc(examId, hallId);
    }

    public List<ExaminationRegistration> getUnallocatedStudentsForExam(Long examId) {
        List<ExaminationRegistration> allRegs = registrationRepository.findByExaminationIdOrderByStudent_RegisterNumberAsc(examId);
        return allRegs.stream()
                .filter(r -> !r.isAllocated())
                .collect(Collectors.toList());
    }

    public static class HallUtilizationDto {
        private Long hallId;
        private String hallName;
        private String buildingName;
        private String roomNumber;
        private int totalCapacity;
        private int physicalSeats;
        private int effectiveCapacity;
        private String status;
        private int currentlyAllocatedCount;
        private double utilizationPercentage;

        public HallUtilizationDto() {}

        public Long getHallId() { return hallId; }
        public void setHallId(Long hallId) { this.hallId = hallId; }
        public String getHallName() { return hallName; }
        public void setHallName(String hallName) { this.hallName = hallName; }
        public String getBuildingName() { return buildingName; }
        public void setBuildingName(String buildingName) { this.buildingName = buildingName; }
        public String getRoomNumber() { return roomNumber; }
        public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
        public int getTotalCapacity() { return totalCapacity; }
        public void setTotalCapacity(int totalCapacity) { this.totalCapacity = totalCapacity; }
        public int getPhysicalSeats() { return physicalSeats; }
        public void setPhysicalSeats(int physicalSeats) { this.physicalSeats = physicalSeats; }
        public int getEffectiveCapacity() { return effectiveCapacity; }
        public void setEffectiveCapacity(int effectiveCapacity) { this.effectiveCapacity = effectiveCapacity; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public int getCurrentlyAllocatedCount() { return currentlyAllocatedCount; }
        public void setCurrentlyAllocatedCount(int currentlyAllocatedCount) { this.currentlyAllocatedCount = currentlyAllocatedCount; }
        public double getUtilizationPercentage() { return utilizationPercentage; }
        public void setUtilizationPercentage(double utilizationPercentage) { this.utilizationPercentage = utilizationPercentage; }
    }

    public List<HallUtilizationDto> getHallUtilizationReport() {
        List<ExaminationHall> halls = hallRepository.findAll();
        List<SeatAllocation> allAllocations = allocationRepository.findAll();

        Map<Long, Long> hallCountMap = allAllocations.stream()
                .collect(Collectors.groupingBy(a -> a.getExaminationHall().getId(), Collectors.counting()));

        List<HallUtilizationDto> dtos = new ArrayList<>();
        for (ExaminationHall hall : halls) {
            HallUtilizationDto dto = new HallUtilizationDto();
            dto.setHallId(hall.getId());
            dto.setHallName(hall.getHallName());
            dto.setBuildingName(hall.getBuildingName());
            dto.setRoomNumber(hall.getRoomNumber());
            dto.setTotalCapacity(hall.getTotalCapacity());
            dto.setPhysicalSeats(hall.getMaxPhysicalSeats());
            dto.setEffectiveCapacity(hall.getEffectiveCapacity());
            dto.setStatus(hall.getStatus());

            int allocated = hallCountMap.getOrDefault(hall.getId(), 0L).intValue();
            dto.setCurrentlyAllocatedCount(allocated);

            double pct = hall.getEffectiveCapacity() > 0
                    ? Math.min(100.0, (double) allocated / hall.getEffectiveCapacity() * 100.0)
                    : 0.0;
            dto.setUtilizationPercentage(Math.round(pct * 10.0) / 10.0);

            dtos.add(dto);
        }
        return dtos;
    }

    public byte[] generateAllocationCsv(Long examId) throws Exception {
        Examination exam = examinationRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Examination not found with ID: " + examId));

        List<SeatAllocation> allocations = allocationRepository
                .findByExaminationIdOrderByExaminationHall_HallNameAscRowNumberAscColumnNumberAsc(examId);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                     .builder()
                     .setHeader("SL No", "Register Number", "Student Name", "Department", "Subject Code",
                             "Subject Name", "Exam Date", "Session", "Hall Name", "Building", "Room",
                             "Seat Number", "Row", "Column", "Attendance Status")
                     .build())) {

            int slNo = 1;
            for (SeatAllocation sa : allocations) {
                csvPrinter.printRecord(
                        slNo++,
                        sa.getStudent().getRegisterNumber(),
                        sa.getStudent().getFullName(),
                        sa.getStudent().getDepartment().getDepartmentCode(),
                        exam.getSubject().getSubjectCode(),
                        exam.getSubject().getSubjectName(),
                        exam.getExaminationDate(),
                        exam.getSession(),
                        sa.getExaminationHall().getHallName(),
                        sa.getExaminationHall().getBuildingName(),
                        sa.getExaminationHall().getRoomNumber(),
                        sa.getSeatNumber(),
                        sa.getRowNumber(),
                        sa.getColumnNumber(),
                        sa.getAttendanceStatus()
                );
            }
            csvPrinter.flush();
        }
        return out.toByteArray();
    }
}
