package com.example.examseating.controller;

import com.example.examseating.dto.ExaminationDto;
import com.example.examseating.dto.HallSeatGridDto;
import com.example.examseating.entity.ExaminationHall;
import com.example.examseating.entity.ExaminationRegistration;
import com.example.examseating.entity.SeatAllocation;
import com.example.examseating.service.ExaminationHallService;
import com.example.examseating.service.ExaminationService;
import com.example.examseating.service.ReportService;
import com.example.examseating.service.SeatAllocationService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/reports")
public class AdminReportController {

    private final ReportService reportService;
    private final ExaminationService examinationService;
    private final ExaminationHallService hallService;
    private final SeatAllocationService allocationService;

    public AdminReportController(ReportService reportService,
                                 ExaminationService examinationService,
                                 ExaminationHallService hallService,
                                 SeatAllocationService allocationService) {
        this.reportService = reportService;
        this.examinationService = examinationService;
        this.hallService = hallService;
        this.allocationService = allocationService;
    }

    @GetMapping
    public String index(Model model) {
        List<ExaminationDto> examinations = examinationService.findAll();
        List<ExaminationHall> halls = hallService.findActiveHalls();

        model.addAttribute("examinations", examinations);
        model.addAttribute("halls", halls);
        model.addAttribute("institutionName", reportService.getInstitutionName());
        return "admin/reports/index";
    }

    @GetMapping("/examination/{examId}")
    public String examinationSeatingReport(@PathVariable Long examId, Model model) {
        ExaminationDto exam = examinationService.findById(examId);
        List<SeatAllocation> allocations = reportService.getExaminationAllocations(examId);

        model.addAttribute("examination", exam);
        model.addAttribute("allocations", allocations);
        model.addAttribute("institutionName", reportService.getInstitutionName());
        model.addAttribute("institutionTagline", reportService.getInstitutionTagline());
        model.addAttribute("academicYear", reportService.getAcademicYear());
        return "admin/reports/exam-seating-report";
    }

    @GetMapping("/attendance/{examId}")
    public String attendanceSheet(@PathVariable Long examId,
                                  @RequestParam(value = "hallId", required = false) Long hallId,
                                  Model model) {
        ExaminationDto exam = examinationService.findById(examId);
        List<SeatAllocation> allocations;
        if (hallId != null) {
            allocations = reportService.getHallAllocationsForExam(examId, hallId);
            model.addAttribute("selectedHall", hallService.findById(hallId));
        } else {
            allocations = reportService.getExaminationAllocations(examId);
        }

        List<ExaminationHall> usedHalls = reportService.getExaminationAllocations(examId).stream()
                .map(SeatAllocation::getExaminationHall)
                .distinct()
                .collect(Collectors.toList());

        model.addAttribute("examination", exam);
        model.addAttribute("allocations", allocations);
        model.addAttribute("usedHalls", usedHalls);
        model.addAttribute("selectedHallId", hallId);
        model.addAttribute("institutionName", reportService.getInstitutionName());
        model.addAttribute("institutionTagline", reportService.getInstitutionTagline());
        model.addAttribute("academicYear", reportService.getAcademicYear());
        return "admin/reports/attendance-sheet";
    }

    @GetMapping("/hall/{hallId}/chart")
    public String hallSeatingChart(@PathVariable Long hallId,
                                   @RequestParam("examId") Long examId,
                                   Model model) {
        HallSeatGridDto grid = allocationService.getHallSeatGrid(examId, hallId);
        model.addAttribute("grid", grid);
        model.addAttribute("institutionName", reportService.getInstitutionName());
        model.addAttribute("institutionTagline", reportService.getInstitutionTagline());
        model.addAttribute("academicYear", reportService.getAcademicYear());
        return "admin/reports/hall-chart";
    }

    @GetMapping("/utilization")
    public String hallCapacityUtilization(Model model) {
        List<ReportService.HallUtilizationDto> report = reportService.getHallUtilizationReport();
        model.addAttribute("report", report);
        model.addAttribute("institutionName", reportService.getInstitutionName());
        return "admin/reports/utilization";
    }

    @GetMapping("/unallocated/{examId}")
    public String unallocatedStudentsReport(@PathVariable Long examId, Model model) {
        ExaminationDto exam = examinationService.findById(examId);
        List<ExaminationRegistration> unallocated = reportService.getUnallocatedStudentsForExam(examId);

        model.addAttribute("examination", exam);
        model.addAttribute("unallocated", unallocated);
        model.addAttribute("institutionName", reportService.getInstitutionName());
        return "admin/reports/unallocated-report";
    }

    @GetMapping("/examination/{examId}/export-csv")
    public void exportCsv(@PathVariable Long examId, HttpServletResponse response) throws Exception {
        byte[] csvData = reportService.generateAllocationCsv(examId);
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"exam_" + examId + "_seating_allocation.csv\"");
        response.getOutputStream().write(csvData);
        response.getOutputStream().flush();
    }
}
