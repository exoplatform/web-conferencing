/*
 * Copyright (C) 2003-2020 eXo Platform SAS.
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
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: OriginId.java 00000 Dec 18, 2020 pnedonosko $
 */
public class OriginId implements Serializable {

  /** The id. */
  protected String id;

  /** The call id. */
  protected String callId;
  
  /** The call type. */
  protected String type;

  /**
   * Instantiates a new participant group id.
   */
  public OriginId() {
  }

  /**
   * Instantiates a new participant group id.
   *
   * @param id the id
   * @param callId the call id
   * @param type the type
   */
  public OriginId(String id, String callId, String type) {
    super();
    this.id = id;
    this.callId = callId;
    this.type = type;
  }

  /**
   * {@inheritDoc}
   */
  public boolean equals(Object o) {
    if (o != null) {
      if (OriginId.class.isAssignableFrom(o.getClass())) {
        OriginId other = OriginId.class.cast(o);
        return id.equals(other.getId()) && callId.equals(other.getCallId()) && type.equals(other.getType());
      }
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  public int hashCode() {
    int prime = 31;
    int res = 7 + id.hashCode();
    res = res * prime + callId.hashCode();
    res = res * prime + type.hashCode();
    return res;
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
   * Gets the call id.
   *
   * @return the call id
   */
  public String getCallId() {
    return callId;
  }

  /**
   * Gets the type.
   *
   * @return the type
   */
  public String getType() {
    return type;
  }

}
