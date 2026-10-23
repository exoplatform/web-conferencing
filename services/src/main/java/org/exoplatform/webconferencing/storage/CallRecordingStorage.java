/*
 * Copyright (C) 2026 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <gnu.org/licenses>.
 */
package org.exoplatform.webconferencing.storage;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.utils.ISO8601;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.jcr.impl.core.query.QueryImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.webconferencing.CallInfo;
import org.exoplatform.webconferencing.UploadFileInfo;
import org.exoplatform.webconferencing.model.RecordingInfo;

/**
 * Reads and stamps the recordings of calls in JCR.
 * <p>
 * A recording saved by this add-on carries the {@value #EXO_CALL_RECORDING}
 * mixin, which names the call it belongs to. The lookup runs a JCR-SQL query in
 * the viewer's own session, so the repository's permissions decide which
 * recordings come back: a viewer gets exactly the files they can open in
 * Documents. A recording saved before the mixin existed is still found by its
 * file name, which Jibri builds as the call id, an underscore and a timestamp,
 * and both kinds are merged.
 */
public class CallRecordingStorage {

  /** The mixin naming the call a recording belongs to. */
  public static final String               EXO_CALL_RECORDING    = "exo:callRecording";

  /** The id of the call. */
  public static final String               EXO_CALL_ID           = "exo:callId";

  /** The type of the call provider. */
  public static final String               EXO_CALL_PROVIDER     = "exo:callProvider";

  /** The owner type of the call: space_event, space, user or chat_room. */
  public static final String               EXO_CALL_OWNER_TYPE   = "exo:callOwnerType";

  /** The owner id of the call. */
  public static final String               EXO_CALL_OWNER_ID     = "exo:callOwnerId";

  /** The title of the call. */
  public static final String               EXO_CALL_TITLE        = "exo:callTitle";

  /** The scheduled start of the call, when it has one. */
  public static final String               EXO_CALL_START_DATE   = "exo:callStartDate";

  /** When the recording was saved. */
  public static final String               EXO_RECORDED_DATE     = "exo:recordedDate";

  /** The folder that holds the recordings of a space or of a user. */
  public static final String               RECORDINGS_FOLDER     = "recordings";

  /** The most recordings one lookup returns. */
  public static final int                  MAX_RECORDINGS        = 100;

  /** The separator between the call title and the date in a recording title. */
  public static final String               TITLE_SEPARATOR       = " – ";

  private static final String              EXO_TITLE             = "exo:title";

  private static final String              EXO_RESTORE_LOCATION  = "exo:restoreLocation";

  private static final String              EXO_SYMLINK           = "exo:symlink";

  private static final String              NT_FILE               = "nt:file";

  private static final String              JCR_CONTENT           = "jcr:content";

  private static final String              JCR_DATA              = "jcr:data";

  private static final String              JCR_MIME_TYPE         = "jcr:mimeType";

  private static final String              JCR_LAST_MODIFIED     = "jcr:lastModified";

  private static final String              CMS_GROUPS_PATH       = "groupsPath";

  private static final String              USER_PRIVATE_PATH     = "userPrivate";

  private static final String              SPACE_DOCUMENTS       = "/Documents/";

  /**
   * The call ids the file-name fallback accepts: the characters a slugified
   * Jitsi call id is made of. Anything else would be read as a JCR name
   * pattern ({@code *}, {@code |}) or is not a legal JCR name character.
   */
  private static final Pattern             NAME_PATTERN_SAFE_ID  = Pattern.compile("[\\w.@-]+");

  private static final DateTimeFormatter   TITLE_DATE_FORMAT     = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  /** The start Jibri writes in a recording's file name, after the call id. */
  private static final Pattern             FILE_NAME_START       =
                                                         Pattern.compile("_(\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2})\\.[^.]+$");

  /**
   * How Jibri writes that start: the local time of the recorder's JVM, with
   * no zone, which {@code exo.webconferencing.recording.recorderTimeZone}
   * names.
   */
  private static final DateTimeFormatter   FILE_NAME_START_FORMAT =
                                                         DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");

  private static final Comparator<RecordingInfo> NEWEST_FIRST    =
                                                             Comparator.comparing(RecordingInfo::getRecordedDate,
                                                                                  Comparator.nullsLast(Comparator.reverseOrder()));

  private static final Log                 LOG                   = ExoLogger.getLogger(CallRecordingStorage.class);

  private final RepositoryService          repositoryService;

  private final NodeHierarchyCreator       nodeCreator;

