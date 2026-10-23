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
package org.exoplatform.webconferencing.rest.util;

import java.util.List;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.webconferencing.model.RecordingInfo;

/**
 * A recordings lookup, run by the REST layer that maps its refusals to
 * statuses.
 */
@FunctionalInterface
public interface RecordingsLookup {

  /**
   * Runs the lookup.
   *
   * @return the recordings
   * @throws ObjectNotFoundException when the call or its provider is unknown
   * @throws IllegalAccessException when the viewer may not read them
   */
  List<RecordingInfo> get() throws ObjectNotFoundException, IllegalAccessException;

}
