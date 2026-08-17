/*
 * Copyright (C) 2026 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

import {normalizeEvent, startedCallIds, buildEntries, LIVE} from './js/VisioMerge.js';
import {loadRooms, addRoom, patchRoom, removeRoom, instantEntries} from './js/VisioInstant.js';

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
    if (resp.status === 404) {
      // Web conferencing has no dependency on agenda: a deployment can run
      // without it. A missing add-on is not a failure — there is simply no
      // schedule to read, and the rooms opened from here still work.
      return {events: []};
    }
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
 * Everything the drawer shows, assembled from the two sources plus the rooms
 * this user opened on the fly.
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
    return Promise.all([
      Promise.all(events.map(event => findCallId(core, event.url, event.providerType)
        .then(callId => Object.assign(event, {callId: callId})))),
      refreshInstantRooms(core, reference),
    ]).then(both => {
      const resolved = both[0];
      const rooms = both[1];
      return describeAdhocCalls(core, resolved, startedIds)
        .then(adhocCalls => buildEntries({
          events: resolved,
          startedIds: startedIds,
          adhocCalls: adhocCalls,
          instant: instantEntries(rooms, startedIds),
          now: reference,
        }))
        .then(entries => withPeople(core, entries));
    });
  });
}

/**
 * The user name every remembered room is filed under.
 *
 * @returns {string} the current eXo user name
 */
function currentUserName() {
  return eXo && eXo.env && eXo.env.portal && eXo.env.portal.userName || '';
}

/**
 * The rooms this user opened, with their share link checked against the server.
 * <p>
 * A room's invitation is deleted server side when the room empties and
 * recreated the next time the call is read, so the link stored at creation can
 * go stale. Reading each room here keeps what the drawer offers to copy in step
 * with what the server would accept, and — as a side effect the platform
 * intends — has the server recreate a missing invitation before anybody clicks
 * a link that would be refused.
 *
 * @param {object} core - the web conferencing module, may be null
 * @param {Date} now - the reference instant
 * @returns {Promise} resolved with the remembered rooms
 */
export function refreshInstantRooms(core, now) {
  const userName = currentUserName();
  const rooms = loadRooms(userName, now);
  if (!rooms.length || !core) {
    return Promise.resolve(rooms);
  }
  return Promise.all(rooms.map(room => getCall(core, room.callId).then(call => {
    if (!call) {
      // A call that cannot be read is indistinguishable here from one that
      // timed out — the core reports both as nothing — so the room is kept as
      // it was rather than forgotten on a slow connection.
      return room;
    }
    const shareUrl = call.inviteId && appendInvite(room.url, call.inviteId) || room.shareUrl;
    const patch = {title: call.title || room.title, shareUrl: shareUrl};
    patchRoom(userName, room.callId, patch, now);
    // A room is created in the started state — the platform has no way to
    // register one that is merely open — so "started" would call every fresh
    // room live before anyone walked in. Who is actually inside is right there
    // in the call that was just read, and it is the honest answer.
    const inside = whoIsIn(call);
    return Object.assign({}, room, patch, {joined: inside.length > 0, people: inside});
  })));
}

/**
 * Fills in who is inside, for entries that are live and do not know yet.
 * <p>
 * A room opened from here already carries its people: the call was read to
 * decide whether anybody had joined. A visio that came from the schedule does
 * not — its liveness arrives as a bare list of started call ids, so the card
 * could say a meeting was live while saying nothing about who was in it, which
 * is the half people actually want.
 * <p>
 * One call per live entry, and live entries are few: a user is in nought or one
 * meeting, occasionally two. Bounded like every other provider round trip, and
 * a call that does not answer leaves the entry exactly as it was rather than
 * emptying it.
 *
 * @param {object} core - the web conferencing module, may be null
 * @param {Array} entries - the merged drawer entries
 * @returns {Promise} resolved with the same entries, the live ones peopled
 */
function withPeople(core, entries) {
  if (!core) {
    return Promise.resolve(entries);
  }
  const pending = (entries || []).filter(entry => entry.state === LIVE && entry.callId && !entry.people);
  if (!pending.length) {
    return Promise.resolve(entries);
  }
  return Promise.all(pending.map(entry => getCall(core, entry.callId)
    .then(call => {
      if (call) {
        entry.people = whoIsIn(call);
      }
    })
    .catch(() => null))).then(() => entries);
}

