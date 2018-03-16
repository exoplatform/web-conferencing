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

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

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
  private static final Log   LOG                               = ExoLogger.getLogger(CallLog.class);

  /** The Constant TRACE_LEVEL. */
  public static final String TRACE_LEVEL                       = "trace".intern();

  /** The Constant DEBUG_LEVEL. */
  public static final String DEBUG_LEVEL                       = "debug".intern();

  /** The Constant INFO_LEVEL. */
  public static final String INFO_LEVEL                        = "info".intern();

  /** The Constant WARN_LEVEL. */
  public static final String WARN_LEVEL                        = "warn".intern();

  /** The Constant ERROR_LEVEL. */
  public static final String ERROR_LEVEL                       = "error".intern();

  /** The Constant MESSAGE_NO_DATA. */
  public static final String MESSAGE_NO_DATA                   = "<no data>";

  /** Log message max length. */
  public static final int    MESSAGE_MAX_LENGTH                = 1024 * 8;

  /** Log message critical length. Values longer of this will be cut by the logger. */
  public static final int    MESSAGE_CRITICAL_LENGTH           = 1024 * 10;

  /** The Constant MESSAGE_CRITICAL_LENGTH_FINAL. */
  protected static final int MESSAGE_CRITICAL_LENGTH_FINAL     = MESSAGE_CRITICAL_LENGTH + 256;

  /** Maximum messages buffer size after which we can flush it to the logger. */
  public static final int    MESSAGES_BUFFER_MAX_SIZE          = 50;

  /** Maximum expected age of a message not flushed to the logger. */
  public static final int    MESSAGES_BUFFER_EXPIRATION_MILLIS = 60000;

  /** How long to wait between checks in flush thread. */
  public static final int    MESSAGES_BUFFER_WAIT_MILLIS       = 20000;

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
    if (msg.length() > MESSAGE_MAX_LENGTH) {
      if (msg.length() > MESSAGE_CRITICAL_LENGTH) {
        LOG.warn(new StringBuilder("Cut too long message: '").append(msg.substring(0, 64))
                                                             .append("...'. It's recommended to use log messages not longer of ")
                                                             .append(MESSAGE_MAX_LENGTH)
                                                             .append(" chars. All messages longer of ")
                                                             .append(MESSAGE_CRITICAL_LENGTH)
                                                             .append(" will be cut.")
                                                             .toString());
        return new StringBuilder(msg.substring(0, MESSAGE_CRITICAL_LENGTH)).append("...").toString();
      } else {
        LOG.warn(new StringBuilder("Message: '").append(msg.substring(0, 64))
                 .append("...' exceeds recommended length of ")
                 .append(MESSAGE_MAX_LENGTH)
                 .append(" chars. Avoid using longer messages due to possible performance impact.")
                 .toString());
      }
    }
    return msg;
  }

  abstract class Message implements Comparable<Message> {

    private final LocalDateTime timestamp;

    /**
     * Instantiates a new message with current time as timestamp.
     */
    Message() {
      timestamp = LocalDateTime.now();
    }

    /**
     * Instantiates a new message with given timestamp, if <code>null</code> given then current time will be
     * used.
     *
     * @param timestamp the timestamp
     */
    Message(LocalDateTime timestamp) {
      if (timestamp == null) {
        timestamp = LocalDateTime.now();
      }
      this.timestamp = timestamp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(Message o) {
      return this.getTimestamp().compareTo(o.getTimestamp());
    }

    /**
     * Gets the message timestamp.
     *
     * @return the timestamp
     */
    LocalDateTime getTimestamp() {
      return timestamp;
    }

    /**
     * Write message to the log.
     */
    abstract void log();
  }

  /**
   * The log messages buffer.
   */
  private final Queue<Message> messages = new ConcurrentLinkedQueue<>();

  /** The flusher for messages queue. */
  private Map<Boolean, Thread> flusher  = new ConcurrentHashMap<>(1);

  /**
   * Instantiates a new call log (for internal use).
   */
  CallLog() {
    try {
      Runtime.getRuntime().addShutdownHook(new Thread() {
        public void run() {
          flushAll();
        }
      });
    } catch (Throwable e) {
      LOG.warn("Failed to register shutdown hook " + e.getMessage());
    }
  }

  /**
   * Info message.
   *
   * @param msg the msg
   */
  public void info(String msg) {
    if (LOG.isInfoEnabled()) {
      messages.add(new Message() {
        /**
         * {@inheritDoc}
         */
        @Override
        void log() {
          LOG.info(validateFinal(msg));
        }
      });
      flush();
    }
  }

  /**
   * Info message with timestamp.
   *
   * @param msg the msg
   * @param timestamp the timestamp, if <code>null</code> then current time will be used
   */
  public void info(String msg, LocalDateTime timestamp) {
    if (LOG.isInfoEnabled()) {
      messages.add(new Message(timestamp) {
        /**
         * {@inheritDoc}
         */
        @Override
        void log() {
          LOG.info(validateFinal(msg));
        }
      });
      flush();
    }
  }

  /**
   * Warn message.
   *
   * @param msg the msg
   */
  public void warn(String msg) {
    if (LOG.isWarnEnabled()) {
      messages.add(new Message() {
        /**
         * {@inheritDoc}
         */
        @Override
        void log() {
          LOG.warn(validateFinal(msg));
        }
      });
      flush();
    }
  }

  /**
   * Warn message with timestamp.
   *
   * @param msg the msg
   * @param timestamp the timestamp, if <code>null</code> then current time will be used
   */
  public void warn(String msg, LocalDateTime timestamp) {
    if (LOG.isWarnEnabled()) {
      messages.add(new Message(timestamp) {
        /**
         * {@inheritDoc}
         */
        @Override
        void log() {
          LOG.warn(validateFinal(msg));
        }
      });
      flush();
    }
  }

  /**
   * Error message.
   *
   * @param msg the msg
   */
  public void error(String msg) {
    if (LOG.isErrorEnabled()) {
      messages.add(new Message() {
        /**
         * {@inheritDoc}
         */
        @Override
        void log() {
          LOG.error(validateFinal(msg));
        }
      });
      flush();
    }
  }

  /**
   * Error message with timestamp.
   *
   * @param msg the msg
   * @param timestamp the timestamp, if <code>null</code> then current time will be used
   */
  public void error(String msg, LocalDateTime timestamp) {
    if (LOG.isErrorEnabled()) {
      messages.add(new Message(timestamp) {
        /**
         * {@inheritDoc}
         */
        @Override
        void log() {
          LOG.error(validateFinal(msg));
        }
      });
      flush();
    }
  }

  /**
   * Debug message.
   *
   * @param msg the msg
   */
  public void debug(String msg) {
    if (LOG.isDebugEnabled()) {
      messages.add(new Message() {
        /**
         * {@inheritDoc}
         */
        @Override
        void log() {
          LOG.debug(validateFinal(msg));
        }
      });
      flush();
    }
  }

  /**
   * Debug message with timestamp.
   *
   * @param msg the msg
   * @param timestamp the timestamp, if <code>null</code> then current time will be used
   */
  public void debug(String msg, LocalDateTime timestamp) {
    if (LOG.isDebugEnabled()) {
      messages.add(new Message(timestamp) {
        /**
         * {@inheritDoc}
         */
        @Override
        void log() {
          LOG.debug(validateFinal(msg));
        }
      });
      flush();
    }
  }

  /**
   * Trace message.
   *
   * @param msg the msg
   */
  public void trace(String msg) {
    if (LOG.isTraceEnabled()) {
      messages.add(new Message() {
        /**
         * {@inheritDoc}
         */
        @Override
        void log() {
          LOG.trace(validateFinal(msg));
        }
      });
      flush();
    }
  }

  /**
   * Trace message with timestamp.
   *
   * @param msg the msg
   * @param timestamp the timestamp, if <code>null</code> then current time will be used
   */
  public void trace(String msg, LocalDateTime timestamp) {
    if (LOG.isTraceEnabled()) {
      messages.add(new Message(timestamp) {
        /**
         * {@inheritDoc}
         */
        @Override
        void log() {
          LOG.trace(validateFinal(msg));
        }
      });
      flush();
    }
  }

  // ********** Internals **********

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
   * Flush all messages from the buffer (but first sort them in timestamp order). It's thread-safe operation.
   */
  private void flushAll() {
    // Flush buffer to real log: we take available messages only
    int buffLen = messages.size();
    if (buffLen > 0) {
      List<Message> buff = new ArrayList<>(buffLen);
      do {
        Message m = messages.poll(); // this removes the message from the queue
        if (m != null) {
          buff.add(m);
          buffLen--;
        } else {
          break;
        }
      } while (buffLen > 0);
      // sort messages (rely on Comparable of Message) and log them
      buff.stream().sorted().forEach(m -> {
        m.log();
      });
    }
  }

  /**
   * Check if need wait for messages buffer, or can flush it to server log.
   *
   * @return true, if successful
   */
  private boolean waitForMessages() {
    if (messages.size() > MESSAGES_BUFFER_MAX_SIZE) {
      // If have more than MESSAGES_BUFFER_MAX_SIZE messages, then can flush
      return false;
    }
    Message m = messages.peek();
    if (m != null && m.getTimestamp()
                      .isBefore(LocalDateTime.now(ZoneOffset.UTC).minus(MESSAGES_BUFFER_EXPIRATION_MILLIS, ChronoUnit.MILLIS))) {
      // If first message in the queue is older of MESSAGES_BUFFER_EXPIRATION_MILLIS, then can flush
      return false;
    }
    // Otherwise wait
    return true;
  }

  /**
   * Create new flush thread.
   *
   * @return the thread
   */
  private Thread newFlushThread() {
    Thread t = new Thread(CallLog.class.getName() + "-flusher") {
      /**
       * {@inheritDoc}
       */
      @Override
      public void run() {
        do {
          try {
            Thread.sleep(MESSAGES_BUFFER_WAIT_MILLIS);
          } catch (InterruptedException e) {
            LOG.warn("Error sleeping in flusher thread: " + e.getMessage());
          }
        } while (waitForMessages());
        try {
          flusher.remove(Boolean.TRUE, this);
          flushAll();
        } catch (Throwable e) {
          LOG.error("Error flushing messages to log", e);
        }
      }
    };
    t.setDaemon(false);
    t.start();
    return t;
  }

  /**
   * Flush available log messages to server log (messages will be sorted timestamp order).
   *
   */
  private void flush() {
    flusher.compute(Boolean.TRUE, (k, t) -> {
      if (t == null || !t.isAlive()) {
        return newFlushThread();
      }
      return t;
    });
  }

}
