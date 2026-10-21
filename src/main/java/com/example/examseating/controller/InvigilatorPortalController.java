package com.example.examseating.controller;

import com.example.examseating.dto.HallSeatGridDto;
import com.example.examseating.entity.Invigilator;
import com.example.examseating.entity.InvigilatorAssignment;
import com.example.examseating.entity.SeatAllocation;
import com.example.examseating.exception.ResourceNotFoundException;
import com.example.examseating.repository.InvigilatorAssignmentRepository;
import com.example.examseating.service.InvigilatorService;
import com.example.examseating.service.ReportService;
import com.example.examseating.service.SeatAllocationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/invigilator")
public class InvigilatorPortalController {

    private final InvigilatorService invigilatorService;
    private final InvigilatorAssignmentRepository assignmentRepository;
    private final SeatAllocationService allocationService;
    private final ReportService reportService;

    public InvigilatorPortalController(InvigilatorService invigilatorService,
                                       InvigilatorAssignmentRepository assignmentRepository,
                                       SeatAllocationService allocationService,
                                       ReportService reportService) {
        this.invigilatorService = invigilatorService;
        this.assignmentRepository = assignmentRepository;
        this.allocationService = allocationService;
        this.reportService = reportService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Invigilator invigilator = getAuthenticatedInvigilator(userDetails);
        List<InvigilatorAssignment> duties = invigilatorService.getUpcomingDutiesForInvigilator(invigilator.getId());

        model.addAttribute("invigilator", invigilator);
        model.addAttribute("duties", duties);
        model.addAttribute("institutionName", reportService.getInstitutionName());
        return "invigilator/dashboard";
    }

    @GetMapping("/duty/{assignmentId}/chart")
    public String viewDutySeatingChart(@AuthenticationPrincipal UserDetails userDetails,
                                       @PathVariable Long assignmentId,
                                       Model model) {
        Invigilator invigilator = getAuthenticatedInvigilator(userDetails);
        InvigilatorAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Duty assignment not found with ID: " + assignmentId));

        // Enforce duty isolation: ensure this assignment belongs to the logged-in invigilator
        if (!assignment.getInvigilator().getId().equals(invigilator.getId())) {
            return "redirect:/access-denied";
        }

        HallSeatGridDto grid = allocationService.getHallSeatGrid(
                assignment.getExamination().getId(),
                assignment.getExaminationHall().getId()
        );

        model.addAttribute("invigilator", invigilator);
        model.addAttribute("assignment", assignment);
        model.addAttribute("grid", grid);
        model.addAttribute("institutionName", reportService.getInstitutionName());
        model.addAttribute("institutionTagline", reportService.getInstitutionTagline());
        model.addAttribute("academicYear", reportService.getAcademicYear());
        return "invigilator/chart";
    }

    @GetMapping("/duty/{assignmentId}/attendance")
    public String viewDutyAttendanceSheet(@AuthenticationPrincipal UserDetails userDetails,
                                          @PathVariable Long assignmentId,
                                          Model model) {
        Invigilator invigilator = getAuthenticatedInvigilator(userDetails);
        InvigilatorAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Duty assignment not found with ID: " + assignmentId));

        if (!assignment.getInvigilator().getId().equals(invigilator.getId())) {
            return "redirect:/access-denied";
        }

        List<SeatAllocation> allocations = reportService.getHallAllocationsForExam(
                assignment.getExamination().getId(),
                assignment.getExaminationHall().getId()
        );

        model.addAttribute("invigilator", invigilator);
        model.addAttribute("assignment", assignment);
        model.addAttribute("allocations", allocations);
        model.addAttribute("institutionName", reportService.getInstitutionName());
        model.addAttribute("institutionTagline", reportService.getInstitutionTagline());
        model.addAttribute("academicYear", reportService.getAcademicYear());
        return "invigilator/attendance";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Invigilator invigilator = getAuthenticatedInvigilator(userDetails);
        model.addAttribute("invigilator", invigilator);
        model.addAttribute("username", userDetails.getUsername());
        return "invigilator/profile";
    }

    private Invigilator getAuthenticatedInvigilator(UserDetails userDetails) {
        return invigilatorService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Invigilator profile not found for user: " + userDetails.getUsername()));
    }
}
