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
package org.exoplatform.webconferencing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.fileupload2.core.FileUploadException;
import javax.jcr.Node;
import javax.jcr.Session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;
import org.exoplatform.wcm.ext.component.document.service.ShareDocumentService;
import org.exoplatform.web.security.codec.CodecInitializer;
import org.exoplatform.webconferencing.dao.CallDAO;
import org.exoplatform.webconferencing.dao.InviteDAO;
import org.exoplatform.webconferencing.dao.OriginDAO;
import org.exoplatform.webconferencing.dao.ParticipantDAO;
import org.exoplatform.webconferencing.model.ConferenceCall;
import org.exoplatform.webconferencing.model.RecordingInfo;
import org.exoplatform.webconferencing.storage.CallRecordingStorage;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The recordings side of the service: the recording upload's own limit, the
 * lookup of a call from its meeting link, and who may list a call's
 * recordings.
 */
@ExtendWith(MockitoExtension.class)
class WebConferencingServiceRecordingsTest {

  private static final String    CALL_ID     = "g_42-0b6e1f0c9d3a4e2f";

  private static final String    GROUP_ID    = "/spaces/sales";

  private static final String    VIEWER      = "john";

  @Mock
  private UploadService          uploadService;

  @Mock
  private IdentityRegistry       identityRegistry;

  @Mock
  private SpaceService           spaceService;

  @Mock
  private CallRecordingStorage   recordingStorage;

  @Mock
  private ListenerService        listenerService;

  @Mock
  private RepositoryService      repositoryService;

  @Mock
  private SessionProviderService sessionProviders;

  @Mock
  private NodeHierarchyCreator   nodeCreator;

  @Mock
  private IdentityManager        identityManager;

  private WebConferencingService service;

  /**
   * A service over mocked collaborators, with its recording settings.
   */
  @BeforeEach
  void setUp() {
    service = spy(newService(null, null));
  }

  /**
   * The recording upload registers its own limit, unlimited by default,
   * before the upload is read, and removes it once done.
   *
   * @throws Exception never
   */
  @Test
  void theRecordingUploadIsUnlimitedByDefault() throws Exception {
    uploadAndCheckLimit(service, 0);
  }

  /**
   * An administrator's limit is the one the recording upload registers.
   *
   * @throws Exception never
   */
  @Test
  void theRecordingUploadTakesTheConfiguredLimit() throws Exception {
    uploadAndCheckLimit(spy(newService("500", null)), 500);
  }

  /**
   * An upload that cannot even be read still removes its limit.
   *
   * @throws Exception never
   */
  @Test
  void aFailedUploadRemovesItsLimit() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    doThrow(new FileUploadException("broken")).when(uploadService).createUploadResource(anyString(), eq(request));

    assertThrows(UploadFileException.class, () -> service.uploadFile(uploadInfo(), request));

