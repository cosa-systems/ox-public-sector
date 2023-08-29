import apps from '$/io.ox/core/api/apps'
import ext from '$/io.ox/core/extensions'

// Nextcloud integration is controlled by the capability filestorage_nextcloud_oauth

ext.point('io.ox.nextcloud/file-picker/options').extend({
  id: 'public-sector',
  after: 'default',
  perform: function (baton) {
    if (!baton.data.accessToken) {
      baton.data.useCookies = true
    }
    return import('@/io.ox.public-sector/ics').then(({ default: ics }) => ics)
  }
})

// Work-around for OXUIB-2522

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
