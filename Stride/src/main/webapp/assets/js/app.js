// Text-size buttons, and "/" to jump to the search box. The site works without this script.
(function () {
    'use strict';

    var buttons = document.querySelectorAll('[data-size]');
    function mark(size) {
        for (var i = 0; i < buttons.length; i++) {
            buttons[i].setAttribute('aria-pressed', String(buttons[i].getAttribute('data-size') === size));
        }
    }
    var current = document.documentElement.getAttribute('data-size') || 'normal';
    mark(current);
    for (var i = 0; i < buttons.length; i++) {
        buttons[i].addEventListener('click', function () {
            var size = this.getAttribute('data-size');
            if (size === 'normal') {
                document.documentElement.removeAttribute('data-size');
            } else {
                document.documentElement.setAttribute('data-size', size);
            }
            try { window.localStorage.setItem('strideTextSize', size); } catch (e) { /* ignore */ }
            mark(size);
        });
    }

    // Print buttons (reports). Inline onclick is blocked by the Content-Security-Policy, so wire it here.
    var printers = document.querySelectorAll('[data-print]');
    for (var p = 0; p < printers.length; p++) {
        printers[p].addEventListener('click', function () { window.print(); });
    }

    var search = document.getElementById('top-search');
    if (search) {
        document.addEventListener('keydown', function (event) {
            var tag = (event.target.tagName || '').toLowerCase();
            var typing = tag === 'input' || tag === 'textarea' || tag === 'select' || event.target.isContentEditable;
            if (event.key === '/' && !typing && !event.ctrlKey && !event.metaKey && !event.altKey) {
                event.preventDefault();
                search.focus();
            }
        });
    }
}());
