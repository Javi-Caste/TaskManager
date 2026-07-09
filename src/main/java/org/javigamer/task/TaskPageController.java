package org.javigamer.task;

import jakarta.validation.Valid;

import org.javigamer.user.CurrentUser;
import org.javigamer.user.CurrentUserResolver;
import org.javigamer.user.UserAccountService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class TaskPageController {

    private final TaskService taskService;
    private final CurrentUserResolver currentUserResolver;
    private final UserAccountService userAccountService;

    public TaskPageController(
            TaskService taskService,
            CurrentUserResolver currentUserResolver,
            UserAccountService userAccountService) {
        this.taskService = taskService;
        this.currentUserResolver = currentUserResolver;
        this.userAccountService = userAccountService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/app/tasks";
    }

    @GetMapping("/app/tasks")
    public String listTasks(
            @RequestParam(required = false) String userQuery,
            @RequestParam(required = false) Long taskId,
            Model model,
            Authentication authentication) {
        CurrentUser currentUser = currentUserResolver.resolve(authentication);
        prepareRoleModel(model, currentUser);
        model.addAttribute("tasks", taskService.getAllTasks(currentUser));

        if (currentUser.isAdmin()) {
            model.addAttribute("userSearchQuery", userQuery);
            model.addAttribute("userResults", userAccountService.searchUsers(currentUser, userQuery));
            model.addAttribute("taskSearchId", taskId);
            if (taskId != null) {
                addTaskSearchResult(model, currentUser, taskId);
            }
        }

        return "tasks/list";
    }

    @GetMapping("/app/tasks/new")
    public String newTask(Model model, Authentication authentication) {
        CurrentUser currentUser = currentUserResolver.resolve(authentication);
        requireUserCanCreate(currentUser);
        prepareRoleModel(model, currentUser);
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
        CurrentUser currentUser = currentUserResolver.resolve(authentication);
        requireUserCanCreate(currentUser);

        if (bindingResult.hasErrors()) {
            prepareRoleModel(model, currentUser);
            prepareForm(model, "/app/tasks", "Nueva tarea");
            return "tasks/form";
        }

        Task task = taskService.createTask(currentUser, form);
        redirectAttributes.addFlashAttribute("successMessage", "Tarea creada correctamente");
        return "redirect:/app/tasks/" + task.getId();
    }

    @GetMapping("/app/tasks/{id}")
    public String taskDetails(@PathVariable Long id, Model model, Authentication authentication) {
        CurrentUser currentUser = currentUserResolver.resolve(authentication);
        prepareRoleModel(model, currentUser);
        model.addAttribute("task", taskService.getTask(id, currentUser));
        return "tasks/detail";
    }

    @GetMapping("/app/tasks/{id}/edit")
    public String editTask(@PathVariable Long id, Model model, Authentication authentication) {
        CurrentUser currentUser = currentUserResolver.resolve(authentication);
        Task task = taskService.getTask(id, currentUser);
        prepareRoleModel(model, currentUser);
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
        CurrentUser currentUser = currentUserResolver.resolve(authentication);

        if (bindingResult.hasErrors()) {
            Task task = taskService.getTask(id, currentUser);
            prepareRoleModel(model, currentUser);
            model.addAttribute("task", task);
            prepareForm(model, "/app/tasks/" + id, "Editar tarea");
            return "tasks/form";
        }

        taskService.updateTask(id, currentUser, form);
        redirectAttributes.addFlashAttribute("successMessage", "Tarea actualizada correctamente");
        return "redirect:/app/tasks/" + id;
    }

    @PostMapping("/app/tasks/{id}/delete")
    public String deleteTask(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        taskService.deleteTask(id, currentUserResolver.resolve(authentication));
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

    private void prepareRoleModel(Model model, CurrentUser currentUser) {
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isAdmin", currentUser.isAdmin());
        model.addAttribute("canCreateTasks", currentUser.isUser());
    }

    private void addTaskSearchResult(Model model, CurrentUser currentUser, Long taskId) {
        try {
            model.addAttribute("taskSearchResult", taskService.getTask(taskId, currentUser));
        } catch (TaskNotFoundException exception) {
            model.addAttribute("taskSearchError", exception.getMessage());
        }
    }

    private void requireUserCanCreate(CurrentUser currentUser) {
        if (!currentUser.isUser()) {
            throw new AccessDeniedException("ADMIN no puede crear tareas");
        }
    }
}
