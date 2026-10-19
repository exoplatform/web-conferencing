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
import java.util.Date;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.webconferencing.CallState;
import org.exoplatform.webconferencing.UserState;
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
  @Deprecated // TODO not used
  public int deleteCallParts(String callId) throws PersistenceException, IllegalStateException, IllegalArgumentException {
    return getEntityManager().createNamedQuery("WebConfCall.deleteCallParts").setParameter("callId", callId).executeUpdate();
  }

  /**
   * Update Participant state by call id
   *
   * @param state
   * @return Number of updated participants
   */
  @ExoTransactional
  public int updateParticipantStateByCallId(String state, String callId) {
    Query query = getEntityManager().createQuery("UPDATE WebConfParticipant p SET p.state = :state  WHERE p.callId = :callId")
            .setParameter("state", state)
            .setParameter("callId", callId);

    return query.executeUpdate();
  }

  /**
   * Marks a call participant as having missed the call, only while the participant is still invited and the
   * call still started, from a start not later than the given date: among concurrent writers (several cluster
   * nodes, a join or a decline committed meanwhile, the call stopped or started again), the participant is
   * marked at most once, and never for a start of the call later than the given date.
   *
   * @param callId the call id
   * @param partId the participant id
   * @param latestStart the latest start date of the call the participant may have missed
   * @return the number of updated participants, 1 or 0
   */
  @ExoTransactional
  public int markParticipantMissed(String callId, String partId, Date latestStart) {
    return markParticipantMissed(getEntityManager(), callId, partId, latestStart);
  }

  static int markParticipantMissed(EntityManager entityManager, String callId, String partId, Date latestStart) {
    return entityManager.createQuery("UPDATE WebConfParticipant p SET p.state = :missed"
        + " WHERE p.callId = :callId AND p.id = :partId AND p.state = :invited"
        + " AND EXISTS (SELECT c.id FROM WebConfCall c WHERE c.id = :callId AND c.state = :started"
        + " AND c.lastDate <= :latestStart)")
                        .setParameter("missed", UserState.MISSED)
                        .setParameter("callId", callId)
                        .setParameter("partId", partId)
                        .setParameter("invited", UserState.INVITED)
                        .setParameter("started", CallState.STARTED)
                        .setParameter("latestStart", latestStart)
                        .executeUpdate();
  }
  /**
   * Marks a participant still invited to a stopped call as missed, only while the call is still stopped, from a
   * start not later than the given date: a call started again has its participants alerted again.
   *
   * @param callId the call id
   * @param partId the participant id
   * @param latestStart the latest start date of the call the participant may be marked for
   * @return the number of updated participants, 1 or 0
   */
  @ExoTransactional
  public int markParticipantMissedAtStop(String callId, String partId, Date latestStart) {
    return markParticipantMissedAtStop(getEntityManager(), callId, partId, latestStart);
  }

  static int markParticipantMissedAtStop(EntityManager entityManager, String callId, String partId, Date latestStart) {
    return entityManager.createQuery("UPDATE WebConfParticipant p SET p.state = :missed"
        + " WHERE p.callId = :callId AND p.id = :partId AND p.state = :invited"
        + " AND EXISTS (SELECT c.id FROM WebConfCall c WHERE c.id = :callId AND c.state = :stopped"
        + " AND c.lastDate <= :latestStart)")
                        .setParameter("missed", UserState.MISSED)
                        .setParameter("callId", callId)
                        .setParameter("partId", partId)
                        .setParameter("invited", UserState.INVITED)
                        .setParameter("stopped", CallState.STOPPED)
                        .setParameter("latestStart", latestStart)
                        .executeUpdate();
  }

  /**
   * Marks a participant who declined a call as notified of the decline, only while still declined and the call
   * from a start not later than the given date, whatever its state: a call started again has its participants
   * alerted again.
   *
   * @param callId the call id
   * @param partId the participant id
   * @param latestStart the latest start date of the call the participant may be marked for
   * @return the number of updated participants, 1 or 0
   */
  @ExoTransactional
  public int markParticipantDeclinedNotified(String callId, String partId, Date latestStart) {
    return markParticipantDeclinedNotified(getEntityManager(), callId, partId, latestStart);
  }

  static int markParticipantDeclinedNotified(EntityManager entityManager, String callId, String partId, Date latestStart) {
    return entityManager.createQuery("UPDATE WebConfParticipant p SET p.state = :notified"
        + " WHERE p.callId = :callId AND p.id = :partId AND p.state = :declined"
        + " AND EXISTS (SELECT c.id FROM WebConfCall c WHERE c.id = :callId AND c.lastDate <= :latestStart)")
                        .setParameter("notified", UserState.DECLINED_NOTIFIED)
                        .setParameter("callId", callId)
                        .setParameter("partId", partId)
                        .setParameter("declined", UserState.DECLINED)
                        .setParameter("latestStart", latestStart)
                        .executeUpdate();
  }


  /**
   * Clear the storage.
   */
  public void clear() {
    getEntityManager().clear();
  }

}
