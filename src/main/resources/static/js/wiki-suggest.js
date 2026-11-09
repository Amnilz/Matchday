/**
 * Wikipedia logo suggestion (generic).
 *
 * Wire up any name input with:
 *   data-wiki-suggest="targetFieldId"   - id of the URL <input> to fill
 *   data-wiki-keywords="football club"  - extra keywords for a more accurate search
 *
 * Elements located by convention (targetId = value of data-wiki-suggest):
 *   #{targetId}              - the URL input to fill
 *   #{targetId}Status        - element that shows status messages
 *   #{targetId}HeadingImg    - optional <img> in the page heading (swaps with HeadingIcon)
 *   #{targetId}HeadingIcon   - optional icon hidden when HeadingImg is shown
 *   #{targetId}AutoSuggest   - optional checkbox; when unchecked, suggestions are disabled
 *
 * Trigger: debounced input (600 ms after the user stops typing).
 * Guard:   skipped when AutoSuggest checkbox exists and is unchecked.
 *          When checkbox is re-checked, a suggestion fires immediately.
 */

(() => {

const debounce = (fn, delay) => {
    let timer;
    return (...args) => {
        clearTimeout(timer);
        timer = setTimeout(() => fn(...args), delay);
    };
};

const searchWikipediaImage = async (name, keywords) => {
    if (keywords.toLowerCase().includes('league')) {
        const exactImage = await fetchWikipediaSummaryImage(name);
        if (exactImage) return exactImage;

        return searchWikipediaLeagueImage(name, keywords);
    }

    return searchWikipediaImageForQuery(keywords ? name + ' ' + keywords : name);
};

const searchWikipediaLeagueImage = async (name, keywords) => {
    const queries = keywords ? [name + ' ' + keywords, name] : [name];
    for (const query of queries) {
        const title = await findPreferredLeagueTitle(query, name);
        if (!title) continue;
        const imageUrl = await fetchWikipediaSummaryImage(title);
        if (imageUrl) return imageUrl;
    }
    return null;
};

const findPreferredLeagueTitle = async (query, name) => {
    const params = new URLSearchParams({
        action:   'query',
        list:     'search',
        srsearch: query,
        format:   'json',
        origin:   '*',
        srlimit:  '5',
        srprop:   ''
    });
    const searchResp = await fetch('https://en.wikipedia.org/w/api.php?' + params);
    if (!searchResp.ok) throw new Error('search failed');

    const hits = (await searchResp.json()).query?.search || [];
    const normalizedName = normalizeTitle(name);
    const match = hits.find(hit => {
        const normalizedTitle = normalizeTitle(hit.title || '');
        return normalizedTitle === normalizedName
                || normalizedTitle.endsWith(' ' + normalizedName);
    });
    return match?.title || null;
};

const normalizeTitle = title => title
    .toLowerCase()
    .replace(/\([^)]*\)/g, '')
    .replace(/[^a-z0-9]+/g, ' ')
    .trim();

const searchWikipediaImageForQuery = async query => {
    // Step 1: find the best matching article using name + contextual keywords
    const params = new URLSearchParams({
        action:   'query',
        list:     'search',
        srsearch: query,
        format:   'json',
        origin:   '*',
        srlimit:  '1',
        srprop:   ''
    });
    const searchResp = await fetch('https://en.wikipedia.org/w/api.php?' + params);
    if (!searchResp.ok) throw new Error('search failed');
    /** @type {{query?: {search?: {title: string}[]}}} */
    const searchData = await searchResp.json();
    const hits = searchData.query?.search;
    if (!hits?.length) return null;

    return fetchWikipediaSummaryImage(hits[0].title);
};

const fetchWikipediaSummaryImage = async title => {
    // Step 2: fetch that article's summary to get its thumbnail
    const summaryResp = await fetch(
        'https://en.wikipedia.org/api/rest_v1/page/summary/' + encodeURIComponent(title),
        { headers: { Accept: 'application/json' } }
    );
    if (!summaryResp.ok) return null;
    /** @type {{thumbnail?: {source?: string}}} */
    const summary = await summaryResp.json();
    return summary.thumbnail?.source ?? null;
};

const updateHeading = (url, headingImg, headingIcon) => {
    if (!headingImg) return;
    if (url) {
        headingImg.src = url;
        headingImg.classList.remove('d-none');
        headingIcon?.classList.add('d-none');
    } else {
        headingImg.classList.add('d-none');
        headingIcon?.classList.remove('d-none');
    }
};

document.querySelectorAll('[data-wiki-suggest]').forEach(nameInput => {
    const targetId    = nameInput.getAttribute('data-wiki-suggest');
    const keywords    = nameInput.getAttribute('data-wiki-keywords') || '';
    const imageInput  = document.getElementById(targetId);
    const statusEl    = document.getElementById(targetId + 'Status');
    const headingImg  = document.getElementById(targetId + 'HeadingImg');
    const headingIcon = document.getElementById(targetId + 'HeadingIcon');
    const checkbox    = document.getElementById(targetId + 'AutoSuggest');

    if (!imageInput) return;

    // Keep heading in sync when admin types a URL manually
    imageInput.addEventListener('input', () =>
        updateHeading(imageInput.value.trim(), headingImg, headingIcon));

    // Hide broken heading image silently
    headingImg?.addEventListener('error', () => {
        headingImg.classList.add('d-none');
        if (headingIcon) headingIcon.classList.remove('d-none');
    });

    const suggest = async () => {
        if (checkbox && !checkbox.checked) return;
        const name = nameInput.value.trim();
        if (name.length < 3) {
            imageInput.value = '';
            updateHeading('', headingImg, headingIcon);
            if (statusEl) statusEl.textContent = '';
            return;
        }

        if (statusEl) statusEl.textContent = 'Searching Wikipedia...';
        try {
            const imgUrl = await searchWikipediaImage(name, keywords);
            if (imgUrl) {
                imageInput.value = imgUrl;
                updateHeading(imgUrl, headingImg, headingIcon);
                if (statusEl) statusEl.textContent = 'Logo suggested from Wikipedia - keep it or paste your own URL.';
            } else {
                if (statusEl) statusEl.textContent = 'No image found on Wikipedia for this name.';
            }
        } catch (_) {
            if (statusEl) statusEl.textContent = '';
        }
    };

    // Debounced input - fires 600 ms after the user stops typing
    nameInput.addEventListener('input', debounce(suggest, 600));

    // Checking the checkbox fires suggest immediately for the current name
    checkbox?.addEventListener('change', () => {
        if (checkbox.checked) void suggest();
    });
});
})();
