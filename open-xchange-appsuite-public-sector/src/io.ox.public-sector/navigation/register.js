import $ from '$/jquery'
import _ from '$/underscore'
import ox from '$/ox'

import apps from '$/io.ox/core/api/apps'
import ext from '$/io.ox/core/extensions'
import appcontrol from '$/io.ox/core/main/appcontrol'

import ics from '@/io.ox.public-sector/ics'
import { settings } from '@/io.ox.public-sector/settings'
import '@/io.ox.public-sector/navigation/style.scss'

const config = ics.then(function (ics) {
  return $.ajax(ics.url + 'navigation.json?language=' +
              ox.language.replace('_', '-'), {
    xhrFields: { withCredentials: true },
    dataType: 'json'
  })
})
const images = settings.get('navigation/color', true)
const oxTab = settings.get('navigation/oxtabname', 'ox')

function getApp (link) {
  const match = /\bapp=([\w./-]+)/.exec(link)
  return match && match[1]
}

function icon (url, launcher) {
  if (images && !launcher) {
    return $('<img class="public-sector-launcher">')
      .css('background', 'none')
      .attr('src', url)
  }

  const mask = `url(${encodeURI(url)}) center ${launcher ? '' : '/ contain'}`
  return $('<span class="public-sector-launcher">').css({
    mask,
    '-webkit-mask': mask
  })
}

// Override launcher icon
ox.ui.appIcons.launcher = icon('io.ox.public-sector/navigation/launcher.svg', true)

config.then(function (config) {
  // Override app icons
  _.each(config.categories, function (category) {
    _.each(category.entries, function (entry) {
      if (entry.target !== oxTab) return
      const id = getApp(entry.link)
      const app = apps.get(id)
      ox.ui.appIcons[id] = icon(entry.icon_url)
      if (app) app.set('icon', ox.ui.appIcons[id])
    })
  })
  ext.point('io.ox/core/main/icons').get('mapping').run()
})

const LaunchersView = appcontrol.LaunchersView.extend({
  initialize () {
    appcontrol.LaunchersView.prototype.initialize.apply(this, arguments)

    // Move dropdown to the left (align with right edge of launcher)
    this.$ul.addClass('dropdown-menu-right')

    // Patch toggle icon
    const title = this.$toggle.children().attr('title')
    this.$toggle.empty()
      .append($(ox.ui.appIcons.launcher).attr('title', title))

    this.update()
  },
  update () {
    config.then(config => {
      this.$ul.empty()

      // Add configured apps
      _.each(config.categories, createCategory, this)

      // draw custom launchers. Some items that appear in the launcher are not full apps, like the enterprise picker dialog
      this.divider()
      ext.point('io.ox/core/appcontrol/customLaunchers').invoke('draw', this.$ul)

      // Check for compose apps on mobile
      if (!_.device('smartphone')) return
      const closable = this.collection.where({ closable: true })

      if (closable.length) {
        // Add compose apps
        this.divider()
        closable.forEach(model => {
          this.$apps.append(new appcontrol.LauncherView({ model }).render().$el)
        })
      }

      // Add menu on mobile
      this.$ul.append(this.$menu)
    }, () => {
      appcontrol.LaunchersView.prototype.update.call(this)
    })
  }
})

function createCategory (category) {
  const group = !!category.display_name
  if (group) this.group(category.display_name)
  _.each(category.entries, function (entry) {
    const makeApp = entry.target === oxTab ? ownApp : foreignApp
    this.append(makeApp(entry))
  }, this)
}

function ownApp (entry) {
  const model = apps.get(getApp(entry.link))
  if (entry.display_name) model.set('title', entry.display_name)
  const launcher = new appcontrol.LauncherView({ model }).render()
  launcher.$el.find('.icon-background').remove()
  // launcher.$icon.removeAttr('style')
  return launcher.$el
}

function foreignApp (entry) {
  return $('<button tabindex="-1" role="menuitem" class="btn btn-toolbar btn-topbar">')
    .append(
      $('<div class="lcell">').append(
        $('<div class="icon-wrap flex-center">').append($(icon(entry.icon_url))),
        $('<div class="title">').text(entry.display_name)
      )
    ).on('click', e => window.open(entry.link, entry.target))
}

ext.point('io.ox/core/appcontrol/left').replace({
  id: 'launcher',
  draw: function () {
    const taskbar = $('<ul class="taskbar list-unstyled m-0" role="toolbar">')
    const appLauncher = window.launchers = new LaunchersView({ collection: apps })
    taskbar.append(appLauncher.render().$el)
    this.append(taskbar)
  }
})

ext.point('io.ox/core/appcontrol/right').disable('launcher')
