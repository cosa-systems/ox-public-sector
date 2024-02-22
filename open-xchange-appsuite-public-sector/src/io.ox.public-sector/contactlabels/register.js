import ox from '$/ox'
import model from '$/io.ox/contacts/model'
import View from '$/io.ox/contacts/edit/view'

import { settings } from '@/io.ox.public-sector/settings'

const language = ox.language
const lang = ox.language.match(/^[a-z]+/)?.[0] || 'default'

const labels = settings.get('contactlabels') || {}
if (labels && typeof labels === 'object') {
  for (let [id, label] of Object.entries(labels)) {
    if (label && typeof label === 'object') {
      label = label[language] || label[lang] || label.default
    }
    if (label) model.fields[id] = View.i18n[id] = label
  }
}
