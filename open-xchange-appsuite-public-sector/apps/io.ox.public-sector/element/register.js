define('io.ox.public-sector/element/register', [
    'io.ox/backbone/views/disposable',
    'io.ox/conference/api',
    'io.ox/core/extensions',
    'io.ox.public-sector/ics',
    'gettext!io.ox.public-sector/i18n'
], function (DisposableView, confAPI, ext, ics, gt) {
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
                    'Accept-Language': ox.language.toLowerCase().replace('_', '-')
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
            return send('POST', 'nob/v1/close', { target_room_id: id });
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
                this.model.set('url', conference.joinURL);
            } else {
                api.create(this.appointment.toJSON()).then(function (room) {
                    model.set({
                        id: room.room_id,
                        url: room.meeting_url
                    });
                });
            }
            this.listenTo(this.model, 'change', this.update);
            this.listenTo(this.appointment, 'create update', this.changeMeeting);
            this.listenTo(this.appointment, 'discard', this.discardMeeting);
            this.on('dispose', this.discardMeeting);
        },

        render: function () {
            this.$el.empty();
            var url = this.model.get('url');
            if (!url) return this.renderPending();
            return this.renderDone();
        },

        renderPending: function () {
            this.link = this.copy = this.actions = null;
            this.$el.empty().append(
                $('<i class="fa fa-video-camera conference-logo" aria-hidden="true">'),
                $.txt(gt('Creating conference room...')),
                $('<i class="fa fa-refresh fa-spin" aria-hidden="true">')
            );
            return this;
        },

        renderDone: function () {
            var url = this.model.get('url');

            this.$el.empty().append(
                $('<i class="fa fa-video-camera conference-logo" aria-hidden="true">'),
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
            var url = this.model.get('url');
            this.appointment.set('conferences', [{
                id: this.model.get('id'),
                uri: url,
                features: ['AUDIO', 'VIDEO', 'CHAT'],
                label: gt('Video Meeting'),
                extendedParameters: { 'X-OX-TYPE': 'element' }
            }]);
            this.renderDone();
        },

        changeMeeting: function () {
            var id = this.model.get('id');
            if (!id) return;
            var data = this.appointment.toJSON();
            // This appointment is an exception of a series - do not change the room
            if (data.seriesId && (data.seriesId !== data.id)) return;
            // This appointment changed to an exception of a series - do not change the room
            if (data.seriesId && (data.seriesId === data.id) && !data.rrule) return;
            api.update(id, data);
            this.off('dispose', this.discardMeeting);
        },

        discardMeeting: function () {
            var id = this.model.get('id');
            if (!id) return;
            api.close(id);
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
        label: gt('Video Meeting'),
        render: function (view) {
            this.append(new ConferenceView({
                appointment: view.appointment
            }).render().$el);
        }

    });

    // move location to later position
    ext.point('io.ox/calendar/edit/section').replace({ id: 'location', index: 750 });
});
