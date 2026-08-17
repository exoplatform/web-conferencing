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
  <div class="mb-4">
    <div class="text-sub-title text-uppercase mb-2">{{ label }}</div>
    <visio-card
      v-for="entry in entries"
      :key="entry.key"
      :entry="entry"
      :now="now"
      :countdown="entry.key === countdownKey" />
  </div>
</template>

<script>
export default {
  props: {
    label: {
      type: String,
      default: '',
    },
    entries: {
      type: Array,
      default: () => [],
    },
    now: {
      type: Date,
      default: null,
    },
    countdownKey: {
      type: String,
      default: '',
    },
  },
};
</script>
