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

import java.io.Serializable;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: ParticipantEntity.java 00000 Dec 22, 2017 pnedonosko $
 * 
 */
public class ParticipantId implements Serializable {

  protected String id;

  protected String callId;

  /**
   * Instantiates a new participant id.
   */
  public ParticipantId() {
  }

  /**
   * Instantiates a new participant id.
   *
   * @param id the id
   * @param callId the call id
   */
  public ParticipantId(String id, String callId) {
    super();
    this.id = id;
    this.callId = callId;
  }

  /**
   * {@inheritDoc}
   */
  public boolean equals(Object o) {
    if (o != null) {
      if (ParticipantId.class.isAssignableFrom(o.getClass())) {
        ParticipantId other = ParticipantId.class.cast(o);
        return id.equals(other.getId()) && callId.equals(other.getCallId());
      }
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  public int hashCode() {
    return (7 + id.hashCode()) * 31 + callId.hashCode();
  }

  public String getId() {
    return id;
  }

  public String getCallId() {
    return callId;
  }

}