  private final LinkManager                linkManager;

  private final ZoneId                     recorderZone;

  /**
   * Builds the storage.
   *
   * @param repositoryService the repository service
   * @param nodeCreator resolves the spaces' and users' folders
   * @param linkManager resolves the links shared to the participants of a call
   * @param recorderZone the time zone the recorder writes a recording's start
   *          in, UTC when null
   */
  public CallRecordingStorage(RepositoryService repositoryService,
                              NodeHierarchyCreator nodeCreator,
                              LinkManager linkManager,
                              ZoneId recorderZone) {
    this.repositoryService = repositoryService;
    this.nodeCreator = nodeCreator;
    this.linkManager = linkManager;
    this.recorderZone = recorderZone == null ? ZoneOffset.UTC : recorderZone;
  }

  /**
   * Builds the JCR-SQL statement listing the stamped recordings of a call,
   * newest first, optionally within a range of recording dates.
   *
   * @param callId the call id, quoted as a string literal
   * @param from the earliest recording date, inclusive, or <code>null</code>
   * @param to the latest recording date, inclusive, or <code>null</code>
   * @return the statement
   */
  public static String buildRecordingsQuery(String callId, Date from, Date to) {
    StringBuilder statement = new StringBuilder("SELECT * FROM ").append(EXO_CALL_RECORDING)
                                                                 .append(" WHERE ")
                                                                 .append(EXO_CALL_ID)
                                                                 .append(" = '")
                                                                 .append(StringUtils.replace(callId, "'", "''"))
                                                                 .append("'");
    if (from != null) {
      statement.append(" AND ").append(EXO_RECORDED_DATE).append(" >= TIMESTAMP '").append(format(from)).append("'");
    }
    if (to != null) {
      statement.append(" AND ").append(EXO_RECORDED_DATE).append(" <= TIMESTAMP '").append(format(to)).append("'");
    }
    return statement.append(" ORDER BY ").append(EXO_RECORDED_DATE).append(" DESC").toString();
  }

  /**
   * Tells whether a file name is the one Jibri gives a recording of a call:
   * the call id, an underscore, a timestamp and an extension. The match is on
   * the whole id, so a call id that prefixes another's (a space and a space
   * whose name starts with it) does not pick the other call's recordings.
   *
   * @param fileName the file name
   * @param callId the call id
   * @return true when the file is a recording of that call
   */
  public static boolean isRecordingFileOf(String fileName, String callId) {
    if (StringUtils.isBlank(fileName) || StringUtils.isBlank(callId)) {
      return false;
    }
    String baseName = fileName;
    int extension = baseName.lastIndexOf('.');
    if (extension > 0) {
      baseName = baseName.substring(0, extension);
    }
    int timestamp = baseName.lastIndexOf('_');
    return timestamp > 0 && baseName.substring(0, timestamp).equals(callId);
  }

  /**
   * Builds the title a recording shows in Documents: the call's title and the
   * moment it was recorded, in the server's time zone.
   *
   * @param callTitle the title of the call
   * @param recordedDate when the recording was saved
   * @return the title, or <code>null</code> when the call has no title
   */
  public static String recordingTitle(String callTitle, Calendar recordedDate) {
    if (StringUtils.isBlank(callTitle) || recordedDate == null) {
      return null;
    }
    String date = recordedDate.toInstant().atZone(ZoneId.systemDefault()).format(TITLE_DATE_FORMAT);
    return callTitle.trim() + TITLE_SEPARATOR + date;
  }

