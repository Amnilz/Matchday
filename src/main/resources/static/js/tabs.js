(() => {
    const tabButtons = document.querySelectorAll('[data-bs-toggle="tab"]');
    if (!tabButtons.length) return;

    const tabTargets = new Set(
        Array.from(tabButtons)
            .map(btn => btn.dataset.bsTarget)
            .filter(Boolean)
    );

    const hash = window.location.hash;
    if (hash) {
        const target = document.querySelector(`[data-bs-target="${hash}"]`);
        if (target) bootstrap.Tab.getOrCreateInstance(target).show();
    }

    tabButtons.forEach(btn => {
        btn.addEventListener('shown.bs.tab', () => {
            history.replaceState(null, '', btn.dataset.bsTarget);
        });
    });

    document.addEventListener('click', event => {
        const link = event.target.closest('a[href]');
        if (!link || !tabTargets.has(window.location.hash)) return;
        if (event.defaultPrevented || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;

        const targetUrl = new URL(link.href, window.location.href);
        const currentUrl = new URL(window.location.href);

        const samePage = targetUrl.origin === currentUrl.origin
            && targetUrl.pathname === currentUrl.pathname
            && targetUrl.search === currentUrl.search;

        if (samePage && !targetUrl.hash) link.hash = window.location.hash;
    });
})();
