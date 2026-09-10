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
 * The merge, kept pure: agenda says WHEN a visio is, web conferencing says
 * whether anyone is IN it, and neither knows about the other on the server.
 * Everything here is a plain function of its arguments so the three states can
 * be reasoned about (and later tested) without a browser, a REST call or a
 * CometD connection.
 */

/** A call record exists and is started: somebody is actually in the room. */
export const LIVE = 'live';

/**
 * Scheduled to be running right now, but no call was ever started. Said
 * plainly instead of being dressed as LIVE — the M365 widget could not tell
 * the difference and claimed "Happening Now" for an empty room.
 */
export const NOW = 'now';

/** Still to come. */
export const UPCOMING = 'upcoming';

/**
 * A room this user opened on the fly, that nobody is in right now. Said as
 * plainly as NOW is: the link works, the room is empty. It is a state of its
 * own rather than a flavour of LIVE, because "the room is ready" and "people
 * are in the room" are the two different things a host needs to tell apart.
 */
export const READY = 'ready';

/** The call state web conferencing persists for a running call. */
const STATE_STARTED = 'started';

/**
 * How close an upcoming meeting must be before joining it is offered. Arriving
 * a quarter of an hour early is intent; a button on next week's meeting is a
 * mis-click waiting to announce a meeting that is not happening.
 * <p>
 * The same window decides which occurrence of a series a running call is held
 * in, so the two answers cannot disagree: what the drawer calls live is what it
 * lets you join.
 */
export const JOIN_AHEAD_MS = 15 * 60 * 1000;

/**
 * Turns the raw REST payload of one agenda event into the flat shape the
 * drawer works with, or null when the event carries no visio at all.
 *
 * @param {object} event - an EventEntity as returned by the agenda REST
 * @returns {object} the normalized entry, or null if the event has no conference
 */
export function normalizeEvent(event) {
  const conferences = event && event.conferences || [];
  const conference = conferences.filter(c => c && c.url)[0];
  if (!conference) {
    return null;
  }
  const start = parseEventDate(event.start, event.allDay, false);
  const end = parseEventDate(event.end, event.allDay, true);
  if (!start || !end) {
    return null;
  }
  // A recurrent series is returned as one computed event per occurrence, each
  // with id 0, all pointing at the parent and all sharing ONE conference URL.
  // Keying on the id alone would collapse the whole series into one card, and a
  // deep link built on it would carry a dead eventId=0.
  const occurrenceId = event.occurrence && event.occurrence.id || '';
  const eventId = event.id || event.parent && event.parent.id || event.parentId || 0;
  return {
    key: `event-${eventId}-${occurrenceId}`,
    eventId: eventId,
    occurrenceId: occurrenceId,
    title: event.summary || '',
    start: start,
    end: end,
    allDay: !!event.allDay,
    url: conference.url,
    providerType: conference.type || '',
    callId: null,
    state: null,
  };
}

/**
 * Parses an agenda date string into a Date.
 * <p>
 * Timed events come back as RFC-3339 WITH the offset of the requested time
 * zone, so they are absolute instants. All-day events come back as a bare
 * `yyyy-MM-dd`, which `new Date()` would read as UTC midnight and shift by the
 * offset — the very corruption the M365 widget shipped — so those are built
 * explicitly as local day boundaries.
 *
 * @param {string} value - the agenda date string
 * @param {boolean} allDay - whether the event is an all-day one
 * @param {boolean} exclusiveEnd - true when parsing the end of an all-day event
 * @returns {Date} the parsed date, or null when unparseable
 */
export function parseEventDate(value, allDay, exclusiveEnd) {
  if (!value) {
    return null;
  }
  if (allDay) {
    const parts = /^(\d{4})-(\d{2})-(\d{2})/.exec(value);
    if (!parts) {
      return null;
    }
    const day = Number(parts[3]) + (exclusiveEnd && 1 || 0);
    return new Date(Number(parts[1]), Number(parts[2]) - 1, day);
  }
  const date = new Date(value);
  return isNaN(date.getTime()) ? null : date;
}

/**
 * The ids of the calls that are started right now.
 *
 * @param {Array} callStates - what getUserGroupCalls() returned: [{id, state}]
 * @returns {Array} the started call ids
 */
