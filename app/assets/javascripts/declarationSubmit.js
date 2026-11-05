(function () {
    'use strict';
    const form = document.querySelector('form');
    const mainContent = document.querySelector('div#main-content');
    const spinnerOverlay = document.getElementById('spinner-overlay');
    const gridColumn = document.querySelector('.govuk-grid-column-two-thirds');
    const backLink = document.querySelector('.govuk-back-link');

    if (!form || !mainContent || !spinnerOverlay || !gridColumn || !backLink) { return; }

    function showSpinner() {
        form.addEventListener('submit', function () {

            gridColumn.classList.remove('govuk-grid-column-two-thirds');
            gridColumn.classList.add('govuk-grid-column-full');
            mainContent.style.display = 'none';
            spinnerOverlay.style.display = 'flex';
            backLink.style.display = 'none';
        });
    }

    showSpinner()
})();
