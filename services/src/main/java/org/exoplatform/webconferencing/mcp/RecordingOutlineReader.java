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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import org.exoplatform.documents.model.TranscriptSpeakers;
import org.exoplatform.documents.service.DocumentFileService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.webconferencing.mcp.model.RecordingChapterModel;
import org.exoplatform.webconferencing.mcp.model.RecordingOutlineModel;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads what a recording's transcript holds before anyone reads it: its
 * chapters and the names of its speakers, as Documents stores them, with the
 * rights of the user listing the recordings.
 * <p>
 * Present only when the Documents add-on is: without it, the recordings are
 * listed without an outline.
 */
@Component
@Profile("mcp-server")
@ConditionalOnClass(name = "org.exoplatform.documents.service.DocumentFileService")
public class RecordingOutlineReader {

  private static final Log          LOG    = ExoLogger.getLogger(RecordingOutlineReader.class);

  private static final ObjectMapper MAPPER = JsonMapper.builder().build();

  private final DocumentFileService documentFileService;

  private final IdentityManager     identityManager;

  /**
   * Builds the reader.
   *
   * @param documentFileService the Documents service storing the transcripts
   * @param identityManager the service resolving the users who speak
   */
  @Autowired
  public RecordingOutlineReader(DocumentFileService documentFileService, IdentityManager identityManager) {
    this.documentFileService = documentFileService;
    this.identityManager = identityManager;
  }

  /**
   * The identity id of a user, the one the Documents reads take.
   *
   * @param username the user's name
   * @return the identity id, or 0 when the user has no identity
   */
  public long getIdentityId(String username) {
    Identity identity = identityManager.getOrCreateUserIdentity(username);
    return identity == null ? 0 : Long.parseLong(identity.getId());
  }

  /**
   * The outline of a recording's transcript: its chapters and its speakers'
   * names. Each is read on its own, so a failure on one never takes the other
   * away, and a failure on both gives an empty outline, never an error: the
   * recording is listed all the same.
   *
   * @param documentId the id of the recording's document
   * @param identityId the identity id of the user listing the recordings
   * @return the outline, {@link RecordingOutlineModel#EMPTY} when there is none
   */
  public RecordingOutlineModel readOutline(String documentId, long identityId) {
    List<RecordingChapterModel> chapters = readChapters(documentId, identityId);
    List<String> speakers = readSpeakers(documentId, identityId);
    return chapters.isEmpty() && speakers.isEmpty() ? RecordingOutlineModel.EMPTY
                                                    : new RecordingOutlineModel(chapters, speakers);
  }

  /**
   * Reads the chapters of a recording's transcript, skipping any without a
   * title or a valid start.
   *
   * @param documentId the id of the recording's document
   * @param identityId the identity id of the user
   * @return the chapters, empty when there are none or they cannot be read
   */
  private List<RecordingChapterModel> readChapters(String documentId, long identityId) {
    try {
      String chapters = documentFileService.getTranscriptionChapters(documentId, identityId);
      if (StringUtils.isBlank(chapters)) {
        return List.of();
      }
      List<RecordingChapterModel> result = new ArrayList<>();
      for (JsonNode chapter : MAPPER.readTree(chapters)) {
        String title = chapter.hasNonNull("title") ? StringUtils.trimToNull(chapter.get("title").asString()) : null;
        double start = chapter.hasNonNull("start") && chapter.get("start").isNumber() ? chapter.get("start").asDouble() : -1;
        if (title != null && Double.isFinite(start) && start >= 0) {
          result.add(new RecordingChapterModel((long) start, TranscriptSpeakers.formatMoment(start), title));
        }
      }
      return result;
    } catch (Exception e) { // NOSONAR the recording is listed without its chapters, whatever the failure
      LOG.debug("The chapters of recording {} cannot be read, it is listed without them", documentId, e);
      return List.of();
    }
  }

  /**
   * Reads the names of the speakers of a recording's transcript, in the order
   * they first speak: a named speaker by the person's full name or the guest's
   * name, any other by its number. A person named for two speakers is listed
   * once.
   *
   * @param documentId the id of the recording's document
   * @param identityId the identity id of the user
   * @return the names, empty when the transcript does not tell its speakers
   *         apart or they cannot be read
   */
  private List<String> readSpeakers(String documentId, long identityId) {
    try {
      String segments = documentFileService.getTranscriptionSegments(documentId, identityId);
      if (StringUtils.isBlank(segments)) {
        return List.of();
      }
      Set<String> speakerNumbers = new LinkedHashSet<>();
      for (JsonNode segment : MAPPER.readTree(segments)) {
        JsonNode speaker = segment.get("speaker");
        if (speaker != null && speaker.isString() && StringUtils.isNotBlank(speaker.asString())) {
          speakerNumbers.add(speaker.asString());
        }
      }
      if (speakerNumbers.isEmpty()) {
        return List.of();
      }
      Map<String, String> namedSpeakers = documentFileService.getTranscriptionSpeakers(documentId, identityId);
      Set<String> names = new LinkedHashSet<>();
      for (String speaker : speakerNumbers) {
        names.add(TranscriptSpeakers.speakerName(speaker, namedSpeakers, this::getFullName));
      }
      return List.copyOf(names);
    } catch (Exception e) { // NOSONAR the recording is listed without its speakers, whatever the failure
      LOG.debug("The speakers of recording {} cannot be read, it is listed without them", documentId, e);
      return List.of();
    }
  }

  /**
   * The full name of an identity.
   *
   * @param identityId the identity id
   * @return the full name, or null when the identity is gone or the id is not
   *         one
   */
  private String getFullName(String identityId) {
    long id = NumberUtils.toLong(identityId, 0);
    if (id <= 0) {
      return null;
    }
    Identity identity = identityManager.getIdentity(id);
    if (identity == null || identity.isDeleted() || identity.getProfile() == null) {
      return null;
    }
    return StringUtils.trimToNull(identity.getProfile().getFullName());
  }

}