  /**
   * Stamps a recording's file with the call it belongs to and gives it the
   * call's title. A failure is logged and leaves the file as it is: the
   * recording itself must never be lost to its stamp.
   *
   * @param fileNode the recording's file, not saved yet
   * @param callId the call id the upload token carries
   * @param call the call, or <code>null</code> when it could not be read
   * @param uploadInfo the upload info, for the owner when the call is unknown
   * @param recordedDate when the recording is saved
   * @return true when the file is stamped
   */
  public boolean stamp(Node fileNode, String callId, CallInfo call, UploadFileInfo uploadInfo, Calendar recordedDate) {
    try {
      fileNode.addMixin(EXO_CALL_RECORDING);
      fileNode.setProperty(EXO_CALL_ID, callId);
      if (call != null && StringUtils.isNotBlank(call.getProviderType())) {
        fileNode.setProperty(EXO_CALL_PROVIDER, call.getProviderType());
      }
      String ownerType = call != null && call.getOwner() != null ? call.getOwner().getType() : uploadInfo.getType();
      String ownerId = call != null && call.getOwner() != null ? call.getOwner().getId() : uploadInfo.getIdentity();
      if (StringUtils.isNotBlank(ownerType)) {
        fileNode.setProperty(EXO_CALL_OWNER_TYPE, ownerType);
      }
      if (StringUtils.isNotBlank(ownerId)) {
        fileNode.setProperty(EXO_CALL_OWNER_ID, ownerId);
      }
      String callTitle = call == null ? null : call.getTitle();
      if (StringUtils.isNotBlank(callTitle)) {
        fileNode.setProperty(EXO_CALL_TITLE, callTitle);
      }
      if (call != null && call.getStartDate() != null) {
        fileNode.setProperty(EXO_CALL_START_DATE, toCalendar(call.getStartDate()));
      }
      fileNode.setProperty(EXO_RECORDED_DATE, recordedDate);
      String title = recordingTitle(callTitle, recordedDate);
      if (title != null) {
        fileNode.setProperty(EXO_TITLE, title);
      }
      return true;
    } catch (RepositoryException e) {
      LOG.warn("Cannot stamp the recording of call {} with its call, it is saved without it", callId, e);
      return false;
    }
  }

  /**
   * Lists the recordings of a call the viewer can open, newest first.
   * <p>
   * The stamped recordings are read by a query in the viewer's session. The
   * recordings saved before the stamp existed are found by listing the
   * recordings folder the call saves into for unstamped files named after the
   * call: the space's for a space or space-event call, the viewer's own
   * private folder otherwise, where the other participants' recordings arrive
   * as links.
   *
   * <p>
   * A JCR session logs in as the thread's current conversation state, whatever
   * the session provider was built with: the viewer is made current for the
   * read, the way ecms {@code WCMCoreUtils.invokeUserSession} does, and the
   * previous state restored, so the repository applies the viewer's own
   * rights whoever calls.
   *
   * @param viewer the viewer's security identity, whose session reads JCR
   * @param callId the call id
   * @param spaceGroupId the group id of the space owning the call, or
   *          <code>null</code> for a user or chat room call
   * @param from the earliest recording date, inclusive, or <code>null</code>
   * @param to the latest recording date, inclusive, or <code>null</code>
   * @return the recordings, newest first, at most {@value #MAX_RECORDINGS}
   * @throws RepositoryException when the repository fails
   */
  public List<RecordingInfo> findRecordings(Identity viewer,
                                            String callId,
                                            String spaceGroupId,
                                            Date from,
                                            Date to) throws RepositoryException {
    ConversationState previousState = ConversationState.getCurrent();
    ConversationState viewerState = new ConversationState(viewer);
    viewerState.setAttribute(ConversationState.SUBJECT, viewer.getSubject());
    SessionProvider sessionProvider = new SessionProvider(viewerState);
    ConversationState.setCurrent(viewerState);
    try {
      ManageableRepository repository = repositoryService.getCurrentRepository();
      Session session = sessionProvider.getSession(repository.getConfiguration().getDefaultWorkspaceName(), repository);
      return findRecordings(sessionProvider, session, viewer.getUserId(), callId, spaceGroupId, from, to);
    } finally {
      sessionProvider.close();
      ConversationState.setCurrent(previousState);
    }
  }

  /**
   * Lists the recordings of a call in an open viewer session: the stamped
   * ones, and the unstamped files named after the call in the recordings
   * folder the call saves into. Both are merged, so the recordings of a series
   * made before and after the stamp existed all come back.
   *
   * @param sessionProvider the viewer's session provider
   * @param session the viewer's session
   * @param userId the viewer's user id
   * @param callId the call id
   * @param spaceGroupId the group id of the space owning the call, or
   *          <code>null</code>
   * @param from the earliest recording date, inclusive, or <code>null</code>
   * @param to the latest recording date, inclusive, or <code>null</code>
   * @return the recordings, newest first, at most {@value #MAX_RECORDINGS}
   * @throws RepositoryException when the repository fails
   */
  List<RecordingInfo> findRecordings(SessionProvider sessionProvider,
                                     Session session,
                                     String userId,
                                     String callId,
                                     String spaceGroupId,
                                     Date from,
                                     Date to) throws RepositoryException {
    List<RecordingInfo> recordings = queryRecordings(session, callId, from, to);
    recordings.addAll(listRecordingsByName(sessionProvider, session, userId, callId, spaceGroupId, from, to));
    recordings.sort(NEWEST_FIRST);
    return recordings.size() > MAX_RECORDINGS ? new ArrayList<>(recordings.subList(0, MAX_RECORDINGS)) : recordings;
  }

