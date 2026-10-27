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
 * The « Meeting » line of a recording's details. Documents runs every module
 * whose name contains "DocumentsExtension" (includeExtensions in its app and
 * in its info drawer) and calls its init(): this bundle is the
 * WebConferencingDocumentsExtension module of DocumentsGRP. Both may run it,
 * so it contributes once.
 */
import './initComponents.js';
import {registerRecordingMeeting, loadRecordingMeetingLabels} from './extensions.js';

let initialized = false;

/**
 * Contributes the line to the details panel and loads its labels, once. The
 * line does not wait for them: the shared i18n is reactive, a label merged
 * later shows at once.
 *
 * @returns {Promise} resolved once the labels are loaded
 */
export function init() {
  if (initialized) {
    return Promise.resolve();
  }
  initialized = true;
  registerRecordingMeeting();
  return loadRecordingMeetingLabels();
}
