package com.example.examseating.controller;

import com.example.examseating.dto.DepartmentDto;
import com.example.examseating.service.DepartmentService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/departments")
public class AdminDepartmentController {

    private final DepartmentService departmentService;

    public AdminDepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    public String list(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        List<DepartmentDto> departments = departmentService.search(keyword);
        model.addAttribute("departments", departments);
        model.addAttribute("keyword", keyword);
        return "admin/departments/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("department", new DepartmentDto());
        model.addAttribute("isEdit", false);
        return "admin/departments/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("department") DepartmentDto dto,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", dto.getId() != null);
            return "admin/departments/form";
        }

        try {
            if (dto.getId() == null) {
                departmentService.create(dto);
                redirectAttributes.addFlashAttribute("successMessage", "Department created successfully!");
            } else {
                departmentService.update(dto.getId(), dto);
                redirectAttributes.addFlashAttribute("successMessage", "Department updated successfully!");
            }
        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("isEdit", dto.getId() != null);
            return "admin/departments/form";
        }

        return "redirect:/admin/departments";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        DepartmentDto dto = departmentService.findById(id);
        model.addAttribute("department", dto);
        model.addAttribute("isEdit", true);
        return "admin/departments/form";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            departmentService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Department deleted successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/departments";
    }
}
