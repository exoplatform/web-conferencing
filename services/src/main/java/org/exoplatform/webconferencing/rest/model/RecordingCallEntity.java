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
package org.exoplatform.webconferencing.rest.model;

/**
 * The call a recording file was made in, as the recordings REST answers it.
 *
 * @param callId the call id
 * @param providerType the call's provider type, or null when unknown
 * @param ownerType the type of the call's owner, <code>space_event</code> for
 *          the call of an agenda event, or null when unknown
 * @param startedDate when the recording started, an ISO 8601 instant in UTC,
 *          or null when its file name does not tell it
 * @param recordedDate when the recording was saved, an ISO 8601 instant in
 *          UTC, or null
 * @param graceHours how many hours after an occurrence's end a recording
 *          saved then still belongs to it, as the recordings of an event's
 *          occurrence are listed
 */
public record RecordingCallEntity(String callId,
                                  String providerType,
                                  String ownerType,
                                  String startedDate,
                                  String recordedDate,
                                  int graceHours) {
}
