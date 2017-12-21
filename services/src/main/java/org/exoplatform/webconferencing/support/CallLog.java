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
package org.exoplatform.webconferencing.support;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Diagnostic logging support for user calls. This class gathers all logs related to the call, from
 * preparation of UI to processing a conversation.<br/>
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CallLog.java 00000 Dec 20, 2017 pnedonosko $
 * 
 */
public class CallLog {

  /** The Constant LOG. */
  private static final Log LOG = ExoLogger.getLogger(CallLog.class);

  /**
   * Instantiates a new call log (for internal use).
   */
  CallLog() {
  }

  public void info(String msg) {
    if (LOG.isInfoEnabled()) {
      LOG.info(msg);
    }
  }

  public void warn(String msg) {
    if (LOG.isWarnEnabled()) {
      LOG.warn(msg);
    }
  }

  public void error(String msg) {
    if (LOG.isErrorEnabled()) {
      LOG.error(msg);
    }
  }

  public void trace(String msg) {
    if (LOG.isTraceEnabled()) {
      LOG.trace(msg);
    }
  }

  /*protected String line(String msg) {
    StringBuilder line = new StringBuilder();
    line.append('[');
    line.append(providerId);
    line.append(']');
    line.append(' ');
    line.append(msg);
    return line.toString();
  }*/

}
