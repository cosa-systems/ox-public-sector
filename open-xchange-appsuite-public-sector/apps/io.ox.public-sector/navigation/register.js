define('io.ox.public-sector/navigation/register', [
    'io.ox/core/api/tab',
    'io.ox/core/extensions',
    'io.ox/core/main/appcontrol',
    'io.ox.public-sector/ics',
    'gettext!io.ox.public-sector/i18n',
    'settings!io.ox.public-sector',
    'less!io.ox.public-sector/navigation/style'
], function (tabAPI, ext, appcontrol, ics, gt, settings) {
    'use strict';

    var config = ics.then(function (ics) {
            return $.ajax(ics.url + 'navigation.json?language=' +
                ox.language.toLowerCase().replace('_', '-'), {
                xhrFields: { withCredentials: true },
                dataType: 'json'
            });
        }),
        images = settings.get('navigation/color', true),
        oxTab = settings.get('navigation/oxtabname', 'ox');

    function getApp(link) {
        var match = /\bapp=([\w./-]+)/.exec(link);
        return match && match[1];
    }

    function icon(url, launcher) {
        if (images && !launcher) {
            return '<img class="public-sector-launcher" src="'
                + encodeURI(url) + '" style="background: none">';
        }

        var mask = "url('" + encodeURI(url) + "') center" +
            (launcher ? '' : '/ contain');
        return '<span class="public-sector-launcher" style="mask: ' + mask +
            '; -webkit-mask: ' + mask + ';">';
    }

    // Override launcher icon
    ox.ui.appIcons.launcher = icon('apps/io.ox.public-sector/navigation/launcher.svg', true);

    config.then(function (config) {

        // Override app icons
        _.each(config.categories, function (category) {
            _.each(category.entries, function (entry) {
                if (entry.tabname !== oxTab) return;
                ox.ui.appIcons[getApp(entry.link)] = icon(entry.icon_url);
            });
        });
        ext.point('io.ox/core/main/icons').get('mapping').run();
    });

    var LaunchersView = appcontrol.LaunchersView.extend({
        initialize: function () {
            appcontrol.LaunchersView.prototype.initialize.apply(this, arguments);

            // Patch toggle icon
            var title = this.$toggle.children().attr('title');
            this.$toggle.empty()
                .append($(ox.ui.appIcons.launcher).attr('title', title));
        },
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
                    self.append(new appcontrol.LauncherView({
                        model: model
                    }).render().$el);
                });
            }, function () {
                appcontrol.LaunchersView.prototype.update.call(self);
            });
        }
    });

    function createCategory(category) {
        var group = !!category.display_name;
        if (group) {
            this.group(category.display_name);
        }
        _.each(category.entries, function (entry) {
            var makeApp = entry.tabname === oxTab ? ownApp : foreignApp;
            this.append(makeApp(entry), { group: group });
        }, this);
    }

    function ownApp(entry) {
        var model = ox.ui.apps.get(getApp(entry.link));
        if (entry.display_name) model.set('title', entry.display_name);
        return new appcontrol.LauncherView({ model: model }).render().$el;
    }

    function foreignApp(entry) {
        return $('<a tabindex="-1" role="menuitem" class="btn btn-link lcell">')
            .attr({
                href: entry.link,
                target: entry.tabname
            }).append(
                $('<div class="lcell">').append(
                    $('<div class="icon">').append($(icon(entry.icon_url))),
                    $('<div class="title">').text(entry.display_name)
                )
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