export function startedCallIds(callStates) {
  return (callStates || []).filter(c => c && c.state === STATE_STARTED).map(c => c.id);
}

/**
 * Matches a conference URL against the started calls, without asking any
 * provider.
 * <p>
 * The authoritative join key is the provider's own findCallId(), but a provider
 * only answers on a page where its script happens to be loaded. A call id is
 * always the last path segment of its own call URL, so comparing the started
 * ids against the URL gives the same answer everywhere. Used as the fallback,
 * never as a way to invent a state: a URL that matches nothing simply stays
 * schedule-only.
 *
 * @param {string} url - the conference URL stored by agenda
 * @param {Array} ids - the started call ids
 * @returns {string} the matching call id, or null
 */
export function matchStartedCallId(url, ids) {
  if (!url || !ids || !ids.length) {
    return null;
  }
  const path = url.split('?')[0].split('#')[0];
  return ids.find(id => id && path.endsWith(`/${id}`)) || null;
}

/**
 * The occurrence each started call is being held in, keyed by call id.
 * <p>
 * A recurrent series shares ONE conference URL across every occurrence, so the
 * call id resolved from that URL is the same for all of them and a running call
 * would otherwise light up the whole series as LIVE — next month's occurrence
 * announced as happening now. The call is held in exactly one of them: the
 * nearest in time among those that may hold it at all, which is the occurrence
 * in progress when there is one, the one that just ended when the meeting
 * overruns, and the next one when people join early — within JOIN_AHEAD_MS,
 * never a later date of the series (see canHoldCall below).
 *
 * @param {Array} events - the normalized scheduled entries
 * @param {Array} startedIds - the ids of the started calls
 * @param {number} nowTime - the reference instant, in milliseconds
 * @returns {object} call id -> the key of the entry holding it; a started call
 *          that no occurrence can hold is absent, and shows nowhere
 */
export function liveCallOwners(events, startedIds, nowTime) {
  // Plain maps: a call id is server data, and `in` on an object literal answers
  // true for every name Object.prototype happens to carry.
  const owners = Object.create(null);
  const distances = Object.create(null);
  (events || []).forEach(event => {
    const callId = callIdOf(event, startedIds);
    if (!isStarted(callId, startedIds) || !canHoldCall(event, nowTime)) {
      return;
    }
    const distance = distanceToNow(event, nowTime);
    if (!(callId in owners) || distance < distances[callId]) {
      owners[callId] = event.key;
      distances[callId] = distance;
    }
  });
  return owners;
}

/**
 * The call id one scheduled entry resolves to, running or not.
 * <p>
 * Written once and read by both passes below: an ownership map keyed on a
 * differently resolved id would stop matching the entries it is compared
 * against, and a whole series would quietly lose its live state with nothing
 * to show for it.
 *
 * @param {object} event - a normalized event entry
 * @param {Array} startedIds - the ids of the started calls
 * @returns {string} the call id, or null
 */
function callIdOf(event, startedIds) {
  return event.callId || matchStartedCallId(event.url, startedIds);
}

/**
 * Whether that call is one of the running ones.
 *
 * @param {string} callId - the resolved call id, may be null
 * @param {Array} startedIds - the ids of the started calls
 * @returns {boolean} true when the call is started
 */
function isStarted(callId, startedIds) {
  return !!callId && (startedIds || []).indexOf(callId) >= 0;
}

/**
 * Whether an occurrence can be the one a running call is being held in: it has
 * begun, or is close enough that people are joining early. A later date the
 * same series happens to have is not a candidate, however lonely the call is —
 * claiming it would announce next week's meeting as happening now, which is the
 * whole point of the attribution.
 * <p>
 * The past side needs no bound of its own: the drawer reads only PAST_WINDOW_MS
 * back, so an occurrence old enough to be a stale claim is never fetched.
 *
 * @param {object} event - a normalized event entry
 * @param {number} nowTime - the reference instant, in milliseconds
 * @returns {boolean} true when the occurrence may hold the call
 */
function canHoldCall(event, nowTime) {
  return event.start.getTime() - nowTime <= JOIN_AHEAD_MS;
}

