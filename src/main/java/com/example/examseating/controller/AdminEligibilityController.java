package com.example.examseating.controller;

import com.example.examseating.dto.ExaminationDto;
import com.example.examseating.entity.ExaminationRegistration;
import com.example.examseating.entity.Student;
import com.example.examseating.service.DepartmentService;
import com.example.examseating.service.ExaminationRegistrationService;
import com.example.examseating.service.ExaminationService;
import com.example.examseating.service.StudentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/examinations/{examId}/eligibility")
public class AdminEligibilityController {

    private final ExaminationService examinationService;
    private final ExaminationRegistrationService registrationService;
    private final DepartmentService departmentService;
    private final StudentService studentService;

    public AdminEligibilityController(ExaminationService examinationService,
                                      ExaminationRegistrationService registrationService,
                                      DepartmentService departmentService,
                                      StudentService studentService) {
        this.examinationService = examinationService;
        this.registrationService = registrationService;
        this.departmentService = departmentService;
        this.studentService = studentService;
    }

    @GetMapping
    public String manageEligibility(@PathVariable Long examId, Model model) {
        ExaminationDto exam = examinationService.findById(examId);
        List<ExaminationRegistration> registrations = registrationService.getRegistrationsForExam(examId);
        List<Student> allStudents = studentService.findAllEntities();

        model.addAttribute("examination", exam);
        model.addAttribute("registrations", registrations);
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("allStudents", allStudents);
        return "admin/eligibility/manage";
    }

    @PostMapping("/add-by-dept")
    public String addStudentsByDeptAndSem(@PathVariable Long examId,
                                         @RequestParam(value = "deptId", required = false) Long deptId,
                                         @RequestParam(value = "semester", required = false) Integer semester,
                                         RedirectAttributes redirectAttributes) {
        try {
            int added = registrationService.registerStudentsByDepartmentAndSemester(examId, deptId, semester);
            redirectAttributes.addFlashAttribute("successMessage", "Registered " + added + " eligible student(s) successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/examinations/" + examId + "/eligibility";
    }

    @PostMapping("/add-student")
    public String addSingleStudent(@PathVariable Long examId,
                                   @RequestParam("studentId") Long studentId,
                                   RedirectAttributes redirectAttributes) {
        try {
            registrationService.registerStudent(examId, studentId);
            redirectAttributes.addFlashAttribute("successMessage", "Student added to eligible candidates.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/examinations/" + examId + "/eligibility";
    }

    @PostMapping("/remove/{registrationId}")
    public String removeRegistration(@PathVariable Long examId,
                                     @PathVariable Long registrationId,
                                     RedirectAttributes redirectAttributes) {
        try {
            registrationService.removeRegistration(registrationId);
            redirectAttributes.addFlashAttribute("successMessage", "Student removed from eligibility list.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/examinations/" + examId + "/eligibility";
    }
}
