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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.NamespaceRegistryImpl;
import org.exoplatform.services.jcr.impl.core.query.DefaultQueryNodeFactory;
import org.exoplatform.services.jcr.impl.core.query.sql.JCRSQLQueryBuilder;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.webconferencing.CallInfo;
import org.exoplatform.webconferencing.IdentityInfo;
import org.exoplatform.webconferencing.UploadFileInfo;
import org.exoplatform.webconferencing.model.RecordingInfo;

@ExtendWith(MockitoExtension.class)
class CallRecordingStorageTest {

  private static final String  CALL_ID         = "g_42-0b6e1f0c9d3a4e2f";

  private static final String  SPACE_GROUP_ID  = "/spaces/sales";

  private static final String  RECORDINGS_PATH = "/Groups/spaces/sales/Documents/recordings";

  @Mock
  private RepositoryService    repositoryService;

  @Mock
  private NodeHierarchyCreator nodeCreator;

  @Mock
  private LinkManager          linkManager;

  @Mock
  private SessionProvider      sessionProvider;

  @Mock
  private Session              session;

  @Mock
  private QueryManager         queryManager;

  @Mock
  private Query                query;

  private CallRecordingStorage storage;

  /**
   * A storage over mocked JCR collaborators, its query manager answering the
   * stamped-recordings query.
   *
   * @throws Exception never
   */
  @BeforeEach
  void setUp() throws Exception {
    storage = new CallRecordingStorage(repositoryService, nodeCreator, linkManager, null);
    Workspace workspace = mock(Workspace.class);
    lenient().when(session.getWorkspace()).thenReturn(workspace);
    lenient().when(workspace.getQueryManager()).thenReturn(queryManager);
    lenient().when(queryManager.createQuery(anyString(), eq(Query.SQL))).thenReturn(query);
    lenient().when(nodeCreator.getJcrPath("groupsPath")).thenReturn("/Groups");
  }

  /**
   * The statement must be accepted by the repository's own JCR-SQL parser,
   * with and without a range, and with a quote in the id.
   *
   * @throws Exception when the parser refuses it
   */
  @Test
  void theQueryIsParsedByTheRepositoryEngine() throws Exception {
    Date from = Date.from(Instant.parse("2026-10-23T09:00:00Z"));
    Date to = Date.from(Instant.parse("2026-10-23T22:00:00Z"));
    for (String statement : List.of(CallRecordingStorage.buildRecordingsQuery(CALL_ID, null, null),
                                    CallRecordingStorage.buildRecordingsQuery(CALL_ID, from, to),
                                    CallRecordingStorage.buildRecordingsQuery("it's", from, null))) {
      assertNotNull(JCRSQLQueryBuilder.createQuery(statement,
                                                   new LocationFactory(new NamespaceRegistryImpl()),
                                                   new DefaultQueryNodeFactory()),
                    statement);
    }
  }

  /**
   * The repository reads as the viewer whoever the calling thread runs as:
   * the viewer is the current state while the session logs in, and the
   * caller's state is back afterwards.
   *
   * @throws Exception never
   */
  @Test
  void theRepositoryReadsAsTheViewerWhateverTheCaller() throws Exception {
    Identity viewer = new Identity("mary");
    ConversationState caller = new ConversationState(new Identity("john"));
    ConversationState.setCurrent(caller);
    try {
      ManageableRepository repository = mock(ManageableRepository.class, RETURNS_DEEP_STUBS);
      when(repositoryService.getCurrentRepository()).thenReturn(repository);
      when(repository.getConfiguration().getDefaultWorkspaceName()).thenReturn("collaboration");
      String[] loggedInAs = new String[1];
      when(repository.login("collaboration")).thenAnswer(invocation -> {
        loggedInAs[0] = ConversationState.getCurrent().getIdentity().getUserId();
        throw new RepositoryException("stop");
      });

      assertThrows(RepositoryException.class,
                   () -> storage.findRecordings(viewer, CALL_ID, SPACE_GROUP_ID, null, null));

      assertEquals("mary", loggedInAs[0]);
      assertEquals(caller, ConversationState.getCurrent());
    } finally {
      ConversationState.setCurrent(null);
    }
  }

