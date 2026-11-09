document.querySelectorAll('[data-debounce-search]').forEach(input => {
    const form       = input.closest('form');
    const target     = input.dataset.searchTarget;
    const clearBtn   = document.getElementById('clearBtn');
    if (!form || !target) return;

    const sortByInput = form.querySelector('[name="sortBy"]');
    const dirInput    = form.querySelector('[name="dir"]');

    let timer;
    let activeController = null;

    const syncClearBtn = () => clearBtn && clearBtn.classList.toggle('d-none', !hasActiveFilters());

    const formAction = form.getAttribute('action') || form.action || window.location.pathname;

    const buildUrl = includeFragment => {
        const url = new URL(formAction, window.location.origin);
        url.search = '';
        if (includeFragment) url.searchParams.set('fragment', 'true');
        const sortByValue = sortByInput?.value?.trim();
        new FormData(form).forEach((value, key) => {
            if (key === 'dir' && !sortByValue) return;
            if (value.toString().trim()) url.searchParams.append(key, value.toString().trim());
        });
        return url;
    };

    const hasActiveFilters = () => {
        const sortByValue = sortByInput?.value?.trim();
        return [...new FormData(form).entries()].some(([key, value]) => {
            if (key === 'dir' && !sortByValue) return false;
            return value.toString().trim() !== '';
        });
    };

    const updateCounterLabels = () =>
        form.querySelectorAll('[data-counter-for]').forEach(btn => {
            const count = form.querySelectorAll(`input[name="${btn.dataset.counterFor}"]:checked`).length;
            const counter = btn.querySelector('[data-counter-text]');
            if (counter) counter.textContent = count > 0 ? ` (${count})` : '';
        });

    const fetchResults = async () => {
        if (activeController) activeController.abort();
        activeController = new AbortController();
        try {
            const response = await fetch(buildUrl(true), { signal: activeController.signal });
            const html = await response.text();
            if (!response.ok) { console.error(`Search failed: ${response.status}`); return; }
            const el = document.querySelector(target);
            if (el) el.outerHTML = html;
            history.replaceState(null, '', buildUrl(false));
            sessionStorage.setItem('listUrl:' + new URL(formAction, location.origin).pathname, location.href);
        } catch (err) {
            if (err.name !== 'AbortError') console.error('Search failed:', err);
        }
    };

    input.addEventListener('input', () => {
        clearTimeout(timer);
        syncClearBtn();
        if (!hasActiveFilters()) {
            void fetchResults();
            return;
        }
        timer = setTimeout(fetchResults, 400);
    });

    form.querySelectorAll('[data-auto-submit]').forEach(el =>
        el.addEventListener('change', () => {
            updateCounterLabels();
            clearTimeout(timer);
            syncClearBtn();
            void fetchResults();
        })
    );

    document.addEventListener('click', e => {
        const btn = e.target.closest('[data-sort-by]');
        if (!btn || !sortByInput || !dirInput) return;
        const col        = btn.dataset.sortBy;
        const currentCol = sortByInput.value;
        const currentDir = dirInput.value || 'desc';
        if (currentCol === col && currentDir === 'asc') {
            sortByInput.value = '';
            dirInput.value    = 'desc';
        } else {
            sortByInput.value = col;
            dirInput.value    = (currentCol === col && currentDir === 'desc') ? 'asc' : 'desc';
        }
        syncClearBtn();
        const sortLabel = form.querySelector('[data-sort-label]');
        if (sortLabel) {
            if (!sortByInput.value) {
                sortLabel.textContent = 'Sort';
            } else {
                const icon = form.querySelector(`[data-sort-icon][data-sort-by="${sortByInput.value}"]`);
                sortLabel.textContent = icon ? icon.dataset.sortIcon + ' ' + (dirInput.value === 'asc' ? '↑' : '↓') : 'Sort';
            }
        }
        clearTimeout(timer);
        void fetchResults();
    });

    if (clearBtn) {
        clearBtn.addEventListener('click', () => {
            form.querySelectorAll('input[type="text"], input[type="search"], select, [data-clearable]')
                .forEach(el => el.value = '');
            form.querySelectorAll('input[type="checkbox"]').forEach(el => el.checked = false);
            updateCounterLabels();
            const sortLabel = form.querySelector('[data-sort-label]');
            if (sortLabel) sortLabel.textContent = 'Sort';
            clearBtn.classList.add('d-none');
            void fetchResults();
            input.focus();
        });
    }

    updateCounterLabels();

    const listPath = new URL(formAction, location.origin).pathname;
    sessionStorage.setItem('listUrl:' + listPath, location.href);

    document.querySelectorAll(`a[href="${listPath}"]`).forEach(a =>
        a.addEventListener('click', e => {
            const saved = sessionStorage.getItem('listUrl:' + listPath);
            if (saved && new URL(saved).search) {
                e.preventDefault();
                if (saved !== location.href) location.href = saved;
            }
        })
    );
});
