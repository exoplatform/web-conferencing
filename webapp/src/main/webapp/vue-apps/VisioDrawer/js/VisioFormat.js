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

import {getLocale, getTimeZoneId} from '../services.js';

/*
 * Every time on a card is rendered through here, with the eXo language and the
 * time zone the schedule was read in — never with whatever the browser would
 * do by default with a zone-less string.
 */

/**
 * A clock time, as the user's language writes it.
 *
 * @param {Date} date - the instant to render
 * @returns {string} e.g. "09:30", or an empty string for no date
 */
export function formatTime(date) {
  if (!date) {
    return '';
  }
  return new Intl.DateTimeFormat(getLocale(), {
    timeZone: getTimeZoneId(),
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).format(date);
}

/**
 * A compact day label, for what is not happening today.
 *
 * @param {Date} date - the instant to render
 * @returns {string} e.g. "Mon, Aug 18"
 */
export function formatDay(date) {
  if (!date) {
    return '';
  }
  return new Intl.DateTimeFormat(getLocale(), {
    timeZone: getTimeZoneId(),
    weekday: 'short',
    month: 'short',
    day: 'numeric',
  }).format(date);
}

/**
 * Whether two instants fall on the same day, in the drawer's time zone.
 *
 * @param {Date} first - an instant
 * @param {Date} second - another instant
 * @returns {boolean} true when both are the same day
 */
export function isSameDay(first, second) {
  if (!first || !second) {
    return false;
  }
  const formatter = new Intl.DateTimeFormat('en-CA', {
    timeZone: getTimeZoneId(),
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  });
  return formatter.format(first) === formatter.format(second);
}

/**
 * How far along a running meeting is.
 *
 * @param {Date} start - when it started
 * @param {Date} end - when it is due to end
 * @param {Date} now - the reference instant
 * @returns {number} the elapsed share, from 0 to 100
 */
export function elapsedPercent(start, end, now) {
  if (!start || !end) {
    return 0;
  }
  const total = end.getTime() - start.getTime();
  if (total <= 0) {
    return 100;
  }
  const elapsed = (now || new Date()).getTime() - start.getTime();
  return Math.min(100, Math.max(0, Math.round(elapsed * 100 / total)));
}

/**
 * Splits a delay into the units a countdown shows.
 *
 * @param {number} milliseconds - the delay
 * @returns {object} {days, hours, minutes}
 */
export function splitDuration(milliseconds) {
  const minutes = Math.max(0, Math.floor(milliseconds / 60000));
  return {
    days: Math.floor(minutes / 1440),
    hours: Math.floor(minutes % 1440 / 60),
    minutes: minutes % 60,
  };
}
