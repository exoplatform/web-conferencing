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
package org.exoplatform.webconferencing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * The clients a user is in a call from, kept in one column of bounded size.
 */
class UserInfoClientIdsTest {

  @Test
  void addedClientsAreKeptInTheirOrderOnce() {
    UserInfo user = new UserInfo("john", "John", "Smith");

    user.addClientId("a");
    user.addClientId("b");
    user.addClientId("a");
    user.addClientId(null);

    assertEquals("a,b", user.getClientId());
    assertTrue(user.hasSameClientId("a"));
    assertTrue(user.hasSameClientId("b"));
    assertFalse(user.hasSameClientId("c"));
    assertFalse(user.hasSameClientId(null));
  }

  @Test
  void removingTheLastClientLeavesNone() {
    UserInfo user = new UserInfo("john", "John", "Smith");
    user.setClientId("a,b");

    assertTrue(user.removeClientId("a"));
    assertEquals("b", user.getClientId());
    assertTrue(user.hasJoinedClientIds());
    assertFalse(user.removeClientId("c"));
    assertEquals("b", user.getClientId());
    assertTrue(user.removeClientId("b"));
    assertNull(user.getClientId());
    assertFalse(user.hasJoinedClientIds());
  }

  @Test
  void theClientACallWasStartedFromIsKnownButNotJoined() {
    UserInfo user = new UserInfo("john", "John", "Smith");

    user.setStarterClientId("page");

    assertTrue(user.hasSameClientId("page"));
    assertFalse(user.hasJoinedClientIds());
    user.addClientId("window");
    assertEquals("*page,window", user.getClientId());
    assertTrue(user.hasJoinedClientIds());
    assertTrue(user.removeClientId("window"));
    assertFalse(user.hasJoinedClientIds());
    assertTrue(user.removeClientId("page"));
    assertNull(user.getClientId());
  }

  @Test
  void theClientACallWasStartedFromJoinsItInPlace() {
    UserInfo user = new UserInfo("john", "John", "Smith");
    user.setStarterClientId("page");
    user.addClientId("window");

    user.addClientId("page");

    assertEquals("page,window", user.getClientId());
    assertTrue(user.removeClientId("window"));
    assertTrue(user.hasJoinedClientIds());
  }

  @Test
  void aClientIdThatCannotBeListedIsIgnored() {
    UserInfo user = new UserInfo("john", "John", "Smith");
    user.addClientId("a");

    user.addClientId("b,c");
    user.addClientId("*d");
    user.addClientId("");

    assertEquals("a", user.getClientId());
    user.setStarterClientId("*e");
    assertNull(user.getClientId());
  }

  @Test
  void theOldestClientsAreDroppedWhenTheListOutgrowsItsColumn() {
    UserInfo user = new UserInfo("john", "John", "Smith");
    String first = UUID.randomUUID().toString();
    user.addClientId(first);
    String second = UUID.randomUUID().toString();
    user.addClientId(second);
    for (int i = 0; i < 5; i++) {
      user.addClientId(UUID.randomUUID().toString());
    }

    assertTrue(user.getClientId().length() <= UserInfo.CLIENT_IDS_MAX_LENGTH);
    assertFalse(user.hasSameClientId(first));
    assertTrue(user.hasSameClientId(second));
    assertEquals(6, user.getClientId().split(UserInfo.CLIENT_IDS_SEPARATOR).length);
  }

  @Test
  void aClientIdIsListedOnlyWhenItFitsTheColumnMarkedAsTheStarter() {
    UserInfo user = new UserInfo("john", "John", "Smith");
    String longestId = "x".repeat(UserInfo.CLIENT_IDS_MAX_LENGTH - 1);

    user.setStarterClientId(longestId);
    assertEquals(UserInfo.CLIENT_IDS_MAX_LENGTH, user.getClientId().length());
    user.setStarterClientId(longestId + "x");
    assertNull(user.getClientId());
    user.addClientId(longestId + "x");
    assertNull(user.getClientId());
    user.addClientId(longestId);
    assertEquals(longestId, user.getClientId());
  }
}
