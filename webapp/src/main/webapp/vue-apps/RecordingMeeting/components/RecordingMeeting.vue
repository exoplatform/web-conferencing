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
  <!-- The « Meeting » line of a recording's details, contributed to the
       DocumentsInfoDetails / documents-info-details extension point: the agenda
       event the recording was made in, opening it. -->
  <documents-info-row
    v-if="meeting"
    :label="$t('webconferencing.recording.meeting')"
    class="webconferencingRecordingMeeting">
    <a
      :href="meeting.url"
      :title="$t('webconferencing.recording.meeting.open', {0: meeting.title})"
      class="text-truncate primary--text">
      {{ meeting.title }} · {{ dateLabel }}
    </a>
  </documents-info-row>
</template>
<script>
import {getRecordingEvent} from '../services.js';

const DATE_FORMAT = {
  year: 'numeric',
  month: 'short',
  day: 'numeric',
};

const DATE_TIME_FORMAT = Object.assign({
  hour: '2-digit',
  minute: '2-digit',
}, DATE_FORMAT);

export default {
  props: {
    file: {
      type: Object,
      default: null,
    },
    documentId: {
      type: String,
      default: null,
    },
    mimetype: {
      type: String,
      default: null,
    },
  },
  data: () => ({
    meeting: null,
    lastRead: 0,
    destroyed: false,
  }),
  computed: {
    /**
     * The event's start in the user's language: its day, and its time
     * unless it lasts all day.
     * @returns {string} the date, empty when unknown
     */
    dateLabel() {
      const start = this.meeting && this.meeting.start && new Date(this.meeting.start);
      if (!start || Number.isNaN(start.getTime())) {
        return '';
      }
      return this.$dateUtil.formatDateObjectToDisplay(start,
        this.meeting.allDay ? DATE_FORMAT : DATE_TIME_FORMAT,
        eXo.env.portal.language);
    },
  },
  watch: {
    documentId: {
      immediate: true,
      handler() {
        this.readMeeting();
      },
    },
  },
  beforeDestroy() {
    this.destroyed = true;
  },
  methods: {
    /**
     * Reads the event the file was recorded in. Only an audio or video file
     * is asked about. An answer that comes back for another file than the
     * one shown, or after a later read was sent, is dropped; a failed read
     * shows no line.
     * @returns {Promise} resolved once the event is read
     */
    readMeeting() {
      this.meeting = null;
      const documentId = this.documentId;
      const read = ++this.lastRead;
      if (!documentId || !this.isMedia()) {
        return Promise.resolve();
      }
      return getRecordingEvent(documentId)
        .catch(() => null)
        .then(meeting => {
          if (!this.destroyed && read === this.lastRead && documentId === this.documentId) {
            this.meeting = meeting && meeting.url && meeting.title ? meeting : null;
          }
        });
    },
    /**
     * Tells whether the file is audio or video, the only files a call
     * recording can be. An unknown type is asked about.
     * @returns {boolean} true when the file may be a recording
     */
    isMedia() {
      return !this.mimetype || this.mimetype.startsWith('video/') || this.mimetype.startsWith('audio/');
    },
  },
};
</script>