/**
 * Opens the eXo mail composer on a subject and a body.
 * <p>
 * Requiring the mail modules is not enough on its own: the composer's listener
 * belongs to the mailbox APP, and on a page where the mailbox has never been
 * opened that app is not mounted, so the event is dispatched into nothing and
 * the click appears to do nothing at all. The app is therefore mounted first
 * when it is absent — the same bootstrap Contacts performs before sharing a
 * card, including its check for every id the app can already be under, since
 * mounting a second instance makes both answer and the composer opens twice.
 *
 * @param {String} subject - the subject to seed
 * @param {String} body - the body to seed
 * @returns {Promise} resolved once the composer has been asked to open
 */
export function openMailComposer(subject, body) {
  return new Promise((resolve, reject) => {
    window.require([
      'SHARED/eXoVueI18n',
      'PORTLET/email-connector/EmailConnectorUserSetting',
      'SHARED/emailConnectorQuickActionExtension',
    ], exoi18n => bootstrapMailApp(exoi18n).then(() => {
      document.dispatchEvent(new CustomEvent('open-email-composer', {
        detail: {to: [], subject: subject, body: body},
      }));
      resolve();
    }).catch(reject), reject);
  });
}

/**
 * Puts the mailbox app on the page, unless it is already there.
 *
 * @param {Object} exoi18n - the platform i18n loader
 * @returns {Promise} resolved once the app is mounted
 */
function bootstrapMailApp(exoi18n) {
  const appId = 'visio-mail-compose';
  // Every id the app can already be mounted under, the mail page's own portlet
  // root included. Missing one mounts a second app, and then both hand the
  // composer the same mail.
  const mounted = document.querySelector(`#emailConnectorMailBox, #emailConenctor-mailBox-quick-actions, #${appId}`);
  if (mounted) {
    return Promise.resolve();
  }
  const host = document.querySelector('#vuetify-apps');
  if (!host) {
    return Promise.reject(new Error('No application host on this page'));
  }
  const parent = document.createElement('div');
  parent.id = appId;
  host.appendChild(parent);
  const lang = eXo.env.portal.language;
  const urls = [
    `/email-connector/i18n/locale.portlet.emailConnector.emailConnectorUserSetting?lang=${lang}`,
    `/email-connector/i18n/locale.portlet.emailConnector.emailConnectorMailBox?lang=${lang}`,
  ];
  return new Promise(resolve => exoi18n.loadLanguageAsync(lang, urls)
    .then(i18n => window.Vue.createApp({
      template: `<email-connector-mail-box-app id="${appId}" />`,
      mounted() {
        resolve();
      },
      vuetify: window.Vue.prototype.vuetifyOptions,
      i18n,
    }, `#${appId}`, 'Visio Compose')));
}

/**
 * Who is in the room at this moment.
 * <p>
 * The call the drawer already reads carries every participant with the state
 * the server has for them, so the people inside cost nothing extra to know —
 * they were previously reduced to a yes/no and thrown away. A name and a face
 * are what decide whether somebody joins: "two people are already in there" is
 * a different invitation from a bare green chip.
 *
 * @param {object} call - the call as the server describes it
 * @returns {Array} the participants who have joined, possibly empty
 */
function whoIsIn(call) {
  return (call.participants || []).filter(participant => participant && participant.state === 'joined');
}

/**
 * Opens a meeting room, right now, with nothing asked.
 * <p>
 * The room is a group call owned by nothing but itself: it is created with a
 * chat-room owner because that is the only owner type the platform models as
 * "a group call whose members are just these people" — a user owner means a one
 * to one call, and a space-event owner requires a real space. See the report:
 * this is a constraint of the existing model, not a preference.
 *
 * @param {string} title - the room's name
 * @returns {Promise} resolved with the created room
 */
export function createInstantVisio(title) {
  const now = new Date();
  return getWebConferencing().then(core => {
    if (!core || !core.addCall || !core.getUser || !core.getUser()) {
      throw new Error('Web conferencing is not available');
    }
    return resolveProviders(core).then(providers => {
      if (!providers.length) {
        throw new Error('No web conferencing provider is available');
      }
      return providers.reduce(
        (previous, provider) => previous.then(room => room || openRoomWith(core, provider, title, now)),
        Promise.resolve(null))
        .then(room => {
          if (!room) {
            throw new Error('No provider could open a room');
          }
          return room;
        });
    });
  });
}

