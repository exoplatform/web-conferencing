<!--
 * Copyright (C) 2023 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
-->
<template>
  <user-notification-template
    :notification="notification"
    :avatar-url="profileAvatarUrl"
    :message="message"
    :url="fileUrl">
    <template #actions>
      <div v-if="callTitle" class="text-truncate">
        <v-icon size="14" class="me-1">fa-solid fa-video</v-icon>
        {{ callTitle }}
      </div>
      <div v-else-if="recordingStatus === 'ok'" class="text-truncate">
        <v-icon size="14" class="me-1">fa-solid fa-video</v-icon>
        {{ fileName }}
      </div>
      <date-format
        v-if="callStartDate"
        :value="callStartDate"
        :format="dateTimeFormat"
        class="text-truncate text-caption" />
    </template>
  </user-notification-template>
</template>

<script>
export default {
  props: {
    notification: {
      type: Object,
      default: null,
    },
  },
  data: () => ({
    dateTimeFormat: {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    },
  }),
  computed: {
    callTitle() {
      return this.notification && this.notification.parameters && this.notification.parameters.CALL_TITLE;
    },
    callStartDate() {
      const startDate = Number(this.notification && this.notification.parameters && this.notification.parameters.CALL_START_DATE);
      return Number.isFinite(startDate) && startDate > 0 ? startDate : null;
    },
    profileAvatarUrl() {
      return this.notification && this.notification.parameters && this.notification.parameters.AVATAR;
    },
    fileUrl() {
      return this.notification && this.notification.parameters && this.notification.parameters.RECORDED_FILE_URL;
    },
    fileName() {
      return this.notification && this.notification.parameters && this.notification.parameters.FILE_NAME;
    },
    recordingStatus() {
      return this.notification && this.notification.parameters && this.notification.parameters.RECORDING_STATUS;
    },
    message() {
      return this.recordingStatus === 'ok' ? this.$t('Notification.webconferencing.callrecording.success') : this.$t('Notification.webconferencing.callrecording.failed');
    },
  }
};
</script>