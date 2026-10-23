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
  <div class="webconferencing-meeting-recording d-flex flex-column mt-2">
    <a
      :href="recording.previewUrl"
      :title="recording.title"
      class="text-truncate primary--text"
      target="_blank"
      rel="noopener">
      {{ recording.title }}
    </a>
    <div class="d-flex align-center flex-wrap">
      <date-format
        v-if="recordedTime"
        :value="recordedTime"
        :format="dateTimeFormat"
        class="text-caption text-sub-title me-2" />
      <v-chip
        v-if="chip"
        :color="chipColor"
        :title="chipReason"
        class="webconferencing-meeting-transcript-chip me-2"
        x-small
        outlined>
        {{ chipLabel }}
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
  </div>
</template>

<script>
import {transcriptChip} from '../js/MeetingRecordingsModel.js';

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
  },
  data: () => ({
    dateTimeFormat: {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    },
  }),
  computed: {
    /**
     * When the recording was saved, in milliseconds: the REST gives an ISO
     * instant, the date-format component takes a number.
     *
     * @returns {Number} the time, or 0 when unknown
     */
    recordedTime() {
      const time = this.recording && this.recording.recordedDate && new Date(this.recording.recordedDate).getTime();
      return time && !Number.isNaN(time) ? time : 0;
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
      case 'ready':
        return this.$t('visio.recordings.transcript.ready');
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
      switch (this.chip && this.chip.kind) {
      case 'ready':
        return 'success';
      case 'failed':
        return 'error';
      default:
        return 'primary';
      }
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
  },
};
</script>