  /**
   * A recording's start is read from the name Jibri gives its file, here in UTC,
   * and is unknown for any other name.
   */
  @Test
  void aRecordingStartIsReadFromItsFileName() {
    assertEquals(Date.from(Instant.parse("2026-10-23T14:05:00Z")),
                 CallRecordingStorage.recordingStart("g_15-41126d_2026-10-23-14-05-00.mp4", ZoneOffset.UTC));
    assertEquals(Date.from(Instant.parse("2026-10-23T14:05:00Z")),
                 CallRecordingStorage.recordingStart("call_with_underscores_2026-10-23-14-05-00.webm", ZoneOffset.UTC));
    assertNull(CallRecordingStorage.recordingStart("ACME demo.mp4", ZoneOffset.UTC));
    assertNull(CallRecordingStorage.recordingStart("g_15_2026-13-45-99-99-99.mp4", ZoneOffset.UTC));
    assertNull(CallRecordingStorage.recordingStart("g_15_2026-10-23-14-05-00.mp4.bak.txt", ZoneOffset.UTC));
    assertNull(CallRecordingStorage.recordingStart(null, ZoneOffset.UTC));
  }

  /**
   * The recorder writes its own local time, so a recorder in Paris names a
   * recording started at 14:05 UTC in October after 16:05.
   */
  @Test
  void aRecordingStartIsReadInTheRecordersTimeZone() {
    assertEquals(Date.from(Instant.parse("2026-10-23T14:05:00Z")),
                 CallRecordingStorage.recordingStart("g_15-41126d_2026-10-23-16-05-00.mp4", ZoneId.of("Europe/Paris")));
  }

  /**
   * Paris writes 02:30 twice on the night its clocks go back, at 00:30 and
   * 01:30 UTC; the name reads as the earlier one.
   */
  @Test
  void aRepeatedLocalTimeReadsAsItsEarlierInstant() {
    assertEquals(Date.from(Instant.parse("2026-10-25T00:30:00Z")),
                 CallRecordingStorage.recordingStart("g_15-41126d_2026-10-25-02-30-00.mp4", ZoneId.of("Europe/Paris")));
  }

  /**
   * The statement selects the mixin by call id, bounds the recording date
   * from below and from above with the right operators, and sorts newest
   * first; a quote in the id is doubled, never left to close the literal.
   */
  @Test
  void theQuerySelectsTheCallWithinTheRange() {
    Date from = Date.from(Instant.parse("2026-10-23T09:00:00Z"));
    Date to = Date.from(Instant.parse("2026-10-23T22:00:00Z"));
    String statement = CallRecordingStorage.buildRecordingsQuery(CALL_ID, from, to);
    assertTrue(statement.startsWith("SELECT * FROM exo:callRecording WHERE exo:callId = '" + CALL_ID + "'"), statement);
    assertTrue(statement.contains(" AND exo:recordedDate >= TIMESTAMP '"), statement);
    assertTrue(statement.contains(" AND exo:recordedDate <= TIMESTAMP '"), statement);
    assertTrue(statement.indexOf(">=") < statement.indexOf("<="), "the lower bound is the from date: " + statement);
    assertTrue(statement.endsWith(" ORDER BY exo:recordedDate DESC"), statement);
    String unbounded = CallRecordingStorage.buildRecordingsQuery(CALL_ID, null, null);
    assertFalse(unbounded.contains("exo:recordedDate >="), unbounded);
    assertFalse(unbounded.contains("exo:recordedDate <="), unbounded);
    assertTrue(CallRecordingStorage.buildRecordingsQuery("it's", null, null).contains("exo:callId = 'it''s'"));
  }

  /**
   * A Jibri file name is the call id, an underscore and a timestamp: the
   * match is on the whole id, so a call whose id prefixes another's does not
   * pick the other's recordings.
   */
  @Test
  void aFileNameMatchesItsWholeCallIdOnly() {
    assertTrue(CallRecordingStorage.isRecordingFileOf("g_sales_2026-10-23-10-00-00.mp4", "g_sales"));
    assertTrue(CallRecordingStorage.isRecordingFileOf(CALL_ID + "_2026-10-23-10-00-00.mp4", CALL_ID));
    assertTrue(CallRecordingStorage.isRecordingFileOf("g_sales_2026-10-23-10-00-00", "g_sales"));
    assertFalse(CallRecordingStorage.isRecordingFileOf("g_sales_team_2026-10-23-10-00-00.mp4", "g_sales"));
    assertFalse(CallRecordingStorage.isRecordingFileOf("g_sales.mp4", "g_sales"));
    assertFalse(CallRecordingStorage.isRecordingFileOf("other_2026.mp4", "g_sales"));
    assertFalse(CallRecordingStorage.isRecordingFileOf(null, "g_sales"));
  }