/**
 * Opens a room with one provider, or gives up on it.
 * <p>
 * Every step is bounded. Not defensiveness for its own sake: a provider that
 * cannot serve the request may reject, but it may equally hand back a promise
 * it then abandons — external-visio does exactly that, throwing inside its own
 * chain when the identity it wanted turns out to be undefined. An unbounded
 * wait on that is indistinguishable, from the user's side, from a button that
 * does nothing at all. Bounded, it becomes "that provider could not", and the
 * next one gets its turn.
 *
 * @param {object} core - the web conferencing module
 * @param {object} provider - the provider to try
 * @param {string} title - the room's title
 * @param {Date} now - creation instant
 * @returns {Promise} resolved with the room, or null when this provider cannot
 */
function openRoomWith(core, provider, title, now) {
  const providerType = provider.getType();
  const roomName = `visio-${randomId()}`;
  return withTimeout(promised(() => provider.getCallId(roomContext(core, roomName))).catch(() => null), null)
    .then(callId => {
      if (!callId) {
        return null;
      }
      return withTimeout(promised(() => core.addCall(callId, {
        owner: roomName,
        ownerType: 'chat_room',
        provider: providerType,
        title: title,
        participants: core.getUser().id,
        group: true,
        // Opened, not entered. Nobody is in it until somebody clicks join, and
        // saying otherwise is what made a fresh room count in the badge and
        // refuse to be renamed.
        start: false,
      })).catch(() => null), null, COMETD_TIMEOUT_MS).then(call => {
        if (!call) {
          return null;
        }
        return withTimeout(promised(() => provider.getCallUrl(callId)).catch(() => null), null).then(url => {
          if (!url) {
            return null;
          }
          const room = {
            callId: callId,
            title: call.title || title,
            providerType: providerType,
            url: url,
            shareUrl: appendInvite(url, call.inviteId),
            createdAt: now.getTime(),
          };
          addRoom(currentUserName(), room, now);
          return room;
        });
      });
    });
}

/**
 * Renames a room.
 * <p>
 * Refused while the room is in use, on purpose: the platform's call update
 * rebuilds the call from what it is given and clears its state in the process,
 * so renaming a running room would drop it out of the live list for everyone
 * watching. Better to say no than to break the very thing the drawer reports.
 *
 * @param {object} room - the remembered room
 * @param {string} title - the new name
 * @returns {Promise} resolved with the renamed room, rejected when in use
 */
export function renameInstantVisio(room, title) {
  const now = new Date();
  return getWebConferencing().then(core => {
    if (!core || !core.updateCall) {
      throw new Error('Web conferencing is not available');
    }
    // The core resolves its own update through the provider, so a page where
    // no provider ever loaded would leave the rename hanging forever.
    return Promise.all([resolveProvider(core), getCall(core, room.callId)]).then(loaded => {
      const call = loaded[1];
      if (call && call.state === 'started') {
        const running = new Error('The room is in use');
        running.running = true;
        throw running;
      }
      return promised(() => core.updateCall(room.callId, {
        owner: call && call.owner && call.owner.id || null,
        ownerType: 'chat_room',
        provider: room.providerType,
        title: title,
        participants: core.getUser && core.getUser() && core.getUser().id || null,
        group: true,
      })).then(() => {
        patchRoom(currentUserName(), room.callId, {title: title}, now);
        return Object.assign({}, room, {title: title});
      });
    });
  });
}

/**
 * Destroys a room for everyone.
 * <p>
 * Not the same act as forgetting one: this removes the call itself, so the
 * link stops working for whoever was sent it. Offered only where nobody has
 * ever joined, and always behind a confirmation.
 *
 * @param {string} callId - the call to delete
 * @param {string} providerType - the provider that owns it
 * @returns {Promise} resolved once the room is gone from the server and the list
 */
export function deleteInstantVisio(callId, providerType) {
  return getWebConferencing().then(core => {
    if (!core || !core.deleteCall) {
      throw new Error('Web conferencing is not available');
    }
    return withTimeout(promised(() => core.deleteCall(callId, providerType)).catch(() => null),
      null, COMETD_TIMEOUT_MS).then(() => {
      // Whatever the server made of it, the shortcut goes: a room that cannot
      // be deleted is not one to keep offering a delete button for.
      removeRoom(currentUserName(), callId, new Date());
    });
  });
}


/**
 * The link that lets anyone in, account or not.
 *
 * @param {string} url - the call URL
 * @param {string} inviteId - the call's invitation id, may be missing
 * @returns {string} the shareable URL
 */
function appendInvite(url, inviteId) {
  if (!url) {
    return '';
  }
  return inviteId && `${url}?inviteId=${encodeURIComponent(inviteId)}` || url;
}

