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
  <!-- The Meeting block of a past event, contributed to the agenda drawer's
       AgendaEvent / agenda-event-body extension point. -->
  <div
    v-if="visible"
    class="webconferencing-meeting-recordings d-flex flex-grow-0 flex-shrink-1 pb-5">
    <v-icon size="20" class="icon-default-color pe-4 align-self-start mt-1">fas fa-film</v-icon>
    <div class="d-flex flex-column flex-grow-1 flex-shrink-1 overflow-hidden">
      <span class="text-body font-weight-bold">{{ $t('visio.recordings.title') }}</span>
      <v-progress-linear
        v-if="loading && !state"
        indeterminate
        height="2"
        class="mt-1" />
      <template v-else-if="state === STATE_READY">
        <meeting-recording-item
          v-for="recording in recordings"
          :key="recording.documentId"
          :recording="recording"
          :transcript="transcripts[recording.documentId]"
          :event="event"
          :transcript-read="transcriptsRead || !!transcripts[recording.documentId]"
          @retry="retryTranscript" />
      </template>
      <div
        v-else-if="message"
        class="d-flex align-center mt-1">
        <span class="webconferencing-meeting-recordings-message text-subtitle">
          {{ message }}
        </span>
        <v-btn
          v-if="refreshVisible"
          :title="$t('visio.recordings.refresh')"
          :loading="loading"
          class="webconferencing-meeting-recordings-refresh ms-1"
          icon
          x-small
          @click="load">
          <v-icon size="12">fas fa-redo</v-icon>
        </v-btn>
      </div>
    </div>
  </div>
</template>

<script>
import {
  STATE_READY,
  STATE_NOT_RECORDED,
  STATE_PROCESSING,
  STATE_OTHER_PROVIDER,
  STATE_NOT_MEMBER,
  STATE_FORBIDDEN,
  STATE_ERROR,
  getConferenceUrl,
  getRecordingsWindow,
  getSpaceName,
  stateFromResponse,
} from '../js/MeetingRecordingsModel.js';
import {getRecordingsByConference, getTranscriptionState, retryTranscription} from '../services.js';

