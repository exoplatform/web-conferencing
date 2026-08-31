/*
 * Copyright (C) 2026 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <gnu.org/licenses>.
 */
import VisioDrawerApp from './components/VisioDrawerApp.vue';
import VisioDrawer from './components/VisioDrawer.vue';
import VisioSection from './components/VisioSection.vue';
import VisioCard from './components/VisioCard.vue';
import VisioCopyLink from './components/VisioCopyLink.vue';
import VisioEmptyState from './components/VisioEmptyState.vue';
import VisioInstantPanel from './components/VisioInstantPanel.vue';

const components = {
  'visio-drawer-app': VisioDrawerApp,
  'visio-drawer': VisioDrawer,
  'visio-section': VisioSection,
  'visio-card': VisioCard,
  'visio-copy-link': VisioCopyLink,
  'visio-empty-state': VisioEmptyState,
  'visio-instant-panel': VisioInstantPanel,
};

for (const key in components) {
  Vue.component(key, components[key]);
}
