import api from '$/io.ox/core/api/mailfilter'
import { gt } from 'gettext'
import ox from '$/ox'
import Backbone from '$/backbone'
import _ from '$/underscore'
import $ from '$/jquery'

import { settings, defaults } from '@/io.ox.public-sector/settings'

const AutonotifyModel = Backbone.Model.extend({

  parse (data) {
    // early return is required for model.save()
    // server does not return usable data
    if (!_.isObject(data)) return {}

    const attr = {
      active: false,
      processSub: true,
      to: '' // note must be in mailto: format
    }

    if (!data) {
      // new rule
      attr.position = 0
      return attr
    }

    // rule already exists
    attr.active = !!data.active
    attr.id = data.id
    attr.position = data.position

    data.actioncmds.forEach(function (value) {
      switch (value.id) {
        case 'notify':
          attr.to = value.method.replace(/mailto:/i, '')
          attr.to = attr.to.replace(/([^?]*)[?](.*)$/, '$1') // strip out from ? to the end a@b.c?body=xxxxx
          break
        case 'stop':
          attr.processSub = false
          break
        // no default
      }
    })

    return attr
  },

  toJSON () {
    const attr = this.attributes
    const subject = settings.get('autonotify/subject', defaults.autonotify.subject)
    const body = settings.get('autonotify/body', defaults.autonotify.body)

    const mailtouri = `mailto:${attr.to}?body=${encodeURI((body[ox.language] || body.default))}`

    const json = {
      actioncmds: [{ id: 'notify', method: mailtouri, message: (subject[ox.language] || subject.default) }],
      active: !!attr.active,
      flags: ['autonotify'],
      position: attr.position || 0,
      rulename: gt('notifications'),
      test: { id: 'true' }
    }

    if (attr.processSub === false) json.actioncmds.push({ id: 'stop' })
    // first rule gets 0 so we check for isNumber
    if (Number.isFinite(attr.id)) json.id = attr.id

    return json
  },

  sync (method, module, options) {
    function fixErrors (resp) {
      if (resp.code === 'MAIL_FILTER-0025') { // error for mailto (invalid sieve)
        resp.error = gt('The notification address is not valid.')
      }
      return $.Deferred().reject(resp).promise()
    }
    switch (method) {
      case 'create':
        return api.create(this.toJSON())
          .then(null, fixErrors)
          .done(this.onUpdate.bind(this))
          .done(options.success).fail(options.error)
      case 'read':
        return api.getRules().then(rules => rules.find(rule => rule?.flags?.[0] === 'autonotify'))
          .then(null, fixErrors)
          .done(options.success).fail(options.error)
      case 'update':
        return api.update(this.toJSON())
          .then(null, fixErrors)
          .done(this.onUpdate.bind(this))
          .done(options.success).fail(options.error)
      case 'delete':
        return api.deleteRule(this.get('id'))
          .then(null, fixErrors)
          .done(this.onUpdate.bind(this))
          .done(options.success).fail(options.error)
      // no default
    }
  },

  onUpdate () {
    // Copying core:
    // an ugly way to propagate changes instead of using a proper single model
    // parameters: new value of active, refresh the list
    ox.trigger('mail:change:auto-notify', this.isActive(), true)
  },

  isActive () {
    return !!this.get('active')
  }
})

export default AutonotifyModel
