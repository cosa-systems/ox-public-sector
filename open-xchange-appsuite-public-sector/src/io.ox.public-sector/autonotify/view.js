import ModalView from '$/io.ox/backbone/views/modal'
import mini from '$/io.ox/backbone/mini-views'
import { input } from '$/io.ox/core/settings/util'
import ext from '$/io.ox/core/extensions'
import yell from '$/io.ox/core/yell'
import { gt } from 'gettext'
import _ from '$/underscore'

import Model from '@/io.ox.public-sector/autonotify/model'
import '@/io.ox.public-sector/autonotify/style.scss'

function open () {
  return getData().then(openModalDialog, fail)
}

function fail (e) {
  if (e.code === 'MAIL_FILTER-0025') { // error for mailto (invalid sieve)
    yell('error', gt('The notification address is not valid.'))
    throw e
  }

  yell('error', e.code === 'MAIL_FILTER-0015'
    ? gt('Unable to load mail filter settings.')
    : gt('Unable to load your notification settings. Please retry later.')
  )
  throw e
}

function openModalDialog (data) {
  return new ModalView({
    async: true,
    focus: 'input[name="active"]',
    model: data.model,
    point: 'io.ox/mail/auto-notify/edit',
    title: gt('Notifications'),
    width: 640
  })
    .inject({
      updateActive () {
        const enabled = this.model.get('active')
        this.$body.toggleClass('disabled', !enabled).find(':input').prop('disabled', !enabled)
      },
      manageSaveButton () {
        const saveButton = this.$footer.find('[data-action="save"]').first()
        const field = this.$body.find('input[name="to"]').first()

        const setStatus = () => {
          saveButton.attr('disabled', field.val().trim() === '' && (!this.model.get('id') || this.model.get('active')))
        }

        if (field.val().trim() === '' && !this.model.get('active')) saveButton.attr('disabled', true)

        field.on('keyup', setStatus)

        this.model.on('change:active', setStatus)
      }
    })
    .build(function () {
      this.data = data
      this.$el.addClass('autonotify-dialog')
    })
    .addCancelButton()
    .addButton({ label: gt('Apply changes'), action: 'save' })
    .on('open', function () {
      this.updateActive()
      this.manageSaveButton()
    })
    .on('save', function () {
      if (this.model.get('id') !== undefined && this.model.get('to') === undefined) {
        this.model.destroy().done(this.close).fail(this.idle).fail(yell)
      } else {
        this.model.save().done(this.close).fail(this.idle).fail(yell)
      }
    })
    .open()
}

ext.point('io.ox/mail/auto-notify/edit').extend(
  {
    index: 100,
    id: 'switch',
    render () {
      const switchView = new mini.SwitchView({
        name: 'active',
        model: this.model,
        label: gt('Notifications'),
        size: _.device('smartphone') ? 'small' : 'large'
      }).render()
      switchView.$el.attr('title', gt('Enable or disable notifications'))
      switchView.$el.find('span').addClass('sr-only')

      this.$header.prepend(switchView.$el)

      this.listenTo(this.model, 'change:active', this.updateActive)
    }
  },
  {
    index: 200,
    id: 'to',
    render () {
      this.$body.append(input('to', gt('Send a notification of all incoming emails to this address'), this.model))
    }
  }

)

//
// Get required data
//
function getData () {
  const model = new Model()
  return model.fetch().then(function () {
    return { model }
  })
}

export default { open }
