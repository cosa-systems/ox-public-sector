import $ from '$/jquery'
import _ from '$/underscore'
import Backbone from '$/backbone'
import { gt } from 'gettext'
import moment from '$/moment'
import ox from '$/ox'

import DisposableView from '$/io.ox/backbone/views/disposable'
import calendarAPI from '$/io.ox/calendar/api'
import confAPI from '$/io.ox/conference/api'
import { getConference } from '$/io.ox/conference/util'
import { createIcon } from '$/io.ox/core/components'
import ext from '$/io.ox/core/extensions'
import http from '$/io.ox/core/http'
import yell from '$/io.ox/core/yell'

import ics from '@/io.ox.public-sector/ics'
import '@/io.ox.public-sector/element/style.scss'

const format = 'YYYY-MM-DDTHH:mm:ssZ'

function toRoom (appt) {
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
  }
}

function send (method, url, data, options) {
  return ics.then(function (ics) {
    return $.ajax(_.extend({
      url: ics.url + url,
      method,
      contentType: 'application/json; charset=utf-8',
      xhrFields: { withCredentials: true },
      headers: {
        'Accept-Language': ox.language.toLowerCase().replace('_', '-'),
        'x-csrf-token': ics.csrfToken
      },
      data: JSON.stringify(data),
      dataType: 'text'
    }, options || {}))
  })
}

const api = {
  create: function (data) {
    return send('POST', 'nob/v1/meeting/create', toRoom(data),
      { dataType: 'json' })
  },
  update: function (id, data) {
    return send('PUT', 'nob/v1/meeting/update',
      _.extend(toRoom(data), { target_room_id: id }))
  },
  close: function (id) {
    return send('POST', 'nob/v1/meeting/close', {
      target_room_id: id,
      method: 'kick_all_participants'
    })
  }
}

const ConferenceView = DisposableView.extend({

  className: 'conference-view element',

  events: {
    'click [data-action="copy-to-location"]': 'copyToLocation'
  },

  initialize: function (options) {
    const model = this.model = new Backbone.Model({ url: '' })
    this.appointment = options.appointment
    const conference = getConference(this.appointment.get('conferences'))
    if (conference && conference.type === 'element' && conference.joinURL) {
      model.set({
        id: conference.id,
        url: conference.joinURL
      })
    } else {
      api.create(this.appointment.toJSON()).then(function (room) {
        model.set({
          id: room.room_id,
          url: room.meeting_url,
          created: true,
          error: null
        })
      }, function () {
        model.set({
          error: gt('Could not create the conference room')
        })
      })
    }
    this.listenTo(model, 'change', this.update)
    this.listenTo(this.appointment, 'create update', this.changeMeeting)
    this.listenTo(this.appointment, 'discard', this.discardMeeting)
    this.on('dispose', this.discardMeeting)
  },

  render: function () {
    this.copy = this.actions = null
    this.$el.empty()
    if (this.model.get('error')) return this.renderError()
    if (!this.model.get('url')) return this.renderPending()
    return this.renderDone()
  },

  renderError: function () {
    this.$el.append(
      $('<div class="conference-logo error">').append(
        $('<i class="fa fa-exclamation" aria-hidden="true">')
      ),
      $.txt(this.model.get('error'))
    )
    return this
  },

  renderPending: function () {
    this.$el.append($('<div class="pending">').append(
      // $('<img class="conference-logo" aria-hidden="true" src="io.ox.public-sector/element/conference.svg">'),
      $('<div class="conference-logo">'),
      $.txt(gt('Creating conference room...')),
      createIcon('bi/arrow-clockwise.svg').addClass('animate-spin')
    ))
    return this
  },

  renderDone: function () {
    const url = this.model.get('url')

    this.$el.append(
      // $('<img class="conference-logo" aria-hidden="true" src="io.ox.public-sector/element/conference.svg">'),
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
          .on('click', e => {
            e.preventDefault()
            navigator.clipboard.writeText(url)
          })
      )
    )

    return this
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
      }])
    }
    this.render()
  },

  changeMeeting: function () {
    const id = this.model.get('id')
    if (!id) return
    const data = this.appointment.toJSON()
    // This appointment is an exception of a series - do not change the room
    if (data.seriesId && (data.seriesId !== data.id)) return
    // This appointment changed to an exception of a series - do not change the room
    if (data.seriesId && (data.seriesId === data.id) && !data.rrule) return
    // or check the model itself
    if (data.seriesId && this.appointment.mode === 'appointment') return
    api.update(id, data).fail(function () {
      yell('error', gt('Could not update the conference room'))
    })
    this.off('dispose', this.discardMeeting)
  },

  discardMeeting: function () {
    if (!this.model.get('created')) return
    api.close(this.model.get('id')).then(null, function () {
      yell('error', gt('Could not delete the conference room'))
    })
    this.off('dispose', this.discardMeeting)
  },

  copyToLocation: function (e) {
    e.preventDefault()
    this.appointment.set('location',
      // #. %1$s contains the link to join the conference
      gt('Link: %1$s', this.model.get('url')))
  }
})

ext.point('io.ox/calendar/conference-solutions').extend({
  id: 'element',
  index: 400,
  value: 'element',
  label: gt('Video conference'),
  render: function (view) {
    this.append(new ConferenceView({
      appointment: view.appointment
    }).render().$el)
  }

})

// move location to later position
ext.point('io.ox/calendar/edit/section').replace({ id: 'location', index: 750 })

confAPI.add('element', { joinLinkTitle: gt('Join video conference') })

calendarAPI.on('beforedelete', function (list) {
  if (!_.isArray(list)) list = [list]
  try {
    http.pause()
    _.forEach(list, function (event) {
      if (event.recurrenceId || event.recurrenceRange) return
      calendarAPI.get(event).then(function (event) {
        const conference = getConference(event.get('conferences'))
        if (!conference || !conference.id) return
        api.close(conference.id).fail(function () {
          yell('error', gt('Could not delete the conference room'))
        })
      })
    })
  } finally {
    http.resume()
  }
})
