import ext from '$/io.ox/core/extensions'
import api from '$/io.ox/core/api/mailfilter'
import filter from '$/io.ox/mail/mailfilter/settings/filter'
import { createIcon } from '$/io.ox/core/components'
import { add, st } from '$/io.ox/settings/index'
import ox from '$/ox'
import $ from '$/jquery'
import { gt } from 'gettext'

import AutonotifyModel from '@/io.ox.public-sector/autonotify/model'

const moduleReady = api.getConfig()

moduleReady.then(config => {
  if (!config.actioncmds.some(a => a.id === 'notify')) return

  add({
    id: 'AUTONOTIFY',
    text: gt('Notifications'),
    page: st.MAIL,
    section: st.RULES,
    selector: 'io.ox/mail [data-section="io.ox/mail/settings/rules"]',
    priority: 2
  })
})

ext.point('io.ox/mail/settings/rules/buttons').extend({
  id: 'auto-notify',
  after: 'vacation-notice',
  async render (baton) {
    const config = await moduleReady
    if (!config.actioncmds.some(a => a.id === 'notify')) return

    const toggle = createIcon('bi/toggle-on.svg').addClass('me-4 mini-toggle')
    this.append(
      $('<button type="button" class="btn btn-default me-16" data-action="edit-auto-notify">')
        .append(
          toggle.hide(),
          $.txt(gt('Notifications') + ' \u2026')
        )
        .on('click', openDialog)
    )

    // check whether it's active
    const model = new AutonotifyModel()
    model.fetch().done(() => updateToggle(model.isActive()))
    baton.view.listenTo(ox, 'mail:change:auto-notify', updateToggle)

    function updateToggle (active, refresh) {
      toggle.toggle(!!active)
      if (refresh) filter.refresh()
    }
  }
})

function openDialog () {
  ox.load(() => import('@/io.ox.public-sector/autonotify/view')).then(function ({ default: view }) {
    view.open()
  })
}

function onEditAutonotify (e) {
  e.preventDefault()
  openDialog()
}

ext.point('io.ox/settings/mailfilter/filter/settings/actions/autonotify').extend({
  index: 200,
  id: 'actions',
  draw: function (model, config) {
    // call common and then apply changes so that click opens autonotify
    ext.point('io.ox/settings/mailfilter/filter/settings/actions/common')
      .invoke('draw', this, model, config)

    this.find('button[data-action="apply"],[data-action=""]').detach()

    this.find('button[data-action="edit"]')
      .attr('data-action', 'edit-autonotify')
      .on('click', onEditAutonotify)

    // wait for this to get attached to the view's $el
    queueMicrotask(() => {
      this.parent().data('view').listenTo(ox, 'mail:change:auto-notify',
        active => model.set('active', active))
    })
    model.on('change:active', function () {
      ox.trigger('mail:change:auto-notify', !!model.get('active'))
    })
  }
})
