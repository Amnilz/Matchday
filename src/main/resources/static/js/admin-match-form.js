(() => {
    const form          = document.getElementById('matchForm');
    const league        = document.getElementById('leagueId');
    const status        = document.getElementById('status');
    const matchDate     = document.getElementById('matchDate');
    const homeTeam      = document.getElementById('homeTeamId');
    const awayTeam      = document.getElementById('awayTeamId');
    const scoreFields   = document.getElementById('scoreFields');
    const eventsTab     = document.getElementById('events-tab');
    const detailsTab    = document.getElementById('details-tab');
    const eventRows     = document.getElementById('eventRows');
    const eventTemplate = document.getElementById('eventRowTemplate');
    const eventCountBadge = document.getElementById('eventCountBadge');
    const addEventRowBtn  = document.getElementById('addEventRowBtn');

    const markChanged = () => form.dispatchEvent(new Event('change', { bubbles: true }));

    const rowCount = () => eventRows.querySelectorAll('[data-event-row]').length;

    const updateEventCount = () => { eventCountBadge.textContent = String(rowCount()); };

    const optionIds = value => (value || '').split(',').filter(Boolean);

    const teamAllowedByLeague = option => {
        if (!league || !league.value || !option.value) return true;
        return optionIds(option.dataset.leagueIds).includes(league.value);
    };

    const reindexRows = () => {
        eventRows.querySelectorAll('[data-event-row]').forEach((row, index) => {
            row.querySelectorAll('[name]').forEach(field => {
                field.name = field.name.replace(/events\[\d+]/, `events[${index}]`);
            });
        });
        updateEventCount();
    };

    const filterTeamSelect = select => {
        let selectedStillAllowed = !select.value;
        select.querySelectorAll('option').forEach(option => {
            if (!option.value) return;
            const allowed = teamAllowedByLeague(option);
            option.hidden = !allowed;
            option.disabled = !allowed;
            if (option.selected && allowed) selectedStillAllowed = true;
        });

        if (!selectedStillAllowed) {
            select.value = '';
            select.dispatchEvent(new Event('change', { bubbles: true }));
        }
    };

    const filterTeamOptions = () => {
        filterTeamSelect(homeTeam);
        filterTeamSelect(awayTeam);
        filterPlayerOptions();
    };

    const filterPlayerOptions = () => {
        const allowedTeams = [homeTeam.value, awayTeam.value].filter(Boolean);
        document.querySelectorAll('.event-player-select').forEach(select => {
            select.querySelectorAll('option[data-team-id]').forEach(option => {
                const allowed = allowedTeams.length === 0 || allowedTeams.includes(option.dataset.teamId);
                option.hidden   = !allowed;
                option.disabled = !allowed;
                if (!allowed && option.selected) {
                    select.value = '';
                    select.dispatchEvent(new Event('change', { bubbles: true }));
                }
            });
        });
    };

    const toggleCompletedControls = () => {
        const completed = status.value === 'COMPLETED';
        scoreFields.classList.toggle('d-none', !completed);
        eventsTab.disabled = !completed;
        eventsTab.classList.toggle('disabled', !completed);
        if (!completed && eventsTab.classList.contains('active')) {
            bootstrap.Tab.getOrCreateInstance(detailsTab).show();
        }
    };

    const addEventRow = () => {
        const html = eventTemplate.innerHTML.replaceAll('__INDEX__', String(rowCount()));
        eventRows.insertAdjacentHTML('beforeend', html);
        filterPlayerOptions();
        updateEventCount();
        markChanged();
    };

    const autoStatusFromDate = () => {
        if (!matchDate.value) return;
        const past = new Date(matchDate.value) < new Date();
        if (past && status.value === 'SCHEDULED') {
            status.value = 'COMPLETED';
            toggleCompletedControls();
        } else if (!past && status.value === 'COMPLETED') {
            status.value = 'SCHEDULED';
            toggleCompletedControls();
        }
    };

    if (league) {
        league.addEventListener('change', filterTeamOptions);
    }
    status.addEventListener('change', () => {
        toggleCompletedControls();
        filterPlayerOptions();
    });
    matchDate.addEventListener('change', autoStatusFromDate);
    homeTeam.addEventListener('change', filterPlayerOptions);
    awayTeam.addEventListener('change', filterPlayerOptions);
    addEventRowBtn.addEventListener('click', addEventRow);
    eventRows.addEventListener('click', event => {
        const button = event.target.closest('[data-remove-event]');
        if (!button) return;
        const eventRow = button.closest('[data-event-row]');
        const nextRow  = eventRow.nextElementSibling;
        if (nextRow && nextRow.hasAttribute('data-event-error-row')) {
            nextRow.remove();
        }
        eventRow.remove();
        reindexRows();
        markChanged();
    });

    toggleCompletedControls();
    filterTeamOptions();
    updateEventCount();
})();
