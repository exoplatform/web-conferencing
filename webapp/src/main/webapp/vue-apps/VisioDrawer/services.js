/*
 * Copyright (C) 2026 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

import {normalizeEvent, startedCallIds, buildEntries} from './js/VisioMerge.js';

/** How far back the schedule is read: enough to catch a meeting already running. */
const PAST_WINDOW_MS = 4 * 60 * 60 * 1000;

/** How far ahead the schedule is read. */
const FUTURE_WINDOW_MS = 7 * 24 * 60 * 60 * 1000;

/** The agenda REST bounds the window by count as well as by date. */
const EVENTS_LIMIT = 100;

/**
 * How long a provider-delegated answer is waited for. The core resolves
 * findCallId()/getCallUrl() through a provider module that is only loaded on
 * pages hosting a call button, and its promise simply never settles when the
 * provider is absent — so every provider call is raced against this.
 */
const PROVIDER_TIMEOUT_MS = 3000;

/** How long a CometD round trip is waited for before giving up on liveness. */
const COMETD_TIMEOUT_MS = 8000;

/** The lazily required web conferencing core, shared by every call. */
let webConferencingPromise;

/**
 * The IANA time zone the drawer reads and writes times in.
 * <p>
 * The portal exposes no user time zone to Javascript (eXo.env.portal carries
 * the language, not the zone), so the browser zone is used — the same source
 * agenda's own UI reads. What matters is that the SAME zone is sent to the REST
 * and used to format: the widget this replaces shifted every time by the UTC
 * offset because it parsed zone-less strings as local ones.
 *
 * @returns {string} an IANA time zone id
 */
export function getTimeZoneId() {
  const portalTimeZone = eXo && eXo.env && eXo.env.portal && eXo.env.portal.timeZone;
  const browserTimeZone = window.Intl && new Intl.DateTimeFormat().resolvedOptions().timeZone;
  return portalTimeZone || browserTimeZone || 'UTC';
}

/**
 * The locale every label and time in the drawer is formatted with: the eXo
 * language, never the browser's.
 *
 * @returns {string} the portal language tag
 */
export function getLocale() {
  return eXo && eXo.env && eXo.env.portal && eXo.env.portal.language || 'en';
}

/**
 * Renders an instant as the wall-clock time it is in the given zone.
 * <p>
 * Agenda parses a date by its LENGTH: 20 characters or less is read as local
 * time in the `timeZoneId` parameter, more than 20 as an absolute instant. A
 * bare `...Z` is exactly 20 characters, so its offset would be parsed then
 * discarded, silently querying the wrong window. Sending 19 characters of local
 * time alongside the zone is the form agenda's own UI sends.
 *
 * @param {Date} date - the instant to render
 * @param {string} timeZoneId - the IANA zone to render it in
 * @returns {string} a 19 character `yyyy-MM-ddTHH:mm:ss`
 */
export function toAgendaDate(date, timeZoneId) {
  const parts = {};
  new Intl.DateTimeFormat('en-CA', {
    timeZone: timeZoneId,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hourCycle: 'h23',
  }).formatToParts(date).forEach(part => parts[part.type] = part.value);
  return `${parts.year}-${parts.month}-${parts.day}T${parts.hour}:${parts.minute}:${parts.second}`;
}

/**
 * Reads the user's scheduled visios from agenda.
 * <p>
 * Conferences are only serialized when they are asked for by name, hence the
 * mandatory `expand=conferences`.
 *
 * @param {Date} now - the reference instant
 * @returns {Promise} resolved with the normalized entries carrying a conference
 */
