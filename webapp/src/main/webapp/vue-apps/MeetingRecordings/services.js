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

/** The recordings REST of web-conferencing (Spring MVC, webconferencing WAR). */
const RECORDINGS_URL = '/webconferencing/rest/recordings/byConference';

/** The transcription state REST of documents. */
const TRANSCRIPTIONS_URL = '/documents/rest/transcriptions';

/**
 * Reads the recordings of the call a conference URL names, as the viewer can
 * open them. The server finds the call from the URL, so no provider script is
 * needed on the page.
 * <p>
 * Never rejects on an HTTP refusal: the block shows a state for each one.
 *
 * @param {String} conferenceUrl - the event's conference URL
 * @param {Object} range - {from, to} as ISO 8601 strings, each optional
 * @returns {Promise<Object>} {status, body}: body is the recordings array on
 *          200, the {message} refusal otherwise, or null when unreadable
 */
export function getRecordingsByConference(conferenceUrl, range) {
  const params = new URLSearchParams();
  params.append('url', conferenceUrl);
  if (range && range.from) {
    params.append('from', range.from);
  }
  if (range && range.to) {
    params.append('to', range.to);
  }
  return fetch(`${RECORDINGS_URL}?${params.toString()}`, {
    method: 'GET',
    credentials: 'include',
  }).then(resp => resp.json()
    .catch(() => null)
    .then(body => ({status: resp.status, body})));
}

/**
 * Reads the transcription state of a recording. Resolves with null whatever
 * goes wrong (no documents add-on, a refusal, a network error): a transcript
 * the block cannot read is a transcript it does not mention.
 *
 * @param {String} documentId - the recording's document id
 * @returns {Promise<Object>} the state ({available, status, progress, error, ...}) or null
 */
export function getTranscriptionState(documentId) {
  return fetch(`${TRANSCRIPTIONS_URL}/${encodeURIComponent(documentId)}`, {
    method: 'GET',
    credentials: 'include',
  }).then(resp => {
    if (!resp.ok) {
      return null;
    }
    return resp.json();
  })
    .catch(() => null);
}

/**
 * Asks for the transcription of a recording to be run again.
 *
 * @param {String} documentId - the recording's document id
 * @returns {Promise} resolved when the server accepted the request, rejected otherwise
 */
export function retryTranscription(documentId) {
  return fetch(`${TRANSCRIPTIONS_URL}/${encodeURIComponent(documentId)}/retry`, {
    method: 'POST',
    credentials: 'include',
  }).then(resp => {
    if (!resp.ok) {
      throw new Error(`transcription retry refused: ${resp.status}`);
    }
  });
}
