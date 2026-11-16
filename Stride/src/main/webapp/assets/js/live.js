// Live attendance: refreshes the rotating code, the QR image and the count without reloading.
// Without JavaScript the session page still works (reload for a new code); the projector page
// refreshes itself with a <noscript> meta refresh.
(function () {
    'use strict';
    var box = document.querySelector('[data-live]');
    if (!box) { return; }
    var url = box.getAttribute('data-live');
    var qrUrl = box.getAttribute('data-qr');
    var codeEl = box.querySelector('[data-code]');
    var secEl = box.querySelector('[data-seconds]');
    var countEl = box.querySelector('[data-count]');
    var closesEl = box.querySelector('[data-closes]');
    var img = box.querySelector('[data-qr-img]');
    var summary = document.getElementById('live-summary');
    var seconds = secEl ? parseInt(secEl.textContent, 10) || 0 : 0;
    var lastCode = codeEl ? codeEl.textContent.trim() : '';
    var stopped = false;

    function poll() {
        if (stopped) { return; }
        var xhr = new XMLHttpRequest();
        xhr.open('GET', url, true);
        xhr.setRequestHeader('Accept', 'application/json');
        xhr.onload = function () {
            if (xhr.status !== 200) { return; }
            var d;
            try { d = JSON.parse(xhr.responseText); } catch (e) { return; }
            if (!d.open) {
                stopped = true;
                if (codeEl) { codeEl.textContent = 'Closed'; }
                if (img) { img.hidden = true; }
                return;
            }
            seconds = d.secondsLeft;
            if (secEl) { secEl.textContent = String(seconds); }
            if (countEl) { countEl.textContent = String(d.checkedIn); }
            if (closesEl) { closesEl.textContent = d.closes; }
            if (summary) {
                summary.textContent = d.checkedIn + ' of ' + d.enrolled + ' checked in'
                    + (d.flagged > 0 ? ', ' + d.flagged + ' to review' : '') + '.';
            }
            if (d.code && d.code !== lastCode) {
                lastCode = d.code;
                if (codeEl) { codeEl.textContent = d.code; }
                if (img && qrUrl) { img.src = qrUrl + '&v=' + Date.now(); }
            }
        };
        xhr.send();
    }

    // Count down every second; ask the server every 3 seconds and right after the code changes.
    setInterval(function () {
        if (stopped) { return; }
        seconds = Math.max(0, seconds - 1);
        if (secEl) { secEl.textContent = String(seconds); }
        if (seconds === 0) { poll(); }
    }, 1000);
    setInterval(poll, 3000);
}());
