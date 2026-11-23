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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.webconferencing.CallState;
import org.exoplatform.webconferencing.UserState;
import org.exoplatform.webconferencing.domain.CallEntity;
import org.exoplatform.webconferencing.domain.ParticipantEntity;
import org.exoplatform.webconferencing.domain.ParticipantId;

/**
 * The missed call mark runs through Hibernate on HSQLDB: it marks a participant
 * only while the participant is still invited and the call still started from
 * a start not later than the given date, and only that participant of that
 * call. Index usage and the other engines' behaviour are not covered by this
 * test, nor the rounding of a start date to whole seconds by a MySQL TIMESTAMP
 * column, which WebConferencingService.LAST_DATE_PRECISION allows for: HSQLDB
 * keeps the fractional seconds.
 */
class ParticipantDAOTest {

  private static final String  CALL_ID       = "p_call-1";

  private static final String  OTHER_CALL_ID = "g_space-1";

  private static final String  JOHN          = "john";

  private static final String  MARY          = "mary";

  private static final String  PAUL          = "paul";

  private static final Date    STARTED_AT    = new Date(1_700_000_000_000L);

  private static final Date    LATEST_START  = new Date(STARTED_AT.getTime() + 1000);

  private static final List<String> SQL = new CopyOnWriteArrayList<>();

  private EntityManagerFactory entityManagerFactory;

  private EntityManager        entityManager;

