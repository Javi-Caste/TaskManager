package org.javigamer.task;

public class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException(Long id) {
        super("No se encontro la tarea con id " + id);
    }
}