/**
 * The context a provider builds a fresh group call id from.
 * <p>
 * Shaped like the contexts the core itself passes: a group that is neither a
 * space nor a space event, so the provider names the room after it and returns
 * an id nothing else can collide with.
 *
 * @param {object} core - the web conferencing module
 * @param {string} roomName - the generated room name
 * @returns {object} the provider context
 */
function roomContext(core, roomName) {
  return {
    currentUser: core.getUser(),
    roomName: roomName,
    roomTitle: roomName,
    isGroup: true,
    isSpace: false,
    isSpaceEvent: false,
    isRoom: true,
    isUser: false,
    details: () => Promise.resolve({id: roomName, title: roomName, members: {}}),
  };
}

/**
 * A provider able to name a room and give it a URL.
 * <p>
 * A provider only exists in this page once its own module has run, and nothing
 * runs it on a page that has no call button — so the configured types are read
 * from the server and their modules required by the platform's naming
 * convention. When none answers, the caller says so instead of half creating
 * something.
 *
 * @param {object} core - the web conferencing module
 * @returns {Promise} resolved with a usable provider, or null
 */
export function resolveProvider(core) {
  return resolveProviders(core).then(providers => providers[0] || null);
}

/**
 * Every provider this deployment has, in the order worth trying.
 * <p>
 * Taking the first configured provider was wrong, and wrong in a way that hung
 * the drawer rather than failing it. A deployment can have several: this one
 * reports <code>externalVisio</code> before <code>jitsi</code>, and
 * external-visio exists to point at meetings held somewhere else — asked to
 * name a room it fetches a social identity it was never given, receives a 404,
 * and throws that rejection inside its own promise chain. The deferred it
 * handed back is then never settled either way, so the caller waits for ever.
 * <p>
 * So the resolution returns candidates rather than a winner, and the caller
 * tries them in turn under a timeout. Order is the server's, with one
 * exception: a provider that cannot be asked to invent a room is no use for an
 * on-the-fly one, and the only honest way to find that out is to ask it and
 * bound the wait.
 *
 * @param {object} core - the web conferencing module
 * @returns {Promise} resolved with an array of usable providers, possibly empty
 */
export function resolveProviders(core) {
  return getProviderTypes().then(types => types.reduce(
    (previous, type) => previous.then(found => loadProvider(core, type)
      .then(provider => provider && found.concat([provider]) || found)),
    Promise.resolve([])));
}

/**
 * The provider types this deployment has active.
 *
 * @returns {Promise} resolved with the type names, empty when unreadable
 */
function getProviderTypes() {
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/webconferencing/providers/configuration`, {
    credentials: 'include',
  }).then(resp => resp.ok && resp.json() || [])
    .then(configs => (configs || []).filter(config => config && config.type && config.active !== false)
      .map(config => config.type))
    .catch(() => []);
}

/**
 * One provider, its module loaded if need be.
 *
 * @param {object} core - the web conferencing module
 * @param {string} type - the provider type
 * @returns {Promise} resolved with the provider, or null
 */
function loadProvider(core, type) {
  const registered = core.findProvider && core.findProvider(type);
  if (isUsableProvider(registered)) {
    return Promise.resolve(registered);
  }
  return new Promise(resolve => {
    // The provider modules are named after their type by the platform's own
    // convention (webConferencing_jitsi for the jitsi provider). A deployment
    // whose provider does not follow it simply does not answer here.
    window.require([`SHARED/webConferencing_${type}`], () => resolve(), () => resolve());
  }).then(() => withTimeout(promised(() => core.getProvider(type)).catch(() => null), null))
    .then(provider => isUsableProvider(provider) && provider
      || isUsableProvider(core.findProvider && core.findProvider(type)) && core.findProvider(type)
      || null);
}

/**
 * Whether a provider can open a room: name it, and say where it is.
 *
 * @param {object} provider - a provider, may be null
 * @returns {boolean} true when the provider can do both
 */
function isUsableProvider(provider) {
  return !!provider && !!provider.getCallId && !!provider.getCallUrl && !!provider.getType && !!provider.getType();
}

/**
 * An identifier nothing else will produce.
 *
 * @returns {string} a random hexadecimal string
 */
function randomId() {
  const crypto = window.crypto || window.msCrypto;
  if (crypto && crypto.getRandomValues) {
    const bytes = new Uint8Array(16);
    crypto.getRandomValues(bytes);
    return Array.prototype.map.call(bytes, byte => `0${byte.toString(16)}`.slice(-2)).join('');
  }
  return `${Date.now().toString(16)}${Math.random().toString(16).slice(2, 10)}`;
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