  @BeforeEach
  void setUp() {
    SQL.clear();
    entityManagerFactory = new Configuration().addAnnotatedClass(ParticipantEntity.class)
                                              .addAnnotatedClass(CallEntity.class)
                                              .setProperty("hibernate.connection.url", "jdbc:hsqldb:mem:wbc-participants")
                                              .setProperty("hibernate.connection.username", "sa")
                                              .setProperty("hibernate.connection.password", "")
                                              .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                                              .setProperty("hibernate.generate_statistics", "true")
                                              .setProperty("hibernate.session_factory.statement_inspector", CapturingInspector.class.getName())
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

  @Test
  void marksTheStillInvitedParticipantsOfABatchAndReturnsThem() {
    List<String> marked = markBatchMissed(CALL_ID, List.of(PAUL, MARY, JOHN, "nobody"), LATEST_START);

    assertEquals(List.of(JOHN, PAUL), marked);
    assertEquals(UserState.MISSED, stateOf(CALL_ID, JOHN));
    assertEquals(UserState.MISSED, stateOf(CALL_ID, PAUL));
    assertEquals(UserState.JOINED, stateOf(CALL_ID, MARY));
    assertEquals(UserState.INVITED, stateOf(OTHER_CALL_ID, JOHN));
  }

  @Test
  void marksNobodyOfABatchOfACallNoLongerStartedOrStartedAgainLater() {
    assertEquals(List.of(), markBatchMissed(CALL_ID, List.of(JOHN, PAUL), new Date(STARTED_AT.getTime() - 1)));
    stop(CALL_ID);
    assertEquals(List.of(), markBatchMissed(CALL_ID, List.of(JOHN, PAUL), LATEST_START));

    assertEquals(UserState.INVITED, stateOf(CALL_ID, JOHN));
    assertEquals(UserState.INVITED, stateOf(CALL_ID, PAUL));
  }

  @Test
  void aSecondBatchMarksNobodyMore() {
    assertEquals(List.of(JOHN, PAUL), markBatchMissed(CALL_ID, List.of(JOHN, PAUL), LATEST_START));

    assertEquals(List.of(), markBatchMissed(CALL_ID, List.of(JOHN, PAUL), LATEST_START));
  }

  @Test
  void aBatchMarksNobodyWhoJoinedMeanwhile() {
    setState(CALL_ID, PAUL, UserState.JOINED);

    assertEquals(List.of(JOHN), markBatchMissed(CALL_ID, List.of(JOHN, PAUL), LATEST_START));
    assertEquals(UserState.JOINED, stateOf(CALL_ID, PAUL));
  }

  @Test
  void aBatchOfNobodyRunsNoQuery() {
    long before = prepareStatementCount();

    assertEquals(List.of(), markBatchMissed(CALL_ID, List.of(), LATEST_START));

    assertEquals(0, prepareStatementCount() - before);
  }

  @Test
  void aBatchOfAFullSizeCostsTwoQueriesWhateverItsSize() {
    List<String> ids = addInvitedParticipants(CALL_ID, 500);
    long before = prepareStatementCount();

    List<String> marked = markBatchMissed(CALL_ID, ids, LATEST_START);

    assertEquals(500, marked.size());
    assertEquals(2, prepareStatementCount() - before);
    assertEquals(UserState.MISSED, stateOf(CALL_ID, ids.get(499)));
    assertEquals(UserState.INVITED, stateOf(CALL_ID, JOHN));
  }

  @Test
  void aBatchOfDeclinesMarksTheStillDeclinedParticipantsOnceAndReturnsThem() {
    setState(CALL_ID, JOHN, UserState.DECLINED);
    setState(CALL_ID, PAUL, UserState.DECLINED);
    setState(CALL_ID, MARY, UserState.DECLINED_NOTIFIED);

    assertEquals(List.of(JOHN, PAUL), markBatchDeclinedNotified(CALL_ID, List.of(PAUL, MARY, JOHN), LATEST_START));
    assertEquals(List.of(), markBatchDeclinedNotified(CALL_ID, List.of(PAUL, MARY, JOHN), LATEST_START));

    assertEquals(UserState.DECLINED_NOTIFIED, stateOf(CALL_ID, JOHN));
    assertEquals(UserState.DECLINED_NOTIFIED, stateOf(CALL_ID, PAUL));
  }

  @Test
  void aBatchOfDeclinesMarksNobodyOfACallStartedAgainLater() {
    setState(CALL_ID, JOHN, UserState.DECLINED);

    assertEquals(List.of(), markBatchDeclinedNotified(CALL_ID, List.of(JOHN), new Date(STARTED_AT.getTime() - 1)));
    assertEquals(UserState.DECLINED, stateOf(CALL_ID, JOHN));
  }

  @Test
  void aBatchOfDeclinesOfAFullSizeCostsTwoQueries() {
    List<String> ids = addInvitedParticipants(CALL_ID, 500);
    for (String id : ids) {
      setStateNoClear(CALL_ID, id, UserState.DECLINED);
    }
    long before = prepareStatementCount();

    List<String> marked = markBatchDeclinedNotified(CALL_ID, ids, LATEST_START);

    assertEquals(500, marked.size());
    assertEquals(2, prepareStatementCount() - before);
  }

  @Test
  void theLockSelectOfEachBatchMethodEndsWithForUpdate() {
    setState(CALL_ID, JOHN, UserState.DECLINED);
    SQL.clear();

    markBatchMissed(CALL_ID, List.of(PAUL), LATEST_START);
    markBatchDeclinedNotified(CALL_ID, List.of(JOHN), LATEST_START);

    List<String> selects = SQL.stream().filter(sql -> sql.startsWith("select")).toList();
    assertEquals(2, selects.size());
    selects.forEach(sql -> assertTrue(sql.trim().endsWith("for update"), sql));
  }

  @Test
  void aWriterWaitsForTheLockedBatchAndThenFindsNobodyInvited() throws Exception {
    EntityManager first = entityManagerFactory.createEntityManager();
    EntityManager second = entityManagerFactory.createEntityManager();
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      first.getTransaction().begin();
      assertEquals(List.of(JOHN, PAUL), ParticipantDAO.markParticipantsMissed(first, CALL_ID, List.of(JOHN, PAUL), LATEST_START));

      Future<List<String>> concurrent = executor.submit(() -> {
        second.getTransaction().begin();
        try {
          return ParticipantDAO.markParticipantsMissed(second, CALL_ID, List.of(JOHN, PAUL), LATEST_START);
        } finally {
          second.getTransaction().commit();
        }
      });
      assertThrows(TimeoutException.class, () -> concurrent.get(700, TimeUnit.MILLISECONDS));

      first.getTransaction().commit();

      assertEquals(List.of(), concurrent.get(10, TimeUnit.SECONDS));
      assertEquals(UserState.MISSED, stateOf(CALL_ID, JOHN));
    } finally {
      executor.shutdownNow();
      if (first.getTransaction().isActive()) {
        first.getTransaction().rollback();
      }
      first.close();
      second.close();
    }
  }