export default {
  props: {
    event: {
      type: Object,
      default: null,
    },
    isPast: {
      type: Boolean,
      default: false,
    },
  },
  data: () => ({
    STATE_READY,
    state: null,
    recordings: [],
    transcripts: {},
    transcriptsRead: false,
    loading: false,
    requestId: 0,
  }),
  computed: {
    /**
     * The event's conference URL.
     *
     * @returns {String} the URL, or null without a conference
     */
    conferenceUrl() {
      return getConferenceUrl(this.event);
    },
    /**
     * Whether the block shows at all: a past occurrence with a conference.
     * Before the end of the meeting nothing is shown, and nothing is asked.
     *
     * @returns {Boolean} true when the block is shown
     */
    visible() {
      return !!(this.isPast && this.conferenceUrl);
    },
    /**
     * The time window the recordings are asked for.
     *
     * @returns {Object} {from, to}, or an empty object for every recording
     */
    recordingsWindow() {
      return getRecordingsWindow(this.event);
    },
    /**
     * What identifies one lookup: when it changes (another event or
     * occurrence opened) the recordings are read again. The block appears on
     * a drawer opened after the meeting's end: isPast does not follow the
     * clock while the drawer stays open.
     *
     * @returns {String} the lookup key, or null when nothing is to be read
     */
    lookupKey() {
      return this.visible
        && JSON.stringify([this.conferenceUrl, this.recordingsWindow.from || '', this.recordingsWindow.to || ''])
        || null;
    },
    /**
     * The sentence shown in place of the recordings.
     *
     * @returns {String} the translated message, or null
     */
    message() {
      switch (this.state) {
      case STATE_NOT_RECORDED:
        return this.$t('visio.recordings.notRecorded');
      case STATE_PROCESSING:
        return this.$t('visio.recordings.processing');
      case STATE_OTHER_PROVIDER:
        return this.$t('visio.recordings.otherProvider');
      case STATE_NOT_MEMBER: {
        const spaceName = getSpaceName(this.event);
        return spaceName
          ? this.$t('visio.recordings.notSpaceMember', {0: spaceName})
          : this.$t('visio.recordings.notSpaceMemberUnnamed');
      }
      case STATE_FORBIDDEN:
        return this.$t('visio.recordings.forbidden');
      case STATE_ERROR:
        return this.$t('visio.recordings.error');
      default:
        return null;
      }
    },
    /**
     * Whether the refresh is offered: only when reading again can change the
     * answer, a recording still being processed or a failed read. Nothing
     * polls, so it is the user's way to look again.
     *
     * @returns {Boolean} true when the refresh is shown
     */
    refreshVisible() {
      return this.state === STATE_PROCESSING || this.state === STATE_ERROR;
    },
  },
  watch: {
    lookupKey: {
      immediate: true,
      handler(key, previousKey) {
        if (key !== previousKey) {
          this.reset();
          // Whatever is still on its way belongs to the previous lookup
          this.requestId++;
          if (key) {
            this.load();
          }
        }
      },
    },
  },
  methods: {
    /**
     * Forgets what the previous lookup showed.
     *
     * @returns {void}
     */
    reset() {
      this.state = null;
      this.recordings = [];
      this.transcripts = {};
      this.transcriptsRead = false;
    },
    /**
     * Reads the recordings of the meeting, then their transcript states. Only
     * the answer to the latest request is shown: one to an earlier request,
     * for another lookup or before a refresh, is dropped.
     *
     * @returns {Promise} resolved once the recordings are shown
     */
    load() {
      if (!this.lookupKey) {
        return Promise.resolve();
      }
      const requestId = ++this.requestId;
      this.loading = true;
      return getRecordingsByConference(this.conferenceUrl, this.recordingsWindow)
        .then(({status, body}) => {
          if (requestId !== this.requestId) {
            return;
          }
          this.state = stateFromResponse(status, body, this.event, Date.now());
          this.recordings = this.state === STATE_READY ? body : [];
          this.transcripts = {};
          this.transcriptsRead = false;
          this.loadTranscripts(requestId)
            .finally(() => {
              if (requestId === this.requestId) {
                this.transcriptsRead = true;
              }
            });
        })
        .catch(() => {
          if (requestId === this.requestId) {
            this.state = STATE_ERROR;
            this.recordings = [];
          }
        })
        .finally(() => {
          if (requestId === this.requestId) {
            this.loading = false;
          }
        });
    },
    /**
     * Reads the transcript state of each recording. The first one is read
     * alone: where no transcriber is installed it says so, and the others are
     * not asked.
     *
     * @param {Number} requestId - the request the recordings were read by
     * @returns {Promise} resolved once every state is read
     */
    loadTranscripts(requestId) {
      const [first, ...others] = this.recordings;
      if (!first) {
        return Promise.resolve();
      }
      return this.loadTranscript(first.documentId, requestId)
        .then(state => state && state.available === true
          && Promise.all(others.map(recording => this.loadTranscript(recording.documentId, requestId))));
    },
    /**
     * Reads the transcript state of one recording into the block.
     *
     * @param {String} documentId - the recording's document id
     * @param {Number} requestId - the request the recording was read by
     * @returns {Promise<Object>} the state, or null
     */
    loadTranscript(documentId, requestId) {
      return getTranscriptionState(documentId)
        .then(state => {
          if (requestId === this.requestId && state) {
            this.$set(this.transcripts, documentId, state);
          }
          return state;
        });
    },
    /**
     * Asks for a failed or stalled transcription to run again, then shows its new state.
     *
     * @param {String} documentId - the recording's document id
     * @returns {Promise} resolved once the new state is shown
     */
    retryTranscript(documentId) {
      const requestId = this.requestId;
      return retryTranscription(documentId)
        .then(() => this.loadTranscript(documentId, requestId))
        .catch(() => this.$root.$emit('alert-message', this.$t('visio.recordings.transcript.retryFailed'), 'error'));
    },
  },
};
</script>
