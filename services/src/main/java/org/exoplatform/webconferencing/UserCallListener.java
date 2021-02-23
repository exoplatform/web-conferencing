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
package org.exoplatform.webconferencing;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: UserCallListener.java 00000 Jul 18, 2017 pnedonosko $
 */
public abstract class UserCallListener {

  /** The user id. */
  protected final String userId;

  /** The client id. */
  protected final String clientId;

  /**
   * Instantiates a new incoming call listener.
   *
   * @param userId the user id
   * @param clientId the client id
   */
  public UserCallListener(String userId, String clientId) {
    this.userId = userId;
    this.clientId = clientId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((clientId == null) ? 0 : clientId.hashCode());
    result = prime * result + ((userId == null) ? 0 : userId.hashCode());
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    UserCallListener other = (UserCallListener) obj;
    if (clientId == null) {
      if (other.clientId != null)
        return false;
    } else if (!clientId.equals(other.clientId))
      return false;
    if (userId == null) {
      if (other.userId != null)
        return false;
    } else if (!userId.equals(other.userId))
      return false;
    return true;
  }

  /**
   * Gets the user id.
   *
   * @return the user id
   */
  public String getUserId() {
    return userId;
  }

  /**
   * Gets the client id.
   *
   * @return the clientId
   */
  public String getClientId() {
    return clientId;
  }

  /**
   * On call state changed.
   *
   * @param callId the call id
   * @param providerType the provider type
   * @param callState the call status
   * @param ownerId the caller id
   * @param ownerType the caller type
   */
  public abstract void onCallStateChanged(String callId, String providerType, String callState, String ownerId, String ownerType);

  /**
   * On participant joined.
   *
   * @param callId the call id
   * @param providerType the provider type
   * @param ownerId the caller id
   * @param ownerType the caller type
   * @param partId the participant user id
   */
  public abstract void onPartJoined(String callId, String providerType, String ownerId, String ownerType, String partId);

  /**
   * On participant leaved.
   *
   * @param callId the call id
   * @param providerType the provider type
   * @param ownerId the caller id
   * @param ownerType the caller type
   * @param partId the participant user id
   */
  public abstract void onPartLeaved(String callId, String providerType, String ownerId, String ownerType, String partId);

}
