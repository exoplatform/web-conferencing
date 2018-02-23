/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
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
 * Identity state (persistent or transient) has wrong, erroneous or unexpected state.<br>
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: IdentityStateException.java 00000 Jan 25, 2018 pnedonosko $
 * 
 */
public class IdentityStateException extends WebConferencingException {

  /**
   * 
   */
  private static final long serialVersionUID = 4573843513604667736L;

  /**
   * Instantiates a new identity state exception.
   *
   * @param message the message
   */
  public IdentityStateException(String message) {
    super(message);
  }

  /**
   * Instantiates a new identity state exception.
   *
   * @param message the message
   * @param cause the cause
   */
  public IdentityStateException(String message, Throwable cause) {
    super(message, cause);
  }

}
