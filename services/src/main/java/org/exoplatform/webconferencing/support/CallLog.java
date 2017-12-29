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
 * preparation of UI to processing a conversation.<br>
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CallLog.java 00000 Dec 20, 2017 pnedonosko $
 * 
 */
public class CallLog {

  /** The Constant LOG. */
  private static final Log   LOG                           = ExoLogger.getLogger(CallLog.class);

  /** The Constant TRACE_LEVEL. */
  public static final String TRACE_LEVEL                   = "trace".intern();

  /** The Constant DEBUG_LEVEL. */
  public static final String DEBUG_LEVEL                   = "debug".intern();

  /** The Constant INFO_LEVEL. */
  public static final String INFO_LEVEL                    = "info".intern();

  /** The Constant WARN_LEVEL. */
  public static final String WARN_LEVEL                    = "warn".intern();

  /** The Constant ERROR_LEVEL. */
  public static final String ERROR_LEVEL                   = "error".intern();

  /** The Constant MESSAGE_NO_DATA. */
  public static final String MESSAGE_NO_DATA               = "<no data>";

  /** Log message max length. */
  public static final int    MESSAGE_MAX_LENGTH            = 1024 * 2;

  /** Log message critical length. Values longer of this will be cut by the logger. */
  public static final int    MESSAGE_CRITICAL_LENGTH       = 1024 * 10;

  /** The Constant MESSAGE_CRITICAL_LENGTH_FINAL. */
  protected static final int MESSAGE_CRITICAL_LENGTH_FINAL = MESSAGE_CRITICAL_LENGTH + 256;

  /**
   * Checks if is message valid. If not valid, a log warn also will be reported about the length.
   *
   * @param msg the msg
   * @return true, if is valid
   */
  public static boolean isSafe(String msg) {
    return msg == null || msg.length() <= MESSAGE_MAX_LENGTH;
  }

  /**
   * Validate a message by cutting it if it is longer of {@value #MESSAGE_CRITICAL_LENGTH} bytes.
   *
   * @param msg the msg
   * @return the string
   */
  public static String validate(String msg) {
    if (msg == null || msg.length() == 0) {
      return MESSAGE_NO_DATA; // we accept empty data
    }
    if (isSafe(msg)) {
      if (msg != null && msg.length() > MESSAGE_CRITICAL_LENGTH) {
        LOG.warn(new StringBuilder("Cut loo long message: '").append(msg.substring(0, 64))
                                                             .append("...'. It's recommended to use log messages not longer of ")
                                                             .append(MESSAGE_MAX_LENGTH)
                                                             .append(" chars. All messages longer of ")
                                                             .append(MESSAGE_CRITICAL_LENGTH)
                                                             .append(" will be cut.")
                                                             .toString());
        return new StringBuilder(msg.substring(0, MESSAGE_CRITICAL_LENGTH)).append("...").toString();
      }
    } else {
      LOG.warn(new StringBuilder("Message: '").append(msg.substring(0, 64))
                                              .append("...' exceeds recommeded length of ")
                                              .append(MESSAGE_MAX_LENGTH)
                                              .append(" chars. Avoid using longer messages due to possible performance impact.")
                                              .toString());
    }
    return msg;
  }

  /**
   * Instantiates a new call log (for internal use).
   */
  CallLog() {
  }

  /**
   * Validate final.
   *
   * @param msg the msg
   * @return the string
   */
  protected String validateFinal(String msg) {
    if (msg != null && msg.length() > MESSAGE_CRITICAL_LENGTH_FINAL) {
      return new StringBuilder(msg.substring(0, MESSAGE_CRITICAL_LENGTH_FINAL)).append("...").toString();
    }
    return msg;
  }

  /**
   * Info.
   *
   * @param msg the msg
   */
  public void info(String msg) {
    if (LOG.isInfoEnabled()) {
      LOG.info(validateFinal(msg));
    }
  }

  /**
   * Warn.
   *
   * @param msg the msg
   */
  public void warn(String msg) {
    if (LOG.isWarnEnabled()) {
      LOG.warn(validateFinal(msg));
    }
  }

  /**
   * Error.
   *
   * @param msg the msg
   */
  public void error(String msg) {
    if (LOG.isErrorEnabled()) {
      LOG.error(validateFinal(msg));
    }
  }

  /**
   * Debug.
   *
   * @param msg the msg
   */
  public void debug(String msg) {
    if (LOG.isDebugEnabled()) {
      LOG.debug(validateFinal(msg));
    }
  }

  /**
   * Trace.
   *
   * @param msg the msg
   */
  public void trace(String msg) {
    if (LOG.isTraceEnabled()) {
      LOG.trace(validate(msg));
    }
  }

}
