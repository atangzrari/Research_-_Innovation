// Applies the saved text size before the page paints (avoids a flash of small text).
(function () {
    'use strict';
    try {
        var saved = window.localStorage.getItem('strideTextSize');
        if (saved === 'large' || saved === 'xlarge') {
            document.documentElement.setAttribute('data-size', saved);
        }
    } catch (e) { /* storage blocked: keep the default size */ }
}());