  /**
   * Runs the stamped-recordings query in the viewer's session.
   *
   * @param session the viewer's session
   * @param callId the call id
   * @param from the earliest recording date, or <code>null</code>
   * @param to the latest recording date, or <code>null</code>
   * @return the recordings found, outside the trash
   * @throws RepositoryException when the repository fails
   */
  private List<RecordingInfo> queryRecordings(Session session, String callId, Date from, Date to) throws RepositoryException {
    Query query = session.getWorkspace()
                         .getQueryManager()
                         .createQuery(buildRecordingsQuery(callId, from, to), Query.SQL);
    if (query instanceof QueryImpl queryImpl) {
      queryImpl.setLimit(MAX_RECORDINGS);
    }
    List<RecordingInfo> recordings = new ArrayList<>();
    NodeIterator nodes = query.execute().getNodes();
    while (nodes.hasNext()) {
      Node node = nodes.nextNode();
      if (!isInTrash(node)) {
        recordings.add(toRecordingInfo(node));
      }
    }
    return recordings;
  }

  /**
   * Lists, in the recordings folder the call saves into, the unstamped files
   * named after the call: the recordings saved before the stamp existed. A
   * stamped file is left out, the query has already read it.
   *
   * @param sessionProvider the viewer's session provider
   * @param session the viewer's session
   * @param userId the viewer's user id
   * @param callId the call id
   * @param spaceGroupId the group id of the space owning the call, or
   *          <code>null</code>
   * @param from the earliest recording date, or <code>null</code>
   * @param to the latest recording date, or <code>null</code>
   * @return the recordings found, possibly empty
   * @throws RepositoryException when the repository fails
   */
  private List<RecordingInfo> listRecordingsByName(SessionProvider sessionProvider,
                                                   Session session,
                                                   String userId,
                                                   String callId,
                                                   String spaceGroupId,
                                                   Date from,
                                                   Date to) throws RepositoryException {
    if (!NAME_PATTERN_SAFE_ID.matcher(callId).matches()) {
      return new ArrayList<>();
    }
    Node folder = getRecordingsFolder(sessionProvider, session, userId, spaceGroupId);
    if (folder == null) {
      return new ArrayList<>();
    }
    List<RecordingInfo> recordings = new ArrayList<>();
    NodeIterator children = folder.getNodes(callId + "_*");
    while (children.hasNext()) {
      Node child = children.nextNode();
      if (!isRecordingFileOf(child.getName(), callId)) {
        continue;
      }
      Node file = resolveFile(child);
      if (file != null && !file.isNodeType(EXO_CALL_RECORDING) && !isInTrash(file)) {
        RecordingInfo recording = toRecordingInfo(file);
        if (isInRange(recording.getRecordedDate(), from, to)) {
          recordings.add(recording);
        }
      }
    }
    return recordings;
  }

  /**
   * Gets the recordings folder a call saves into, as the viewer sees it.
   *
   * @param sessionProvider the viewer's session provider
   * @param session the viewer's session
   * @param userId the viewer's user id
   * @param spaceGroupId the group id of the space owning the call, or
   *          <code>null</code> for the viewer's private folder
   * @return the folder, or <code>null</code> when it does not exist or the
   *         viewer cannot read it
   * @throws RepositoryException when the repository fails
   */
  private Node getRecordingsFolder(SessionProvider sessionProvider,
                                   Session session,
                                   String userId,
                                   String spaceGroupId) throws RepositoryException {
    try {
      if (spaceGroupId != null) {
        String path = nodeCreator.getJcrPath(CMS_GROUPS_PATH) + spaceGroupId + SPACE_DOCUMENTS + RECORDINGS_FOLDER;
        return (Node) session.getItem(path);
      }
      Node userNode = nodeCreator.getUserNode(sessionProvider, userId);
      return userNode.getNode(nodeCreator.getJcrPath(USER_PRIVATE_PATH)).getNode(RECORDINGS_FOLDER);
    } catch (PathNotFoundException | AccessDeniedException e) {
      LOG.debug("No readable recordings folder for user {} and space group {}", userId, spaceGroupId, e);
      return null;
    } catch (RepositoryException e) {
      throw e;
    } catch (Exception e) {
      throw new RepositoryException("Cannot read the private folder of user " + userId, e);
    }
  }

