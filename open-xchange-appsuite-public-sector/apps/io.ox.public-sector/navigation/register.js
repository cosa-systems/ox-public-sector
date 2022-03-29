define('io.ox.public-sector/navigation/register', [
    'io.ox/backbone/mini-views/dropdown',
    'io.ox/core/api/tab',
    'io.ox/core/extensions',
    'io.ox/core/main/appcontrol',
    'gettext!io.ox.public-sector/i18n',
    'settings!io.ox.public-sector',
    'less!io.ox.public-sector/navigation/style'
], function (Dropdown, tabAPI, ext, appcontrol, gt, settings) {
    'use strict';

    var URL = settings.get('navigation/url');
    if (!URL) return;

    var config = $.ajax(URL + '?lang=' + ox.language, { dataType: 'json' }),
        OXCategory = settings.get('navigation/oxcategory', 'ox');

    config.then(function (config) {

        // Override app icons
        _.each(config.categories, function (category) {
            if (category.identifier !== OXCategory) return;
            _.each(category.entries, function (entry) {
                ox.ui.appIcons[entry.identifier] =
                    '<img src="' + encodeURI(entry.icon_url) + '">';
            });
        });
        ext.point('io.ox/core/main/icons').get('mapping').run();
    });

    var LauncherView = appcontrol.LauncherView.extend({
        attributes: function () {
            return {
                href: this.model.get('link'),
                target: this.model.get('identifier'),
                role: 'menuitem',
                tabindex: -1
            };
        }
    });

    var LaunchersView = appcontrol.LaunchersView.extend({
        update: function () {
            this.$ul.empty();
            var self = this;
            config.then(function (config) {
                // Add configured apps
                _.each(config.categories, createCategory, self);

                // Check for compose apps on mobile
                if (!_.device('smartphone')) return;
                var closable = self.collection.where({ closable: true });
                if (!closable.length) return;

                // Add compose apps
                self.divider();
                closable.forEach(function (model) {
                    self.append(
                        new LauncherView({ model: model }).render().$el
                    );
                });
            });
        }
    });

    function createCategory(category) {
        this.group(category.display_name);
        var makeApp = category.identifier === OXCategory ? ownApp : foreignApp;
        _.each(category.entries, makeApp, this);
    }

    function ownApp(entry) {
        this.append(new LauncherView({
            model: ox.ui.apps.get(entry.identifier)
        }).render().$el, { group: true });
    }

    function foreignApp(entry) {
        this.append(
            $('<a tabindex="-1" role="menuitem" class="btn btn-link lcell">').attr({
                href: entry.link,
                target: entry.target || entry.identifier
            }).append(
                $('<div class="lcell">').append(
                    $('<div class="icon">').append(
                        $('<img>').attr('src', entry.icon_url)
                    ),
                    $('<div class="title">').text(entry.display_name)
                )
            ),
            { group: true }
        );
    }

    ext.point('io.ox/core/appcontrol').replace({
        id: 'left',
        draw: function () {
            var taskbar = $('<ul class="taskbar list-unstyled" role="toolbar">');
            this.append($('<div id="io-ox-topleftbar">').append(taskbar));
            ext.point('io.ox/core/appcontrol/left').invoke('draw', taskbar);
        }
    });

    ext.point('io.ox/core/appcontrol/left').replace({
        id: 'launcher',
        draw: function () {
            new LaunchersView({
                collection: ox.ui.apps,
                dontProcessOnMobile: true,
                margin: 12
            }).render().$el.appendTo(this);
        }
    });

    ext.point('io.ox/core/appcontrol/right').disable('launcher');
});