  /**
   * The displayed title is the call's title and the recording's date; a call
   * with no title keeps the file's name.
   */
  @Test
  void theTitleNamesTheMeetingAndItsDate() {
    Calendar recordedDate = new GregorianCalendar(TimeZone.getDefault());
    recordedDate.set(2026, Calendar.OCTOBER, 23, 10, 5, 0);
    assertEquals("ACME demo – 2026-10-23 10:05", CallRecordingStorage.recordingTitle(" ACME demo ", recordedDate));
    assertNull(CallRecordingStorage.recordingTitle(" ", recordedDate));
    assertNull(CallRecordingStorage.recordingTitle(null, recordedDate));
  }

  /**
   * A saved recording records its call, provider, owner, title, start and
   * recording date, and shows the meeting's title.
   *
   * @throws Exception never
   */
  @Test
  void stampRecordsTheCall() throws Exception {
    Node fileNode = mock(Node.class);
    IdentityInfo owner = mock(IdentityInfo.class);
    when(owner.getType()).thenReturn("space_event");
    when(owner.getId()).thenReturn("42");
    CallInfo call = new CallInfo(CALL_ID, "ACME demo", owner, "jitsi");
    Date start = Date.from(Instant.parse("2026-10-23T09:00:00Z"));
    call.setStartDate(start);
    Calendar recordedDate = new GregorianCalendar();

    assertTrue(storage.stamp(fileNode, CALL_ID, call, uploadInfo(), recordedDate));

    verify(fileNode).addMixin("exo:callRecording");
    verify(fileNode).setProperty("exo:callId", CALL_ID);
    verify(fileNode).setProperty("exo:callProvider", "jitsi");
    verify(fileNode).setProperty("exo:callOwnerType", "space_event");
    verify(fileNode).setProperty("exo:callOwnerId", "42");
    verify(fileNode).setProperty("exo:callTitle", "ACME demo");
    verify(fileNode).setProperty(eq("exo:callStartDate"), any(Calendar.class));
    verify(fileNode).setProperty("exo:recordedDate", recordedDate);
    verify(fileNode).setProperty("exo:title", CallRecordingStorage.recordingTitle("ACME demo", recordedDate));
  }

  /**
   * When the call cannot be read, the stamp still records the call id and the
   * owner the upload token names, and keeps the file's own title.
   *
   * @throws Exception never
   */
  @Test
  void stampWithoutTheCallUsesTheUploadToken() throws Exception {
    Node fileNode = mock(Node.class);
    Calendar recordedDate = new GregorianCalendar();

    assertTrue(storage.stamp(fileNode, CALL_ID, null, uploadInfo(), recordedDate));

    verify(fileNode).setProperty("exo:callId", CALL_ID);
    verify(fileNode).setProperty("exo:callOwnerType", "space_event");
    verify(fileNode).setProperty("exo:callOwnerId", "42");
    verify(fileNode).setProperty("exo:recordedDate", recordedDate);
    verify(fileNode, never()).setProperty(eq("exo:title"), anyString());
    verify(fileNode, never()).setProperty(eq("exo:callProvider"), anyString());
  }

  /**
   * A repository without the mixin must not lose the recording: the stamp
   * fails quietly and the caller saves the file.
   *
   * @throws Exception never
   */
  @Test
  void aStampFailureNeverThrows() throws Exception {
    Node fileNode = mock(Node.class);
    doThrow(new NoSuchNodeTypeException("exo:callRecording")).when(fileNode).addMixin("exo:callRecording");

    assertFalse(storage.stamp(fileNode, CALL_ID, null, uploadInfo(), new GregorianCalendar()));
  }

  /**
   * Stamped recordings come from the query, the trashed one left out; the
   * folder adds the unstamped ones of a series recorded before the stamp,
   * never a stamped one a second time; all newest first.
   *
   * @throws Exception never
   */
  @Test
  void stampedAndOldRecordingsAreMergedOnce() throws Exception {
    ExtendedNode older = file("id-older", CALL_ID + "_2026-10-22-10-00-00.mp4", "2026-10-22T10:00:00Z", true, false);
    ExtendedNode newer = file("id-newer", CALL_ID + "_2026-10-23-10-00-00.mp4", "2026-10-23T10:00:00Z", true, false);
    ExtendedNode trashed = file("id-trashed", "trashed.mp4", "2026-10-24T10:00:00Z", true, true);
    queryAnswers(older, trashed, newer);
    ExtendedNode old = file("id-old", CALL_ID + "_2026-10-01-10-00-00.mp4", "2026-10-01T11:00:00Z", false, false);
    Node folder = mock(Node.class);
    when(session.getItem(RECORDINGS_PATH)).thenReturn(folder);
    NodeIterator children = iterator(older, old, newer);
    when(folder.getNodes(CALL_ID + "_*")).thenReturn(children);

    List<RecordingInfo> recordings = storage.findRecordings(sessionProvider, session, "john", CALL_ID, SPACE_GROUP_ID, null, null);

    assertEquals(List.of("id-newer", "id-older", "id-old"), recordings.stream().map(RecordingInfo::getDocumentId).toList());
    assertEquals(CALL_ID + "_2026-10-23-10-00-00.mp4 title", recordings.get(0).getTitle());
    assertEquals(1234L, recordings.get(0).getSizeBytes());
    assertEquals("video/mp4", recordings.get(0).getMimeType());
    assertEquals(Date.from(Instant.parse("2026-10-23T10:00:00Z")), recordings.get(0).getStartedDate());
  }

