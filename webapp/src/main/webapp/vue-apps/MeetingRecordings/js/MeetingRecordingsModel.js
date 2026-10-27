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
 * The decisions of the Meeting block, kept free of Vue and of the network so
 * that each one reads, and can be run, on its own.
 */

/** The block shows nothing: no conference, or the occurrence is not over. */
export const STATE_HIDDEN = 'hidden';

/** The recordings are being read. */
export const STATE_LOADING = 'loading';

/** At least one recording. */
export const STATE_READY = 'ready';

/** No recording, and the meeting ended too long ago for one to be on its way. */
export const STATE_NOT_RECORDED = 'notRecorded';

/** No recording yet, but the meeting ended a moment ago: it may still be uploading. */
export const STATE_PROCESSING = 'processing';

/** The conference is not held on a provider of this platform (Teams, Zoom, a typed link). */
export const STATE_OTHER_PROVIDER = 'otherProvider';

/** The viewer is not a member of the event's space. */
export const STATE_NOT_MEMBER = 'notMember';

/** The viewer may not read this call's recordings for another reason. */
export const STATE_FORBIDDEN = 'forbidden';

/** The recordings could not be read. */
export const STATE_ERROR = 'error';

/**
 * How long after the end of an occurrence an empty answer still reads as « the
 * recording is being processed »: Jibri finalises and uploads the file once
 * the call is over, which takes a few minutes for a long meeting.
 */
export const PROCESSING_WINDOW_MS = 15 * 60 * 1000;

/**
 * How long a transcription may stay queued or in PROCESSING without an update
 * before it is no longer moving: the documents transcript drawer and the
 * server's own TranscriptionState.isInProgress apply the same hour, and the
 * server accepts a retry of such a transcription.
 */
export const TRANSCRIPT_STALE_MS = 60 * 60 * 1000;

/** The refusal codes of the recordings REST that this block tells apart. */
const CODE_OTHER_PROVIDER = 'webconferencing.recordings.otherProvider';
const CODE_CALL_NOT_FOUND = 'webconferencing.recordings.callNotFound';
const CODE_NOT_SPACE_MEMBER = 'webconferencing.recordings.notSpaceMember';

/**
 * The conference URL of an event: its first conference's, as the agenda drawer
 * shows a single one.
 *
 * @param {Object} event - the agenda event of the drawer
 * @returns {String} the URL, or null when the event has no conference
 */
export function getConferenceUrl(event) {
  const conference = event && event.conferences && event.conferences.length && event.conferences[0];
  const url = conference && conference.url;
  return url && String(url).trim() || null;
}

/**
 * Whether the event is an occurrence of a recurring series: then its
 * recordings are those of the occurrence opened.
 *
 * @param {Object} event - the agenda event of the drawer
 * @returns {Boolean} true for an occurrence or a recurring event
 */
export function isRecurring(event) {
  return !!(event && (event.occurrence || event.parent || event.recurrence));
}

/**
 * Reads a date the agenda drawer holds: a Date, a time in milliseconds, or a
 * REST string, which agenda gives as a wall-clock time in the viewer's zone
 * (its own toDate drops the offset the same way).
 *
 * @param {Date|Number|String} value - the date
 * @returns {Date} the date, or null when it is missing or unreadable
 */
export function toDate(value) {
  if (value === null || typeof value === 'undefined' || value === '') {
    return null;
  }
  let date;
  if (typeof value === 'string' && value.indexOf('T') === 10 && value.length > 19) {
    date = new Date(value.substring(0, 19));
  } else if (typeof value === 'string' && value.length === 10) {
    date = new Date(`${value}T00:00:00`);
  } else {
    date = new Date(value);
  }
  return Number.isNaN(date.getTime()) ? null : date;
}

/**
 * The instants an occurrence spans. An all-day event has dates without time
 * whose end day is included, so it runs from the start day's midnight to the
 * midnight after its end day, in the viewer's zone.
 *
 * @param {Object} event - the agenda event of the drawer
 * @returns {Object} {start, end} as Dates, or null when a date is missing
 */
