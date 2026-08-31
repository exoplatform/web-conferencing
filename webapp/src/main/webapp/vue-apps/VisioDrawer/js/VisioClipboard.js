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
 * Copying a link, in the two worlds this actually runs in.
 *
 * navigator.clipboard only exists in a secure context: it is there on the
 * https deployments and absent on the plain http://localhost:8080 every
 * developer runs. A copy button that silently does nothing on half the
 * installations is worse than no copy button, so the old execCommand path is
 * kept as a real fallback rather than as a comment.
 */

/**
 * Puts a text in the clipboard.
 *
 * @param {string} text - what to copy
 * @returns {Promise} resolved when the text is in the clipboard, rejected when
 *          no mechanism worked
 */
export function copyText(text) {
  if (!text) {
    return Promise.reject(new Error('Nothing to copy'));
  }
  if (window.navigator && window.navigator.clipboard && window.navigator.clipboard.writeText) {
    // Rejected on an insecure origin even when the object exists, hence the
    // fallback on the rejection and not only on the missing API.
    return window.navigator.clipboard.writeText(text).catch(() => copyBySelection(text));
  }
  return copyBySelection(text);
}

/**
 * The pre-clipboard-API way: a hidden textarea, selected and copied.
 * <p>
 * Deprecated for years and still the only thing that works outside a secure
 * context. The textarea is positioned off screen rather than hidden, because a
 * `display: none` element cannot be selected.
 *
 * @param {string} text - what to copy
 * @returns {Promise} resolved when the copy command reported success
 */
function copyBySelection(text) {
  return new Promise((resolve, reject) => {
    const area = document.createElement('textarea');
    area.value = text;
    area.setAttribute('readonly', 'readonly');
    area.style.position = 'fixed';
    area.style.top = '-1000px';
    area.style.opacity = '0';
    document.body.appendChild(area);
    try {
      area.select();
      area.setSelectionRange(0, area.value.length);
      if (document.execCommand && document.execCommand('copy')) {
        resolve();
      } else {
        reject(new Error('Copy command refused'));
      }
    } catch (error) {
      reject(error);
    } finally {
      document.body.removeChild(area);
    }
  });
}