/**
 * How far one occurrence is from now: nothing while it is being held, the wait
 * before it starts, or the time since it ended.
 *
 * @param {object} event - a normalized event entry
 * @param {number} nowTime - the reference instant, in milliseconds
 * @returns {number} the distance in milliseconds
 */
function distanceToNow(event, nowTime) {
  const start = event.start.getTime();
  const end = event.end.getTime();
  if (start <= nowTime && end > nowTime) {
    return 0;
  }
  return start > nowTime ? start - nowTime : nowTime - end;
}

/**
 * The tri-state model, built from what each side actually knows.
 * <p>
 * Ended entries are dropped rather than falling through to "upcoming" (the
 * M365 widget's fall-through bug). An event whose conference has no call record
 * at all — created through MCP, or a hand-pasted URL — stays schedule-only with
 * a working Join; it is never promoted to LIVE.
 *
 * @param {object} args - {events, startedIds, adhocCalls, instant, now}
 * @returns {Array} the drawer entries, sorted by state then by start time
 */
export function buildEntries({events, startedIds, adhocCalls, instant, now}) {
  const nowTime = (now || new Date()).getTime();
  // The rooms opened from this drawer are listed from what the browser
  // remembered, which carries their invitation link. They are also, while
  // running, ordinary started calls, so they are claimed here first and left
  // out of the ad-hoc pass below rather than shown twice.
  const entries = (instant || []).slice();
  const matched = entries.map(entry => entry.callId);
  const owners = liveCallOwners(events, startedIds, nowTime);
  (events || []).forEach(event => {
    const callId = callIdOf(event, startedIds);
    const started = isStarted(callId, startedIds);
    if (started) {
      // Claimed by the series as a whole: whichever occurrence holds the call,
      // it is a scheduled one and must not be listed again as an ad-hoc call.
      matched.push(callId);
    }
    const live = started && owners[callId] === event.key;
    const state = eventState(event, live, nowTime);
    if (state) {
      entries.push(Object.assign({}, event, {
        callId: callId,
        state: state,
        // What agenda stored already IS the shareable link: the conference URL
        // it saved carries the call's invitation id, so forwarding it to
        // somebody outside works without creating anything.
        shareUrl: event.url,
      }));
    }
  });
  // A call started from a space button or a 1-1 call has no scheduled
  // counterpart: it belongs under LIVE on its own, with whatever the call
  // record itself says about it.
  (adhocCalls || []).filter(call => matched.indexOf(call.id) < 0).forEach(call => entries.push({
    key: `call-${call.id}`,
    eventId: 0,
    title: call.title || '',
    start: call.startDate && new Date(call.startDate) || null,
    end: null,
    allDay: false,
    url: null,
    shareUrl: '',
    providerType: call.providerType || '',
    callId: call.id,
    state: LIVE,
  }));
  return entries.sort(compareEntries);
}

/**
 * The state one scheduled entry is in, or null when it is over.
 *
 * @param {object} event - a normalized event entry
 * @param {boolean} live - whether its call is started
 * @param {number} nowTime - the reference instant, in milliseconds
 * @returns {string} LIVE, NOW, UPCOMING, or null for an ended entry
 */
function eventState(event, live, nowTime) {
  if (live) {
    return LIVE;
  } else if (event.start.getTime() > nowTime) {
    return UPCOMING;
  } else if (event.end.getTime() > nowTime) {
    return NOW;
  } else {
    return null;
  }
}

/**
 * Orders the drawer: what is running first, what should be running next, then
 * the timeline.
 *
 * @param {object} first - an entry
 * @param {object} second - another entry
 * @returns {number} the comparison result
 */
function compareEntries(first, second) {
  const rank = {[LIVE]: 0, [READY]: 1, [NOW]: 2, [UPCOMING]: 3};
  if (rank[first.state] !== rank[second.state]) {
    return rank[first.state] - rank[second.state];
  }
  const firstTime = first.start && first.start.getTime() || 0;
  const secondTime = second.start && second.start.getTime() || 0;
  // Rooms you opened read backwards from the rest: the one you just made is
  // the one you are about to send, whereas the next meeting is the next thing
  // that happens to you.
  return first.state === READY && secondTime - firstTime || firstTime - secondTime;
}
