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
package org.exoplatform.webconferencing.mcp;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

import org.exoplatform.documents.service.DocumentFileService;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.webconferencing.mcp.model.RecordingChapterModel;
import org.exoplatform.webconferencing.mcp.model.RecordingOutlineModel;

/**
 * The outline of a recording's transcript: its chapters and its speakers'
 * names, read with the listing user's rights, and never an error.
 */
@ExtendWith(MockitoExtension.class)
class RecordingOutlineReaderTest {

  private static final String    DOC      = "doc1";

  private static final long      USER     = 7L;

  private static final String    SEGMENTS = """
      [{"start":0.0,"end":4.2,"text":"Hello","speaker":"2"},
       {"start":4.2,"end":9.0,"text":"Hi","speaker":"1"},
       {"start":9.0,"end":12.0,"text":"Welcome","speaker":"3"},
       {"start":12.0,"end":15.0,"text":"Thanks","speaker":"2"},
       {"start":15.0,"end":18.0,"text":"Hum"},
       {"start":18.0,"end":20.0,"text":"Yes","speaker":"4"}]
      """;

  @Mock
  private DocumentFileService    documentFileService;

  @Mock
  private IdentityManager        identityManager;

  private RecordingOutlineReader reader;

  /**
   * The reader over the mocked Documents and identities.
   */
  @BeforeEach
  void setUp() {
    reader = new RecordingOutlineReader(documentFileService, identityManager);
  }

  /**
   * The chapters come in their order with the moment they start, the ones
   * without a title or a valid start left out.
   *
   * @throws Exception never
   */
  @Test
  void theChaptersAreReadWithTheirMoment() throws Exception {
    when(documentFileService.getTranscriptionChapters(DOC, USER)).thenReturn("""
        [{"start":0,"title":"Welcome","summary":"s"},
         {"start":1380.6,"title":" Pricing "},
         {"start":3723,"title":"Wrap-up"},
         {"start":-1,"title":"Negative"},
         {"start":"10","title":"Not a number"},
         {"title":"No start"},
         {"start":50,"title":"  "}]
        """);

    RecordingOutlineModel outline = reader.readOutline(DOC, USER);

    assertEquals(List.of(new RecordingChapterModel(0, "00:00", "Welcome"),
                         new RecordingChapterModel(1380, "23:00", "Pricing"),
                         new RecordingChapterModel(3723, "1:02:03", "Wrap-up")),
                 outline.chapters());
  }

  /**
   * The speakers come in the order they first speak, each once: a named user
   * by full name, a named guest by its name, a user who is gone, a speaker
   * stored with a value that is no identity id and an unnamed speaker by
   * number, the others being named all the same.
   *
   * @throws Exception never
   */
  @Test
  void theSpeakersAreNamedInTheOrderTheyFirstSpeak() throws Exception {
    when(documentFileService.getTranscriptionSegments(DOC, USER)).thenReturn(SEGMENTS);
    when(documentFileService.getTranscriptionSpeakers(DOC,
                                                      USER)).thenReturn(Map.of("1", "11", "2", "name:Alice", "3", "not-an-id", "4", "12"));
    Identity john = new Identity("11");
    Profile profile = new Profile(john);
    profile.setProperty(Profile.FULL_NAME, "John Smith");
    john.setProfile(profile);
    when(identityManager.getIdentity(11L)).thenReturn(john);
    Identity gone = mock(Identity.class);
    when(gone.isDeleted()).thenReturn(true);
    when(identityManager.getIdentity(12L)).thenReturn(gone);

    assertEquals(List.of("Alice", "John Smith", "Speaker 3", "Speaker 4"), reader.readOutline(DOC, USER).speakers());
  }

  /**
   * One person named for two speakers is listed once.
   *
   * @throws Exception never
   */
  @Test
  void aPersonNamedTwiceIsListedOnce() throws Exception {
    when(documentFileService.getTranscriptionSegments(DOC, USER)).thenReturn(SEGMENTS);
    when(documentFileService.getTranscriptionSpeakers(DOC, USER)).thenReturn(Map.of("1", "name:Alice", "2", "name:Alice"));

    assertEquals(List.of("Alice", "Speaker 3", "Speaker 4"), reader.readOutline(DOC, USER).speakers());
  }

  /**
   * A transcript whose segments name no speaker lists none, and the speakers'
   * names are then not even read.
   *
   * @throws Exception never
   */
  @Test
  void aTranscriptWithoutSpeakersListsNone() throws Exception {
    when(documentFileService.getTranscriptionSegments(DOC, USER)).thenReturn("[{\"start\":0,\"end\":2,\"text\":\"Hi\"}]");

    assertSame(RecordingOutlineModel.EMPTY, reader.readOutline(DOC, USER));
    verify(documentFileService, never()).getTranscriptionSpeakers(anyString(), anyLong());
  }

  /**
   * The chapters failing to read never take the speakers away, nor the other
   * way round, and both failing gives an empty outline, never an error.
   *
   * @throws Exception never
   */
  @Test
  void aFailureOnOnePartKeepsTheOther() throws Exception {
    when(documentFileService.getTranscriptionChapters(DOC, USER)).thenThrow(new IllegalAccessException("denied"));
    when(documentFileService.getTranscriptionSegments(DOC, USER)).thenReturn(SEGMENTS);
    when(documentFileService.getTranscriptionSpeakers(DOC, USER)).thenReturn(Map.of());
    assertEquals(List.of("Speaker 2", "Speaker 1", "Speaker 3", "Speaker 4"), reader.readOutline(DOC, USER).speakers());

    when(documentFileService.getTranscriptionChapters("doc2", USER)).thenReturn("[{\"start\":0,\"title\":\"Welcome\"}]");
    when(documentFileService.getTranscriptionSegments("doc2", USER)).thenReturn("not json");
    RecordingOutlineModel chaptersOnly = reader.readOutline("doc2", USER);
    assertEquals(1, chaptersOnly.chapters().size());
    assertTrue(chaptersOnly.speakers().isEmpty());

    when(documentFileService.getTranscriptionChapters("doc3", USER)).thenReturn("{");
    when(documentFileService.getTranscriptionSegments("doc3", USER)).thenThrow(new UnsupportedOperationException());
    assertSame(RecordingOutlineModel.EMPTY, reader.readOutline("doc3", USER));
  }

  /**
   * The identity id the reads take is the listing user's.
   */
  @Test
  void theIdentityIsTheListingUsers() {
    when(identityManager.getOrCreateUserIdentity("john")).thenReturn(new Identity("42"));
    assertEquals(42L, reader.getIdentityId("john"));
    assertEquals(0L, reader.getIdentityId("nobody"));
  }

  /**
   * The reader is a bean only when the Documents add-on is on the classpath:
   * the condition names its service by name, so that the class is never loaded
   * without it. Removing the annotation would fail the add-on's context on a
   * server without Documents.
   */
  @Test
  void theReaderExistsOnlyWithDocuments() {
    ConditionalOnClass condition = RecordingOutlineReader.class.getAnnotation(ConditionalOnClass.class);
    assertArrayEquals(new String[] { DocumentFileService.class.getName() }, condition.name());
    assertEquals(0, condition.value().length);
  }

}
