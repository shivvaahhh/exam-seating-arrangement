package com.example.examseating.controller;

import com.example.examseating.dto.ExaminationDto;
import com.example.examseating.entity.Examination;
import com.example.examseating.service.DepartmentService;
import com.example.examseating.service.ExaminationService;
import com.example.examseating.service.SubjectService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/examinations")
public class AdminExaminationController {

    private final ExaminationService examinationService;
    private final DepartmentService departmentService;
    private final SubjectService subjectService;

    public AdminExaminationController(ExaminationService examinationService,
                                      DepartmentService departmentService,
                                      SubjectService subjectService) {
        this.examinationService = examinationService;
        this.departmentService = departmentService;
        this.subjectService = subjectService;
    }

    @GetMapping
    public String list(@RequestParam(value = "deptId", required = false) Long deptId,
                       @RequestParam(value = "semester", required = false) Integer semester,
                       @RequestParam(value = "status", required = false) String status,
                       Model model) {
        List<ExaminationDto> exams = examinationService.findWithFilters(deptId, semester, status);
        model.addAttribute("examinations", exams);
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("selectedDeptId", deptId);
        model.addAttribute("selectedSemester", semester);
        model.addAttribute("selectedStatus", status);
        return "admin/examinations/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        ExaminationDto dto = new ExaminationDto();
        dto.setAcademicYear("2026-2027");
        dto.setSession("FORENOON");
        model.addAttribute("examination", dto);
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("subjects", subjectService.findAll());
        model.addAttribute("isEdit", false);
        return "admin/examinations/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("examination") ExaminationDto dto,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (!dto.isTimeValid()) {
            bindingResult.rejectValue("endTime", "error.time", "Start time must be strictly before end time.");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("departments", departmentService.findAll());
            model.addAttribute("subjects", subjectService.findAll());
            model.addAttribute("isEdit", dto.getId() != null);
            return "admin/examinations/form";
        }

        try {
            if (dto.getId() == null) {
                examinationService.create(dto);
                redirectAttributes.addFlashAttribute("successMessage", "Examination scheduled successfully!");
            } else {
                examinationService.update(dto.getId(), dto);
                redirectAttributes.addFlashAttribute("successMessage", "Examination updated successfully!");
            }
        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("departments", departmentService.findAll());
            model.addAttribute("subjects", subjectService.findAll());
            model.addAttribute("isEdit", dto.getId() != null);
            return "admin/examinations/form";
        }

        return "redirect:/admin/examinations";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        ExaminationDto dto = examinationService.findById(id);
        model.addAttribute("examination", dto);
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("subjects", subjectService.findAll());
        model.addAttribute("isEdit", true);
        return "admin/examinations/form";
    }

    @GetMapping("/view/{id}")
    public String viewDetails(@PathVariable Long id, Model model) {
        ExaminationDto dto = examinationService.findById(id);
        model.addAttribute("examination", dto);
        return "admin/examinations/view";
    }

    @PostMapping("/cancel/{id}")
    public String cancel(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            examinationService.cancel(id);
            redirectAttributes.addFlashAttribute("successMessage", "Examination marked as CANCELLED.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/examinations/view/" + id;
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            examinationService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Examination deleted successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/examinations";
    }
}
