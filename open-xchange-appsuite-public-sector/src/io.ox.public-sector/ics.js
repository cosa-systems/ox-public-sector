import $ from '$/jquery'

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

export default promise
