(() => {
    const SELECT_SOURCE_CLASS = 'searchable-select-source';

    // ── data-dropdown-search: live filtering for checkbox dropdowns ─────────
    document.addEventListener('input', e => {
        const input = e.target.closest('[data-dropdown-search]');
        if (!input) return;

        const menu = input.closest('.dropdown-menu');
        if (!menu) return;

        const term = input.value.trim().toLowerCase();
        let visibleCount = 0;

        menu.querySelectorAll('[data-dropdown-option]').forEach(option => {
            const visible = !term || option.textContent.toLowerCase().includes(term);
            option.classList.toggle('d-none', !visible);
            if (visible) visibleCount++;
        });

        const empty = menu.querySelector('[data-dropdown-empty]');
        if (empty) empty.classList.toggle('d-none', visibleCount > 0);
    });

    // ── data-searchable-select: custom single-select component ─────────────
    const optionLabel = option => option ? option.textContent.trim() : 'Select';

    const syncButton = (select, button) => {
        const selected = select.selectedOptions[0];
        button.textContent = optionLabel(selected);
        button.classList.toggle('text-muted', !selected || !selected.value);
    };

    const renderItems = (select, menu, button) => {
        const list   = menu.querySelector('[data-searchable-select-list]');
        const empty  = menu.querySelector('[data-searchable-select-empty]');
        const search = menu.querySelector('[data-searchable-select-input]');
        const term   = search.value.trim().toLowerCase();
        list.innerHTML = '';
        let count = 0;

        Array.from(select.options).forEach(option => {
            if (option.hidden || option.disabled) return;
            if (term && !optionLabel(option).toLowerCase().includes(term)) return;

            const item = document.createElement('button');
            item.type = 'button';
            item.className = 'dropdown-item text-wrap';
            item.textContent = optionLabel(option);
            item.dataset.value = option.value;
            item.classList.toggle('active', option.selected);
            item.addEventListener('click', () => {
                select.value = option.value;
                select.dispatchEvent(new Event('change', { bubbles: true }));
                syncButton(select, button);
                bootstrap.Dropdown.getOrCreateInstance(button).hide();
            });
            list.appendChild(item);
            count++;
        });

        empty.classList.toggle('d-none', count > 0);
    };

    const enhanceSelect = select => {
        if (select.dataset.searchableSelectReady === 'true') return;
        select.dataset.searchableSelectReady = 'true';

        const wrapper = document.createElement('div');
        wrapper.className = 'dropdown searchable-select';

        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'btn btn-outline-secondary dropdown-toggle text-start w-100';
        if (select.classList.contains('form-select-sm')) button.classList.add('btn-sm');
        button.setAttribute('data-bs-toggle', 'dropdown');
        button.setAttribute('data-bs-auto-close', 'outside');

        const menu = document.createElement('div');
        menu.className = 'dropdown-menu p-2 w-100';

        const search = document.createElement('input');
        search.type = 'search';
        search.className = 'form-control form-control-sm mb-2';
        search.placeholder = select.dataset.searchPlaceholder || 'Search...';
        search.autocomplete = 'off';
        search.setAttribute('data-searchable-select-input', '');

        const list = document.createElement('div');
        list.setAttribute('data-searchable-select-list', '');

        const empty = document.createElement('div');
        empty.className = 'text-muted small d-none';
        empty.textContent = 'No matching options.';
        empty.setAttribute('data-searchable-select-empty', '');

        menu.append(search, list, empty);
        wrapper.append(button, menu);
        select.after(wrapper);
        select.classList.add(SELECT_SOURCE_CLASS);
        select.tabIndex = -1;

        syncButton(select, button);
        renderItems(select, menu, button);

        wrapper.addEventListener('show.bs.dropdown', () => {
            search.value = '';
            renderItems(select, menu, button);
            setTimeout(() => search.focus(), 0);
        });
        search.addEventListener('input', () => renderItems(select, menu, button));
        select.addEventListener('change', () => {
            syncButton(select, button);
            renderItems(select, menu, button);
        });
    };

    const initSearchableSelects = root =>
        root.querySelectorAll('select[data-searchable-select]').forEach(enhanceSelect);

    initSearchableSelects(document);

    new MutationObserver(mutations => {
        mutations.forEach(mutation => {
            mutation.addedNodes.forEach(node => {
                if (node.nodeType === 1) initSearchableSelects(node);
            });
        });
    }).observe(document.documentElement, { childList: true, subtree: true });
})();
