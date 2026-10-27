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

/** The bundle holding the line's labels, merged into the platform's shared i18n. */
const BUNDLE = 'locale.portlet.visio.Visio';

/**
 * Contributes the « Meeting » line to a file's details: the extension point
 * of the details rows (app DocumentsInfoDetails, type documents-info-details). The
 * panel hands the props file, documentId and mimetype; the line decides from
 * them whether it shows.
 *
 * @returns {void}
 */
export function registerRecordingMeeting() {
  extensionRegistry.registerComponent('DocumentsInfoDetails', 'documents-info-details', {
    id: 'webconferencing-recording-meeting',
    vueComponent: Vue.options.components['webconferencing-recording-meeting'],
    rank: 10,
  });
}

/**
 * Loads the line's labels into the platform's shared i18n, which the details
 * panel renders with.
 *
 * @returns {Promise} resolved once the labels are loaded, even when they could not be
 */
export function loadRecordingMeetingLabels() {
  const lang = eXo.env.portal.language;
  const url = `${eXo.env.portal.context}/${eXo.env.portal.rest}/i18n/bundle/${BUNDLE}-${lang}.json`;
  return new Promise(resolve => window.require(['SHARED/eXoVueI18n'], exoi18n => exoi18n.loadLanguageAsync(lang, url)
    .catch(() => null)
    .then(resolve)));
}
