/*
 * Copyright (C) 2026 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <gnu.org/licenses>.
 */

/*
 * The Visio quick action. Its id is the same string as the DRAWER application's
 * url in applications.json ("visio"): that is how the applications launcher
 * binds the catalog entry to this click.
 */
extensionRegistry.registerExtension('QuickAction', 'Extension', {
  id: 'visio',
  icon: 'fa-video',
  name: 'quickActions.visio.name',
  description: 'quickActions.visio.description',
  click: () => {
    window.require(['SHARED/eXoVueI18n'], exoi18n => initVisioDrawer(exoi18n));
  },
});

/*
 * Opens the visio drawer from anywhere in the platform — Chat, Agenda, a
 * notification — without the caller knowing how this app is mounted. Same
 * cross-app contract as 'open-email-box-mail'.
 */
document.addEventListener('open-visio-drawer', () => {
  window.require(['SHARED/eXoVueI18n'], exoi18n => initVisioDrawer(exoi18n));
});

/**
 * Mounts the app on first use, then asks it to open its drawer.
 * <p>
 * Nothing of this app — nor the web conferencing core it later needs — is on a
 * page until somebody actually clicks.
 *
 * @param {object} exoi18n - the platform i18n loader
 * @returns {Promise} resolved once the open event is dispatched
 */
async function initVisioDrawer(exoi18n) {
  const appId = 'visio-drawer-quick-action';
  if (!document.querySelector(`#${appId}`)) {
    const parent = document.createElement('div');
    parent.id = appId;
    document.querySelector('#vuetify-apps').appendChild(parent);
    await initVisioDrawerApp(appId, exoi18n);
  }
  document.dispatchEvent(new CustomEvent('quick-action-visio-drawer'));
}

/**
 * Builds the visio Vue app into the given mount point, with its translations
 * loaded.
 *
 * @param {string} appId - the mount element id
 * @param {object} exoi18n - the platform i18n loader
 * @returns {Promise} resolved when the app is mounted
 */
function initVisioDrawerApp(appId, exoi18n) {
  const lang = eXo.env.portal.language;
  const url = `${eXo.env.portal.context}/${eXo.env.portal.rest}/i18n/bundle/locale.portlet.visio.Visio-${lang}.json`;
  return new Promise(resolve => exoi18n.loadLanguageAsync(lang, url)
    .then(i18n => Vue.createApp({
      template: `<visio-drawer-app id="${appId}" />`,
      mounted() {
        document.dispatchEvent(new CustomEvent('hideTopBarLoading'));
        resolve();
      },
      vuetify: Vue.prototype.vuetifyOptions,
      i18n,
    }, `#${appId}`, 'Visio Quick Action')));
}
