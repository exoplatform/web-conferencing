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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Date;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.exoplatform.webconferencing.CallState;
import org.exoplatform.webconferencing.UserState;
import org.exoplatform.webconferencing.domain.CallEntity;
import org.exoplatform.webconferencing.domain.ParticipantEntity;
import org.exoplatform.webconferencing.domain.ParticipantId;

/**
 * The missed call mark runs through Hibernate on HSQLDB: it marks a participant
 * only while the participant is still invited and the call still started from
 * a start not later than the given date, and only that participant of that
 * call. Index usage, the other engines' behaviour and the rounding of a start
 * date stored in whole seconds are not covered by this test.
 */
class ParticipantDAOTest {

  private static final String  CALL_ID       = "p_call-1";

  private static final String  OTHER_CALL_ID = "g_space-1";

  private static final String  JOHN          = "john";

  private static final String  MARY          = "mary";

  private static final String  PAUL          = "paul";

  private static final Date    STARTED_AT    = new Date(1_700_000_000_000L);

  private static final Date    LATEST_START  = new Date(STARTED_AT.getTime() + 1000);

  private EntityManagerFactory entityManagerFactory;

  private EntityManager        entityManager;

  @BeforeEach
  void setUp() {
    entityManagerFactory = new Configuration().addAnnotatedClass(ParticipantEntity.class)
                                              .addAnnotatedClass(CallEntity.class)
                                              .setProperty("hibernate.connection.url", "jdbc:hsqldb:mem:wbc-participants")
                                              .setProperty("hibernate.connection.username", "sa")
                                              .setProperty("hibernate.connection.password", "")
                                              .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                                              .buildSessionFactory();
    entityManager = entityManagerFactory.createEntityManager();
    entityManager.getTransaction().begin();
    entityManager.persist(call(CALL_ID, CallState.STARTED, STARTED_AT));
    entityManager.persist(call(OTHER_CALL_ID, CallState.STARTED, STARTED_AT));
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
  void marksTheStillInvitedParticipantOfTheCallOnly() {
    assertEquals(1, markMissed(CALL_ID, JOHN, LATEST_START));

    assertEquals(UserState.MISSED, stateOf(CALL_ID, JOHN));
    assertEquals(UserState.JOINED, stateOf(CALL_ID, MARY));
    assertEquals(UserState.INVITED, stateOf(CALL_ID, PAUL));
    assertEquals(UserState.INVITED, stateOf(OTHER_CALL_ID, JOHN));
  }

  @Test
  void marksAParticipantOnceOnly() {
    assertEquals(1, markMissed(CALL_ID, JOHN, LATEST_START));

    assertEquals(0, markMissed(CALL_ID, JOHN, LATEST_START));
    assertEquals(UserState.MISSED, stateOf(CALL_ID, JOHN));
  }

  @Test
  void leavesAParticipantNoLongerInvitedUnchanged() {
    assertEquals(0, markMissed(CALL_ID, MARY, LATEST_START));

    assertEquals(UserState.JOINED, stateOf(CALL_ID, MARY));
  }

  @Test
  void marksAParticipantOfACallStartedUpToTheLatestStart() {
    assertEquals(1, markMissed(CALL_ID, JOHN, STARTED_AT));
  }

  @Test
  void marksNobodyOfACallStartedAgainLater() {
    assertEquals(0, markMissed(CALL_ID, JOHN, new Date(STARTED_AT.getTime() - 1)));

    assertEquals(UserState.INVITED, stateOf(CALL_ID, JOHN));
  }

  @Test
  void marksNobodyOfACallNoLongerStarted() {
    entityManager.getTransaction().begin();
    entityManager.find(CallEntity.class, CALL_ID).setState(CallState.STOPPED);
    entityManager.getTransaction().commit();
    entityManager.clear();

    assertEquals(0, markMissed(CALL_ID, JOHN, LATEST_START));
    assertEquals(UserState.INVITED, stateOf(CALL_ID, JOHN));
  }

  @Test
  void marksTheStillInvitedParticipantOfAStoppedCall() {
    stop(CALL_ID);

    assertEquals(1, markMissedAtStop(CALL_ID, JOHN, LATEST_START));

    assertEquals(UserState.MISSED, stateOf(CALL_ID, JOHN));
    assertEquals(UserState.JOINED, stateOf(CALL_ID, MARY));
    assertEquals(UserState.INVITED, stateOf(CALL_ID, PAUL));
    assertEquals(UserState.INVITED, stateOf(OTHER_CALL_ID, JOHN));
  }

  @Test
  void marksAParticipantOfAStoppedCallOnceOnly() {
    stop(CALL_ID);
    assertEquals(1, markMissedAtStop(CALL_ID, JOHN, LATEST_START));

    assertEquals(0, markMissedAtStop(CALL_ID, JOHN, LATEST_START));
    assertEquals(UserState.MISSED, stateOf(CALL_ID, JOHN));
  }

  @Test
  void leavesAParticipantOfAStoppedCallNoLongerInvitedUnchanged() {
    stop(CALL_ID);

    assertEquals(0, markMissedAtStop(CALL_ID, MARY, LATEST_START));

    assertEquals(UserState.JOINED, stateOf(CALL_ID, MARY));
  }

  @Test
  void marksNobodyOfAStoppedCallStartedAgainLater() {
    stop(CALL_ID);

    assertEquals(0, markMissedAtStop(CALL_ID, JOHN, new Date(STARTED_AT.getTime() - 1)));

    assertEquals(UserState.INVITED, stateOf(CALL_ID, JOHN));
  }

  @Test
  void marksNobodyOfAStoppedCallStartedAgainWithinTheSameSecond() {
    assertEquals(0, markMissedAtStop(CALL_ID, JOHN, LATEST_START));

    assertEquals(UserState.INVITED, stateOf(CALL_ID, JOHN));
  }

  @Test
  void marksTheStillInvitedParticipantOfAStartedCallAsDeclined() {
    setClientId(CALL_ID, JOHN, "client-1");

    assertEquals(1, markDeclined(CALL_ID, JOHN));

    assertEquals(UserState.DECLINED, stateOf(CALL_ID, JOHN));
    assertNull(entityManager.find(ParticipantEntity.class, new ParticipantId(JOHN, CALL_ID)).getClientId());
    assertEquals(UserState.INVITED, stateOf(CALL_ID, PAUL));
    assertEquals(UserState.INVITED, stateOf(OTHER_CALL_ID, JOHN));
  }

  @Test
  void leavesAParticipantNoLongerInvitedUndeclined() {
    setState(CALL_ID, PAUL, UserState.MISSED);

    assertEquals(0, markDeclined(CALL_ID, MARY));
    assertEquals(0, markDeclined(CALL_ID, PAUL));

    assertEquals(UserState.JOINED, stateOf(CALL_ID, MARY));
    assertEquals(UserState.MISSED, stateOf(CALL_ID, PAUL));
  }

  @Test
  void marksNoDeclineOfACallNoLongerStarted() {
    stop(CALL_ID);

    assertEquals(0, markDeclined(CALL_ID, JOHN));

    assertEquals(UserState.INVITED, stateOf(CALL_ID, JOHN));
  }

  @Test
  void findsTheDeclinedUserParticipantsOfTheCallOnly() {
    setState(CALL_ID, JOHN, UserState.DECLINED);
    setState(CALL_ID, PAUL, UserState.DECLINED_NOTIFIED);
    setState(OTHER_CALL_ID, JOHN, UserState.DECLINED);

    assertEquals(List.of(JOHN), ParticipantDAO.findDeclinedParticipantIds(entityManager, CALL_ID));
  }

  @Test
  void findsADeclineSavedAfterTheParticipantWasLoaded() {
    // The request that stops a call loaded its participants before a decline was saved
    assertEquals(UserState.INVITED, stateOf(CALL_ID, JOHN));
    entityManager.getTransaction().begin();
    ParticipantDAO.markParticipantDeclined(entityManager, CALL_ID, JOHN);
    entityManager.getTransaction().commit();

    assertEquals(UserState.INVITED, stateOf(CALL_ID, JOHN));
    assertEquals(List.of(JOHN), ParticipantDAO.findDeclinedParticipantIds(entityManager, CALL_ID));
  }

  @Test
  void marksTheDeclinedParticipantOfTheCallOnlyAsNotified() {
    setState(CALL_ID, JOHN, UserState.DECLINED);
    setState(OTHER_CALL_ID, JOHN, UserState.DECLINED);

    assertEquals(1, markDeclinedNotified(CALL_ID, JOHN, LATEST_START));

    assertEquals(UserState.DECLINED_NOTIFIED, stateOf(CALL_ID, JOHN));
    assertEquals(UserState.JOINED, stateOf(CALL_ID, MARY));
    assertEquals(UserState.INVITED, stateOf(CALL_ID, PAUL));
    assertEquals(UserState.DECLINED, stateOf(OTHER_CALL_ID, JOHN));
  }

  @Test
  void marksADeclineAsNotifiedOnceOnly() {
    setState(CALL_ID, JOHN, UserState.DECLINED);
    assertEquals(1, markDeclinedNotified(CALL_ID, JOHN, LATEST_START));

    assertEquals(0, markDeclinedNotified(CALL_ID, JOHN, LATEST_START));
    assertEquals(UserState.DECLINED_NOTIFIED, stateOf(CALL_ID, JOHN));
  }

  @Test
  void leavesAParticipantWhoDidNotDeclineUnchanged() {
    assertEquals(0, markDeclinedNotified(CALL_ID, PAUL, LATEST_START));
    assertEquals(0, markDeclinedNotified(CALL_ID, MARY, LATEST_START));

    assertEquals(UserState.INVITED, stateOf(CALL_ID, PAUL));
    assertEquals(UserState.JOINED, stateOf(CALL_ID, MARY));
  }

  @Test
  void marksADeclineOfACallStartedUpToTheLatestStart() {
    setState(CALL_ID, JOHN, UserState.DECLINED);

    assertEquals(1, markDeclinedNotified(CALL_ID, JOHN, STARTED_AT));
  }

  @Test
  void marksNoDeclineOfACallStartedAgainLater() {
    setState(CALL_ID, JOHN, UserState.DECLINED);

    assertEquals(0, markDeclinedNotified(CALL_ID, JOHN, new Date(STARTED_AT.getTime() - 1)));

    assertEquals(UserState.DECLINED, stateOf(CALL_ID, JOHN));
  }

  @Test
  void marksTheDeclineOfAStoppedCallAsNotified() {
    setState(CALL_ID, JOHN, UserState.DECLINED);
    stop(CALL_ID);

    assertEquals(1, markDeclinedNotified(CALL_ID, JOHN, LATEST_START));

    assertEquals(UserState.DECLINED_NOTIFIED, stateOf(CALL_ID, JOHN));
  }

  private void setState(String callId, String partId, String state) {
    entityManager.getTransaction().begin();
    entityManager.find(ParticipantEntity.class, new ParticipantId(partId, callId)).setState(state);
    entityManager.getTransaction().commit();
    entityManager.clear();
  }

  private void setClientId(String callId, String partId, String clientId) {
    entityManager.getTransaction().begin();
    entityManager.find(ParticipantEntity.class, new ParticipantId(partId, callId)).setClientId(clientId);
    entityManager.getTransaction().commit();
    entityManager.clear();
  }

  private int markDeclined(String callId, String partId) {
    entityManager.getTransaction().begin();
    int updated = ParticipantDAO.markParticipantDeclined(entityManager, callId, partId);
    entityManager.getTransaction().commit();
    entityManager.clear();
    return updated;
  }

  private int markDeclinedNotified(String callId, String partId, Date latestStart) {
    entityManager.getTransaction().begin();
    int updated = ParticipantDAO.markParticipantDeclinedNotified(entityManager, callId, partId, latestStart);
    entityManager.getTransaction().commit();
    entityManager.clear();
    return updated;
  }

  private void stop(String callId) {
    entityManager.getTransaction().begin();
    entityManager.find(CallEntity.class, callId).setState(CallState.STOPPED);
    entityManager.getTransaction().commit();
    entityManager.clear();
  }

  private int markMissedAtStop(String callId, String partId, Date latestStart) {
    entityManager.getTransaction().begin();
    int updated = ParticipantDAO.markParticipantMissedAtStop(entityManager, callId, partId, latestStart);
    entityManager.getTransaction().commit();
    entityManager.clear();
    return updated;
  }

  private int markMissed(String callId, String partId, Date latestStart) {
    entityManager.getTransaction().begin();
    int updated = ParticipantDAO.markParticipantMissed(entityManager, callId, partId, latestStart);
    entityManager.getTransaction().commit();
    entityManager.clear();
    return updated;
  }

  private String stateOf(String callId, String partId) {
    return entityManager.find(ParticipantEntity.class, new ParticipantId(partId, callId)).getState();
  }

  private CallEntity call(String callId, String state, Date lastDate) {
    CallEntity entity = new CallEntity();
    entity.setId(callId);
    entity.setProviderType("pexip");
    entity.setOwnerType("space");
    entity.setOwnerId(callId);
    entity.setState(state);
    entity.setLastDate(lastDate);
    return entity;
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
