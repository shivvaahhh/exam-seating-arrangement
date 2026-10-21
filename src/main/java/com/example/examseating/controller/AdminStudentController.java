package com.example.examseating.controller;

import com.example.examseating.dto.StudentDto;
import com.example.examseating.entity.SeatAllocation;
import com.example.examseating.service.DepartmentService;
import com.example.examseating.service.SeatAllocationService;
import com.example.examseating.service.StudentService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.PrintWriter;
import java.util.List;

@Controller
@RequestMapping("/admin/students")
public class AdminStudentController {

    private final StudentService studentService;
    private final DepartmentService departmentService;
    private final SeatAllocationService allocationService;

    public AdminStudentController(StudentService studentService,
                                  DepartmentService departmentService,
                                  SeatAllocationService allocationService) {
        this.studentService = studentService;
        this.departmentService = departmentService;
        this.allocationService = allocationService;
    }

    @GetMapping
    public String list(@RequestParam(value = "deptId", required = false) Long deptId,
                       @RequestParam(value = "semester", required = false) Integer semester,
                       @RequestParam(value = "keyword", required = false) String keyword,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       @RequestParam(value = "size", defaultValue = "15") int size,
                       Model model) {
        Page<StudentDto> studentPage = studentService.findWithFiltersPaged(
                deptId, semester, keyword,
                PageRequest.of(page, size, Sort.by("registerNumber").ascending())
        );

        model.addAttribute("studentPage", studentPage);
        model.addAttribute("students", studentPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", studentPage.getTotalPages());
        model.addAttribute("totalItems", studentPage.getTotalElements());
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("selectedDeptId", deptId);
        model.addAttribute("selectedSemester", semester);
        model.addAttribute("keyword", keyword);
        return "admin/students/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        StudentDto dto = new StudentDto();
        dto.setAcademicYear("2026-2027");
        dto.setStatus("ACTIVE");
        model.addAttribute("student", dto);
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("isEdit", false);
        return "admin/students/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("student") StudentDto dto,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("departments", departmentService.findAll());
            model.addAttribute("isEdit", dto.getId() != null);
            return "admin/students/form";
        }

        try {
            if (dto.getId() == null) {
                StudentDto created = studentService.create(dto);
                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        "Student '" + created.getFullName() + "' created! Username: '" +
                        created.getRegisterNumber().toLowerCase() + "', Initial Password: '" +
                        created.getRegisterNumber().toLowerCase() + "' (Must change on first login)."
                );
            } else {
                studentService.update(dto.getId(), dto);
                redirectAttributes.addFlashAttribute("successMessage", "Student updated successfully!");
            }
        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("departments", departmentService.findAll());
            model.addAttribute("isEdit", dto.getId() != null);
            return "admin/students/form";
        }

        return "redirect:/admin/students";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        StudentDto dto = studentService.findById(id);
        model.addAttribute("student", dto);
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("isEdit", true);
        return "admin/students/form";
    }

    @GetMapping("/view/{id}")
    public String viewDetails(@PathVariable Long id, Model model) {
        StudentDto dto = studentService.findById(id);
        List<SeatAllocation> allocations = allocationService.getAllocationsForStudent(id);
        model.addAttribute("student", dto);
        model.addAttribute("allocations", allocations);
        return "admin/students/view";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            studentService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Student record deleted successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/students";
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            String defaultPass = studentService.resetPassword(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Password reset to default (register number: '" + defaultPass + "'). Student must change it upon next login.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/students";
    }

    @PostMapping("/{id}/toggle-active")
    public String toggleActive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            boolean active = studentService.toggleActive(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Student account " + (active ? "activated" : "deactivated") + " successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/students";
    }

    @GetMapping("/import")
    public String showImportPage() {
        return "admin/students/import";
    }

    @PostMapping("/import-csv")
    public String importCsv(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a valid CSV file to upload.");
            return "redirect:/admin/students/import";
        }

        try {
            StudentService.CsvImportSummary summary = studentService.importFromCsvWithSummary(file);
            String message = "Import completed! Successfully imported " + summary.getImportedCount() + " of " +
                    summary.getTotalRows() + " rows. (Duplicates skipped: " +
                    (summary.getDuplicateRegCount() + summary.getDuplicateEmailCount()) +
                    ", Unknown Depts: " + summary.getUnknownDeptCount() +
                    ", Invalid Emails: " + summary.getInvalidEmailCount() + ").";

            redirectAttributes.addFlashAttribute("successMessage", message);
            if (!summary.getMessages().isEmpty()) {
                redirectAttributes.addFlashAttribute("warningMessage", String.join("; ", summary.getMessages()));
            }
            return "redirect:/admin/students";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "CSV Import Error: " + ex.getMessage());
            return "redirect:/admin/students/import";
        }
    }

    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) throws Exception {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"students_template.csv\"");
        try (PrintWriter writer = response.getWriter()) {
            writer.println("RegisterNumber,FullName,Email,PhoneNumber,DepartmentCode,Semester,AcademicYear");
            writer.println("2024CSE101,John Doe,john.doe@college.edu,9876543210,CSE,6,2026-2027");
            writer.println("2024IT102,Jane Smith,jane.smith@college.edu,9876543211,IT,6,2026-2027");
            writer.flush();
        }
    }
}