  /**
   * A storage told the recorder runs in Paris reads a listed recording's
   * start in that zone.
   *
   * @throws Exception never
   */
  @Test
  void aListedRecordingStartsInTheRecordersTimeZone() throws Exception {
    storage = new CallRecordingStorage(repositoryService, nodeCreator, linkManager, ZoneId.of("Europe/Paris"));
    ExtendedNode newer = file("id-newer", CALL_ID + "_2026-10-23-12-00-00.mp4", "2026-10-23T10:30:00Z", true, false);
    queryAnswers(newer);
    when(session.getItem(RECORDINGS_PATH)).thenThrow(new PathNotFoundException());

    List<RecordingInfo> recordings = storage.findRecordings(sessionProvider, session, "john", CALL_ID, SPACE_GROUP_ID, null, null);

    assertEquals(Date.from(Instant.parse("2026-10-23T10:00:00Z")), recordings.get(0).getStartedDate());
  }

  /**
   * With no stamped recording, the space's recordings folder is listed for
   * the files of that call only, within the range, links resolved.
   *
   * @throws Exception never
   */
  @Test
  void oldRecordingsAreFoundByTheirFileName() throws Exception {
    queryAnswers();
    Node folder = mock(Node.class);
    when(session.getItem(RECORDINGS_PATH)).thenReturn(folder);
    ExtendedNode inRange = file("id-in", CALL_ID + "_2026-10-23-10-00-00.mp4", "2026-10-23T11:00:00Z", false, false);
    ExtendedNode outOfRange = file("id-out", CALL_ID + "_2026-10-20-10-00-00.mp4", "2026-10-20T11:00:00Z", false, false);
    ExtendedNode otherCall = file("id-other", CALL_ID + "x_2026-10-23-10-00-00.mp4", "2026-10-23T11:00:00Z", false, false);
    Node link = mock(Node.class);
    when(link.getName()).thenReturn(CALL_ID + "_2026-10-23-12-00-00.mp4");
    when(link.isNodeType("exo:symlink")).thenReturn(true);
    ExtendedNode linked = file("id-linked", "target.mp4", "2026-10-23T13:00:00Z", false, false);
    when(linkManager.getTarget(link)).thenReturn(linked);
    NodeIterator children = iterator(inRange, outOfRange, otherCall, link);
    when(folder.getNodes(CALL_ID + "_*")).thenReturn(children);

    List<RecordingInfo> recordings = storage.findRecordings(sessionProvider,
                                                            session,
                                                            "john",
                                                            CALL_ID,
                                                            SPACE_GROUP_ID,
                                                            Date.from(Instant.parse("2026-10-23T09:00:00Z")),
                                                            Date.from(Instant.parse("2026-10-23T23:00:00Z")));

    assertEquals(List.of("id-linked", "id-in"), recordings.stream().map(RecordingInfo::getDocumentId).toList());
  }

  /**
   * A user or chat-room call's old recordings are in the viewer's own private
   * recordings folder, where the shared ones arrive as links.
   *
   * @throws Exception never
   */
  @Test
  void oldRecordingsOfAUserCallAreInTheViewersPrivateFolder() throws Exception {
    queryAnswers();
    Node userNode = mock(Node.class);
    Node privateNode = mock(Node.class);
    Node folder = mock(Node.class);
    when(nodeCreator.getUserNode(sessionProvider, "john")).thenReturn(userNode);
    when(nodeCreator.getJcrPath("userPrivate")).thenReturn("Private");
    when(userNode.getNode("Private")).thenReturn(privateNode);
    when(privateNode.getNode("recordings")).thenReturn(folder);
    ExtendedNode recording = file("id-p2p", "p_john-mary_2026-10-23-10-00-00.mp4", "2026-10-23T11:00:00Z", false, false);
    NodeIterator children = iterator(recording);
    when(folder.getNodes("p_john-mary_*")).thenReturn(children);

    List<RecordingInfo> recordings = storage.findRecordings(sessionProvider, session, "john", "p_john-mary", null, null, null);

    assertEquals(List.of("id-p2p"), recordings.stream().map(RecordingInfo::getDocumentId).toList());
  }

