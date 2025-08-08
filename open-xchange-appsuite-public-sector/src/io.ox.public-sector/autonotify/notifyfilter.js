/* eslint-disable no-unreachable */
import ext from '$/io.ox/core/extensions'
import api from '$/io.ox/core/api/mailfilter'
import util from '$/io.ox/mail/mailfilter/settings/filter/actions/util'
import _ from '$/underscore'
import ox from '$/ox'
import { gt } from 'gettext'

import { settings, defaults } from '@/io.ox.public-sector/settings'

const subjects = settings.get('autonotify/subject', defaults.autonotify.subject)
const bodies = settings.get('autonotify/body', defaults.autonotify.body)

api.getConfig().then(notifyprocessConfig)

function notifyprocessConfig (config) {
  if (!config.actioncmds.some(a => a.id === 'notify')) return
  ext.point('io.ox/mail/mailfilter/actions').extend({

    id: 'notify',
    index: 310,

    actions: {
      notify: {
        id: 'notify',
        message: (subjects[ox.language] || subjects.default),
        method: '' // Note: this has to be like mailto:aaa@bbb.ccc
      }
    },

    translations: { notify: gt('Notify to') },

    actionCapabilities: { notify: 'notify' },

    order () {}, // prevent manual creation of new rules

    draw: function (baton, actionKey, amodel, filterValues, action) {
      const subject = subjects[ox.language] || subjects.default
      const body = bodies[ox.language] || bodies.default

      amodel.set('to', amodel.get('method').match(/^(?:mailto:)?([^?]*)/)[1])
      amodel.on('change:to', () => {
        amodel.set({
          message: subject,
          method: `mailto:${amodel.get('to')}?body=${encodeURI(body)}`
        })
      })

      const inputId = _.uniqueId('notify_')
      this.append(
        util.drawAction({
          actionKey,
          inputId,
          title: baton.view.actionsTranslations[action.id],
          inputLabel: baton.view.actionsTranslations.notify,
          inputOptions: { name: 'to', model: amodel, className: 'form-control', id: inputId },
          errorView: true
        })
      )
    }

  })
}
