package org.javigamer.task;

import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class TaskPageController {

    private final TaskService taskService;
    private final TaskOwnerResolver ownerResolver;

    public TaskPageController(TaskService taskService, TaskOwnerResolver ownerResolver) {
        this.taskService = taskService;
        this.ownerResolver = ownerResolver;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/app/tasks";
    }

    @GetMapping("/app/tasks")
    public String listTasks(Model model, Authentication authentication) {
        model.addAttribute("tasks", taskService.getAllTasks(ownerResolver.resolve(authentication)));
        return "tasks/list";
    }

    @GetMapping("/app/tasks/new")
    public String newTask(Model model) {
        model.addAttribute("taskForm", new TaskForm());
        model.addAttribute("formAction", "/app/tasks");
        model.addAttribute("pageTitle", "Nueva tarea");
        return "tasks/form";
    }

    @PostMapping("/app/tasks")
    public String createTask(
            @Valid @ModelAttribute("taskForm") TaskForm form,
            BindingResult bindingResult,
            Model model,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, "/app/tasks", "Nueva tarea");
            return "tasks/form";
        }

        Task task = taskService.createTask(ownerResolver.resolve(authentication), form);
        redirectAttributes.addFlashAttribute("successMessage", "Tarea creada correctamente");
        return "redirect:/app/tasks/" + task.getId();
    }

    @GetMapping("/app/tasks/{id}")
    public String taskDetails(@PathVariable Long id, Model model, Authentication authentication) {
        model.addAttribute("task", taskService.getTask(id, ownerResolver.resolve(authentication)));
        return "tasks/detail";
    }

    @GetMapping("/app/tasks/{id}/edit")
    public String editTask(@PathVariable Long id, Model model, Authentication authentication) {
        Task task = taskService.getTask(id, ownerResolver.resolve(authentication));
        model.addAttribute("task", task);
        model.addAttribute("taskForm", TaskForm.from(task));
        prepareForm(model, "/app/tasks/" + id, "Editar tarea");
        return "tasks/form";
    }

    @PostMapping("/app/tasks/{id}")
    public String updateTask(
            @PathVariable Long id,
            @Valid @ModelAttribute("taskForm") TaskForm form,
            BindingResult bindingResult,
            Model model,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            Task task = taskService.getTask(id, ownerResolver.resolve(authentication));
            model.addAttribute("task", task);
            prepareForm(model, "/app/tasks/" + id, "Editar tarea");
            return "tasks/form";
        }

        taskService.updateTask(id, ownerResolver.resolve(authentication), form);
        redirectAttributes.addFlashAttribute("successMessage", "Tarea actualizada correctamente");
        return "redirect:/app/tasks/" + id;
    }

    @PostMapping("/app/tasks/{id}/delete")
    public String deleteTask(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        taskService.deleteTask(id, ownerResolver.resolve(authentication));
        redirectAttributes.addFlashAttribute("successMessage", "Tarea eliminada correctamente");
        return "redirect:/app/tasks";
    }

    @ExceptionHandler(TaskNotFoundException.class)
    public String handleNotFound(TaskNotFoundException exception, Model model) {
        model.addAttribute("errorMessage", exception.getMessage());
        return "tasks/not-found";
    }

    private void prepareForm(Model model, String formAction, String pageTitle) {
        model.addAttribute("formAction", formAction);
        model.addAttribute("pageTitle", pageTitle);
    }
}
