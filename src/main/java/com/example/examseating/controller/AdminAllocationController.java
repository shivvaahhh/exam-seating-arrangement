package com.example.examseating.controller;

import com.example.examseating.dto.AllocationRequestDto;
import com.example.examseating.dto.AllocationResultDto;
import com.example.examseating.dto.ExaminationDto;
import com.example.examseating.dto.HallSeatGridDto;
import com.example.examseating.entity.ExaminationHall;
import com.example.examseating.entity.SeatAllocation;
import com.example.examseating.exception.AllocationException;
import com.example.examseating.exception.InsufficientCapacityException;
import com.example.examseating.service.ExaminationHallService;
import com.example.examseating.service.ExaminationService;
import com.example.examseating.service.SeatAllocationService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/allocations")
public class AdminAllocationController {

    private final SeatAllocationService allocationService;
    private final ExaminationService examinationService;
    private final ExaminationHallService hallService;

    public AdminAllocationController(SeatAllocationService allocationService,
                                     ExaminationService examinationService,
                                     ExaminationHallService hallService) {
        this.allocationService = allocationService;
        this.examinationService = examinationService;
        this.hallService = hallService;
    }

    @GetMapping
    public String overview(Model model) {
        List<ExaminationDto> scheduledExams = examinationService.findAll();
        model.addAttribute("examinations", scheduledExams);
        return "admin/allocations/index";
    }

    @GetMapping("/generate")
    public String showGenerateForm(@RequestParam(value = "examId", required = false) Long examId, Model model) {
        AllocationRequestDto requestDto = new AllocationRequestDto();
        if (examId != null) {
            requestDto.setExaminationId(examId);
        }

        List<ExaminationDto> eligibleExams = examinationService.findAll().stream()
                .filter(e -> !"CANCELLED".equalsIgnoreCase(e.getStatus()))
                .collect(Collectors.toList());

        List<ExaminationHall> activeHalls = hallService.findActiveHalls();

        model.addAttribute("allocationRequest", requestDto);
        model.addAttribute("examinations", eligibleExams);
        model.addAttribute("activeHalls", activeHalls);
        model.addAttribute("selectedExamId", examId);
        return "admin/allocations/generate";
    }

    @PostMapping("/generate")
    public String executeAllocation(@Valid @ModelAttribute("allocationRequest") AllocationRequestDto requestDto,
                                    BindingResult bindingResult,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("examinations", examinationService.findAll());
            model.addAttribute("activeHalls", hallService.findActiveHalls());
            return "admin/allocations/generate";
        }

        try {
            AllocationResultDto result = allocationService.allocateSeats(requestDto);
            redirectAttributes.addFlashAttribute("successMessage", result.getMessage());
            redirectAttributes.addFlashAttribute("allocationResult", result);
            return "redirect:/admin/allocations/results/" + requestDto.getExaminationId();
        } catch (InsufficientCapacityException ex) {
            model.addAttribute("capacityErrorMessage", ex.getMessage());
            model.addAttribute("examinations", examinationService.findAll());
            model.addAttribute("activeHalls", hallService.findActiveHalls());
            return "admin/allocations/generate";
        } catch (AllocationException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("examinations", examinationService.findAll());
            model.addAttribute("activeHalls", hallService.findActiveHalls());
            model.addAttribute("canRegenerate", ex.getMessage().contains("regenerate"));
            return "admin/allocations/generate";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Unexpected error: " + ex.getMessage());
            model.addAttribute("examinations", examinationService.findAll());
            model.addAttribute("activeHalls", hallService.findActiveHalls());
            return "admin/allocations/generate";
        }
    }

    @GetMapping("/results/{examId}")
    public String viewResults(@PathVariable Long examId,
                              @RequestParam(value = "hallId", required = false) Long hallId,
                              @RequestParam(value = "deptCode", required = false) String deptCode,
                              Model model) {
        ExaminationDto exam = examinationService.findById(examId);
        List<SeatAllocation> allocations = allocationService.getAllocationsForExam(examId);

        // Filter if requested
        if (hallId != null) {
            allocations = allocations.stream()
                    .filter(a -> a.getExaminationHall().getId().equals(hallId))
                    .collect(Collectors.toList());
        }
        if (deptCode != null && !deptCode.trim().isEmpty() && !"ALL".equalsIgnoreCase(deptCode)) {
            allocations = allocations.stream()
                    .filter(a -> a.getStudent().getDepartment().getDepartmentCode().equalsIgnoreCase(deptCode))
                    .collect(Collectors.toList());
        }

        List<ExaminationHall> usedHalls = allocationService.getAllocationsForExam(examId).stream()
                .map(SeatAllocation::getExaminationHall)
                .distinct()
                .collect(Collectors.toList());

        model.addAttribute("examination", exam);
        model.addAttribute("allocations", allocations);
        model.addAttribute("usedHalls", usedHalls);
        model.addAttribute("selectedHallId", hallId);
        model.addAttribute("selectedDeptCode", deptCode);
        return "admin/allocations/results";
    }

    @GetMapping("/grid/{examId}/{hallId}")
    public String viewHallGrid(@PathVariable Long examId, @PathVariable Long hallId, Model model) {
        HallSeatGridDto grid = allocationService.getHallSeatGrid(examId, hallId);
        model.addAttribute("grid", grid);
        return "admin/allocations/seating-chart";
    }

    @PostMapping("/regenerate/{examId}")
    public String regenerate(@PathVariable Long examId,
                             @RequestParam("hallIds") List<Long> hallIds,
                             @RequestParam(value = "sortStrategy", defaultValue = "DEPT_REGNO") String sortStrategy,
                             RedirectAttributes redirectAttributes) {
        try {
            AllocationRequestDto request = new AllocationRequestDto();
            request.setExaminationId(examId);
            request.setHallIds(hallIds);
            request.setSortStrategy(sortStrategy);
            request.setForceRegenerate(true);

            AllocationResultDto result = allocationService.allocateSeats(request);
            redirectAttributes.addFlashAttribute("successMessage", "Allocations regenerated successfully! " + result.getMessage());
            return "redirect:/admin/allocations/results/" + examId;
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/allocations/results/" + examId;
        }
    }

    @PostMapping("/clear/{examId}")
    public String clearAllocations(@PathVariable Long examId, RedirectAttributes redirectAttributes) {
        try {
            allocationService.clearAllocations(examId);
            redirectAttributes.addFlashAttribute("successMessage", "All seat allocations cleared for this examination.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/allocations/results/" + examId;
    }

    @PostMapping("/attendance/{allocationId}")
    public String updateAttendance(@PathVariable Long allocationId,
                                   @RequestParam("status") String status,
                                   @RequestParam("examId") Long examId,
                                   RedirectAttributes redirectAttributes) {
        try {
            allocationService.updateAttendance(allocationId, status);
            redirectAttributes.addFlashAttribute("successMessage", "Attendance updated.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/allocations/results/" + examId;
    }
}
