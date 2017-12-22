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
package org.exoplatform.webconferencing.domain;

import java.util.Collection;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.exoplatform.commons.api.persistence.ExoEntity;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CallEntity.java 00000 Dec 22, 2017 pnedonosko $
 * 
 */
@Entity(name = "WebConfCall")
@ExoEntity
@Table(name = "WBC_CALLS")
@NamedQueries({ 
  @NamedQuery(
              name = "WebConfCall.findGroupCallByOwnerId", 
              query = "SELECT c FROM WebConfCall c WHERE c.isGroup = 1 AND c.ownerId = :ownerId"
  ),
  @NamedQuery(
              name = "WebConfCall.findUserGroupCalls",
              query = "SELECT c FROM WebConfCall c, WebConfParticipant p WHERE c.id = p.callId AND p.id = :userId ORDER BY c.callDate"
  ),
  @NamedQuery(
              name = "WebConfCall.deleteOwnerOlderCalls",
              query = "DELETE FROM WebConfCall WHERE ownerType = :ownerType AND callDate <= :expiredDate"
  )
})
public class CallEntity {

  /** The id. */
  @Id
  @Column(name = "ID")
  protected String                        id;

  @Column(name = "PROVIDER_TYPE")
  protected String                        providerType;

  @Column(name = "OWNER_TYPE")
  protected String                        ownerType;

  @Column(name = "OWNER_ID")
  protected String                        ownerId;

  @Column(name = "STATE")
  protected String                        state;

  @Column(name = "TITLE")
  protected String                        title;

  @Column(name = "SETTINGS")
  protected String                        settings;

  @Column(name = "CALL_DATE")
  // @Temporal(TemporalType.DATE)
  protected Date                          callDate;

  @Column(name = "IS_GROUP")
  protected Integer                       isGroup;

  @Column(name = "IS_USER")
  protected Integer                       isUser;

  @OneToMany
  @JoinColumn(name = "CALL_ID")
  protected Collection<ParticipantEntity> participants;

  /**
   * 
   */
  public CallEntity() {
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getProviderType() {
    return providerType;
  }

  public void setProviderType(String providerType) {
    this.providerType = providerType;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public String getSettings() {
    return settings;
  }

  public void setSettings(String settings) {
    this.settings = settings;
  }

  public Date getCallDate() {
    return callDate;
  }

  public void setCallDate(Date callDate) {
    this.callDate = callDate;
  }

  public String getOwnerType() {
    return ownerType;
  }

  public void setOwnerType(String ownerType) {
    this.ownerType = ownerType;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
  }

  public Integer getIsGroup() {
    return isGroup;
  }

  public void setIsGroup(Integer isGroup) {
    this.isGroup = isGroup;
  }

  public Integer isUser() {
    return isUser;
  }

  public void setIsUser(Integer isUser) {
    this.isUser = isUser;
  }

  public Collection<ParticipantEntity> getParticipants() {
    return participants;
  }

  public void setParticipants(Collection<ParticipantEntity> participants) {
    this.participants = participants;
  }

}
