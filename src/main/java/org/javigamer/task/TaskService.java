package org.javigamer.task;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.javigamer.user.CurrentUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class TaskService {

    private static final String DEFAULT_OWNER = "guest";

    private final TaskRepository taskRepository;
    private final Clock clock;

    public TaskService(TaskRepository taskRepository, Clock clock) {
        this.taskRepository = taskRepository;
        this.clock = clock;
    }

    public Task createTask(CurrentUser currentUser, TaskForm form) {
        if (currentUser.isAdmin()) {
            throw new AccessDeniedException("ADMIN no puede crear tareas");
        }

        Task task = new Task();
        task.setOwner(normalizeOwner(currentUser.username()));
        task.setStartedAt(LocalDateTime.now(clock));
        applyForm(task, form);
        return taskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public Task getTask(Long id, CurrentUser currentUser) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        authorizeTaskAccess(task, currentUser);
        return task;
    }

    @Transactional(readOnly = true)
    public List<Task> getAllTasks(CurrentUser currentUser) {
        if (currentUser.isAdmin()) {
            return taskRepository.findAllByFinishedAtIsNullOrderByStartedAtAscIdAsc();
        }

        return taskRepository.findAllByOwnerAndFinishedAtIsNullOrderByStartedAtAscIdAsc(
                normalizeOwner(currentUser.username()));
    }

    public Task updateTask(Long id, CurrentUser currentUser, TaskForm form) {
        Task existingTask = getActiveTask(id);
        authorizeTaskAccess(existingTask, currentUser);
        applyForm(existingTask, form);
        return taskRepository.save(existingTask);
    }

    public void deleteTask(Long id, CurrentUser currentUser) {
        if (currentUser.isAdmin()) {
            throw new AccessDeniedException("ADMIN no puede finalizar tareas");
        }

        Task task = getActiveTask(id);
        authorizeTaskAccess(task, currentUser);
        task.setFinishedAt(LocalDateTime.now(clock));
        taskRepository.save(task);
    }

    private void applyForm(Task task, TaskForm form) {
        task.setName(form.getName().strip());
        task.setDescription(StringUtils.hasText(form.getDescription()) ? form.getDescription().strip() : null);
    }

    private Task getActiveTask(Long id) {
        return taskRepository.findByIdAndFinishedAtIsNull(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }

    private void authorizeTaskAccess(Task task, CurrentUser currentUser) {
        if (currentUser.isAdmin()) {
            return;
        }

        if (!normalizeOwner(task.getOwner()).equals(normalizeOwner(currentUser.username()))) {
            throw new AccessDeniedException("No tienes permiso para acceder a esta tarea");
        }
    }

    private String normalizeOwner(String owner) {
        return StringUtils.hasText(owner) ? owner.strip() : DEFAULT_OWNER;
    }
}