  /**
   * Resolves a recordings folder entry to the recording's file: itself, or the
   * target of the link a shared recording arrives as.
   *
   * @param child the folder entry
   * @return the file, or <code>null</code> when the entry is not a readable
   *         file
   * @throws RepositoryException when the repository fails
   */
  private Node resolveFile(Node child) throws RepositoryException {
    Node file = child;
    if (child.isNodeType(EXO_SYMLINK)) {
      try {
        file = linkManager.getTarget(child);
      } catch (ItemNotFoundException | AccessDeniedException e) {
        LOG.debug("The target of recording link {} is gone or not readable", child.getName(), e);
        return null;
      }
    }
    return file != null && file.isNodeType(NT_FILE) ? file : null;
  }

  /**
   * Reads a recording's file.
   *
   * @param file the recording's file
   * @return the recording
   * @throws RepositoryException when the repository fails
   */
  private RecordingInfo toRecordingInfo(Node file) throws RepositoryException {
    String title = file.hasProperty(EXO_TITLE) ? file.getProperty(EXO_TITLE).getString() : file.getName();
    Date recordedDate = null;
    long size = -1;
    String mimeType = null;
    Node content = file.hasNode(JCR_CONTENT) ? file.getNode(JCR_CONTENT) : null;
    if (file.hasProperty(EXO_RECORDED_DATE)) {
      recordedDate = file.getProperty(EXO_RECORDED_DATE).getDate().getTime();
    } else if (content != null && content.hasProperty(JCR_LAST_MODIFIED)) {
      recordedDate = content.getProperty(JCR_LAST_MODIFIED).getDate().getTime();
    }
    if (content != null) {
      if (content.hasProperty(JCR_DATA)) {
        size = content.getProperty(JCR_DATA).getLength();
      }
      if (content.hasProperty(JCR_MIME_TYPE)) {
        mimeType = content.getProperty(JCR_MIME_TYPE).getString();
      }
    }
    return new RecordingInfo(((ExtendedNode) file).getIdentifier(),
                             title,
                             recordedDate,
                             recordingStart(file.getName(), recorderZone),
                             size,
                             mimeType);
  }

  /**
   * Reads when a recording started from its file name, which Jibri writes as
   * {@code <callId>_<yyyy-MM-dd-HH-mm-ss>.<ext>} in its own local time. The
   * node keeps that name when its displayed title changes. A local time the
   * zone repeats when its clocks go back is ambiguous and reads as the earlier
   * of its two instants.
   *
   * @param fileName the recording's file name
   * @param zone the time zone the recorder writes that time in
   * @return the start, or null when the name does not carry one
   */
  static Date recordingStart(String fileName, ZoneId zone) {
    Matcher matcher = fileName == null ? null : FILE_NAME_START.matcher(fileName);
    if (matcher == null || !matcher.find()) {
      return null;
    }
    try {
      return Date.from(LocalDateTime.parse(matcher.group(1), FILE_NAME_START_FORMAT).atZone(zone).toInstant());
    } catch (DateTimeParseException e) {
      LOG.debug("The recording file name {} carries no readable start", fileName, e);
      return null;
    }
  }

  /**
   * Tells whether a node is in the trash.
   *
   * @param node the node
   * @return true when the node carries the trash's restore location
   * @throws RepositoryException when the repository fails
   */
  private static boolean isInTrash(Node node) throws RepositoryException {
    return node.isNodeType(EXO_RESTORE_LOCATION);
  }

  /**
   * Tells whether a date is within a range, each bound optional and
   * inclusive. An unknown date is in range only when no bound is given.
   *
   * @param date the date
   * @param from the lower bound, or <code>null</code>
   * @param to the upper bound, or <code>null</code>
   * @return true when the date is in range
   */
  private static boolean isInRange(Date date, Date from, Date to) {
    if (date == null) {
      return from == null && to == null;
    }
    return (from == null || !date.before(from)) && (to == null || !date.after(to));
  }

  /**
   * Formats a date as a JCR-SQL timestamp literal.
   *
   * @param date the date
   * @return its ISO 8601 form
   */
  private static String format(Date date) {
    return ISO8601.format(toCalendar(date));
  }

  /**
   * Converts a date to a calendar.
   *
   * @param date the date
   * @return the calendar
   */
  private static Calendar toCalendar(Date date) {
    Calendar calendar = new GregorianCalendar();
    calendar.setTime(date);
    return calendar;
  }

}
