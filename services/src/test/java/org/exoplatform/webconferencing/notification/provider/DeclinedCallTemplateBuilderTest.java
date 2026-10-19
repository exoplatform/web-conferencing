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
package org.exoplatform.webconferencing.notification.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.ChannelKey;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.api.notification.service.template.TemplateContext;
import org.exoplatform.commons.notification.template.TemplateUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.HTMLEntityEncoder;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.notification.LinkProviderUtils;
import org.exoplatform.webconferencing.notification.plugin.DeclinedCallPlugin;
import org.exoplatform.webconferencing.notification.utils.DeclinedCallNotificationUtils;
import org.exoplatform.webconferencing.notification.utils.NotificationConstants;

/**
 * The email of a declined call is built once its declines are closed, never for
 * a single decline: it names the participant who declined when only one did,
 * and counts them otherwise.
 */
class DeclinedCallTemplateBuilderTest {

  private DeclinedCallTemplateBuilder builder;

  private NotificationContext         ctx;

  @BeforeEach
  void setUp() {
    builder = new DeclinedCallTemplateBuilder(ChannelKey.key("MAIL_CHANNEL")) {
      @Override
      protected String getLanguage(NotificationInfo notification) {
        return "en";
      }
    };
    ctx = mock(NotificationContext.class);
  }

  @Test
  void aSingleDeclineBuildsNoEmail() {
    when(ctx.getNotificationInfo()).thenReturn(notification("john", false));

    try (MockedStatics statics = new MockedStatics()) {
      assertNull(builder.makeMessage(ctx));
    }
  }

  @Test
  void closedDeclinesWithoutDeclinerBuildNoEmail() {
    when(ctx.getNotificationInfo()).thenReturn(notification("", true));

    try (MockedStatics statics = new MockedStatics()) {
      assertNull(builder.makeMessage(ctx));
    }
  }

  /**
   * Lets an email be built, so that a notification that must build none fails
   * on its result rather than on the template engine.
   */
  private static class MockedStatics implements AutoCloseable {

    private final MockedStatic<TemplateContext> templateContexts = mockStatic(TemplateContext.class);

    private final MockedStatic<TemplateUtils>   templateUtils    = mockStatic(TemplateUtils.class);

    private final MockedStatic<CommonsUtils>    commonsUtils     = mockStatic(CommonsUtils.class);

    private final MockedStatic<LinkProviderUtils> links          = mockStatic(LinkProviderUtils.class);

    private final MockedStatic<DeclinedCallNotificationUtils> utils = mockStatic(DeclinedCallNotificationUtils.class,
                                                                                  CALLS_REAL_METHODS);

    MockedStatics() {
      templateContexts.when(() -> TemplateContext.newChannelInstance(any(ChannelKey.class), anyString(), anyString()))
                      .thenReturn(mock(TemplateContext.class));
      templateUtils.when(() -> TemplateUtils.processSubject(any())).thenReturn("subject");
      templateUtils.when(() -> TemplateUtils.processGroovy(any())).thenReturn("body");
      commonsUtils.when(() -> CommonsUtils.getService(IdentityManager.class)).thenReturn(mock(IdentityManager.class));
      links.when(() -> LinkProviderUtils.getUserAvatarUrl(any())).thenReturn("/avatar");
      links.when(LinkProviderUtils::getBaseUrl).thenReturn("https://exo.test");
      utils.when(() -> DeclinedCallNotificationUtils.getAbsoluteDeclineLink(any())).thenReturn("https://exo.test");
    }

    @Override
    public void close() {
      templateContexts.close();
      templateUtils.close();
      commonsUtils.close();
      links.close();
      utils.close();
    }
  }

  @Test
  void closedDeclinesBuildTheEmailNamingTheLatestDeclinerAndCountingThem() {
    NotificationInfo notification = notification("mary,john", true);
    when(ctx.getNotificationInfo()).thenReturn(notification);
    TemplateContext templateContext = mock(TemplateContext.class);
    IdentityManager identityManager = mock(IdentityManager.class);
    Identity mary = new Identity("2");
    Profile profile = new Profile(mary);
    profile.setProperty(Profile.FULL_NAME, "Mary Williams");
    mary.setProfile(profile);
    when(identityManager.getOrCreateUserIdentity("mary")).thenReturn(mary);

    try (MockedStatic<TemplateContext> templateContexts = mockStatic(TemplateContext.class);
         MockedStatic<TemplateUtils> templateUtils = mockStatic(TemplateUtils.class);
         MockedStatic<CommonsUtils> commonsUtils = mockStatic(CommonsUtils.class);
         MockedStatic<LinkProviderUtils> links = mockStatic(LinkProviderUtils.class);
         MockedStatic<DeclinedCallNotificationUtils> utils = mockStatic(DeclinedCallNotificationUtils.class, CALLS_REAL_METHODS)) {
      templateContexts.when(() -> TemplateContext.newChannelInstance(any(ChannelKey.class), anyString(), anyString()))
                      .thenReturn(templateContext);
      templateUtils.when(() -> TemplateUtils.processSubject(templateContext)).thenReturn("Your call was declined");
      templateUtils.when(() -> TemplateUtils.processGroovy(templateContext)).thenReturn("body");
      commonsUtils.when(() -> CommonsUtils.getService(IdentityManager.class)).thenReturn(identityManager);
      links.when(() -> LinkProviderUtils.getUserAvatarUrl(profile)).thenReturn("/avatar/mary");
      links.when(LinkProviderUtils::getBaseUrl).thenReturn("https://exo.test");
      utils.when(() -> DeclinedCallNotificationUtils.getAbsoluteDeclineLink(notification)).thenReturn("https://exo.test/space");

      MessageInfo message = builder.makeMessage(ctx);

      assertEquals("Your call was declined", message.getSubject());
      assertEquals("body", message.getBody());
      verify(templateContext).put("DECLINER_NAME", "Mary Williams");
      verify(templateContext).put("DECLINERS_COUNT", "2");
      verify(templateContext).put("CALL_LINK", HTMLEntityEncoder.getInstance().encode("https://exo.test/space"));
      verify(templateContext).put("USER", "root");
    }
  }

  private NotificationInfo notification(String declinerIds, boolean closed) {
    NotificationInfo notification = NotificationInfo.instance()
                                                    .setTo("root")
                                                    .with(NotificationConstants.DECLINER_IDS.getKey(), declinerIds)
                                                    .key(PluginKey.key(DeclinedCallPlugin.ID));
    if (closed) {
      notification.with(NotificationConstants.DECLINES_CLOSED.getKey(), "true");
    }
    return notification;
  }
}
