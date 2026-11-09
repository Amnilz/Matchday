(() => {
    const spinner = document.getElementById('page-spinner');
    if (!spinner) return;

    let showTimer = null;

    const show = () => spinner.classList.add('visible');
    const hide = () => {
        clearTimeout(showTimer);
        showTimer = null;
        spinner.classList.remove('visible');
    };
    const showIfNavigationIsSlow = () => {
        clearTimeout(showTimer);
        showTimer = setTimeout(show, 180);
    };

    window.addEventListener('beforeunload', () => {
        showIfNavigationIsSlow();
        // If the user cancels the navigation (e.g. beforeunload dialog),
        // no browser event fires to reset the spinner, so we hide it after
        // a short delay. If navigation proceeds, the page unloads first.
        setTimeout(hide, 900);
    });
    window.addEventListener('pageshow', e => { if (e.persisted) hide(); });
})();
