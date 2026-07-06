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


@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;
    private final TaskOwnerResolver ownerResolver;

    public TaskController(TaskService taskService, TaskOwnerResolver ownerResolver) {
        this.taskService = taskService;
        this.ownerResolver = ownerResolver;
    }

    @GetMapping("/{id}")
    public Task getTask(@PathVariable Long id, Authentication authentication) {
        return taskService.getTask(id, ownerResolver.resolve(authentication));
    }
    
    @GetMapping
    public List<Task> getAllTasks(Authentication authentication) {
        return taskService.getAllTasks(ownerResolver.resolve(authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Task createTask(@Valid @RequestBody TaskForm form, Authentication authentication) {
        return taskService.createTask(ownerResolver.resolve(authentication), form);
    }

    @PutMapping("/{id}")
    public Task updateTask(@PathVariable Long id, @Valid @RequestBody TaskForm form, Authentication authentication) {
        return taskService.updateTask(id, ownerResolver.resolve(authentication), form);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id, Authentication authentication) {
        taskService.deleteTask(id, ownerResolver.resolve(authentication));
        return ResponseEntity.noContent().build();
    }

}
