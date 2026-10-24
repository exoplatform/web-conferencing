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
          rel="noopener"
          @click="openRecording">
          <v-icon size="12" class="primary--text me-1">fas fa-play</v-icon>
          {{ label }}
        </a>
        <a
          :href="recording.previewUrl"
          :title="$t('visio.recordings.openInDocuments')"
          :aria-label="$t('visio.recordings.openInDocuments')"
          class="webconferencing-meeting-recording-documents flex-shrink-0 ms-1"
          target="_blank"
          rel="noopener">
          <v-icon size="12" class="icon-default-color">fas fa-external-link-alt</v-icon>
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
        <span
          v-if="chip && (chip.kind !== 'ready' || !excerpt)"
          :class="chipColor"
          :title="chipReason"
          class="webconferencing-meeting-transcript-state text-caption me-2">
          {{ chipLabel }}
        </span>
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
          rel="noopener"
          @click.prevent="openTranscript">
          {{ $t('visio.recordings.transcript.readAll') }}
        </a>
      </template>
    </div>
  </div>
</template>

<script>
import {durationParts, formatSize, getOccurrenceBounds, recordingDuration, recordingStartTime, transcriptChip, transcriptText} from '../js/MeetingRecordingsModel.js';
import {getTranscriptionText, probeRecordingDuration} from '../services.js';

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
    several: {
      type: Boolean,
      default: false,
    },
  },
  data: () => ({
    excerpt: null,
    probedDuration: null,
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
     * How long the recording lasts: stored by the server, else measured by
     * its transcription, else read from the media.
     *
     * @returns {Number} the duration in seconds, or null while unknown
     */
    duration() {
      return recordingDuration(this.recording, this.transcript) || this.probedDuration;
    },
    /**
     * The recording's length as the row writes it: « 17 s », « 58 min » or
     * « 1 h 05 ».
     *
     * @returns {String} the translated length, or null while unknown
     */
    length() {
      const parts = durationParts(this.duration);
      switch (parts && parts.unit) {
      case 'seconds':
        return this.$t('visio.recordings.duration.seconds', {0: parts.value});
      case 'minutes':
        return this.$t('visio.recordings.duration.minutes', {0: parts.value});
      case 'hours':
        return this.$t('visio.recordings.duration.hours', {0: parts.value, 1: parts.minutes});
      default:
        return null;
      }
    },
    /**
     * When the recording started, as the row writes it: its time, with its
     * day when that is not the occurrence's.
     *
     * @returns {String} the formatted start, or null when unknown
     */
    start() {
      if (!this.startTime) {
        return null;
      }
      const start = new Date(this.startTime);
      const bounds = getOccurrenceBounds(this.event);
      const sameDay = bounds && bounds.start.toDateString() === start.toDateString();
      const options = sameDay
        ? {hour: '2-digit', minute: '2-digit'}
        : {month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'};
      const language = eXo && eXo.env && eXo.env.portal && eXo.env.portal.language || navigator.language;
      return new Intl.DateTimeFormat(language, options).format(start);
    },
    /**
     * The row's label. The meeting is the drawer's own subject, so its date
     * says nothing: a recording is named by its length, and by when it
     * started too when the block holds several. Until the length is known, by
     * when it started.
     *
     * @returns {String} the translated label
     */
    label() {
      if (this.length && this.several && this.start) {
        return this.$t('visio.recordings.recordingStarted', {0: this.length, 1: this.start});
      }
      if (this.length || this.start) {
        return this.$t('visio.recordings.recording', {0: this.length || this.start});
      }
      return this.recording && this.recording.title;
    },
    /**
     * What identifies the media to probe for its length: a video or audio
     * recording whose length neither the server nor its transcription gives.
     *
     * @returns {String} the document id, or null when there is nothing to probe
     */
    probeKey() {
      const mimeType = this.recording && this.recording.mimeType || '';
      const playable = !mimeType || mimeType.indexOf('video/') === 0 || mimeType.indexOf('audio/') === 0;
      const stored = this.recording && this.recording.durationSeconds;
      return playable && !(typeof stored === 'number' && stored > 0) && this.recording && this.recording.documentId || null;
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
     * The transcript state the recording's transcription calls for.
     *
     * @returns {Object} {kind, progress, error}, or null for no chip
     */
    chip() {
      return transcriptChip(this.transcript);
    },
    /**
     * The text of the transcript's state.
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
      case 'ready':
        return this.$t('visio.recordings.transcript.ready');
      default:
        return '';
      }
    },
    /**
     * The colour of the transcript's state: red when it failed, the
     * secondary text colour otherwise.
     *
     * @returns {String} a text colour class
     */
    chipColor() {
      return this.chip && this.chip.kind === 'failed' ? 'error--text' : 'text-sub-title';
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
    probeKey: {
      immediate: true,
      handler(documentId) {
        this.probedDuration = null;
        if (documentId) {
          this.probeDuration(documentId);
        }
      },
    },
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
     * Opens a recording clicked by its name in the transcript drawer, which
     * plays it beside its transcript. A click with a modifier key or another
     * button keeps the link's own behaviour, a new tab on the Documents
     * preview.
     *
     * @param {MouseEvent} event - the click
     * @returns {void}
     */
    openRecording(event) {
      if (event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) {
        return;
      }
      event.preventDefault();
      this.openTranscript();
    },
    /**
     * Opens the recording's transcript drawer. A cancelable
     * documents-open-transcript event carries the document id; the documents
     * drawer cancels it when it opens. Where nothing cancels it (no drawer on
     * the page, or an edition without one), the Documents preview opens.
     *
     * @returns {void}
     */
    openTranscript() {
      const event = new CustomEvent('documents-open-transcript', {
        detail: {documentId: this.recording.documentId},
        cancelable: true,
      });
      if (document.dispatchEvent(event)) {
        window.open(this.recording.previewUrl, '_blank', 'noopener');
      }
    },
    /**
     * Reads the recording's length from the media. An answer that arrives
     * once the row shows another recording, or once the server or the
     * transcription gave the length, is dropped.
     *
     * @param {String} documentId - the recording's document id
     * @returns {Promise} resolved once the probe is over
     */
    probeDuration(documentId) {
      return probeRecordingDuration(documentId)
        .then(duration => {
          if (this.probeKey === documentId) {
            this.probedDuration = duration;
          }
        });
    },
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
