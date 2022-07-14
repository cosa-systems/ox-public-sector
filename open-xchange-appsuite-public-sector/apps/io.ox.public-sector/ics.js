define('io.ox.public-sector/ics', [
    'io.ox/core/extensions',
    'settings!io.ox.public-sector'
], function (ext, settings) {
    'use strict';

    var session = $.Deferred(), promise = session.promise(),
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

    // This is executed in the core namespace thanks to navigation.
    // Once there is more generic code, it can move into a separate file.
    ext.point('io.ox.nextcloud/file-picker/options').extend({
        id: 'public-sector',
        after: 'default',
        perform: function (baton) {
            if (!baton.data.accessToken) {
                baton.data.useCookies = true;
            }
            return promise;
        }
    });

    return promise;
});
