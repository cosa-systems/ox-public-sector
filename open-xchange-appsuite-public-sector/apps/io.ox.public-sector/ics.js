define('io.ox.public-sector/ics', [
    'settings!io.ox.public-sector'
], function (settings) {
    'use strict';

    var session = $.Deferred(),
        icsURL = new URL(settings.get('ics/url', location.origin));

    $(window).on('message', messageHandler);

    var iframe = $('<iframe style="display: none">')
        .attr('src', icsURL + 'silent')
        .appendTo(document.body);

    function messageHandler(e) {
        if (e.originalEvent.origin !== icsURL.origin) return;

        var data = e.originalEvent.data;
        if (data.loggedIn) {
            session.resolve({ url: icsURL });
        } else {
            session.reject(false);
        }
        $(window).off('message', messageHandler);
        iframe.remove();
    }

    return session.promise();
});
