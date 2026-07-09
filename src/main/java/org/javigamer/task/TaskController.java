package org.javigamer.task;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.javigamer.user.CurrentUserResolver;


@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;
    private final CurrentUserResolver currentUserResolver;

    public TaskController(TaskService taskService, CurrentUserResolver currentUserResolver) {
        this.taskService = taskService;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping("/{id}")
    public Task getTask(@PathVariable Long id, Authentication authentication) {
        return taskService.getTask(id, currentUserResolver.resolve(authentication));
    }
    
    @GetMapping
    public List<Task> getAllTasks(Authentication authentication) {
        return taskService.getAllTasks(currentUserResolver.resolve(authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Task createTask(@Valid @RequestBody TaskForm form, Authentication authentication) {
        return taskService.createTask(currentUserResolver.resolve(authentication), form);
    }

    @PutMapping("/{id}")
    public Task updateTask(@PathVariable Long id, @Valid @RequestBody TaskForm form, Authentication authentication) {
        return taskService.updateTask(id, currentUserResolver.resolve(authentication), form);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id, Authentication authentication) {
        taskService.deleteTask(id, currentUserResolver.resolve(authentication));
        return ResponseEntity.noContent().build();
    }

}