export function getScheduledVisios(now) {
  const reference = now || new Date();
  const timeZoneId = getTimeZoneId();
  const userIdentityId = eXo && eXo.env && eXo.env.portal && eXo.env.portal.userIdentityId;
  const params = new URLSearchParams();
  params.append('start', toAgendaDate(new Date(reference.getTime() - PAST_WINDOW_MS), timeZoneId));
  params.append('end', toAgendaDate(new Date(reference.getTime() + FUTURE_WINDOW_MS), timeZoneId));
  params.append('timeZoneId', timeZoneId);
  params.append('limit', EVENTS_LIMIT);
  params.append('expand', 'conferences');
  if (userIdentityId) {
    // Sent only when known: the parameter is a long on the server side, and an
    // empty one is a conversion error rather than "no filter".
    params.append('attendeeIdentityId', userIdentityId);
  }
  ['ACCEPTED', 'TENTATIVE', 'NEEDS_ACTION'].forEach(type => params.append('responseTypes', type));
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/v1/agenda/events?${params.toString()}`, {
    method: 'GET',
    credentials: 'include',
  }).then(resp => {
    if (!resp.ok) {
      throw new Error(`Agenda events request failed with status ${resp.status}`);
    }
    return resp.json();
  }).then(data => (data && data.events || []).map(normalizeEvent).filter(entry => !!entry));
}

/**
 * The web conferencing core, required on demand.
 * <p>
 * It drags jquery and cometd behind it, so it is never a dependency of the
 * quick action module: it is fetched on the first drawer open and kept. When it
 * cannot be loaded or initialized, the drawer degrades to the schedule alone
 * rather than failing — an unknown liveness is shown as unknown, never as live.
 *
 * @returns {Promise} resolved with the initialized core, or with null
 */
export function getWebConferencing() {
  if (!webConferencingPromise) {
    webConferencingPromise = new Promise(resolve => {
      window.require(['SHARED/webConferencing'], core => resolve(core || null), () => resolve(null));
    }).then(core => core && initWebConferencing(core) || null);
  }
  return webConferencingPromise;
}

/**
 * Initializes the core the way its own portlet does, when nothing else on the
 * page has done it yet.
 *
 * @param {object} core - the web conferencing module
 * @returns {Promise} resolved with the core once its context is set, or null
 */
function initWebConferencing(core) {
  if (core.getUser && core.getUser()) {
    return Promise.resolve(core);
  }
  return Promise.all([
    core.loadUserInfo(eXo.env.portal.userName),
    core.loadContext(eXo.env.portal.userName, eXo.env.portal.language),
  ]).then(loaded => {
    core.init(loaded[0], loaded[1]);
    return core;
  }).catch(() => null);
}

/**
 * The states of the calls the user takes part in — the only trustworthy source
 * of "somebody is in this room right now".
 *
 * @param {object} core - the web conferencing module, may be null
 * @returns {Promise} resolved with [{id, state}], empty when unavailable
 */
export function getCallStates(core) {
  if (!core || !core.getUserGroupCalls) {
    return Promise.resolve([]);
  }
  const states = promised(() => core.getUserGroupCalls()).then(calls => calls || []).catch(() => []);
  return withTimeout(states, [], COMETD_TIMEOUT_MS);
}

/**
 * Reads one call record, for the ongoing calls that match no scheduled event.
 *
 * @param {object} core - the web conferencing module
 * @param {string} callId - the call id
 * @returns {Promise} resolved with the call, or null
 */
export function getCall(core, callId) {
  if (!core || !core.getCall) {
    return Promise.resolve(null);
  }
  return withTimeout(promised(() => core.getCall(callId)).catch(() => null), null, COMETD_TIMEOUT_MS);
}

/**
 * Asks the provider which call a conference URL points at — the only join key
 * that exists between agenda and web conferencing, neither of which references
 * the other on the server.
 *
 * @param {object} core - the web conferencing module
 * @param {string} url - the conference URL
 * @param {string} providerType - the conference type agenda stored
 * @returns {Promise} resolved with the call id, or null when nothing answers
 */
export function findCallId(core, url, providerType) {
  if (!core || !core.findCallId || !url || !providerType) {
    return Promise.resolve(null);
  }
  return withTimeout(promised(() => core.findCallId(url, providerType)).catch(() => null), null);
}

/**
 * The URL to join a call, for an ongoing call the drawer only knows by its id.
 *
 * @param {object} core - the web conferencing module
 * @param {string} providerType - the call provider
 * @param {string} callId - the call id
 * @returns {Promise} resolved with the URL, or null
 */
export function getCallUrl(core, providerType, callId) {
  if (!core || !core.getProvider || !providerType) {
    return Promise.resolve(null);
  }
  const url = promised(() => core.getProvider(providerType))
    .then(provider => provider && provider.getCallUrl && promised(() => provider.getCallUrl(callId)) || null)
    .catch(() => null);
  return withTimeout(url, null);
}

/**
 * Everything the drawer shows, assembled from the two sources.
 *
 * @param {Date} now - the reference instant
 * @returns {Promise} resolved with the sorted entries
 */
export function getVisios(now) {
  const reference = now || new Date();
  const liveness = getWebConferencing()
    .then(core => getCallStates(core).then(states => ({core: core, states: states})));
  return Promise.all([getScheduledVisios(reference), liveness]).then(loaded => {
    const events = loaded[0];
    const core = loaded[1].core;
    const startedIds = startedCallIds(loaded[1].states);
    return Promise.all(events.map(event => findCallId(core, event.url, event.providerType)
      .then(callId => Object.assign(event, {callId: callId}))))
      .then(resolved => describeAdhocCalls(core, resolved, startedIds)
        .then(adhocCalls => buildEntries({
          events: resolved,
          startedIds: startedIds,
          adhocCalls: adhocCalls,
          now: reference,
        })));
  });
}

/**
 * Describes the ongoing calls that no scheduled event accounts for — a call
 * started from a space button, or a one to one call — so they can be listed
 * with a title and a way in.
 *
 * @param {object} core - the web conferencing module
 * @param {Array} events - the scheduled entries, with their call ids resolved
 * @param {Array} startedIds - the ids of the started calls
 * @returns {Promise} resolved with the described calls
 */
function describeAdhocCalls(core, events, startedIds) {
  const scheduled = events.map(event => event.callId).filter(callId => !!callId);
  const unmatched = startedIds.filter(id => scheduled.indexOf(id) < 0);
  if (!unmatched.length || !core) {
    return Promise.resolve([]);
  }
  return Promise.all(unmatched.map(id => getCall(core, id).then(call => call && {
    id: id,
    title: call.title,
    providerType: call.providerType,
    startDate: call.startDate,
  } || null))).then(calls => calls.filter(call => !!call));
}

/**
 * Subscribes to the user's call channel, so a room that fills up or empties is
 * reflected without reopening the drawer.
 *
 * @param {object} core - the web conferencing module, may be null
 * @param {Function} callback - called on every call update
 * @returns {object} something with an off() method, never null
 */
export function subscribeCallUpdates(core, callback) {
  if (!core || !core.onUserUpdate || !core.getUser || !core.getUser()) {
    return {off: noop};
  }
  // A subscription error is not shown: the schedule is still on screen, only
  // the live flags stop moving until the next refresh.
  return core.onUserUpdate(core.getUser().id, callback, noop);
}

/**
 * Does nothing, on purpose.
 *
 * @returns {void}
 */
function noop() {
  return;
}

/**
 * Adapts a jQuery promise — everything the web conferencing core returns — to a
 * native one.
 *
 * @param {Function} call - produces the jQuery promise
 * @returns {Promise} the native equivalent
 */
function promised(call) {
  return new Promise((resolve, reject) => {
    try {
      const result = call();
      if (result && result.then) {
        result.then(resolve, reject);
      } else {
        resolve(result);
      }
    } catch (error) {
      reject(error);
    }
  });
}

/**
 * Bounds a promise that may never settle.
 *
 * @param {Promise} promise - the promise to bound
 * @param {object} fallback - what to resolve with when it takes too long
 * @param {number} delay - the bound, in milliseconds
 * @returns {Promise} the bounded promise
 */
function withTimeout(promise, fallback, delay) {
  return Promise.race([
    promise,
    new Promise(resolve => window.setTimeout(() => resolve(fallback), delay || PROVIDER_TIMEOUT_MS)),
  ]);
}
