package com.example.examseating.controller;

import com.example.examseating.entity.ExaminationRegistration;
import com.example.examseating.entity.SeatAllocation;
import com.example.examseating.entity.Student;
import com.example.examseating.exception.ResourceNotFoundException;
import com.example.examseating.repository.ExaminationRegistrationRepository;
import com.example.examseating.service.ReportService;
import com.example.examseating.service.SeatAllocationService;
import com.example.examseating.service.StudentService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/student")
public class StudentPortalController {

    private final StudentService studentService;
    private final SeatAllocationService allocationService;
    private final ExaminationRegistrationRepository registrationRepository;
    private final ReportService reportService;

    public StudentPortalController(StudentService studentService,
                                   SeatAllocationService allocationService,
                                   ExaminationRegistrationRepository registrationRepository,
                                   ReportService reportService) {
        this.studentService = studentService;
        this.allocationService = allocationService;
        this.registrationRepository = registrationRepository;
        this.reportService = reportService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Student student = getAuthenticatedStudent(userDetails);
        List<SeatAllocation> allocations = allocationService.getAllocationsForStudent(student.getId());
        List<ExaminationRegistration> registrations = registrationRepository.findByStudentId(student.getId());

        model.addAttribute("student", student);
        model.addAttribute("allocations", allocations);
        model.addAttribute("registeredExamsCount", registrations.size());
        model.addAttribute("allocatedExamsCount", allocations.size());
        return "student/dashboard";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Student student = getAuthenticatedStudent(userDetails);
        model.addAttribute("student", student);
        return "student/profile";
    }

    @GetMapping("/find-seat")
    public String findSeat(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Student student = getAuthenticatedStudent(userDetails);
        List<SeatAllocation> allAllocations = allocationService.getAllocationsForStudent(student.getId());

        // Upcoming and past allocations
        LocalDate today = LocalDate.now();
        List<SeatAllocation> upcomingAllocations = allAllocations.stream()
                .filter(a -> !a.getExamination().getExaminationDate().isBefore(today))
                .collect(Collectors.toList());

        List<ExaminationRegistration> registrations = registrationRepository.findByStudentId(student.getId());
        List<ExaminationRegistration> pendingRegistrations = registrations.stream()
                .filter(r -> !r.isAllocated())
                .collect(Collectors.toList());

        model.addAttribute("student", student);
        model.addAttribute("upcomingAllocations", upcomingAllocations);
        model.addAttribute("allAllocations", allAllocations);
        model.addAttribute("pendingRegistrations", pendingRegistrations);
        model.addAttribute("institutionName", reportService.getInstitutionName());
        return "student/find-seat";
    }

    @GetMapping("/exam/{examId}/seat")
    public String viewExamSeat(@AuthenticationPrincipal UserDetails userDetails,
                               @PathVariable Long examId,
                               Model model) {
        Student student = getAuthenticatedStudent(userDetails);

        SeatAllocation allocation = allocationService.getAllocationsForStudent(student.getId()).stream()
                .filter(a -> a.getExamination().getId().equals(examId))
                .findFirst()
                .orElse(null);

        model.addAttribute("student", student);
        model.addAttribute("allocation", allocation);
        model.addAttribute("institutionName", reportService.getInstitutionName());
        return "student/seat-details";
    }

    private Student getAuthenticatedStudent(UserDetails userDetails) {
        return studentService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found for user: " + userDetails.getUsername()));
    }
}
