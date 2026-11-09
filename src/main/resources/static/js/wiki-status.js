(() => {
    let intervalId = null;

    const setFetchButtons = disabled => {
        document.querySelectorAll('[data-wiki-fetch-btn]').forEach(btn => {
            const permanentlyDisabled = btn.dataset.wikiFetchDisabled === 'true';
            btn.disabled = disabled || permanentlyDisabled;
            const spinner = btn.querySelector('[data-wiki-spinner]');
            const icon    = btn.querySelector('[data-wiki-icon]');
            if (spinner) spinner.classList.toggle('d-none', !disabled || permanentlyDisabled);
            if (icon)    icon.classList.toggle('d-none', disabled && !permanentlyDisabled);
        });
    };

    const poll = () => {
        fetch('/admin/wiki-status')
            .then(r => r.json())
            .then(data => {
                setFetchButtons(data.running);

                if (data.running) return;

                if (data.message) {
                    const page = document.querySelector('main[data-wiki-page]')?.dataset.wikiPage;
                    if (!page || page === data.type) showAlert(data.message);
                }

                // Nothing running and nothing pending - stop polling
                stopPolling();
            })
            .catch(() => {});
    };

    const stopPolling = () => {
        if (intervalId !== null) {
            clearInterval(intervalId);
            intervalId = null;
        }
    };

    const showAlert = message => {
        const main = document.querySelector('main.container');
        if (!main) return;
        const alertEl = document.createElement('div');
        alertEl.className = 'alert alert-success alert-dismissible fade show';
        alertEl.setAttribute('role', 'alert');

        const text = document.createTextNode(message + ' Refresh the page to see the results.');
        const closeBtn = document.createElement('button');
        closeBtn.type = 'button';
        closeBtn.className = 'btn-close';
        closeBtn.setAttribute('data-bs-dismiss', 'alert');

        alertEl.appendChild(text);
        alertEl.appendChild(closeBtn);
        main.insertBefore(alertEl, main.firstChild);
    };

    poll(); // immediate check on page load
    intervalId = setInterval(poll, 3000);
})();
