document.addEventListener("DOMContentLoaded", () => {
    const filterInput = document.querySelector("[data-task-filter]");
    const taskCards = Array.from(document.querySelectorAll("[data-task-card]"));

    if (filterInput) {
        filterInput.addEventListener("input", () => {
            const query = filterInput.value.trim().toLowerCase();

            taskCards.forEach((card) => {
                const text = (card.dataset.taskCard || "").toLowerCase();
                card.hidden = query.length > 0 && !text.includes(query);
            });
        });
    }

    document.querySelectorAll("[data-confirm-delete]").forEach((form) => {
        form.addEventListener("submit", (event) => {
            if (!window.confirm("Finalizar esta tarea?")) {
                event.preventDefault();
            }
        });
    });
});
