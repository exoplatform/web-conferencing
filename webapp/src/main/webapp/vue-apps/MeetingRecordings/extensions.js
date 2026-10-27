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

/** The bundle holding the block's labels, merged into the platform's shared i18n. */
const BUNDLE = 'locale.portlet.visio.Visio';

/**
 * Contributes the Meeting block to the agenda drawer: the extension point
 * right after an event's conference block (app AgendaEvent, type
 * agenda-event-body). The drawer hands the props event and isPast; the block
 * decides what it shows from them.
 *
 * @returns {void}
 */
export function registerMeetingBlock() {
  extensionRegistry.registerComponent('AgendaEvent', 'agenda-event-body', {
    id: 'webconferencing-meeting-recordings',
    vueComponent: Vue.options.components['webconferencing-meeting-recordings'],
    rank: 10,
  });
}

/**
 * Loads the block's labels into the platform's shared i18n, which the agenda
 * application renders with.
 *
 * @returns {Promise} resolved once the labels are loaded, even when they could not be
 */
export function loadMeetingBlockLabels() {
  const lang = eXo.env.portal.language;
  const url = `${eXo.env.portal.context}/${eXo.env.portal.rest}/i18n/bundle/${BUNDLE}-${lang}.json`;
  return new Promise(resolve => window.require(['SHARED/eXoVueI18n'], exoi18n => exoi18n.loadLanguageAsync(lang, url)
    .catch(() => null)
    .then(resolve)));
}
