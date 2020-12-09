package org.exoplatform.webconferencing.dao;

import java.util.Collections;
import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.PersistenceException;
import javax.persistence.TypedQuery;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.webconferencing.domain.InviteEntity;
import org.exoplatform.webconferencing.domain.InviteId;

/**
 * The Class InviteDAO.
 */
public class InviteDAO extends GenericDAOJPAImpl<InviteEntity, InviteId> {

  /**
   * Find call invites.
   *
   * @param callId the call id
   * @return the list
   * @throws PersistenceException the persistence exception
   * @throws IllegalStateException the illegal state exception
   * @throws IllegalArgumentException the illegal argument exception
   */
  public List<InviteEntity> findCallInvites(String callId) throws PersistenceException,
                                                           IllegalStateException,
                                                           IllegalArgumentException {
    TypedQuery<InviteEntity> query = getEntityManager().createNamedQuery("WebConfInvite.findCallInvites", InviteEntity.class)
                                                       .setParameter("callId", callId);
    try {
      return query.getResultList();
    } catch (NoResultException e) {
      return Collections.emptyList();
    }
  }

  /**
   * Delete call invites.
   *
   * @param callId the call id
   * @return the int
   * @throws PersistenceException the persistence exception
   * @throws IllegalStateException the illegal state exception
   * @throws IllegalArgumentException the illegal argument exception
   */
  @Deprecated // TODO not used
  public int deleteCallInvites(String callId) throws PersistenceException, IllegalStateException, IllegalArgumentException {
    return getEntityManager().createNamedQuery("WebConfInvite.deleteCallInvites").setParameter("callId", callId).executeUpdate();
  }
  
  /**
   * Clear the storage.
   */
  public void clear() {
    getEntityManager().clear();
  }

}