  /**
   * A missing or unreadable folder, or an id that is not a safe name pattern,
   * answers no recording rather than an error.
   *
   * @throws Exception never
   */
  @Test
  void noFolderOrAnUnsafeIdAnswersNothing() throws Exception {
    queryAnswers();
    when(session.getItem(RECORDINGS_PATH)).thenThrow(new PathNotFoundException(RECORDINGS_PATH));
    assertTrue(storage.findRecordings(sessionProvider, session, "john", CALL_ID, SPACE_GROUP_ID, null, null).isEmpty());

    queryAnswers();
    assertTrue(storage.findRecordings(sessionProvider, session, "john", "a*|b", SPACE_GROUP_ID, null, null).isEmpty());
    verify(session, never()).getItem("/Groups/spaces/sales/Documents/recordings/a*|b");
  }

  /**
   * The upload info of a space-event recording.
   *
   * @return the upload info
   */
  private static UploadFileInfo uploadInfo() {
    return new UploadFileInfo(CALL_ID, "42", "space_event", "john", List.of("john", "mary"));
  }

  /**
   * Makes the stamped-recordings query answer the given nodes.
   *
   * @param nodes the nodes
   * @throws Exception never
   */
  private void queryAnswers(Node... nodes) throws Exception {
    QueryResult result = mock(QueryResult.class);
    NodeIterator iterator = iterator(nodes);
    when(query.execute()).thenReturn(result);
    when(result.getNodes()).thenReturn(iterator);
  }

  /**
   * A node iterator over the given nodes.
   *
   * @param nodes the nodes
   * @return the iterator
   */
  private static NodeIterator iterator(Node... nodes) {
    Iterator<Node> delegate = Arrays.asList(nodes).iterator();
    NodeIterator iterator = mock(NodeIterator.class);
    lenient().when(iterator.hasNext()).thenAnswer(invocation -> delegate.hasNext());
    lenient().when(iterator.nextNode()).thenAnswer(invocation -> delegate.next());
    return iterator;
  }

  /**
   * A recording's file.
   *
   * @param id its identifier
   * @param name its name
   * @param date its recording date, ISO 8601
   * @param stamped true when the date is the stamp's, false when it is the
   *          content's last modification
   * @param trashed true when the file is in the trash
   * @return the file
   * @throws Exception never
   */
  private static ExtendedNode file(String id, String name, String date, boolean stamped, boolean trashed) throws Exception {
    ExtendedNode file = mock(ExtendedNode.class);
    Calendar calendar = new GregorianCalendar();
    calendar.setTime(Date.from(Instant.parse(date)));
    lenient().when(file.getIdentifier()).thenReturn(id);
    lenient().when(file.getName()).thenReturn(name);
    lenient().when(file.isNodeType("nt:file")).thenReturn(true);
    lenient().when(file.isNodeType("exo:restoreLocation")).thenReturn(trashed);
    lenient().when(file.hasProperty("exo:title")).thenReturn(true);
    Property title = mock(Property.class);
    lenient().when(title.getString()).thenReturn(name + " title");
    lenient().when(file.getProperty("exo:title")).thenReturn(title);
    Node content = mock(Node.class);
    lenient().when(file.hasNode("jcr:content")).thenReturn(true);
    lenient().when(file.getNode("jcr:content")).thenReturn(content);
    Property dateProperty = mock(Property.class);
    lenient().when(dateProperty.getDate()).thenReturn(calendar);
    lenient().when(file.isNodeType("exo:callRecording")).thenReturn(stamped);
    if (stamped) {
      lenient().when(file.hasProperty("exo:recordedDate")).thenReturn(true);
      lenient().when(file.getProperty("exo:recordedDate")).thenReturn(dateProperty);
    } else {
      lenient().when(content.hasProperty("jcr:lastModified")).thenReturn(true);
      lenient().when(content.getProperty("jcr:lastModified")).thenReturn(dateProperty);
    }
    Property data = mock(Property.class);
    lenient().when(data.getLength()).thenReturn(1234L);
    lenient().when(content.hasProperty("jcr:data")).thenReturn(true);
    lenient().when(content.getProperty("jcr:data")).thenReturn(data);
    Property mimeType = mock(Property.class);
    lenient().when(mimeType.getString()).thenReturn("video/mp4");
    lenient().when(content.hasProperty("jcr:mimeType")).thenReturn(true);
    lenient().when(content.getProperty("jcr:mimeType")).thenReturn(mimeType);
    return file;
  }

}
