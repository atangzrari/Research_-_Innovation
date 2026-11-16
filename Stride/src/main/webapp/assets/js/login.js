// Show / hide password. The form works without this script.
(function () {
    'use strict';
    var button = document.getElementById('toggle-password');
    var field = document.getElementById('password');
    if (!button || !field) { return; }
    button.hidden = false;
    button.addEventListener('click', function () {
        var showing = field.type === 'text';
        field.type = showing ? 'password' : 'text';
        button.textContent = showing ? 'Show' : 'Hide';
        button.setAttribute('aria-pressed', String(!showing));
        button.setAttribute('aria-label', showing ? 'Show password' : 'Hide password');
    });
}());
