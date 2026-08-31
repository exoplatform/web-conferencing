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

/*
 * The rooms this user opened on the fly, and where they are remembered.
 *
 * A room created here is a group call with no schedule behind it, so neither
 * source the drawer reads can list it once it is empty: agenda never heard of
 * it, and web conferencing only reports calls that are RUNNING. Nothing
 * server-side answers "the rooms I opened today" — see the report, that is a
 * gap, not an oversight — so the browser remembers them itself. This is only a
 * pointer list: the call, its title and its invitation all live on the server,
 * and losing this storage loses the shortcut, never the meeting.
 */

import {LIVE, READY} from './VisioMerge.js';

/** How long a created room stays listed. Long enough for "I sent the link this morning". */
const ROOM_TTL_MS = 12 * 60 * 60 * 1000;

/** How many rooms are kept, newest first, so the list cannot grow forever. */
const MAX_ROOMS = 20;

/** The storage key, per user: a shared browser must not leak one user's rooms to the next. */
const STORAGE_PREFIX = 'visioInstantRooms_';

/**
 * The storage key of the given user.
 *
 * @param {string} userName - the eXo user name
 * @returns {string} the local storage key
 */
function storageKey(userName) {
  return `${STORAGE_PREFIX}${userName || 'anonymous'}`;
}

/**
 * The rooms this user created and that have not expired.
 * <p>
 * Every read is defensive: local storage can be unavailable (private browsing),
 * full, or hold something another version wrote. A room list is a convenience,
 * so any problem reading it degrades to "no remembered room" instead of
 * breaking the drawer.
 *
 * @param {string} userName - the eXo user name
 * @param {Date} now - the reference instant
 * @returns {Array} the stored rooms, newest first
 */
export function loadRooms(userName, now) {
  const limit = (now && now.getTime() || Date.now()) - ROOM_TTL_MS;
  try {
    const raw = window.localStorage.getItem(storageKey(userName));
    const rooms = raw && JSON.parse(raw) || [];
    return rooms.filter(room => room && room.callId && room.createdAt > limit)
      .sort((first, second) => second.createdAt - first.createdAt)
      .slice(0, MAX_ROOMS);
  } catch (error) {
    return [];
  }
}

/**
 * Writes the room list back.
 *
 * @param {string} userName - the eXo user name
 * @param {Array} rooms - the rooms to keep
 * @returns {Array} the rooms that were written
 */
export function saveRooms(userName, rooms) {
  const kept = (rooms || []).slice(0, MAX_ROOMS);
  try {
    window.localStorage.setItem(storageKey(userName), JSON.stringify(kept));
  } catch (error) {
    // A full or unavailable storage costs the shortcut, nothing else.
  }
  return kept;
}

/**
 * Remembers one more room.
 *
 * @param {string} userName - the eXo user name
 * @param {object} room - {callId, title, url, shareUrl, providerType, createdAt}
 * @param {Date} now - the reference instant
 * @returns {Array} the resulting room list
 */
export function addRoom(userName, room, now) {
  const rooms = loadRooms(userName, now).filter(kept => kept.callId !== room.callId);
  rooms.unshift(room);
  return saveRooms(userName, rooms);
}

/**
 * Updates one remembered room — a renamed title, a refreshed invitation link.
 *
 * @param {string} userName - the eXo user name
 * @param {string} callId - the room's call id
 * @param {object} patch - the fields to overwrite
 * @param {Date} now - the reference instant
 * @returns {Array} the resulting room list
 */
export function patchRoom(userName, callId, patch, now) {
  return saveRooms(userName, loadRooms(userName, now)
    .map(room => room.callId === callId && Object.assign({}, room, patch) || room));
}

/**
 * Forgets one room.
 *
 * @param {string} userName - the eXo user name
 * @param {string} callId - the room's call id
 * @param {Date} now - the reference instant
 * @returns {Array} the resulting room list
 */
export function removeRoom(userName, callId, now) {
  return saveRooms(userName, loadRooms(userName, now).filter(room => room.callId !== callId));
}

/**
 * Turns the remembered rooms into drawer entries.
 * <p>
 * The state is read from the server, never from the storage: a room is live
 * because somebody is in it, and this side of the browser is the last place
 * that should be deciding that. When the call could be read, its own
 * participants answer it; otherwise the call being started is the best that is
 * known.
 *
 * @param {Array} rooms - the remembered rooms
 * @param {Array} startedIds - the ids of the calls that are started
 * @returns {Array} the entries to merge into the drawer list
 */
/**
 * The same address with any invitation dropped.
 *
 * @param  {string} url - a room address, with or without an invitation
 * @returns {string} the address nobody is admitted as a guest through
 */
function withoutInvite(url) {
  if (!url) {
    return '';
  }
  const invite = url.indexOf('inviteId=');
  if (invite < 0) {
    return url;
  }
  const cut = url.lastIndexOf('?', invite);
  return cut > 0 && url.substring(0, cut) || url;
}

export function instantEntries(rooms, startedIds) {
  const started = startedIds || [];
  return (rooms || []).map(room => ({
    key: `instant-${room.callId}`,
    eventId: 0,
    occurrenceId: '',
    title: room.title || '',
    start: new Date(room.createdAt),
    end: null,
    allDay: false,
    // Two addresses that must not be confused. `url` is where the button takes
    // the owner of the room, `shareUrl` is what they hand to somebody else, and
    // they differ by the `inviteId` that tells the platform to admit whoever
    // follows it as a guest — an eXo user following one is added as a guest of
    // the call just the same. Opening it yourself means no participant row of
    // type user: the room counts in nobody's badge and shows nobody in it,
    // while you sit in it. Stripped rather than merely preferred, because a
    // room remembered before this was understood has the invitation in both
    // fields, and the poisoned participant row it creates is keyed by
    // (user, call) — so it is written once and never corrects itself.
    url: withoutInvite(room.url || room.shareUrl || ''),
    shareUrl: room.shareUrl || room.url || '',
    providerType: room.providerType || '',
    callId: room.callId,
    instant: true,
    people: room.people || [],
    state: isRoomLive(room, started) && LIVE || READY,
  }));
}

/**
 * Whether a remembered room has people in it.
 *
 * @param {object} room - a remembered room, possibly refreshed from its call
 * @param {Array} startedIds - the ids of the calls that are started
 * @returns {boolean} true when the room counts as live
 */
function isRoomLive(room, startedIds) {
  if (typeof room.joined === 'boolean') {
    return room.joined;
  }
  return startedIds.indexOf(room.callId) >= 0;
}
