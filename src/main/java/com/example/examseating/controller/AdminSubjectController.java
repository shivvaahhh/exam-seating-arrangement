package com.example.examseating.controller;

import com.example.examseating.dto.SubjectDto;
import com.example.examseating.service.DepartmentService;
import com.example.examseating.service.SubjectService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/subjects")
public class AdminSubjectController {

    private final SubjectService subjectService;
    private final DepartmentService departmentService;

    public AdminSubjectController(SubjectService subjectService, DepartmentService departmentService) {
        this.subjectService = subjectService;
        this.departmentService = departmentService;
    }

    @GetMapping
    public String list(@RequestParam(value = "deptId", required = false) Long deptId,
                       @RequestParam(value = "semester", required = false) Integer semester,
                       @RequestParam(value = "keyword", required = false) String keyword,
                       Model model) {
        List<SubjectDto> subjects;
        if (keyword != null && !keyword.trim().isEmpty()) {
            subjects = subjectService.search(keyword);
        } else if (deptId != null || semester != null) {
            subjects = subjectService.findByDepartmentAndSemester(deptId, semester);
        } else {
            subjects = subjectService.findAll();
        }

        model.addAttribute("subjects", subjects);
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("selectedDeptId", deptId);
        model.addAttribute("selectedSemester", semester);
        model.addAttribute("keyword", keyword);
        return "admin/subjects/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("subject", new SubjectDto());
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("isEdit", false);
        return "admin/subjects/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("subject") SubjectDto dto,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("departments", departmentService.findAll());
            model.addAttribute("isEdit", dto.getId() != null);
            return "admin/subjects/form";
        }

        try {
            if (dto.getId() == null) {
                subjectService.create(dto);
                redirectAttributes.addFlashAttribute("successMessage", "Subject created successfully!");
            } else {
                subjectService.update(dto.getId(), dto);
                redirectAttributes.addFlashAttribute("successMessage", "Subject updated successfully!");
            }
        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("departments", departmentService.findAll());
            model.addAttribute("isEdit", dto.getId() != null);
            return "admin/subjects/form";
        }

        return "redirect:/admin/subjects";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        SubjectDto dto = subjectService.findById(id);
        model.addAttribute("subject", dto);
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("isEdit", true);
        return "admin/subjects/form";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            subjectService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Subject deleted successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/subjects";
    }
}
