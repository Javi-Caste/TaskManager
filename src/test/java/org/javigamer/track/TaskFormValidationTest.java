package org.javigamer.track;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.javigamer.task.TaskForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class TaskFormValidationTest {

    private Validator validator;

    @BeforeEach
    void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validFormHasNoViolations() {
        TaskForm form = new TaskForm("Task", "Description");

        assertThat(validator.validate(form)).isEmpty();
    }

    @Test
    void nameIsRequired() {
        TaskForm form = new TaskForm("", "Description");

        assertThat(propertyNames(validator.validate(form))).contains("name");
    }

    @Test
    void descriptionCannotExceedLimit() {
        TaskForm form = new TaskForm("Task", "x".repeat(501));

        assertThat(propertyNames(validator.validate(form))).contains("description");
    }

    private Set<String> propertyNames(Set<ConstraintViolation<TaskForm>> violations) {
        return violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
    }
}
