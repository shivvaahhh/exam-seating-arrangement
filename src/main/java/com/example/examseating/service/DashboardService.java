package com.example.examseating.service;

import com.example.examseating.dto.DashboardStatsDto;
import com.example.examseating.dto.ExaminationDto;
import com.example.examseating.entity.Examination;
import com.example.examseating.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final StudentRepository studentRepository;
    private final DepartmentRepository departmentRepository;
    private final ExaminationHallRepository hallRepository;
    private final ExaminationRepository examinationRepository;
    private final SeatAllocationRepository allocationRepository;
    private final ExaminationService examinationService;
    private final InvigilatorRepository invigilatorRepository;
    private final InvigilatorAssignmentRepository assignmentRepository;

    public DashboardService(StudentRepository studentRepository,
                            DepartmentRepository departmentRepository,
                            ExaminationHallRepository hallRepository,
                            ExaminationRepository examinationRepository,
                            SeatAllocationRepository allocationRepository,
                            ExaminationService examinationService,
                            InvigilatorRepository invigilatorRepository,
                            InvigilatorAssignmentRepository assignmentRepository) {
        this.studentRepository = studentRepository;
        this.departmentRepository = departmentRepository;
        this.hallRepository = hallRepository;
        this.examinationRepository = examinationRepository;
        this.allocationRepository = allocationRepository;
        this.examinationService = examinationService;
        this.invigilatorRepository = invigilatorRepository;
        this.assignmentRepository = assignmentRepository;
    }

    public DashboardStatsDto getAdminDashboardStats() {
        DashboardStatsDto stats = new DashboardStatsDto();

        long totalStudents = studentRepository.count();
        long totalDepartments = departmentRepository.count();
        long totalHalls = hallRepository.count();
        long upcomingExams = examinationRepository.countByExaminationDateGreaterThanEqual(LocalDate.now());
        long totalAllocatedStudents = allocationRepository.countTotalAllocatedStudents();
        long totalInvigilators = invigilatorRepository.count();
        long totalAssignedDuties = assignmentRepository.count();
        Long totalActiveCapacity = hallRepository.getTotalActiveCapacity();

        // Compute real unallocated / pending candidates across all scheduled exams
        List<ExaminationDto> allExams = examinationService.findAll();
        long pendingSeatSum = 0;
        for (ExaminationDto exam : allExams) {
            if (!"CANCELLED".equalsIgnoreCase(exam.getStatus())) {
                long pendingForExam = Math.max(0, exam.getEligibleStudentCount() - exam.getAllocatedStudentCount());
                pendingSeatSum += pendingForExam;
            }
        }

        stats.setTotalStudents(totalStudents);
        stats.setTotalDepartments(totalDepartments);
        stats.setTotalHalls(totalHalls);
        stats.setUpcomingExams(upcomingExams);
        stats.setTotalAllocatedStudents(totalAllocatedStudents);
        stats.setStudentsNotYetAllocated(pendingSeatSum);
        stats.setTotalInvigilators(totalInvigilators);
        stats.setTotalAssignedDuties(totalAssignedDuties);
        stats.setTotalActiveCapacity(totalActiveCapacity != null ? totalActiveCapacity : 0L);

        // Department-wise student breakdown for Chart.js
        Map<String, Long> deptDist = new LinkedHashMap<>();
        List<Object[]> rows = studentRepository.countStudentsByDepartment();
        for (Object[] row : rows) {
            String deptName = (String) row[0];
            Long count = (Long) row[1];
            deptDist.put(deptName, count);
        }
        stats.setDepartmentStudentDistribution(deptDist);

        return stats;
    }

    public List<ExaminationDto> getUpcomingExaminations(int limit) {
        return examinationService.findUpcoming().stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<ExaminationDto> getRecentAllocations(int limit) {
        return examinationRepository.findByStatus("ALLOCATED").stream()
                .sorted(Comparator.comparing(Examination::getExaminationDate).reversed())
                .limit(limit)
                .map(examinationService::toDto)
                .collect(Collectors.toList());
    }
}
