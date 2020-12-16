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

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CallInfo.java 00000 Jun 19, 2017 pnedonosko $
 */
public class CallInfo {

  /** The id. */
  protected final String        id;

  /** The title. */
  protected final String        title;

  /** The participants. */
  protected final Set<UserInfo> participants = new LinkedHashSet<>();
  
  /** The origins. */
  protected final Set<OriginInfo> origins = new LinkedHashSet<>();

  /** The owner. */
  protected final IdentityInfo  owner;

  /** The provider type. */
  protected final String        providerType;

  /** The state. */
  protected String              state;

  /** The last date. */
  protected Date                lastDate;
  
  /** The start date. */
  protected Date                startDate;
  
  /** The end date. */
  protected Date                endDate;

  /** The invite id. */
  protected String              inviteId;

  /**
   * Instantiates a new call info.
   *
   * @param id the id
   * @param title the title
   * @param owner the owner
   * @param providerType the provider type
   */
  public CallInfo(String id, String title, IdentityInfo owner, String providerType) {
    super();
    this.id = id;
    this.title = title;
    this.owner = owner;
    this.providerType = providerType;
  }

  /**
   * Gets the id.
   *
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * Gets the title. Can be <code>null</code>.
   *
   * @return the title or <code>null</code> 
   */
  public String getTitle() {
    return title;
  }

  /**
   * Gets the participants (actual users for the call).
   *
   * @return the participants
   */
  public Set<UserInfo> getParticipants() {
    return Collections.unmodifiableSet(participants);
  }
  
  /**
   * Gets the origins (direct users and groups involved in the call).
   *
   * @return the origins
   */
  public Set<OriginInfo> getOrigins() {
    return Collections.unmodifiableSet(origins);
  }
  
  /**
   * Adds the origins.
   *
   * @param origins the origins
   */
  public void addOrigins(Collection<OriginInfo> origins) {
    for (OriginInfo origin : origins) {
      addOrigin(origin);
    }
  }
  
  /**
   * Adds the origin.
   *
   * @param origin the origin
   */
  public void addOrigin(OriginInfo origin) {
    origins.add(origin);
  }
  
  /**
   * Gets the owner.
   *
   * @return the owner
   */
  public IdentityInfo getOwner() {
    return owner;
  }

  /**
   * Gets the provider type.
   *
   * @return the provider type
   */
  public String getProviderType() {
    return providerType;
  }

  /**
   * Adds the participants. This method will add only if no such participant already exists (like {@link Set} works).
   *
   * @param parts the parts
   */
  public void addParticipants(Collection<UserInfo> parts) {
    for (UserInfo part : parts) {
      addParticipant(part);
    }
  }

  /**
   * Adds the participant. This method will add only if no such participant already exists (like {@link Set} works).
   *
   * @param part the part
   */
  public void addParticipant(UserInfo part) {
    participants.add(part);
  }

  /**
   * Removes the participants.
   *
   * @param parts the parts
   */
  public void removeParticipants(Collection<UserInfo> parts) {
    for (UserInfo part : parts) {
      removeParticipant(part);
    }
  }

  /**
   * Removes the participant.
   *
   * @param part the part
   */
  public void removeParticipant(UserInfo part) {
    participants.remove(part);
  }
  
  /**
   * Gets the state.
   *
   * @return the state
   */
  public String getState() {
    return state;
  }

  /**
   * Sets the state.
   *
   * @param state the new state
   */
  public void setState(String state) {
    this.state = state;
  }

  /**
   * Gets the last use date.
   *
   * @return the lastDate
   */
  public Date getLastDate() {
    return lastDate;
  }

  /**
   * Sets the last use date.
   *
   * @param lastDate the date to set
   */
  public void setLastDate(Date lastDate) {
    this.lastDate = lastDate;
  }

  /**
   * Gets the invite id.
   *
   * @return the invite id
   */
  public String getInviteId() {
    return inviteId;
  }

  /**
   * Sets the invite id.
   *
   * @param inviteId the new invite id
   */
  public void setInviteId(String inviteId) {
    this.inviteId = inviteId;
  }

  /**
   * Gets the start date.
   *
   * @return the startDate
   */
  public Date getStartDate() {
    return startDate;
  }

  /**
   * Sets the start date.
   *
   * @param startDate the startDate to set
   */
  public void setStartDate(Date startDate) {
    this.startDate = startDate;
  }

  /**
   * Gets the end date.
   *
   * @return the endDate
   */
  public Date getEndDate() {
    return endDate;
  }

  /**
   * Sets the end date.
   *
   * @param endDate the endDate to set
   */
  public void setEndDate(Date endDate) {
    this.endDate = endDate;
  }
}
