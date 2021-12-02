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

import static org.exoplatform.webconferencing.WebConferencingService.OWNER_TYPE_USER;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.PersistenceException;
import javax.persistence.TypedQuery;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.webconferencing.domain.CallEntity;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CallDAO.java 00000 Dec 22, 2017 pnedonosko $
 */
public class CallDAO extends GenericDAOJPAImpl<CallEntity, String> {

  /** The Constant USER_CALL_DAYS_LIVETIME. */
  public static final int USER_CALL_DAYS_LIVETIME = 2;

  /**
   * Instantiates a new call DAO.
   */
  public CallDAO() {
  }

  /**
   * Find group call by owner id.
   *
   * @param ownerId the owner id
   * @return the call entity or <code>null</code> if no call found
   * @throws PersistenceException the persistence exception
   * @throws IllegalStateException the illegal state exception
   * @throws IllegalArgumentException the illegal argument exception
   */
  public CallEntity findGroupCallByOwnerId(String ownerId) throws PersistenceException,
                                                           IllegalStateException,
                                                           IllegalArgumentException {
    TypedQuery<CallEntity> query = getEntityManager().createNamedQuery("WebConfCall.findGroupCallByOwnerId", CallEntity.class)
                                                     .setParameter("ownerId", ownerId);

    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
  
  /**
   * Find group call by owner type and id.
   *
   * @param ownerId the owner id
   * @param ownerType the owner type
   * @return the list with call entities or empty list if nothing found
   * @throws PersistenceException the persistence exception
   * @throws IllegalStateException the illegal state exception
   * @throws IllegalArgumentException the illegal argument exception
   */
  public CallEntity findGroupCallByOwnerTypeId(String ownerId, String ownerType) throws PersistenceException,
                                                           IllegalStateException,
                                                           IllegalArgumentException {
    TypedQuery<CallEntity> query = getEntityManager().createNamedQuery("WebConfCall.findGroupCallByOwnerTypeId", CallEntity.class)
                                                     .setParameter("ownerId", ownerId)
                                                     .setParameter("ownerType", ownerType);

    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
  
  /**
   * Find group calls by owner type and id.
   *
   * @param ownerId the owner id
   * @param ownerType the owner type
   * @return the list with call entities or empty list if nothing found
   * @throws PersistenceException the persistence exception
   * @throws IllegalStateException the illegal state exception
   * @throws IllegalArgumentException the illegal argument exception
   */
  public List<CallEntity> findGroupCallsByOwnerTypeId(String ownerId, String ownerType) throws PersistenceException,
                                                           IllegalStateException,
                                                           IllegalArgumentException {
    TypedQuery<CallEntity> query = getEntityManager().createNamedQuery("WebConfCall.findGroupCallByOwnerTypeId", CallEntity.class)
                                                     .setParameter("ownerId", ownerId)
                                                     .setParameter("ownerType", ownerType);

    try {
      return query.getResultList();
    } catch (NoResultException e) {
      return Collections.emptyList();
    }
  }

  /**
   * Find user group calls.
   *
   * @param userId the user id
   * @return the list, it will be empty if no calls found
   * @throws PersistenceException the persistence exception
   * @throws IllegalStateException the illegal state exception
   * @throws IllegalArgumentException the illegal argument exception
   */
  public List<CallEntity> findUserGroupCalls(String userId) throws PersistenceException,
                                                            IllegalStateException,
                                                            IllegalArgumentException {
    TypedQuery<CallEntity> query = getEntityManager().createNamedQuery("WebConfCall.findUserGroupCalls", CallEntity.class)
                                                     .setParameter("userId", userId);

    try {
      return query.getResultList();
    } catch (NoResultException e) {
      return Collections.emptyList();
    }
  }

  /**
   * Delete all users calls older of {@value #USER_CALL_DAYS_LIVETIME} days.
   *
   * @return the int number of actually removed calls
   * @throws PersistenceException the persistence exception
   * @throws IllegalStateException the illegal state exception
   * @throws IllegalArgumentException the illegal argument exception
   */
  public int deleteAllUsersCalls() throws PersistenceException, IllegalStateException, IllegalArgumentException {
    LocalDateTime expired = LocalDate.now().atStartOfDay().minusDays(USER_CALL_DAYS_LIVETIME);
    return getEntityManager().createNamedQuery("WebConfCall.deleteOwnerOlderCalls")
                             .setParameter("ownerType", OWNER_TYPE_USER)
                             .setParameter("expiredDate", Timestamp.valueOf(expired))
                             .executeUpdate();
  }

  /**
   * Clear the storage.
   */
  public void clear() {
    getEntityManager().clear();
  }

}
