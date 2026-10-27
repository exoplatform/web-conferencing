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

/** The owner type of the call of an agenda event. */
export const EVENT_CALL_OWNER_TYPE = 'space_event';

/**
 * Reads the call a file was recorded in, as the server stamps it on the
 * recording: the identifier, provider and owner type of the call, and when
 * the recording started and was saved.
 *
 * @param {string} documentId the file
 * @returns {Promise<Object>} the call, or null when the file is not a stamped
 *          recording the user can read
 */
export function getRecordingCall(documentId) {
  return fetch(`/webconferencing/rest/recordings/documents/${encodeURIComponent(documentId)}/call`, {
    credentials: 'include',
    method: 'GET',
  }).then(resp => readOrNull(resp));
}

/**
 * Reads the agenda event a call was held for, as the user may read it. The
 * moment asked about is when the recording was saved, and the grace the one
 * the recordings of an occurrence are listed with: the line opens the
 * occurrence whose recordings list holds the recording. A recording whose
 * save date is unknown is asked about by its start.
 *
 * @param {Object} call the call of the recording
 * @returns {Promise<Object>} the event, with its title, dates and url, or
 *          null when no event the user can read refers to the call
 */
export function getCallEvent(call) {
  const params = new URLSearchParams({
    providerType: call.providerType,
    callId: call.callId,
    moment: call.recordedDate || call.startedDate,
  });
  if (call.graceHours >= 0) {
    params.set('graceHours', call.graceHours);
  }
  return fetch(`/agenda/rest/conference-event?${params}`, {
    credentials: 'include',
    method: 'GET',
  }).then(resp => readOrNull(resp));
}

/**
 * Finds the agenda event a recording was made in. Only the call of an agenda
 * event is looked up in the agenda; any other call, or a call whose provider
 * or date is unknown, has none.
 *
 * @param {string} documentId the recording's file
 * @returns {Promise<Object>} the event, or null
 */
export function getRecordingEvent(documentId) {
  return getRecordingCall(documentId)
    .then(call => {
      if (isEventCall(call)) {
        return getCallEvent(call);
      }
      return null;
    });
}

/**
 * Tells whether a recording's call is the call of an agenda event that can be
 * looked up.
 *
 * @param {Object} call the call of the recording, or null
 * @returns {boolean} true when the agenda is worth asking
 */
export function isEventCall(call) {
  return !!call
    && call.ownerType === EVENT_CALL_OWNER_TYPE
    && !!call.callId
    && !!call.providerType
    && !!(call.startedDate || call.recordedDate);
}

/**
 * Reads a JSON answer: a body on 200, null on 204 or on any refusal, the line
 * being an extra that shows only when everything answers.
 *
 * @param {Response} resp the answer
 * @returns {Promise<Object>} the body, or null
 */
function readOrNull(resp) {
  if (resp && resp.ok && resp.status !== 204) {
    return resp.json();
  }
  return null;
}
