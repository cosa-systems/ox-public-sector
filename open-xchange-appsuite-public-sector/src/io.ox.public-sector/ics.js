import $ from '$/jquery'

import apps from '$/io.ox/core/api/apps'
import ext from '$/io.ox/core/extensions'
import { settings } from '@/io.ox.public-sector/settings'

const session = $.Deferred()
const promise = session.promise()
const icsURL = new URL(settings.get('ics/url', location.origin))

$(window).on('message', messageHandler)

const iframe = $('<iframe style="display: none">')
  .on('load', () => {
    setTimeout(() => session.reject(false), settings.get('ics/timeout', 10000))
  })
  .attr('src', icsURL + 'silent')
  .appendTo(document.body)

function messageHandler (e) {
  if (e.originalEvent.origin !== icsURL.origin) return

  const data = e.originalEvent.data
  if (data.loggedIn) {
    session.resolve({
      csrfToken: data.csrftoken,
      url: icsURL
    })
  } else {
    session.reject(false)
  }
  $(window).off('message', messageHandler)
  iframe.remove()
}

// This is executed in the core namespace thanks to navigation.
// Once there is more generic code, it can move into a separate file.
ext.point('io.ox.nextcloud/file-picker/options').extend({
  id: 'public-sector',
  after: 'default',
  perform: function (baton) {
    if (!baton.data.accessToken) {
      baton.data.useCookies = true
    }
    return promise
  }
})

function observe (header) {
  const mo = new MutationObserver(() => {
    mo.disconnect()
    const view = header.find('input[name="searchQuery"]').data('view')
    view.model.get('contacts').comparator = 'sort_name'
  })
  mo.observe(header.get(0), { childList: true })
}

import('$/io.ox/contacts/enterprisepicker/dialog').then(({ default: api }) => {
  const open = api.open
  api.open = (callback, options) => {
    const result = open(callback, options)
    if (options.selection.behavior !== 'none') {
      // dialog
      observe(result.$header)
    } else {
      // app
      result.then(() => {
        const app = apps.find(app => app.get('name') === 'io.ox/contacts/enterprisepicker')
        observe(app.get('window').nodes.main)
      })
    }
    return result
  }
})

export default promise
