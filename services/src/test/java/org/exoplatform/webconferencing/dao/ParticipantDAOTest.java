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
package org.exoplatform.webconferencing.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.exoplatform.webconferencing.UserState;
import org.exoplatform.webconferencing.domain.ParticipantEntity;
import org.exoplatform.webconferencing.domain.ParticipantId;

/**
 * The conditional participant state update runs through Hibernate on HSQLDB: it
 * changes a participant only while the participant still has the expected
 * state, and only that participant of that call. Index usage and the other
 * engines' behaviour are not covered by this test.
 */
class ParticipantDAOTest {

  private static final String  CALL_ID       = "p_call-1";

  private static final String  OTHER_CALL_ID = "g_space-1";

  private static final String  JOHN          = "john";

  private static final String  MARY          = "mary";

  private static final String  PAUL          = "paul";

  private EntityManagerFactory entityManagerFactory;

  private EntityManager        entityManager;

  @BeforeEach
  void setUp() {
    entityManagerFactory = new Configuration().addAnnotatedClass(ParticipantEntity.class)
                                              .setProperty("hibernate.connection.url", "jdbc:hsqldb:mem:wbc-participants")
                                              .setProperty("hibernate.connection.username", "sa")
                                              .setProperty("hibernate.connection.password", "")
                                              .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                                              .buildSessionFactory();
    entityManager = entityManagerFactory.createEntityManager();
    entityManager.getTransaction().begin();
    entityManager.persist(participant(CALL_ID, JOHN, UserState.INVITED));
    entityManager.persist(participant(CALL_ID, MARY, UserState.JOINED));
    entityManager.persist(participant(CALL_ID, PAUL, UserState.INVITED));
    entityManager.persist(participant(OTHER_CALL_ID, JOHN, UserState.INVITED));
    entityManager.getTransaction().commit();
    entityManager.clear();
  }

  @AfterEach
  void tearDown() {
    entityManager.close();
    entityManagerFactory.close();
  }

  @Test
  void updatesTheParticipantStillInTheExpectedStateOnly() {
    assertEquals(1, update(CALL_ID, JOHN, UserState.INVITED, UserState.MISSED));

    assertEquals(UserState.MISSED, stateOf(CALL_ID, JOHN));
    assertEquals(UserState.JOINED, stateOf(CALL_ID, MARY));
    assertEquals(UserState.INVITED, stateOf(CALL_ID, PAUL));
    assertEquals(UserState.INVITED, stateOf(OTHER_CALL_ID, JOHN));
  }

  @Test
  void updatesNothingOnceTheStateChanged() {
    assertEquals(1, update(CALL_ID, JOHN, UserState.INVITED, UserState.MISSED));

    assertEquals(0, update(CALL_ID, JOHN, UserState.INVITED, UserState.MISSED));
    assertEquals(UserState.MISSED, stateOf(CALL_ID, JOHN));
  }

  @Test
  void leavesAParticipantInAnotherStateUnchanged() {
    assertEquals(0, update(CALL_ID, MARY, UserState.INVITED, UserState.MISSED));

    assertEquals(UserState.JOINED, stateOf(CALL_ID, MARY));
  }

  private int update(String callId, String partId, String expectedState, String newState) {
    entityManager.getTransaction().begin();
    int updated = ParticipantDAO.updateParticipantState(entityManager, callId, partId, expectedState, newState);
    entityManager.getTransaction().commit();
    entityManager.clear();
    return updated;
  }

  private String stateOf(String callId, String partId) {
    return entityManager.find(ParticipantEntity.class, new ParticipantId(partId, callId)).getState();
  }

  private ParticipantEntity participant(String callId, String partId, String state) {
    ParticipantEntity entity = new ParticipantEntity();
    entity.setCallId(callId);
    entity.setId(partId);
    entity.setType("user");
    entity.setState(state);
    return entity;
  }
}
