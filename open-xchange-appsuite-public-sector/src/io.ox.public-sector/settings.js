import { Settings } from '$/io.ox/core/settings'

export const defaults = {
  autonotify: {
    subject: {
      default: 'There are new items in your mailbox',
      de_DE: 'Es liegen neue Nachrichten in Ihrem Postfach'
    },
    body: {
      default: 'There are new items in your mailbox',
      de_DE: 'Es liegen neue Nachrichten in Ihrem Postfach'
    }
  }
}

export const settings = new Settings('io.ox.public-sector', () => ({}))
