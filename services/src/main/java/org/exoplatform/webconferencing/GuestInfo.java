
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

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Represent guests in calls. 
 * 
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: GuestInfo.java 00000 Feb 23, 2017 pnedonosko $
 */
public class GuestInfo extends UserInfo {

  /** The Constant TYPE_NAME. */
  public static final String TYPE_NAME = "guest".intern();

  /** The Constant LOG. */
  protected static final Log LOG       = ExoLogger.getLogger(GuestInfo.class);

  /**
   * Instantiates a new user info.
   *
   * @param userId the user id in the system
   */
  public GuestInfo(String userId) {
    super(userId, EMPTY, EMPTY);
  }
  
  /**
   * Instantiates a new user info.
   *
   * @param userId the user id in the system
   * @param firstName the first type
   * @param lastName the last type
   */
  public GuestInfo(String userId, String firstName, String lastName) {
    super(userId, firstName, lastName);
  }
  
  /**
   * Instantiates a new guest info based on userInfo
   *
   * @param userInfo the user info
   */
  public GuestInfo(UserInfo userInfo) {
    super(userInfo.getId(), userInfo.getFirstName(), userInfo.getLastName());
    this.setAvatarLink(userInfo.getAvatarLink());
    this.setProfileLink(userInfo.getProfileLink());
    this.setClientId(userInfo.getClientId());
    this.setState(userInfo.getState());
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public String getType() {
    return TYPE_NAME;
  }
}