  @Test
  void aBatchWaitsForAConcurrentDeclineAndMarksNobodyWhoDeclined() throws Exception {
    EntityManager decliner = entityManagerFactory.createEntityManager();
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      decliner.getTransaction().begin();
      assertEquals(1, ParticipantDAO.markParticipantDeclined(decliner, CALL_ID, JOHN));

      Future<List<String>> missed = executor.submit(() -> markBatchMissed(CALL_ID, List.of(JOHN, PAUL), LATEST_START));
      assertThrows(TimeoutException.class, () -> missed.get(700, TimeUnit.MILLISECONDS));

      decliner.getTransaction().commit();

      assertEquals(List.of(PAUL), missed.get(10, TimeUnit.SECONDS));
      assertEquals(UserState.DECLINED, stateOf(CALL_ID, JOHN));
    } finally {
      executor.shutdownNow();
      if (decliner.getTransaction().isActive()) {
        decliner.getTransaction().rollback();
      }
      decliner.close();
    }
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
    int updated = ParticipantDAO.markParticipantsDeclinedNotified(entityManager, callId, List.of(partId), latestStart).size();
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

  /** Records the SQL Hibernate sends, to check the lock clause the unit engine would not enforce on its own. */
  public static class CapturingInspector implements StatementInspector {
    @Override
    public String inspect(String sql) {
      SQL.add(sql);
      return sql;
    }
  }

  private long prepareStatementCount() {
    return entityManagerFactory.unwrap(SessionFactory.class).getStatistics().getPrepareStatementCount();
  }

  private List<String> markBatchMissed(String callId, List<String> partIds, Date latestStart) {
    EntityManager em = entityManagerFactory.createEntityManager();
    try {
      em.getTransaction().begin();
      List<String> marked = ParticipantDAO.markParticipantsMissed(em, callId, partIds, latestStart);
      em.getTransaction().commit();
      return marked;
    } finally {
      em.close();
    }
  }

  private List<String> markBatchDeclinedNotified(String callId, List<String> partIds, Date latestStart) {
    EntityManager em = entityManagerFactory.createEntityManager();
    try {
      em.getTransaction().begin();
      List<String> marked = ParticipantDAO.markParticipantsDeclinedNotified(em, callId, partIds, latestStart);
      em.getTransaction().commit();
      return marked;
    } finally {
      em.close();
    }
  }

  private List<String> addInvitedParticipants(String callId, int count) {
    List<String> ids = new ArrayList<>();
    entityManager.getTransaction().begin();
    for (int i = 0; i < count; i++) {
      String id = String.format("guest%05d", i);
      ids.add(id);
      entityManager.persist(participant(callId, id, UserState.INVITED));
    }
    entityManager.getTransaction().commit();
    entityManager.clear();
    return ids;
  }

  private void setStateNoClear(String callId, String partId, String state) {
    entityManager.getTransaction().begin();
    entityManager.find(ParticipantEntity.class, new ParticipantId(partId, callId)).setState(state);
    entityManager.getTransaction().commit();
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

  /**
   * The lock select and the update of a batch run in one transaction only because the DAO method is annotated:
   * this class calls the static seams inside transactions it opens itself, and the service tests mock the
   * storage. Without the annotation the two queries run on the request's EntityManager with no transaction around
   * them, and a failing batch is only logged.
   */
  @Test
  void theBatchMethodsRunInATransactionOfTheirOwn() throws Exception {
    Method missed = ParticipantDAO.class.getMethod("markParticipantsMissed", String.class, Collection.class, Date.class);
    Method declined = ParticipantDAO.class.getMethod("markParticipantsDeclinedNotified", String.class, Collection.class, Date.class);

    assertTrue(missed.isAnnotationPresent(ExoTransactional.class), "markParticipantsMissed must be @ExoTransactional");
    assertTrue(declined.isAnnotationPresent(ExoTransactional.class), "markParticipantsDeclinedNotified must be @ExoTransactional");
  }
}
