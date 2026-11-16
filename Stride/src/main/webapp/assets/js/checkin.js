// Optional location factor for check-in. Only when the college has switched it on
// (data-location="on") and the student allows it; otherwise the form submits as normal.
(function () {
    'use strict';
    var forms = document.querySelectorAll('.checkin-form[data-location="on"]');
    if (!forms.length || !navigator.geolocation) { return; }
    for (var i = 0; i < forms.length; i++) {
        forms[i].addEventListener('submit', function (event) {
            var form = this;
            if (form.getAttribute('data-located') === 'yes') { return; }
            event.preventDefault();
            var done = false;
            function send() {
                if (done) { return; }
                done = true;
                form.setAttribute('data-located', 'yes');
                form.submit();
            }
            setTimeout(send, 5000);
            navigator.geolocation.getCurrentPosition(function (pos) {
                form.elements.lat.value = pos.coords.latitude;
                form.elements.lon.value = pos.coords.longitude;
                form.elements.acc.value = Math.round(pos.coords.accuracy);
                send();
            }, send, { enableHighAccuracy: true, timeout: 4500, maximumAge: 0 });
        });
    }
}());
