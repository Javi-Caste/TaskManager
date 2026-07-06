package org.javigamer.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TaskForm {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    private String name;

    @Size(max = 500, message = "La descripcion no puede superar 500 caracteres")
    private String description;

    public TaskForm() {
    }

    public TaskForm(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public static TaskForm from(Task task) {
        return new TaskForm(task.getName(), task.getDescription());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
