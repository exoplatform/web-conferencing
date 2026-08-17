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
package org.exoplatform.webconferencing.plugin;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import org.exoplatform.webconferencing.CallState;
import org.exoplatform.webconferencing.WebConferencingService;
import org.exoplatform.webconferencing.dao.StorageException;

import io.meeds.appcenter.service.ApplicationBadgePluginRegistry;

/**
 * The badge must report exactly the calls the Visio drawer shows as live, and
 * must never keep Web Conferencing from starting when App Center is absent.
 */
@ExtendWith(MockitoExtension.class)
class VisioApplicationBadgePluginTest {

  private static final String            USERNAME = "testuser";

  @Mock
  private ApplicationBadgePluginRegistry registry;

  @Mock
  private WebConferencingService         webConferencingService;

  @InjectMocks
  private VisioApplicationBadgePlugin    plugin;

  /**
   * Participant rows survive a call being stopped, so the query returns stopped
   * and paused calls too. Counting them would show a badge for visios that
   * ended hours ago.
   */
  @Test
  void countsOnlyStartedCalls() throws Exception {
    when(webConferencingService.getUserCalls(USERNAME)).thenReturn(new CallState[] {
        new CallState("call-1", CallState.STARTED), new CallState("call-2", CallState.STOPPED),
        new CallState("call-3", CallState.STARTED), new CallState("call-4", CallState.PAUSED) });

    assertEquals(2L, plugin.countBadge(USERNAME));
  }

  /**
   * A user with participant rows but no live call must show no badge at all,
   * not an empty one.
   */
  @Test
  void countsZeroWhenNothingIsLive() throws Exception {
    when(webConferencingService.getUserCalls(USERNAME)).thenReturn(new CallState[] {
        new CallState("call-1", CallState.STOPPED) });

    assertEquals(0L, plugin.countBadge(USERNAME));
  }

  /**
   * A storage failure must degrade to "no badge", never propagate: the counter
   * runs on the read path of every application tile.
   */
  @Test
  void countsZeroWhenTheCallStorageFails() throws Exception {
    when(webConferencingService.getUserCalls(USERNAME)).thenThrow(new StorageException("boom"));

    assertEquals(0L, plugin.countBadge(USERNAME));
  }

  /**
   * The count is per user; a blank username must not reach the storage at all.
   */
  @Test
  void countsZeroForABlankUsernameWithoutQuerying() throws Exception {
    assertEquals(0L, plugin.countBadge("  "));

    verify(webConferencingService, never()).getUserCalls(org.mockito.ArgumentMatchers.anyString());
  }

  /**
   * App Center owns the caching: declaring self-caching without owning a cache
   * and its single-flight would put an uncached query on every tile render.
   */
  @Test
  void isNotSelfCachedSoAppCenterOwnsTheCaching() {
    assertFalse(plugin.isSelfCached());
  }

  /**
   * Everyone can be invited to a visio, so nobody is opted out — but an
   * unidentified caller still is.
   */
  @Test
  void isEnabledForAnyIdentifiedUser() {
    assertTrue(plugin.isEnabled(USERNAME));
    assertFalse(plugin.isEnabled(""));
  }

  /**
   * The drawer binding is one string, and it has to be the {@code url} of the
   * {@code visio} descriptor: a mismatch resolves to no badge, silently.
   */
  @Test
  void declaresItsDrawerBinding() {
    ReflectionTestUtils.setField(plugin, "drawerNames", List.of("visio"));

    assertEquals(List.of("visio"), plugin.getDrawerNames());
    assertEquals(VisioApplicationBadgePlugin.BADGE_NAME, plugin.getName());
  }

  /**
   * Self-registration is what makes the contribution independent of WAR boot
   * order.
   */
  @Test
  void registersItselfWhenTheRegistryIsPresent() {
    plugin.init();

    verify(registry).addPlugin(plugin);
  }

  /**
   * The badge is a nicety: a deployment without App Center must still boot Web
   * Conferencing.
   */
  @Test
  void startsWithoutTheApplicationCenterRegistry() {
    ReflectionTestUtils.setField(plugin, "applicationBadgePluginRegistry", null);

    assertDoesNotThrow(() -> plugin.init());
  }

}
