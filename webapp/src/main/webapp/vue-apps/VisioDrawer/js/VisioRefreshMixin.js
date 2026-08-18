/*
 * Copyright (C) 2026 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

/*
 * Everything that keeps an open drawer truthful, kept out of the drawer itself:
 * a list of meetings that is not current is worse than no list at all, and the
 * widget this replaces never refetched at all — its timer only redrew stale
 * data. Three things can change the answer while the drawer is open: time
 * passing, a call starting or emptying, and the user coming back to the tab.
 */

/** How often the elapsed bar and the countdown are recomputed. */
const TICK_MS = 30000;

/**
 * How long call updates are coalesced. One call starting in a large space
 * reports every participant joining it, and each of those is one push: they all
 * mean the same single refresh.
 */
const PUSH_DEBOUNCE_MS = 1000;

export default {
  data: () => ({
    now: new Date(),
    ticker: null,
    subscription: null,
    pushTimer: null,
  }),
  beforeDestroy() {
    this.stopWatching();
  },
  methods: {
    /**
     * Starts the tick, the call channel subscription and the tab listener.
     *
     * @returns {void}
     */
    startWatching() {
      if (!this.ticker) {
        this.ticker = window.setInterval(() => this.now = new Date(), TICK_MS);
      }
      document.addEventListener('visibilitychange', this.onVisibilityChange);
      window.addEventListener('focus', this.onWindowFocus);
      if (!this.subscription) {
        this.$visioService.getWebConferencing()
          .then(core => {
            if (this.drawer) {
              this.subscription = this.$visioService.subscribeCallUpdates(core, this.onCallUpdate);
            }
          });
      }
    },
    /**
     * Releases all three: nothing outlives the closed drawer.
     *
     * @returns {void}
     */
    stopWatching() {
      if (this.ticker) {
        window.clearInterval(this.ticker);
        this.ticker = null;
      }
      document.removeEventListener('visibilitychange', this.onVisibilityChange);
      window.removeEventListener('focus', this.onWindowFocus);
      if (this.subscription) {
        this.subscription.off();
        this.subscription = null;
      }
      if (this.pushTimer) {
        window.clearTimeout(this.pushTimer);
        this.pushTimer = null;
      }
    },
    /**
     * Reacts to a call starting, ending, or somebody joining it — coalesced,
     * because a busy room pushes one message per participant.
     *
     * @returns {void}
     */
    onCallUpdate() {
      if (this.pushTimer) {
        window.clearTimeout(this.pushTimer);
      }
      this.pushTimer = window.setTimeout(() => {
        this.pushTimer = null;
        if (this.drawer) {
          this.refresh();
        }
      }, PUSH_DEBOUNCE_MS);
    },
    /**
     * Refreshes when the tab comes back: a list read ten minutes ago says
     * nothing about who is in a call now.
     *
     * @returns {void}
     */
    onVisibilityChange() {
      if (!document.hidden && this.drawer) {
        this.refresh();
      }
    },
    /**
     * Refreshes when this window is focused again.
     * <p>
     * A call opens in a window of its own, and joining a room that is merely
     * ready is what brings it to life — through a path that deliberately
     * notifies nobody, so no push ever announces it. Coming back to the portal
     * beside an open call window does not hide this tab either, so
     * {@code visibilitychange} stays silent and the drawer would go on showing
     * a room as ready while the viewer sits in it. Focus is the one moment the
     * list is looked at again, so it is the moment to make it true.
     *
     * @returns {void}
     */
    onWindowFocus() {
      if (this.drawer) {
        // Same coalescing as a push: focus and a call update often land together.
        this.onCallUpdate();
      }
    },
  },
};