export function getOccurrenceBounds(event) {
  if (!event) {
    return null;
  }
  const start = toDate(event.startDate || event.start);
  const end = toDate(event.endDate || event.end);
  if (!start || !end) {
    return null;
  }
  if (event.allDay) {
    return {
      start: new Date(start.getFullYear(), start.getMonth(), start.getDate()),
      end: new Date(end.getFullYear(), end.getMonth(), end.getDate() + 1),
    };
  }
  return {start, end};
}

/**
 * The time window the recordings are asked for. A single event has none:
 * every recording of its call is its own. An
 * occurrence asks for its start to its end; the server adds the configured
 * grace to the end, so a recording saved after a meeting that overran still
 * counts.
 *
 * @param {Object} event - the agenda event of the drawer
 * @returns {Object} {from, to} as ISO 8601 strings, or an empty object for no window
 */
export function getRecordingsWindow(event) {
  if (!isRecurring(event)) {
    return {};
  }
  const bounds = getOccurrenceBounds(event);
  if (!bounds) {
    return {};
  }
  return {
    from: bounds.start.toISOString(),
    to: bounds.end.toISOString(),
  };
}

/**
 * The state an answer of the recordings REST puts the block in.
 *
 * @param {Number} status - the HTTP status
 * @param {Array|Object} body - the parsed body: the recordings, or {message}
 * @param {Object} event - the agenda event of the drawer
 * @param {Number} now - the current time in milliseconds
 * @returns {String} one of the STATE_* constants
 */
export function stateFromResponse(status, body, event, now) {
  if (status === 200) {
    if (Array.isArray(body) && body.length) {
      return STATE_READY;
    }
    return endedRecently(event, now) ? STATE_PROCESSING : STATE_NOT_RECORDED;
  }
  const code = body && body.message;
  if (status === 404 && code === CODE_OTHER_PROVIDER) {
    return STATE_OTHER_PROVIDER;
  }
  if (status === 404 && code === CODE_CALL_NOT_FOUND) {
    // The meeting room was never opened: nobody joined, nothing was recorded
    return endedRecently(event, now) ? STATE_PROCESSING : STATE_NOT_RECORDED;
  }
  if (status === 403) {
    return code === CODE_NOT_SPACE_MEMBER ? STATE_NOT_MEMBER : STATE_FORBIDDEN;
  }
  return STATE_ERROR;
}

/**
 * Whether the occurrence ended less than PROCESSING_WINDOW_MS ago.
 *
 * @param {Object} event - the agenda event of the drawer
 * @param {Number} now - the current time in milliseconds
 * @returns {Boolean} true while a recording may still be on its way
 */
export function endedRecently(event, now) {
  const bounds = getOccurrenceBounds(event);
  if (!bounds) {
    return false;
  }
  const end = bounds.end.getTime();
  return end <= now && now - end < PROCESSING_WINDOW_MS;
}

/**
 * The name of the space an event belongs to, for « you are not a member of
 * space X ».
 *
 * @param {Object} event - the agenda event of the drawer
 * @returns {String} the space display name, or its pretty name, or null
 */
export function getSpaceName(event) {
  const owner = event && event.calendar && event.calendar.owner;
  if (!owner) {
    return null;
  }
  const profile = owner.profile || owner.space;
  return profile && (profile.displayName || profile.fullname || profile.fullName)
    || owner.displayName
    || owner.remoteId
    || null;
}

/**
 * Reads the transcript chip a transcription state calls for, or none. The
 * documents REST says available=false where no transcriber is installed
 * (community): then, as for any failure to read it, nothing is shown. A run
 * left in PROCESSING with no update for TRANSCRIPT_STALE_MS, or none at all,
 * was lost: it reads as failed, with the reason stopped, and can be retried.
 * A transcription queued that long without an error still reads as queued,
 * and can be retried too.
 *
 * @param {Object} state - the body of GET /documents-portlet/rest/transcriptions/{id}, or null
 * @param {Number} now - the current time in milliseconds
 * @returns {Object} {kind, progress, error, retryable} with kind one of queued, processing, ready, failed; or null
 */
