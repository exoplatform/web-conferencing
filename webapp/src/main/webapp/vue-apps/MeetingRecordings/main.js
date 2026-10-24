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
 * The Meeting block of the agenda drawer. Agenda runs every module whose name
 * contains "AgendaExtension" (includeExtensions('AgendaExtension') in its
 * AgendaCommon module), and calls its init(): this bundle is the
 * WebConferencingAgendaExtension module of AgendaGrp.
 */
import './initComponents.js';
import {registerMeetingBlock, loadMeetingBlockLabels} from './extensions.js';

/**
 * Contributes the block to the drawer and loads its labels. The block does
 * not wait for them: the shared i18n is reactive, a label merged later shows
 * at once.
 *
 * @returns {Promise} resolved once the labels are loaded
 */
export function init() {
  registerMeetingBlock();
  return loadMeetingBlockLabels();
}
