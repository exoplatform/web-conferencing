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
      <div v-if="recordingStatus === 'ok'" class="text-truncate">
        <v-icon size="14" class="me-1">fa-solid fa-video</v-icon>
        {{ fileName }}
      </div>
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
  computed: {
    profileAvatarUrl() {
      return this.notification && this.notification.parameters && this.notification.parameters.AVATAR_URL;
    },
    fileUrl() {
      return this.notification && this.notification.parameters && this.notification.parameters.FILE_URL;
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