export function transcriptChip(state, now) {
  if (!state || state.available !== true || !state.status) {
    return null;
  }
  const status = String(state.status).toUpperCase();
  const updated = typeof state.updated === 'number' ? state.updated : 0;
  const stale = !updated || now - updated >= TRANSCRIPT_STALE_MS;
  if (status === 'QUEUED') {
    return stale && !state.error ? {kind: 'queued', retryable: true} : {kind: 'queued'};
  }
  if (status === 'PROCESSING') {
    if (stale) {
      return {kind: 'failed', error: 'stopped'};
    }
    return {kind: 'processing', progress: state.progress || null};
  }
  if (status === 'DONE') {
    return {kind: 'ready'};
  }
  if (status === 'FAILED') {
    return {kind: 'failed', error: state.error || null};
  }
  return null;
}

/**
 * When a recording started, in milliseconds: the start the recorder wrote in
 * its file name, else the moment the file was saved.
 *
 * @param {Object} recording - a recording of the recordings REST
 * @returns {Number} the time, or 0 when unknown
 */
export function recordingStartTime(recording) {
  const date = recording && (recording.startedDate || recording.recordedDate);
  const time = date ? new Date(date).getTime() : 0;
  return time && !Number.isNaN(time) ? time : 0;
}

/**
 * How long a recording lasts, in seconds: the length the server read from
 * its file when it was saved, else the length its transcription measured.
 *
 * @param {Object} recording - a recording of the recordings REST
 * @param {Object} transcript - its transcription state, or null
 * @returns {Number} the duration, or null when neither tells it
 */
export function recordingDuration(recording, transcript) {
  const stored = recording && recording.durationSeconds;
  if (typeof stored === 'number' && stored > 0) {
    return stored;
  }
  const measured = transcript && transcript.available === true && transcript.durationSeconds;
  return typeof measured === 'number' && measured > 0 ? measured : null;
}

/**
 * Splits a duration the way the block writes it: seconds under a minute,
 * minutes under an hour, then hours and two-digit minutes (« 1 h 05 »).
 * Minutes are rounded to the nearest one.
 *
 * @param {Number} seconds - the duration
 * @returns {Object} {unit, value, minutes}: unit is seconds, minutes or
 *          hours; minutes is the two-digit remainder for hours; or null when
 *          the duration is unknown
 */
export function durationParts(seconds) {
  if (typeof seconds !== 'number' || !Number.isFinite(seconds) || seconds <= 0) {
    return null;
  }
  if (seconds < 59.5) {
    return {unit: 'seconds', value: Math.max(1, Math.round(seconds))};
  }
  const totalMinutes = Math.round(seconds / 60);
  if (totalMinutes < 60) {
    return {unit: 'minutes', value: totalMinutes};
  }
  return {
    unit: 'hours',
    value: Math.floor(totalMinutes / 60),
    minutes: String(totalMinutes % 60).padStart(2, '0'),
  };
}

/**
 * Formats a size in bytes the way a file list shows it: one decimal under
 * ten units, none above.
 *
 * @param {Number} bytes - the size, -1 or absent when unknown
 * @returns {String} the size with its unit, or null when unknown
 */
export function formatSize(bytes) {
  if (typeof bytes !== 'number' || bytes < 0) {
    return null;
  }
  const units = ['B', 'KB', 'MB', 'GB'];
  let value = bytes;
  let unit = 0;
  while (value >= 1024 && unit < units.length - 1) {
    value /= 1024;
    unit++;
  }
  const digits = unit > 0 && value < 10 ? 1 : 0;
  return `${value.toFixed(digits)} ${units[unit]}`;
}

/**
 * Reads the text of a transcript from the documents REST answer, a JSON
 * object carrying it in text or the plain text itself, with its white space
 * collapsed for a short excerpt.
 *
 * @param {Object|String} body - the answer of GET /documents-portlet/rest/transcriptions/{id}/text
 * @returns {String} the text, or null when there is none
 */
export function transcriptText(body) {
  const text = body && typeof body === 'object' ? body.text : body;
  const collapsed = typeof text === 'string' ? text.replace(/\s+/g, ' ').trim() : '';
  return collapsed || null;
}