    ArgumentCaptor<String> uploadId = ArgumentCaptor.forClass(String.class);
    verify(uploadService).addUploadLimit(uploadId.capture(), eq(0));
    verify(uploadService).removeUploadLimit(uploadId.getValue());
  }

  /**
   * A saved recording is stamped with the call the upload token names, before
   * the folder is saved.
   *
   * @throws Exception never
   */
  @Test
  void aSavedRecordingIsStampedWithItsCall() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    File stored = File.createTempFile("recording", ".mp4");
    stored.deleteOnExit();
    UploadResource resource = mock(UploadResource.class);
    when(uploadService.getUploadResource(anyString())).thenReturn(resource);
    when(resource.getStatus()).thenReturn(UploadResource.UPLOADED_STATUS);
    when(resource.getFileName()).thenReturn(CALL_ID + "_2026-10-23-10-00-00.mp4");
    when(resource.getStoreLocation()).thenReturn(stored.getAbsolutePath());
    CallInfo call = spaceEventCall();
    doReturn(call).when(service).getCall(CALL_ID);
    viewer();
    org.exoplatform.social.core.identity.model.Identity spaceIdentity = new org.exoplatform.social.core.identity.model.Identity("space",
                                                                                                                                   "sales");
    when(identityManager.getIdentity("42")).thenReturn(spaceIdentity);
    Space space = space();
    space.setPrettyName("sales");
    when(spaceService.getSpaceByPrettyName("sales")).thenReturn(space);
    when(spaceService.isMember(space, VIEWER)).thenReturn(true);
    ManageableRepository repository = mock(ManageableRepository.class, RETURNS_DEEP_STUBS);
    when(repositoryService.getCurrentRepository()).thenReturn(repository);
    when(repository.getConfiguration().getDefaultWorkspaceName()).thenReturn("collaboration");
    SessionProvider sessionProvider = mock(SessionProvider.class);
    Session session = mock(Session.class);
    when(sessionProviders.getSystemSessionProvider(null)).thenReturn(sessionProvider);
    when(sessionProviders.getSessionProvider(any())).thenReturn(sessionProvider);
    when(sessionProvider.getSession("collaboration", repository)).thenReturn(session);
    when(nodeCreator.getJcrPath("groupsPath")).thenReturn("/Groups");
    Node spaceRoot = mock(Node.class);
    Node documents = mock(Node.class);
    Node recordingsFolder = mock(Node.class);
    Node fileNode = mock(Node.class);
    when(session.getItem("/Groups" + GROUP_ID)).thenReturn(spaceRoot);
    when(spaceRoot.getNode("Documents")).thenReturn(documents);
    when(documents.getPath()).thenReturn("/Groups" + GROUP_ID + "/Documents");
    when(session.getItem("/Groups" + GROUP_ID + "/Documents")).thenReturn(documents);
    when(documents.hasNode("recordings")).thenReturn(true);
    when(documents.getNode("recordings")).thenReturn(recordingsFolder);
    when(recordingsFolder.isNodeType("nt:unstructured")).thenReturn(true);
    when(recordingsFolder.addNode(CALL_ID + "_2026-10-23-10-00-00.mp4", "nt:file")).thenReturn(fileNode);
    when(fileNode.addNode("jcr:content", "nt:resource")).thenReturn(mock(Node.class));

    service.uploadFile(uploadInfo(), request);

    InOrder order = inOrder(recordingStorage, documents);
    order.verify(recordingStorage).stamp(eq(fileNode), eq(CALL_ID), eq(call), any(UploadFileInfo.class), any(Calendar.class));
    order.verify(documents).save();
  }

  /**
   * A recording the platform refuses for another reason than its size still
   * notifies the failure, with the owner the upload token names.
   *
   * @throws Exception never
   */
  @Test
  @SuppressWarnings("unchecked")
  void aFailedUploadNotifiesItsFailure() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    UploadResource resource = mock(UploadResource.class);
    when(uploadService.getUploadResource(anyString())).thenReturn(resource);
    when(resource.getStatus()).thenReturn(UploadResource.FAILED_STATUS);
    when(resource.getEstimatedSize()).thenReturn(1048576d);
    CallInfo call = new CallInfo(CALL_ID, "ACME demo", new UserInfo(VIEWER, "John", "Smith"), "jitsi");
    doReturn(call).when(service).getCall(CALL_ID);

    service.uploadFile(uploadInfo(), request);

    ArgumentCaptor<Map<String, String>> info = ArgumentCaptor.forClass(Map.class);
    verify(listenerService).broadcast(eq(WebConferencingService.EVENT_CALL_RECORDED), eq(call), info.capture());
    assertEquals(WebConferencingService.STATUS_NOT_OK, info.getValue().get("status"));
    assertEquals("space_event", info.getValue().get("type"));
    assertEquals("42", info.getValue().get("identity"));
  }

  /**
   * A malformed or negative setting falls back to its default.
   */
  @Test
  void aMalformedSettingFallsBackToItsDefault() {
    PropertiesParam params = new PropertiesParam();
    params.setProperty("a", "-3");
    params.setProperty("b", "abc");
    params.setProperty("c", " 7 ");
    assertEquals(12, WebConferencingService.readNonNegativeInt(params, "a", 12));
    assertEquals(12, WebConferencingService.readNonNegativeInt(params, "b", 12));
    assertEquals(7, WebConferencingService.readNonNegativeInt(params, "c", 12));
    assertEquals(12, WebConferencingService.readNonNegativeInt(params, "missing", 12));
    assertEquals(12, WebConferencingService.readNonNegativeInt(null, "a", 12));
  }

  /**
   * The recorder's time zone is read as a zone id, UTC when it is absent or
   * not a zone.
   */
  @Test
  void theRecordersTimeZoneFallsBackToUtc() {
    PropertiesParam params = new PropertiesParam();
    params.setProperty("paris", " Europe/Paris ");
    params.setProperty("bad", "Mars/Olympus");
    assertEquals(ZoneId.of("Europe/Paris"), WebConferencingService.readZone(params, "paris"));
    assertEquals(ZoneOffset.UTC, WebConferencingService.readZone(params, "bad"));
    assertEquals(ZoneOffset.UTC, WebConferencingService.readZone(params, "missing"));
    assertEquals(ZoneOffset.UTC, WebConferencingService.readZone(null, "paris"));
  }

  /**
   * A meeting link is read by the active provider that recognises it; an
   * inactive provider is not asked.
   */
  @Test
  void aMeetingLinkIsReadByAnActiveProvider() {
    CallProvider inactive = provider("other", false, CALL_ID);
    CallProvider jitsi = provider("jitsi", true, CALL_ID);
    service.providers.put("other", inactive);
    service.providers.put("jitsi", jitsi);

    ConferenceCall conferenceCall = service.findCallByUrl("https://host/jitsi/meet/" + CALL_ID + "?inviteId=x");

    assertEquals(new ConferenceCall("jitsi", CALL_ID), conferenceCall);
    verify(inactive, never()).findCallId(anyString());
  }

  /**
   * A link no active provider recognises reads as held on another provider.
   */
  @Test
  void aLinkNoProviderRecognisesIsAnotherProvider() {
    service.providers.put("jitsi", provider("jitsi", true, null));

    assertNull(service.findCallByUrl("https://teams.microsoft.com/l/meetup-join/x"));
    ObjectNotFoundException e = assertThrows(ObjectNotFoundException.class,
                                             () -> service.getCallRecordingsByConference("https://teams.microsoft.com/x",
                                                                                         VIEWER,
                                                                                         null,
                                                                                         null));
    assertEquals(WebConferencingService.RECORDINGS_OTHER_PROVIDER, e.getMessage());
  }

  /**
   * A space-event call's recordings are listed for a member, in the space's
   * folder, the grace added to the end of the range only.
   *
   * @throws Exception never
   */
  @Test
  void aMemberListsTheSpaceRecordingsWithTheGraceAdded() throws Exception {
    Identity viewer = viewer();
    Space space = space();
    when(spaceService.isMember(space, VIEWER)).thenReturn(true);
    doReturn(spaceEventCall()).when(service).getCall(CALL_ID);
    doReturn("preview").when(service).buildDocumentPreviewUrl(anyString());
    List<RecordingInfo> found = new ArrayList<>(List.of(new RecordingInfo("doc1", "t", new Date(), 1, "video/mp4")));
    Date from = Date.from(Instant.parse("2026-10-23T09:00:00Z"));
    Date to = Date.from(Instant.parse("2026-10-23T10:00:00Z"));
    when(recordingStorage.findRecordings(viewer, CALL_ID, GROUP_ID, from, Date.from(Instant.parse("2026-10-23T22:00:00Z"))))
                                                                                                                        .thenReturn(found);

    List<RecordingInfo> recordings = service.getCallRecordings(CALL_ID, VIEWER, from, to);

    assertSame(found, recordings);
    assertEquals("preview", recordings.get(0).getPreviewUrl());
  }

  /**
   * With no range, nothing is added.
   *
   * @throws Exception never
   */
  @Test
  void noRangeStaysOpen() throws Exception {
    Identity viewer = viewer();
    when(spaceService.isMember(space(), VIEWER)).thenReturn(true);
    doReturn(spaceEventCall()).when(service).getCall(CALL_ID);
    when(recordingStorage.findRecordings(viewer, CALL_ID, GROUP_ID, null, null)).thenReturn(new ArrayList<>());

    assertEquals(List.of(), service.getCallRecordings(CALL_ID, VIEWER, null, null));
  }

  /**
   * A non-member of the call's space, invited directly, is refused before
   * any read, with the code the Meeting block shows.
   *
   * @throws Exception never
   */
  @Test
  void aNonMemberOfTheSpaceIsRefused() throws Exception {
    viewer();
    Space space = space();
    when(spaceService.isMember(space, VIEWER)).thenReturn(false);
    when(spaceService.isSuperManager(VIEWER)).thenReturn(false);
    CallInfo call = spaceEventCall();
    call.addParticipant(new UserInfo(VIEWER, "John", "Smith"));
    doReturn(call).when(service).getCall(CALL_ID);

    IllegalAccessException e = assertThrows(IllegalAccessException.class,
                                            () -> service.getCallRecordings(CALL_ID, VIEWER, null, null));

    assertEquals(WebConferencingService.RECORDINGS_NOT_SPACE_MEMBER, e.getMessage());
    verify(recordingStorage, never()).findRecordings(any(), anyString(), any(), any(), any());
  }

  /**
   * A platform space administrator is let through to the repository, whose
   * permissions still decide.
   *
   * @throws Exception never
   */
  @Test
  void aSpaceAdministratorIsLetThrough() throws Exception {
    Identity viewer = viewer();
    Space space = space();
    when(spaceService.isMember(space, VIEWER)).thenReturn(false);
    when(spaceService.isSuperManager(VIEWER)).thenReturn(true);
    doReturn(spaceEventCall()).when(service).getCall(CALL_ID);
    when(recordingStorage.findRecordings(viewer, CALL_ID, GROUP_ID, null, null)).thenReturn(new ArrayList<>());

    assertEquals(List.of(), service.getCallRecordings(CALL_ID, VIEWER, null, null));
  }

  /**
   * A space call (not an event's) is checked against its space as well.
   *
   * @throws Exception never
   */
  @Test
  void aSpaceCallIsCheckedAgainstItsSpace() throws Exception {
    viewer();
    Space space = space();
    when(spaceService.isMember(space, VIEWER)).thenReturn(false);
    WebConferencingService.SpaceInfo owner = mock(WebConferencingService.SpaceInfo.class);
    when(owner.getGroupId()).thenReturn(GROUP_ID);
    doReturn(new CallInfo(CALL_ID, "Sales", owner, "jitsi")).when(service).getCall(CALL_ID);

    assertThrows(IllegalAccessException.class, () -> service.getCallRecordings(CALL_ID, VIEWER, null, null));
  }

  /**
   * A user or chat-room call's recordings are listed for a participant, in
   * the viewer's own folder, and refused to anyone else.
   *
   * @throws Exception never
   */
  @Test
  void aUserCallIsListedForItsParticipantsOnly() throws Exception {
    Identity viewer = viewer();
    UserInfo owner = new UserInfo("mary", "Mary", "Doe");
    CallInfo call = new CallInfo("p_john-mary", "John and Mary", owner, "jitsi");
    call.addParticipant(new UserInfo(VIEWER, "John", "Smith"));
    doReturn(call).when(service).getCall("p_john-mary");
    when(recordingStorage.findRecordings(eq(viewer), eq("p_john-mary"), isNull(), isNull(), isNull())).thenReturn(new ArrayList<>());

    assertEquals(List.of(), service.getCallRecordings("p_john-mary", VIEWER, null, null));

    CallInfo strangers = new CallInfo("p_ann-mary", "Ann and Mary", owner, "jitsi");
    strangers.addParticipant(new UserInfo("ann", "Ann", "Lee"));
    doReturn(strangers).when(service).getCall("p_ann-mary");
    IllegalAccessException e = assertThrows(IllegalAccessException.class,
                                            () -> service.getCallRecordings("p_ann-mary", VIEWER, null, null));
    assertEquals(WebConferencingService.RECORDINGS_NOT_PARTICIPANT, e.getMessage());
  }

  /**
   * The owner of a call is let through even when not listed as a
   * participant.
   *
   * @throws Exception never
   */
  @Test
  void theOwnerOfAUserCallIsLetThrough() throws Exception {
    Identity viewer = viewer();
    CallInfo call = new CallInfo("p_john-mary", "John and Mary", new UserInfo(VIEWER, "John", "Smith"), "jitsi");
    doReturn(call).when(service).getCall("p_john-mary");
    when(recordingStorage.findRecordings(eq(viewer), eq("p_john-mary"), isNull(), isNull(), isNull())).thenReturn(new ArrayList<>());

    assertEquals(List.of(), service.getCallRecordings("p_john-mary", VIEWER, null, null));
  }

  /**
   * An unknown call, an unknown viewer, an invalid id and an inverted range
   * are refused with their codes.
   *
   * @throws Exception never
   */
  @Test
  void refusalsCarryTheirCodes() throws Exception {
    viewer();
    doReturn(null).when(service).getCall(CALL_ID);
    assertEquals(WebConferencingService.RECORDINGS_CALL_NOT_FOUND,
                 assertThrows(ObjectNotFoundException.class,
                              () -> service.getCallRecordings(CALL_ID, VIEWER, null, null)).getMessage());

    doThrow(new InvalidCallException("broken")).when(service).getCall("broken");
    assertThrows(ObjectNotFoundException.class, () -> service.getCallRecordings("broken", VIEWER, null, null));

    assertEquals(WebConferencingService.RECORDINGS_NO_IDENTITY,
                 assertThrows(IllegalAccessException.class,
                              () -> service.getCallRecordings(CALL_ID, null, null, null)).getMessage());

    assertEquals(WebConferencingService.RECORDINGS_INVALID_CALL_ID,
                 assertThrows(IllegalArgumentException.class,
                              () -> service.getCallRecordings("", VIEWER, null, null)).getMessage());

    Date later = Date.from(Instant.parse("2026-10-24T00:00:00Z"));
    Date earlier = Date.from(Instant.parse("2026-10-23T00:00:00Z"));
    assertEquals(WebConferencingService.RECORDINGS_INVALID_RANGE,
                 assertThrows(IllegalArgumentException.class,
                              () -> service.getCallRecordings(CALL_ID, VIEWER, later, earlier)).getMessage());
  }

  /**
   * Uploads a recording that the platform refuses and checks the limit the
   * upload registered, removed once done, and before the upload is read.
   *
   * @param webConferencing the service
   * @param expectedLimit the limit expected
   * @throws Exception never
   */
  private void uploadAndCheckLimit(WebConferencingService webConferencing, int expectedLimit) throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    UploadResource resource = mock(UploadResource.class);
    when(uploadService.getUploadResource(anyString())).thenReturn(resource);
    when(resource.getStatus()).thenReturn(UploadResource.FAILED_STATUS);
    doReturn(new CallInfo(CALL_ID, "ACME demo", new UserInfo(VIEWER, "John", "Smith"), "jitsi")).when(webConferencing)
                                                                                               .getCall(CALL_ID);

    webConferencing.uploadFile(uploadInfo(), request);

    ArgumentCaptor<String> uploadId = ArgumentCaptor.forClass(String.class);
    InOrder order = inOrder(uploadService);
    order.verify(uploadService).addUploadLimit(uploadId.capture(), eq(expectedLimit));
    assertEquals(uploadId.getValue(), UUID.fromString(uploadId.getValue()).toString(), "the upload id is a random UUID");
    order.verify(uploadService).createUploadResource(uploadId.getValue(), request);
    order.verify(uploadService).removeUploadLimit(uploadId.getValue());
  }

  /**
   * Builds a service with its recording settings.
   *
   * @param uploadLimit the upload limit setting, or <code>null</code>
   * @param graceHours the grace setting, or <code>null</code>
   * @return the service
   */
  private WebConferencingService newService(String uploadLimit, String graceHours) {
    InitParams params = new InitParams();
    PropertiesParam jwt = new PropertiesParam();
    jwt.setName("jwt-configuration");
    jwt.setProperty("secret-key", "secret");
    params.addParameter(jwt);
    PropertiesParam recording = new PropertiesParam();
    recording.setName("recording-configuration");
    if (uploadLimit != null) {
      recording.setProperty("upload-limit-mb", uploadLimit);
    }
    if (graceHours != null) {
      recording.setProperty("occurrence-grace-hours", graceHours);
    }
    params.addParameter(recording);
    WebConferencingService webConferencing = new WebConferencingService(mock(OrganizationService.class),
                                                                        identityManager,
                                                                        listenerService,
                                                                        mock(SettingService.class),
                                                                        mock(CallDAO.class),
                                                                        mock(ParticipantDAO.class),
                                                                        mock(OriginDAO.class),
                                                                        mock(InviteDAO.class),
                                                                        uploadService,
                                                                        repositoryService,
                                                                        sessionProviders,
                                                                        nodeCreator,
                                                                        identityRegistry,
                                                                        mock(Authenticator.class),
                                                                        mock(ShareDocumentService.class),
                                                                        params,
                                                                        mock(LinkManager.class),
                                                                        mock(CodecInitializer.class));
    ReflectionTestUtils.setField(webConferencing, "recordingStorage", recordingStorage);
    ReflectionTestUtils.setField(webConferencing, "spaceService", spaceService);
    return webConferencing;
  }

  /**
   * Registers the viewer's security identity.
   *
   * @return the identity
   */
  private Identity viewer() {
    Identity viewer = new Identity(VIEWER);
    lenient().when(identityRegistry.getIdentity(VIEWER)).thenReturn(viewer);
    return viewer;
  }

  /**
   * The space owning the space-event call.
   *
   * @return the space
   */
  private Space space() {
    Space space = new Space();
    space.setGroupId(GROUP_ID);
    when(spaceService.getSpaceByGroupId(GROUP_ID)).thenReturn(space);
    return space;
  }

  /**
   * A space-event call of the sales space.
   *
   * @return the call
   */
  private CallInfo spaceEventCall() {
    WebConferencingService.SpaceEventInfo owner = mock(WebConferencingService.SpaceEventInfo.class);
    lenient().when(owner.getGroupId()).thenReturn(GROUP_ID);
    return new CallInfo(CALL_ID, "ACME demo", owner, "jitsi");
  }

  /**
   * A provider of a type, active or not, reading a given call id.
   *
   * @param type the type
   * @param active whether it is active
   * @param callId the call id it reads out of any link
   * @return the provider
   */
  private CallProvider provider(String type, boolean active, String callId) {
    CallProvider provider = mock(CallProvider.class);
    lenient().when(provider.getType()).thenReturn(type);
    lenient().when(provider.isActive()).thenReturn(active);
    lenient().when(provider.findCallId(anyString())).thenReturn(callId);
    doReturn(provider).when(service).getProvider(type);
    return provider;
  }

  /**
   * The upload info of a space-event recording.
   *
   * @return the upload info
   */
  private static UploadFileInfo uploadInfo() {
    return new UploadFileInfo(CALL_ID, "42", "space_event", VIEWER, List.of(VIEWER));
  }

}
