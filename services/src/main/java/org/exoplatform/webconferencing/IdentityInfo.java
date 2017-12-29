
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
 * Identity abstraction for conversations in eXo video calls.<br>
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: IdentityInfo.java 00000 Mar 3, 2017 pnedonosko $
 * 
 */
public abstract class IdentityInfo {

  /** The Constant ID_MAX_LENGTH. */
  public static final int    ID_MAX_LENGTH = 255;

  /** The Constant EMPTY. */
  public static final String EMPTY         = "".intern();

  /** The title. */
  protected final String     title;

  /** The id. */
  protected final String     id;

  /** The avatar link. */
  protected String           avatarLink;

  /** The profile link. */
  protected String           profileLink;

  /**
   * Checks is ID valid (not null, not empty and not longer of {@value #ID_MAX_LENGTH}) chars.
   *
   * @param id the id
   * @return true, if is valid id
   */
  public static boolean isValidId(String id) {
    return id != null && id.length() > 0 && id.length() <= ID_MAX_LENGTH;
  }

  /**
   * Instantiates a new identity info.
   *
   * @param id the id
   * @param title the title
   */
  public IdentityInfo(String id, String title) {
    this.id = id;
    this.title = title;
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
   * Gets the title.
   *
   * @return the title
   */
  public String getTitle() {
    return title;
  }

  /**
   * Gets the avatar link.
   *
   * @return the avatar link
   */
  public String getAvatarLink() {
    return avatarLink;
  }

  /**
   * Gets the profile link.
   *
   * @return the profile link
   */
  public String getProfileLink() {
    return profileLink;
  }

  /**
   * Sets the avatar link.
   *
   * @param avatarLink the new avatar link
   */
  protected void setAvatarLink(String avatarLink) {
    this.avatarLink = avatarLink;
  }

  /**
   * Sets the profile link.
   *
   * @param profileLink the new profile link
   */
  protected void setProfileLink(String profileLink) {
    this.profileLink = profileLink;
  }

  // TODO
  // /**
  // * Gets the entity.
  // *
  // * @return the entity
  // */
  // @Transient // to avoid serialization to JSON
  // protected ParticipantEntity getEntity() {
  // return entity;
  // }
  //
  // /**
  // * Sets the entity.
  // *
  // * @param entity the entity to set
  // */
  // @Transient // to avoid serialization to JSON
  // protected void setEntity(ParticipantEntity entity) {
  // this.entity = entity;
  // }

  /**
   * Checks if it is a group.
   *
   * @return true, if is a group
   */
  public abstract boolean isGroup();

  /**
   * Gets the identity type.
   *
   * @return the type
   */
  public abstract String getType();

}
