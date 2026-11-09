(() => {
    const modal = document.getElementById('confirmSubmitModal');
    if (!modal) {
        return;
    }

    const title = modal.querySelector('[data-confirm-title]');
    const message = modal.querySelector('[data-confirm-message]');
    const icon = modal.querySelector('[data-confirm-icon]');
    const submitButton = modal.querySelector('[data-confirm-submit]');
    const submitIcon = modal.querySelector('[data-confirm-button-icon]');
    const submitLabel = modal.querySelector('[data-confirm-label]');

    modal.addEventListener('show.bs.modal', event => {
        const trigger = event.relatedTarget;
        if (!trigger) {
            return;
        }

        submitButton.dataset.confirmForm = trigger.getAttribute('data-confirm-form') || '';
        title.textContent = trigger.getAttribute('data-confirm-title') || 'Confirm action';
        message.textContent = trigger.getAttribute('data-confirm-message') || 'Are you sure you want to continue?';
        submitLabel.textContent = trigger.getAttribute('data-confirm-label') || 'Confirm';
        submitButton.className = 'btn ' + (trigger.getAttribute('data-confirm-button-class') || 'btn-danger');
        icon.className = trigger.getAttribute('data-confirm-icon') || 'bi bi-exclamation-triangle-fill text-warning me-2';
        submitIcon.className = trigger.getAttribute('data-confirm-button-icon') || 'bi bi-check2 me-1';
    });

    submitButton.addEventListener('click', () => {
        const form = document.getElementById(submitButton.dataset.confirmForm);
        if (form) {
            form.requestSubmit();
        }
    });
})();
