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
  <div class="mb-4">
    <div class="text-sub-title mb-2">{{ label }}</div>
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
