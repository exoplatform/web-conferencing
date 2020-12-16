
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
 * Identity abstraction for conversations in eXo web conferencing.<br>
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: IdentityInfo.java 00000 Mar 3, 2017 pnedonosko $
 * 
 */
public abstract class IdentityInfo {

  /** The Constant EMPTY. */
  public static final String EMPTY = "".intern();

  /** The title. */
  protected final String     title;

  /** The id. */
  protected final String     id;

  /** The avatar link. */
  protected String           avatarLink;

  /** The profile link. */
  protected String           profileLink;

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
  public void setAvatarLink(String avatarLink) {
    this.avatarLink = avatarLink;
  }

  /**
   * Sets the profile link.
   *
   * @param profileLink the new profile link
   */
  public void setProfileLink(String profileLink) {
    this.profileLink = profileLink;
  }

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

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((avatarLink == null) ? 0 : avatarLink.hashCode());
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    result = prime * result + ((profileLink == null) ? 0 : profileLink.hashCode());
    result = prime * result + ((title == null) ? 0 : title.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    IdentityInfo other = (IdentityInfo) obj;
    if (avatarLink == null) {
      if (other.avatarLink != null)
        return false;
    } else if (!avatarLink.equals(other.avatarLink))
      return false;
    if (id == null) {
      if (other.id != null)
        return false;
    } else if (!id.equals(other.id))
      return false;
    if (profileLink == null) {
      if (other.profileLink != null)
        return false;
    } else if (!profileLink.equals(other.profileLink))
      return false;
    if (title == null) {
      if (other.title != null)
        return false;
    } else if (!title.equals(other.title))
      return false;
    return true;
  }
  
}
