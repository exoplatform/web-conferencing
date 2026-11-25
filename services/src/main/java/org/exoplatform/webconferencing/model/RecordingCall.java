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
package org.exoplatform.webconferencing.model;

import java.util.Date;

/**
 * The call a recording file was made in, as the file's stamp records it.
 *
 * @param callId the call id
 * @param providerType the call's provider type, or <code>null</code> when the
 *          stamp does not hold it
 * @param ownerType the type of the call's owner, such as
 *          <code>space_event</code> for the call of an agenda event, or
 *          <code>null</code>
 * @param startedDate when the recording started, read from its file name, or
 *          <code>null</code>
 * @param recordedDate when the recording was saved, or <code>null</code>
 */
public record RecordingCall(String callId, String providerType, String ownerType, Date startedDate, Date recordedDate) {
}
