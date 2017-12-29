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

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.exoplatform.commons.api.persistence.ExoEntity;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CallEntity.java 00000 Dec 22, 2017 pnedonosko $
 */
@Entity(name = "WebConfCall")
@ExoEntity
@Table(name = "WBC_CALLS")
@NamedQueries({
    @NamedQuery(name = "WebConfCall.findGroupCallByOwnerId",
                query = "SELECT c FROM WebConfCall c WHERE c.isGroup = 1 AND c.ownerId = :ownerId"),
    @NamedQuery(name = "WebConfCall.findUserGroupCalls",
                query = "SELECT c FROM WebConfCall c, WebConfParticipant p WHERE c.id = p.callId AND p.id = :userId ORDER BY c.lastDate"),
    @NamedQuery(name = "WebConfCall.deleteOwnerOlderCalls",
                query = "DELETE FROM WebConfCall WHERE ownerType = :ownerType AND lastDate <= :expiredDate") })
public class CallEntity {

  /** The id. */
  @Id
  @Column(name = "ID")
  protected String  id;

  /** The provider type. */
  @Column(name = "PROVIDER_TYPE")
  protected String  providerType;

  /** The owner type. */
  @Column(name = "OWNER_TYPE")
  protected String  ownerType;

  /** The owner id. */
  @Column(name = "OWNER_ID")
  protected String  ownerId;

  /** The state. */
  @Column(name = "STATE")
  protected String  state;

  /** The title. */
  @Column(name = "TITLE")
  protected String  title;

  /** The settings. */
  @Column(name = "SETTINGS")
  protected String  settings;

  /** The call date. */
  @Column(name = "LAST_DATE")
  // @Temporal(TemporalType.DATE)
  protected Date    lastDate;

  /** The is group. */
  @Column(name = "IS_GROUP")
  protected Integer isGroup;

  /** The is user. */
  @Column(name = "IS_USER")
  protected Integer isUser;

  /**
   * Instantiates a new call entity.
   */
  public CallEntity() {
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
   * Sets the id.
   *
   * @param id the new id
   */
  public void setId(String id) {
    this.id = id;
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
   * Sets the provider type.
   *
   * @param providerType the new provider type
   */
  public void setProviderType(String providerType) {
    this.providerType = providerType;
  }

  /**
   * Gets the title.
   *
   * @return the title
   */
  public String getTitle() {
    return title;
  }

  /**
   * Sets the title.
   *
   * @param title the new title
   */
  public void setTitle(String title) {
    this.title = title;
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
   * Gets the settings.
   *
   * @return the settings
   */
  public String getSettings() {
    return settings;
  }

  /**
   * Sets the settings.
   *
   * @param settings the new settings
   */
  public void setSettings(String settings) {
    this.settings = settings;
  }

  /**
   * Gets the call last use date.
   *
   * @return the call last date
   */
  public Date getLastDate() {
    return lastDate;
  }

  /**
   * Sets the call last use date.
   *
   * @param lastDate the call last date
   */
  public void setLastDate(Date lastDate) {
    this.lastDate = lastDate;
  }

  /**
   * Gets the owner type.
   *
   * @return the owner type
   */
  public String getOwnerType() {
    return ownerType;
  }

  /**
   * Sets the owner type.
   *
   * @param ownerType the new owner type
   */
  public void setOwnerType(String ownerType) {
    this.ownerType = ownerType;
  }

  /**
   * Gets the owner id.
   *
   * @return the owner id
   */
  public String getOwnerId() {
    return ownerId;
  }

  /**
   * Sets the owner id.
   *
   * @param ownerId the new owner id
   */
  public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
  }

  /**
   * Gets the checks if is group.
   *
   * @return the checks if is group
   */
  public Integer getIsGroup() {
    return isGroup;
  }

  /**
   * Sets the checks if is group.
   *
   * @param isGroup the new checks if is group
   */
  public void setIsGroup(Integer isGroup) {
    this.isGroup = isGroup;
  }

  /**
   * Checks if is user.
   *
   * @return the integer
   */
  public Integer isUser() {
    return isUser;
  }

  /**
   * Sets the checks if is user.
   *
   * @param isUser the new checks if is user
   */
  public void setIsUser(Integer isUser) {
    this.isUser = isUser;
  }
}
