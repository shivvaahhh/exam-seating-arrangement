package com.example.examseating.controller;

import com.example.examseating.dto.DepartmentDto;
import com.example.examseating.dto.ExaminationDto;
import com.example.examseating.dto.InvigilatorDto;
import com.example.examseating.entity.ExaminationHall;
import com.example.examseating.entity.InvigilatorAssignment;
import com.example.examseating.service.DepartmentService;
import com.example.examseating.service.ExaminationHallService;
import com.example.examseating.service.ExaminationService;
import com.example.examseating.service.InvigilatorService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/invigilators")
public class AdminInvigilatorController {

    private final InvigilatorService invigilatorService;
    private final DepartmentService departmentService;
    private final ExaminationService examinationService;
    private final ExaminationHallService hallService;

    public AdminInvigilatorController(InvigilatorService invigilatorService,
                                      DepartmentService departmentService,
                                      ExaminationService examinationService,
                                      ExaminationHallService hallService) {
        this.invigilatorService = invigilatorService;
        this.departmentService = departmentService;
        this.examinationService = examinationService;
        this.hallService = hallService;
    }

    @GetMapping
    public String list(@RequestParam(value = "deptId", required = false) Long deptId,
                       @RequestParam(value = "keyword", required = false) String keyword,
                       Model model) {
        List<InvigilatorDto> invigilators = invigilatorService.findWithFilters(deptId, keyword);
        List<DepartmentDto> departments = departmentService.findAll();

        model.addAttribute("invigilators", invigilators);
        model.addAttribute("departments", departments);
        model.addAttribute("selectedDeptId", deptId);
        model.addAttribute("keyword", keyword);
        return "admin/invigilators/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("invigilator", new InvigilatorDto());
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("isNew", true);
        return "admin/invigilators/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("invigilator") InvigilatorDto dto,
                         BindingResult result,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (result.hasErrors()) {
            model.addAttribute("departments", departmentService.findAll());
            model.addAttribute("isNew", true);
            return "admin/invigilators/form";
        }

        try {
            InvigilatorDto created = invigilatorService.create(dto);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Invigilator '" + created.getName() + "' created successfully! Login Username: '" +
                    created.getEmail() + "', Initial Temporary Password: '" + created.getGeneratedPassword() +
                    "' (Must be changed on first login)."
            );
            return "redirect:/admin/invigilators";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("departments", departmentService.findAll());
            model.addAttribute("isNew", true);
            return "admin/invigilators/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        InvigilatorDto dto = invigilatorService.findById(id);
        model.addAttribute("invigilator", dto);
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("isNew", false);
        return "admin/invigilators/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("invigilator") InvigilatorDto dto,
                         BindingResult result,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (result.hasErrors()) {
            model.addAttribute("departments", departmentService.findAll());
            model.addAttribute("isNew", false);
            return "admin/invigilators/form";
        }

        try {
            invigilatorService.update(id, dto);
            redirectAttributes.addFlashAttribute("successMessage", "Invigilator updated successfully!");
            return "redirect:/admin/invigilators";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("departments", departmentService.findAll());
            model.addAttribute("isNew", false);
            return "admin/invigilators/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            invigilatorService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Invigilator deleted successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/invigilators";
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            String newPassword = invigilatorService.resetPassword(id);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Password reset successfully! New temporary password: '" + newPassword +
                    "' (Invigilator will be prompted to change it upon login)."
            );
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/invigilators";
    }

    @PostMapping("/{id}/toggle-active")
    public String toggleActive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            boolean active = invigilatorService.toggleActive(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Invigilator account " + (active ? "activated" : "deactivated") + " successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/invigilators";
    }

    // ========== Examination Duty Assignments ==========

    @GetMapping("/assign")
    public String assignDutiesPage(@RequestParam(value = "examId", required = false) Long examId,
                                   Model model) {
        List<ExaminationDto> exams = examinationService.findAll();
        model.addAttribute("examinations", exams);

        if (examId != null) {
            ExaminationDto exam = examinationService.findById(examId);
            List<InvigilatorAssignment> assignments = invigilatorService.getAssignmentsForExam(examId);
            List<ExaminationHall> activeHalls = hallService.findActiveHalls();
            List<InvigilatorDto> activeInvigilators = invigilatorService.findAll().stream()
                    .filter(i -> "ACTIVE".equalsIgnoreCase(i.getStatus()))
                    .toList();

            model.addAttribute("selectedExam", exam);
            model.addAttribute("assignments", assignments);
            model.addAttribute("halls", activeHalls);
            model.addAttribute("invigilators", activeInvigilators);
        }

        model.addAttribute("selectedExamId", examId);
        return "admin/invigilators/assign";
    }

    @PostMapping("/assign")
    public String submitAssignment(@RequestParam("examinationId") Long examId,
                                   @RequestParam("hallId") Long hallId,
                                   @RequestParam("invigilatorId") Long invigilatorId,
                                   RedirectAttributes redirectAttributes) {
        try {
            invigilatorService.assignInvigilatorToHall(examId, hallId, invigilatorId);
            redirectAttributes.addFlashAttribute("successMessage", "Invigilator assigned to hall successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/invigilators/assign?examId=" + examId;
    }

    @PostMapping("/assign/{assignmentId}/delete")
    public String removeAssignment(@PathVariable Long assignmentId,
                                   @RequestParam("examId") Long examId,
                                   RedirectAttributes redirectAttributes) {
        try {
            invigilatorService.removeAssignment(assignmentId);
            redirectAttributes.addFlashAttribute("successMessage", "Invigilator duty unassigned successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/invigilators/assign?examId=" + examId;
    }
}
