define('io.ox.phoenix/navigation/register', [
    'io.ox/backbone/mini-views/dropdown',
    'io.ox/core/extensions',
    'io.ox/core/main/appcontrol',
    'gettext!io.ox.phoenix/i18n',
    'settings!io.ox.phoenix',
    'less!io.ox.phoenix/navigation/style'
], function (Dropdown, ext, appcontrol, gt, settings) {
    'use strict';

    if (_.device('smartphone')) return;

    var config = $.ajax(
        settings.get('navigation', 'navigation.json'),
        { dataType: 'json' }
    );

    var LaunchersView = appcontrol.LaunchersView.extend({
        initialize: function () {
            Dropdown.prototype.initialize.apply(this, arguments);
            var self = this;
            config.then(function (config) {
                _(config.categories).forEach(function (category) {
                    self.group(category.display_name,
                        _(category.entries).map(function (entry) {
                            return $('<a>').attr('href', entry.link).append(
                                $('<img>').attr('src', entry.icon_url),
                                $.txt(entry.display_name)
                            );
                        })
                    );
                });
            });
        }
    });

    ext.point('io.ox/core/appcontrol/left').replace({
        id: 'launcher',
        draw: function () { this.append(new LaunchersView().render().$el); }
    });
});
