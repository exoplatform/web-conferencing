/*
 * Copyright (C) 2003-2017 eXo Platform SAS.
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
package org.exoplatform.webconferencing.dao;

import java.util.Collections;
import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.PersistenceException;
import javax.persistence.TypedQuery;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.webconferencing.domain.OriginEntity;
import org.exoplatform.webconferencing.domain.OriginId;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: OriginDAO.java 00000 Dec 18, 2020 pnedonosko $
 */
public class OriginDAO extends GenericDAOJPAImpl<OriginEntity, OriginId> {

  /**
   * Instantiates a new origins DAO.
   */
  public OriginDAO() {
  }

  /**
   * Find call's origins.
   *
   * @param callId the call id
   * @param type the type of participant
   * @return the list
   * @throws PersistenceException the persistence exception
   * @throws IllegalStateException the illegal state exception
   * @throws IllegalArgumentException the illegal argument exception
   */
  public List<OriginEntity> findCallOrigins(String callId, String type) throws PersistenceException,
                                                                       IllegalStateException,
                                                                       IllegalArgumentException {
    TypedQuery<OriginEntity> query = getEntityManager().createNamedQuery("WebConfOrigin.findCallOrigins", OriginEntity.class)
                                                       .setParameter("callId", callId)
                                                       .setParameter("type", type);
    try {
      return query.getResultList();
    } catch (NoResultException e) {
      return Collections.emptyList();
    }
  }

  /**
   * Delete call's origins.
   *
   * @param callId the call id
   * @return the int
   * @throws PersistenceException the persistence exception
   * @throws IllegalStateException the illegal state exception
   * @throws IllegalArgumentException the illegal argument exception
   */
  public int deleteCallOrigins(String callId) throws PersistenceException, IllegalStateException, IllegalArgumentException {
    return getEntityManager().createNamedQuery("WebConfOrigin.deleteCallOrigins").setParameter("callId", callId).executeUpdate();
  }

  /**
   * Clear the storage.
   */
  public void clear() {
    getEntityManager().clear();
  }

}
