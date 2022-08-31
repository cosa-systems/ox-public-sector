define('io.ox.public-sector/element/register', [
    'io.ox/backbone/views/disposable',
    'io.ox/calendar/api',
    'io.ox/conference/api',
    'io.ox/core/extensions',
    'io.ox/core/http',
    'io.ox/core/notifications',
    'io.ox.public-sector/ics',
    'gettext!io.ox.public-sector/i18n',
    'less!io.ox.public-sector/element/style.less'
], function (
    DisposableView, calendarAPI, confAPI, ext, http, notifications, ics, gt
) {
    'use strict';

    var format = 'YYYY-MM-DDThh:mm:ssZ';

    function toRoom(appt) {
        return {
            description: appt.description || '',
            enable_auto_deletion: false,
            end_time: moment(appt.endDate.value).format(format),
            external_data: {
                'io.ox': {
                    folder: appt.folder,
                    id: appt.id,
                    rrules: appt.rrule ? [appt.rrule] : []
                }
            },
            start_time: moment(appt.startDate.value).format(format),
            title: appt.summary || ''
        };
    }

    function send(method, url, data) {
        return ics.then(function (ics) {
            return $.ajax({
                url: ics.url + url,
                method: method,
                contentType: 'application/json; charset=utf-8',
                xhrFields: { withCredentials: true },
                headers: {
                    'Accept-Language': ox.language.toLowerCase().replace('_', '-'),
                    'x-csrf-token': ics.csrfToken
                },
                data: JSON.stringify(data),
                dataType: 'json'
            });
        });
    }

    var api = {
        create: function (data) {
            return send('POST', 'nob/v1/meeting/create', toRoom(data));
        },
        update: function (id, data) {
            return send('PUT', 'nob/v1/meeting/update',
                _.extend(toRoom(data), { target_room_id: id }));
        },
        close: function (id) {
            return send('POST', 'nob/v1/meeting/close', {
                target_room_id: id,
                method: 'kick_all_participants'
            });
        }
    };

    var ConferenceView = DisposableView.extend({

        className: 'conference-view element',

        events: {
            'click [data-action="copy-to-location"]': 'copyToLocation',
            'click [data-action="copy-to-description"]': 'copyToDescription'
        },

        initialize: function (options) {
            var model = this.model = new Backbone.Model({ url: '' });
            this.appointment = options.appointment;
            var conference = confAPI.getConference(this.appointment.get('conferences'));
            if (conference && conference.type === 'element' && conference.joinURL) {
                model.set({
                    id: conference.id,
                    url: conference.joinURL
                });
            } else {
                api.create(this.appointment.toJSON()).then(function (room) {
                    model.set({
                        id: room.room_id,
                        url: room.meeting_url,
                        created: true,
                        error: null
                    });
                }, function () {
                    model.set({
                        error: gt('Could not create the conference room')
                    });
                });
            }
            this.listenTo(model, 'change', this.update);
            this.listenTo(this.appointment, 'create update', this.changeMeeting);
            this.listenTo(this.appointment, 'discard', this.discardMeeting);
            this.on('dispose', this.discardMeeting);
        },

        render: function () {
            this.copy = this.actions = null;
            this.$el.empty();
            if (this.model.get('error')) return this.renderError();
            if (!this.model.get('url')) return this.renderPending();
            return this.renderDone();
        },

        renderError: function () {
            this.$el.append(
                $('<div class="conference-logo error">').append(
                    $('<i class="fa fa-exclamation" aria-hidden="true">')
                ),
                $.txt(this.model.get('error'))
            );
            return this;
        },

        renderPending: function () {
            this.$el.append(
                // $('<img class="conference-logo" aria-hidden="true" src="apps/io.ox.public-sector/element/conference.svg">'),
                $('<div class="conference-logo">'),
                $.txt(gt('Creating conference room...')),
                $('<i class="fa fa-refresh fa-spin" aria-hidden="true">')
            );
            return this;
        },

        renderDone: function () {
            var url = this.model.get('url');

            this.$el.append(
                // $('<img class="conference-logo" aria-hidden="true" src="apps/io.ox.public-sector/element/conference.svg">'),
                $('<div class="conference-logo">'),
                $('<div class="ellipsis">').append(
                    $('<b>').text(gt('Link:')),
                    $.txt(' '),
                    $('<a target="_blank" rel="noopener">').attr('href', url).text(gt.noI18n(url))
                ),
                this.actions = $('<div>').append(
                    $('<a href="#" class="secondary-action" data-action="copy-to-location">')
                        .text(gt('Copy to location field')),
                    this.copy = $('<a href="#" class="secondary-action">')
                        .text(gt('Copy to clipboard'))
                        .attr('data-clipboard-text', url)
                        .on('click', false),
                    $('<a href="#" class="secondary-action" data-action="copy-to-description">')
                        .text(gt('Copy to description'))
                )
            );

            require(['static/3rd.party/clipboard.min.js'], function (Clipboard) {
                new Clipboard(this.copy.get(0));
            }.bind(this));

            return this;
        },

        update: function () {
            if (!this.model.get('error')) {
                this.appointment.set('conferences', [{
                    uri: this.model.get('url'),
                    features: ['VIDEO', 'AUDIO', 'CHAT'],
                    label: gt('Video conference'),
                    extendedParameters: {
                        'X-OX-TYPE': 'element',
                        'X-OX-ID': this.model.get('id'),
                        'X-OX-OWNER': ox.user_id
                    }
                }]);
            }
            this.render();
        },

        changeMeeting: function () {
            var id = this.model.get('id');
            if (!id) return;
            var data = this.appointment.toJSON();
            // This appointment is an exception of a series - do not change the room
            if (data.seriesId && (data.seriesId !== data.id)) return;
            // This appointment changed to an exception of a series - do not change the room
            if (data.seriesId && (data.seriesId === data.id) && !data.rrule) return;
            // or check the model itself
            if (data.seriesId && this.appointment.mode === 'appointment') return;
            var model = this.model;
            api.update(id, data).fail(function () {
                model.set('error', gt('Could not update the conference room'));
            });
            this.off('dispose', this.discardMeeting);
        },

        discardMeeting: function () {
            if (!this.model.get('created')) return;
            api.close(this.model.get('id')).fail(function () {
                notifications.yell('error',
                    gt('Could not delete the conference room'));
            });
            this.off('dispose', this.discardMeeting);
        },

        copyToLocation: function (e) {
            e.preventDefault();
            this.appointment.set('location',
                //#. %1$s contains the link to join the conference
                gt('Link: %1$s', this.model.get('url')));
        },

        copyToDescription: function (e) {
            e.preventDefault();
            var description = this.appointment.get('description') || '';
            this.appointment.set('description',
                //#. %1$s is the meeting link
                gt('Link: %1$s', this.model.get('url')) +
                '\n\n' + description);
        }
    });

    ext.point('io.ox/calendar/conference-solutions').extend({
        id: 'element',
        index: 400,
        value: 'element',
        label: gt('Video conference'),
        render: function (view) {
            this.append(new ConferenceView({
                appointment: view.appointment
            }).render().$el);
        }

    });

    // move location to later position
    ext.point('io.ox/calendar/edit/section').replace({ id: 'location', index: 750 });

    confAPI.add('element', { joinLinkTitle: gt('Join video conference') });

    calendarAPI.on('beforedelete', function (list) {
        if (!_.isArray(list)) list = [list];
        try {
            http.pause();
            _.forEach(list, function (event) {
                if (event.recurrenceId || event.recurrenceRange) return;
                calendarAPI.get(event).then(function (event) {
                    var conference = confAPI.getConference(event.get('conferences'));
                    if (!conference || !conference.id) return;
                    api.close(conference.id).fail(function () {
                        notifications.yell('error',
                            gt('Could not delete the conference room'));
                    });
                });
            });
        } finally {
            http.resume();
        }
    });
});
