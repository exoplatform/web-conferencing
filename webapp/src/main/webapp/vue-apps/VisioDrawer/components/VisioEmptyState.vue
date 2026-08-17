<!--
Copyright (C) 2026 eXo Platform SAS.

This is free software; you can redistribute it and/or modify it
under the terms of the GNU Lesser General Public License as
published by the Free Software Foundation; either version 2.1 of
the License, or (at your option) any later version.

This software is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this software; if not, write to the Free
Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
02110-1301 USA, or see the FSF site: http://www.fsf.org.
-->
<template>
  <!-- "Nothing on" and "we could not find out" are different answers, and the
       widget this replaces gave the first one for both. -->
  <div class="d-flex flex-column align-center justify-center text-center py-8">
    <v-icon size="48" class="mb-3">{{ error && 'fa-triangle-exclamation' || 'fa-calendar-xmark' }}</v-icon>
    <div class="text-body font-weight-bold">
      {{ error && $t('visio.drawer.error.title') || $t('visio.drawer.empty.title') }}
    </div>
    <div class="text-caption text-sub-title mt-1">
      {{ error && $t('visio.drawer.error.subtitle') || $t('visio.drawer.empty.subtitle') }}
    </div>
    <v-btn
      v-if="error"
      small
      depressed
      color="primary"
      class="mt-4"
      @click="$emit('retry')">
      {{ $t('visio.drawer.error.retry') }}
    </v-btn>
  </div>
</template>

<script>
export default {
  props: {
    error: {
      type: Boolean,
      default: false,
    },
  },
};
</script>
