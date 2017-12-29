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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.exoplatform.commons.api.persistence.ExoEntity;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: ParticipantEntity.java 00000 Dec 22, 2017 pnedonosko $
 */
@Entity(name = "WebConfParticipant")
@ExoEntity
@Table(name = "WBC_PARTICIPANTS")
@IdClass(ParticipantId.class)
@NamedQueries({
    @NamedQuery(name = "WebConfCall.findCallParts",
                query = "SELECT p FROM WebConfParticipant p WHERE p.callId = :callId ORDER BY p.state, p.type"),
    @NamedQuery(name = "WebConfCall.deleteCallParts", 
                query = "DELETE FROM WebConfParticipant WHERE callId = :callId")
})
public class ParticipantEntity {

  /** The id. */
  @Id
  @Column(name = "ID")
  protected String id;

  /** The call id. */
  @Id
  @Column(name = "CALL_ID")
  protected String callId;

  /** The type. */
  @Column(name = "TYPE")
  protected String type;

  /** The state. */
  @Column(name = "STATE")
  protected String state;

  /**
   * Instantiates a new participant entity.
   */
  public ParticipantEntity() {
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
   * Gets the call id.
   *
   * @return the call id
   */
  public String getCallId() {
    return callId;
  }

  /**
   * Sets the call id.
   *
   * @param callId the new call id
   */
  public void setCallId(String callId) {
    this.callId = callId;
  }

  /**
   * Gets the type.
   *
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * Sets the type.
   *
   * @param type the new type
   */
  public void setType(String type) {
    this.type = type;
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
   * @param state the state to set
   */
  public void setState(String state) {
    this.state = state;
  }

}
