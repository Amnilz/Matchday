(() => {
    const form = document.querySelector('form[data-guard]');
    if (!form) return;

    const modalEl = document.getElementById('formGuardModal');
    if (!modalEl) return;

    let dirty = false;
    let guardArmed = false;
    let dirtyOnLoad = false;
    const modal = bootstrap.Modal.getOrCreateInstance(modalEl);
    const leaveBtn = document.getElementById('formGuardLeaveBtn');
    const cancelLink = form.querySelector('[data-guard-cancel]');
    if (!leaveBtn) return;

    const markDirty = () => {
        dirty = true;
        if (guardArmed) return;
        history.pushState({ guard: true }, '', window.location.href);
        guardArmed = true;
    };

    form.addEventListener('input',  markDirty);
    form.addEventListener('change', markDirty);
    form.addEventListener('submit', () => {
        dirty = false;
        guardArmed = false;
        dirtyOnLoad = false;
    });

    document.addEventListener('submit', e => {
        if (!e.target.matches('[data-guard-ignore]')) return;
        dirty = false;
        guardArmed = false;
        dirtyOnLoad = false;
    }, true);

    if (form.querySelector('.is-invalid, [data-guard-dirty-on-load]')) {
        dirtyOnLoad = true;
        markDirty();
    }

    // Refresh / tab-close: only the native dialog can intercept these
    window.addEventListener('beforeunload', e => {
        if (!dirty) return;
        e.preventDefault();
    });

    const showGuard = onLeave => {
        leaveBtn.onclick = () => {
            dirty = false;
            // Wait for modal fade-out to finish before navigating
            modalEl.addEventListener('hidden.bs.modal', onLeave, { once: true });
            modal.hide();
        };
        modal.show();
    };

    // Intercept all link clicks on the page
    document.addEventListener('click', e => {
        if (!dirty) return;
        const link = e.target.closest('a[href]');
        if (!link) return;
        const href = link.getAttribute('href');
        if (!href || href.startsWith('#')) return;
        e.preventDefault();
        const target = link.href;
        showGuard(() => { window.location.href = target; });
    });

    // Intercept browser back button only after the form has actual unsaved edits.
    window.addEventListener('popstate', () => {
        if (!dirty) {
            guardArmed = false;
            return;
        }
        history.pushState({ guard: true }, '');
        showGuard(() => {
            dirty = false;
            guardArmed = false;
            if (dirtyOnLoad && cancelLink && cancelLink.href) {
                window.location.href = cancelLink.href;
                return;
            }
            history.go(-2);
        });
    });
})();
