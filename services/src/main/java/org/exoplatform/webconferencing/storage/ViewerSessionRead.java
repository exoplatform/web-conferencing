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
package org.exoplatform.webconferencing.storage;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.services.jcr.ext.common.SessionProvider;

/**
 * A read made in a viewer's JCR session.
 *
 * @param <T> what the read returns
 */
@FunctionalInterface
interface ViewerSessionRead<T> {

  /**
   * Reads in the viewer's session.
   *
   * @param sessionProvider the viewer's session provider
   * @param session the viewer's session
   * @return what was read
   * @throws RepositoryException when the repository fails
   */
  T read(SessionProvider sessionProvider, Session session) throws RepositoryException;

}
