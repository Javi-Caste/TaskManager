package org.javigamer.task;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final Clock clock;

    public TaskService(TaskRepository taskRepository, Clock clock) {
        this.taskRepository = taskRepository;
        this.clock = clock;
    }

    public Task createTask(String owner, TaskForm form) {
        Task task = new Task();
        task.setOwner(normalizeOwner(owner));
        task.setStartedAt(LocalDateTime.now(clock));
        applyForm(task, form);
        return taskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public Task getTask(Long id, String owner) {
        return taskRepository.findByIdAndOwner(id, normalizeOwner(owner))
                .orElseThrow(() -> new TaskNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Task> getAllTasks(String owner) {
        return taskRepository.findAllByOwnerAndFinishedAtIsNullOrderByStartedAtAscIdAsc(normalizeOwner(owner));
    }

    public Task updateTask(Long id, String owner, TaskForm form) {
        Task existingTask = getActiveTask(id, owner);
        applyForm(existingTask, form);
        return taskRepository.save(existingTask);
    }

    public void deleteTask(Long id, String owner) {
        Task task = getActiveTask(id, owner);
        task.setFinishedAt(LocalDateTime.now(clock));
        taskRepository.save(task);
    }

    private void applyForm(Task task, TaskForm form) {
        task.setName(form.getName().trim());
        task.setDescription(StringUtils.hasText(form.getDescription()) ? form.getDescription().trim() : null);
    }

    private Task getActiveTask(Long id, String owner) {
        return taskRepository.findByIdAndOwnerAndFinishedAtIsNull(id, normalizeOwner(owner))
                .orElseThrow(() -> new TaskNotFoundException(id));
    }

    private String normalizeOwner(String owner) {
        return StringUtils.hasText(owner) ? owner.trim() : TaskOwnerResolver.DEFAULT_OWNER;
    }
}
