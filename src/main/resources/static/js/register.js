(() => {
    document.querySelectorAll('[data-bs-toggle="tooltip"]')
        .forEach(el => bootstrap.Tooltip.getOrCreateInstance(el));

    const invalid = document.querySelector('.is-invalid');
    const target = invalid || document.getElementById('username');

    if (target) {
        target.focus();

        if (typeof target.setSelectionRange === 'function') {
            const end = target.value.length;
            target.setSelectionRange(end, end);
        }
    }

    const password = document.getElementById('password');
    const confirm = document.getElementById('confirmPassword');

    if (!password || !confirm) return;

    const checkMatch = () => {
        confirm.classList.remove('is-valid', 'is-invalid');

        if (!confirm.value) return;

        confirm.classList.add(password.value === confirm.value ? 'is-valid' : 'is-invalid');
    };

    password.addEventListener('input', checkMatch);
    confirm.addEventListener('input', checkMatch);
})();
