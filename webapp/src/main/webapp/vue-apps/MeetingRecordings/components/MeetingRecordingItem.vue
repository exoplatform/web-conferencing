<!--
Copyright (C) 2026 eXo Platform SAS.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
-->
<template>
  <div class="webconferencing-meeting-recording d-flex mt-2">
    <div class="d-flex flex-column flex-grow-1 flex-shrink-1 overflow-hidden">
      <div class="d-flex align-center">
        <a
          :href="recording.previewUrl"
          :title="recording.title"
          class="text-truncate primary--text"
          target="_blank"
          rel="noopener">
          <v-icon size="12" class="primary--text me-1">fas fa-play</v-icon>
          {{ label }}
        </a>
        <!--
          The extension point for actions on this recording (the enterprise
          concierge), right after its name: app MeetingRecordings, type
          recording-actions, the contributors receive the props event and
          recordings, this one only.
        -->
        <extension-registry-components
          :params="actionParams"
          name="MeetingRecordings"
          type="recording-actions"
          parent-element="div"
          element="div"
          class="webconferencing-recording-actions flex-shrink-0 ms-1" />
      </div>
      <div class="d-flex align-center flex-wrap">
        <span
          v-if="size"
          class="text-caption text-sub-title me-2">
          {{ size }}
        </span>
        <v-chip
          v-if="chip && chip.kind !== 'ready'"
          :color="chipColor"
          :title="chipReason"
          class="webconferencing-meeting-transcript-chip me-2"
          x-small
          outlined>
          {{ chipLabel }}
        </v-chip>
        <v-chip
          v-else-if="chip && !excerpt"
          color="success"
          class="webconferencing-meeting-transcript-chip me-2"
          x-small
          outlined>
          {{ $t('visio.recordings.transcript.ready') }}
        </v-chip>
        <a
          v-if="chip && chip.kind === 'failed'"
          class="webconferencing-meeting-transcript-retry text-caption primary--text"
          @click="$emit('retry', recording.documentId)">
          {{ $t('visio.recordings.transcript.retry') }}
        </a>
      </div>
      <span
        v-if="chip && chip.kind === 'failed' && chipReason"
        class="text-caption text-sub-title">
        {{ chipReason }}
      </span>
      <template v-if="excerpt">
        <span
          class="webconferencing-meeting-transcript-excerpt text-caption text-sub-title mt-1"
          style="display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden;">
          {{ excerpt }}
        </span>
        <a
          :href="recording.previewUrl"
          class="text-caption primary--text"
          target="_blank"
          rel="noopener">
          {{ $t('visio.recordings.transcript.readAll') }}
        </a>
      </template>
    </div>
  </div>
</template>

<script>
import {formatSize, getOccurrenceBounds, recordingStartTime, transcriptChip, transcriptText} from '../js/MeetingRecordingsModel.js';
import {getTranscriptionText} from '../services.js';

export default {
  props: {
    recording: {
      type: Object,
      default: null,
    },
    transcript: {
      type: Object,
      default: null,
    },
    event: {
      type: Object,
      default: null,
    },
  },
  data: () => ({
    excerpt: null,
  }),
  computed: {
    /**
     * When the recording started, in milliseconds.
     *
     * @returns {Number} the time, or 0 when unknown
     */
    startTime() {
      return recordingStartTime(this.recording);
    },
    /**
     * The row's label: the meeting is the drawer's own subject, so a
     * recording is named by when it started, with its day when that is not
     * the occurrence's.
     *
     * @returns {String} the translated label
     */
    label() {
      if (!this.startTime) {
        return this.recording && this.recording.title;
      }
      const start = new Date(this.startTime);
      const bounds = getOccurrenceBounds(this.event);
      const sameDay = bounds && bounds.start.toDateString() === start.toDateString();
      const options = sameDay
        ? {hour: '2-digit', minute: '2-digit'}
        : {month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'};
      const language = eXo && eXo.env && eXo.env.portal && eXo.env.portal.language || navigator.language;
      return this.$t('visio.recordings.recording', {0: new Intl.DateTimeFormat(language, options).format(start)});
    },
    /**
     * The recording's size, as a file list shows it.
     *
     * @returns {String} the size, or null when unknown
     */
    size() {
      return formatSize(this.recording && this.recording.sizeBytes);
    },
    /**
     * The params handed to the actions contributed on this recording.
     *
     * @returns {Object} the event and this recording, as a list
     */
    actionParams() {
      return {
        event: this.event,
        recordings: [this.recording],
      };
    },
    /**
     * The transcript chip the recording's transcription state calls for.
     *
     * @returns {Object} {kind, progress, error}, or null for no chip
     */
    chip() {
      return transcriptChip(this.transcript);
    },
    /**
     * The chip's text.
     *
     * @returns {String} the translated label
     */
    chipLabel() {
      switch (this.chip && this.chip.kind) {
      case 'queued':
        return this.$t('visio.recordings.transcript.queued');
      case 'processing':
        return this.chip.progress
          ? this.$t('visio.recordings.transcript.processingProgress', {0: this.chip.progress})
          : this.$t('visio.recordings.transcript.processing');
      case 'failed':
        return this.$t('visio.recordings.transcript.failed');
      default:
        return '';
      }
    },
    /**
     * The chip's colour.
     *
     * @returns {String} a Vuetify colour name
     */
    chipColor() {
      return this.chip && this.chip.kind === 'failed' ? 'error' : 'primary';
    },
    /**
     * The human-readable reason of a failed transcription: the documents
     * REST gives a message code, each known code has its sentence.
     *
     * @returns {String} the translated reason, or null
     */
    chipReason() {
      const error = this.chip && this.chip.kind === 'failed' && this.chip.error;
      if (!error) {
        return null;
      }
      const key = `visio.recordings.transcript.error.${error}`;
      return this.$te(key) ? this.$t(key) : this.$t('visio.recordings.transcript.error.unknown');
    },
    /**
     * What identifies the excerpt to read: a recording whose transcript is
     * ready.
     *
     * @returns {String} the document id, or null when there is no excerpt to read
     */
    excerptKey() {
      return this.chip && this.chip.kind === 'ready' && this.recording && this.recording.documentId || null;
    },
  },
  watch: {
    excerptKey: {
      immediate: true,
      handler(documentId) {
        this.excerpt = null;
        if (documentId) {
          this.loadExcerpt(documentId);
        }
      },
    },
  },
  methods: {
    /**
     * Reads the start of a ready transcript. An answer that arrives once the
     * row shows another recording is dropped.
     *
     * @param {String} documentId - the recording's document id
     * @returns {Promise} resolved once the excerpt is known
     */
    loadExcerpt(documentId) {
      return getTranscriptionText(documentId)
        .then(body => {
          if (this.excerptKey === documentId) {
            this.excerpt = transcriptText(body);
          }
        });
    },
  },
};
</script>
