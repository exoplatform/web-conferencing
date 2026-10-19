/*
 * Copyright (C) 2026 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.webconferencing.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.Date;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.exoplatform.webconferencing.CallState;
import org.exoplatform.webconferencing.domain.CallEntity;
import org.exoplatform.webconferencing.domain.ParticipantEntity;

/**
 * Runs the Liquibase changelog of the module on HSQLDB, then checks the JPA entities against the schema it
 * creates.
 */
class WebConferencingChangelogTest {

  private static final String  URL       = "jdbc:hsqldb:mem:wbc-changelog";

  private static final String  CHANGELOG = "db/changelog/webconferencing.db.changelog-1.0.0.xml";

  private Connection           connection;

  private Liquibase            liquibase;

  private EntityManagerFactory entityManagerFactory;

  @BeforeEach
  void setUp() throws Exception {
    connection = DriverManager.getConnection(URL, "sa", "");
    liquibase = new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), new JdbcConnection(connection));
    liquibase.update();
    entityManagerFactory = new Configuration().addAnnotatedClass(ParticipantEntity.class)
                                              .addAnnotatedClass(CallEntity.class)
                                              .setProperty("hibernate.connection.url", URL)
                                              .setProperty("hibernate.connection.username", "sa")
                                              .setProperty("hibernate.connection.password", "")
                                              .setProperty("hibernate.hbm2ddl.auto", "validate")
                                              .buildSessionFactory();
  }

  @AfterEach
  void tearDown() throws Exception {
    if (entityManagerFactory != null) {
      entityManagerFactory.close();
    }
    connection.createStatement().execute("SHUTDOWN");
    connection.close();
  }

  @Test
  void theCallKeepsTheUserWhoStartedIt() {
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    try {
      CallEntity call = new CallEntity();
      call.setId("g_space-1");
      call.setProviderType("pexip");
      call.setOwnerType("space");
      call.setOwnerId("space-1");
      call.setState(CallState.STARTED);
      call.setLastDate(new Date());
      call.setCallerId("root");
      entityManager.getTransaction().begin();
      entityManager.persist(call);
      entityManager.getTransaction().commit();
      entityManager.clear();

      assertEquals("root", entityManager.find(CallEntity.class, "g_space-1").getCallerId());
    } finally {
      entityManager.close();
    }
  }

  @Test
  void theCallerColumnIsRolledBack() throws Exception {
    assertTrue(hasCallerColumn());

    liquibase.rollback(1, (String) null);

    assertFalse(hasCallerColumn());
  }

  private boolean hasCallerColumn() throws Exception {
    try (ResultSet columns = connection.getMetaData().getColumns(null, null, "WBC_CALLS", "CALLER_ID")) {
      return columns.next();
    }
  }
}
