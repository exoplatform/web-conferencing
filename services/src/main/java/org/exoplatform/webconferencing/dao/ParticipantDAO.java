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
import org.exoplatform.webconferencing.domain.ParticipantEntity;
import org.exoplatform.webconferencing.domain.ParticipantId;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: ParticipantDAO.java 00000 Dec 22, 2017 pnedonosko $
 */
public class ParticipantDAO extends GenericDAOJPAImpl<ParticipantEntity, ParticipantId> {

  /**
   * Instantiates a new participant DAO.
   */
  public ParticipantDAO() {
  }

  /**
   * Find call participants.
   *
   * @param callId the call id
   * @return the list
   * @throws PersistenceException the persistence exception
   * @throws IllegalStateException the illegal state exception
   * @throws IllegalArgumentException the illegal argument exception
   */
  public List<ParticipantEntity> findCallParts(String callId) throws PersistenceException,
                                                              IllegalStateException,
                                                              IllegalArgumentException {
    TypedQuery<ParticipantEntity> query =
                                        getEntityManager().createNamedQuery("WebConfCall.findCallParts", ParticipantEntity.class)
                                                          .setParameter("callId", callId);
    try {
      return query.getResultList();
    } catch (NoResultException e) {
      return Collections.emptyList();
    }
  }

  /**
   * Delete call participants.
   *
   * @param callId the call id
   * @return the int
   * @throws PersistenceException the persistence exception
   * @throws IllegalStateException the illegal state exception
   * @throws IllegalArgumentException the illegal argument exception
   */
  public int deleteCallParts(String callId) throws PersistenceException, IllegalStateException, IllegalArgumentException {
    return getEntityManager().createNamedQuery("WebConfCall.deleteCallParts").setParameter("callId", callId).executeUpdate();
  }

  /**
   * Clear the storage.
   */
  public void clear() {
    getEntityManager().clear();
  }

}
