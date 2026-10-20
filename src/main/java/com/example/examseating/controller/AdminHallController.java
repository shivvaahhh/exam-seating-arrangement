package com.example.examseating.controller;

import com.example.examseating.dto.ExaminationHallDto;
import com.example.examseating.service.ExaminationHallService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/halls")
public class AdminHallController {

    private final ExaminationHallService hallService;

    public AdminHallController(ExaminationHallService hallService) {
        this.hallService = hallService;
    }

    @GetMapping
    public String list(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        List<ExaminationHallDto> halls = hallService.search(keyword);
        model.addAttribute("halls", halls);
        model.addAttribute("keyword", keyword);
        return "admin/halls/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("hall", new ExaminationHallDto());
        model.addAttribute("isEdit", false);
        return "admin/halls/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("hall") ExaminationHallDto dto,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", dto.getId() != null);
            return "admin/halls/form";
        }

        try {
            if (dto.getId() == null) {
                hallService.create(dto);
                redirectAttributes.addFlashAttribute("successMessage", "Examination hall created successfully!");
            } else {
                hallService.update(dto.getId(), dto);
                redirectAttributes.addFlashAttribute("successMessage", "Examination hall updated successfully!");
            }
        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("isEdit", dto.getId() != null);
            return "admin/halls/form";
        }

        return "redirect:/admin/halls";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        ExaminationHallDto dto = hallService.findById(id);
        model.addAttribute("hall", dto);
        model.addAttribute("isEdit", true);
        return "admin/halls/form";
    }

    @GetMapping("/view/{id}")
    public String viewDetails(@PathVariable Long id, Model model) {
        ExaminationHallDto dto = hallService.findById(id);
        model.addAttribute("hall", dto);
        return "admin/halls/view";
    }

    @PostMapping("/toggle-status/{id}")
    public String toggleStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            hallService.toggleStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Hall status updated.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/halls";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            hallService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Examination hall deleted successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/halls";
    }
}
