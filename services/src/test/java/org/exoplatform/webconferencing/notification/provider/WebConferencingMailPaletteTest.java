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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.ChannelKey;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.api.notification.plugin.NotificationPluginUtils;
import org.exoplatform.commons.api.notification.service.template.TemplateContext;
import org.exoplatform.commons.notification.mail.MailPalette;
import org.exoplatform.commons.notification.template.TemplateUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.groovyscript.GroovyTemplate;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.notification.LinkProviderUtils;
import org.exoplatform.webconferencing.notification.plugin.CallRecordingPlugin;
import org.exoplatform.webconferencing.notification.plugin.MissedCallPlugin;
import org.exoplatform.webconferencing.notification.plugin.DeclinedCallPlugin;
import org.exoplatform.webconferencing.notification.utils.DeclinedCallNotificationUtils;
import org.exoplatform.webconferencing.notification.utils.MissedCallNotificationUtils;
import org.exoplatform.webconferencing.notification.utils.NotificationConstants;

/**
 * The call mails are written on the recipient's palette: their builders bind
 * it before the template is processed, and the templates keep no palette
 * colour as a literal and render the bound palette. The templates live in the
 * <code>web-conferencing-webapp</code> module and are read from the source
 * tree of the reactor.
 */
class WebConferencingMailPaletteTest {

  private static final Path    TEMPLATES       = Paths.get("../webapp/src/main/webapp/WEB-INF/notification/templates/mail");

  // the light literals the palette replaces
  private static final Pattern PALETTE_LITERAL = Pattern.compile("(?i)(bgcolor=\"|background-color:\\s*)#(ffffff|f1f1f1)"
      + "|(?<![a-z-])color:\\s*(#333333|#2f5e92)|border[a-z-]*:\\s*\\d+px solid\\s*#(d8d8d8|c7c7c7|bbbbbb)");

  @Test
  void missedCallBuilderBindsThePaletteOfTheRecipient() {
    TemplateContext templateContext = mock(TemplateContext.class);
    NotificationInfo notification = NotificationInfo.instance()
                                                    .setTo("root")
                                                    .key(PluginKey.key(MissedCallPlugin.ID))
                                                    .with(NotificationConstants.CALLER_ID.getKey(), "john");
    NotificationContext ctx = mock(NotificationContext.class);
    when(ctx.getNotificationInfo()).thenReturn(notification);
    MissedCallTemplateBuilder builder = new MissedCallTemplateBuilder(ChannelKey.key("MAIL_CHANNEL")) {
      @Override
      protected String getLanguage(NotificationInfo notification) {
        return "en";
      }
    };
    try (MockedStatic<TemplateContext> contexts = mockStatic(TemplateContext.class);
         MockedStatic<TemplateUtils> utils = mockStatic(TemplateUtils.class);
         MockedStatic<CommonsUtils> commons = mockStatic(CommonsUtils.class);
         MockedStatic<LinkProviderUtils> links = mockStatic(LinkProviderUtils.class);
         MockedStatic<MissedCallNotificationUtils> calls = mockStatic(MissedCallNotificationUtils.class, CALLS_REAL_METHODS);
         MockedStatic<MailPalette> palette = mockStatic(MailPalette.class)) {
      contexts.when(() -> TemplateContext.newChannelInstance(any(ChannelKey.class), anyString(), anyString()))
              .thenReturn(templateContext);
      commons.when(() -> CommonsUtils.getService(IdentityManager.class)).thenReturn(mock(IdentityManager.class));
      links.when(() -> LinkProviderUtils.getUserAvatarUrl(any())).thenReturn("/avatar");
      links.when(LinkProviderUtils::getBaseUrl).thenReturn("https://exo.test");
      calls.when(() -> MissedCallNotificationUtils.getAbsoluteCallLink(notification)).thenReturn("https://exo.test/call");

      builder.makeMessage(ctx);

      palette.verify(() -> MailPalette.bind(templateContext, "root"));
    }
  }

  @Test
  void declinedCallBuilderBindsThePaletteOfTheRecipient() {
    TemplateContext templateContext = mock(TemplateContext.class);
    NotificationInfo notification = NotificationInfo.instance()
                                                    .setTo("root")
                                                    .key(PluginKey.key(DeclinedCallPlugin.ID))
                                                    .with(NotificationConstants.DECLINER_IDS.getKey(), "mary,john")
                                                    .with(NotificationConstants.DECLINES_CLOSED.getKey(), "true");
    NotificationContext ctx = mock(NotificationContext.class);
    when(ctx.getNotificationInfo()).thenReturn(notification);
    DeclinedCallTemplateBuilder builder = new DeclinedCallTemplateBuilder(ChannelKey.key("MAIL_CHANNEL")) {
      @Override
      protected String getLanguage(NotificationInfo notification) {
        return "en";
      }
    };
    try (MockedStatic<TemplateContext> contexts = mockStatic(TemplateContext.class);
         MockedStatic<TemplateUtils> utils = mockStatic(TemplateUtils.class);
         MockedStatic<CommonsUtils> commons = mockStatic(CommonsUtils.class);
         MockedStatic<LinkProviderUtils> links = mockStatic(LinkProviderUtils.class);
         MockedStatic<DeclinedCallNotificationUtils> declines = mockStatic(DeclinedCallNotificationUtils.class,
                                                                           CALLS_REAL_METHODS);
         MockedStatic<MailPalette> palette = mockStatic(MailPalette.class)) {
      contexts.when(() -> TemplateContext.newChannelInstance(any(ChannelKey.class), anyString(), anyString()))
              .thenReturn(templateContext);
      commons.when(CommonsUtils::getCurrentDomain).thenReturn("https://exo.test");
      links.when(() -> LinkProviderUtils.getUserAvatarUrl(any())).thenReturn("/avatar");
      links.when(LinkProviderUtils::getBaseUrl).thenReturn("https://exo.test");
      declines.when(() -> DeclinedCallNotificationUtils.getAbsoluteDeclineLink(notification)).thenReturn("https://exo.test/call");

      builder.makeMessage(ctx);

      palette.verify(() -> MailPalette.bind(templateContext, "root"));
    }
  }

  @Test
  void callRecordingBuilderBindsThePaletteOfTheRecipient() {
    TemplateContext templateContext = mock(TemplateContext.class);
    NotificationInfo notification = NotificationInfo.instance()
                                                    .setTo("root")
                                                    .key(PluginKey.key(CallRecordingPlugin.ID))
                                                    .with(NotificationConstants.RECORDING_STATUS.getKey(), "ok")
                                                    .with(NotificationConstants.FILE_NAME.getKey(), "recording.mp4")
                                                    .with(NotificationConstants.RECORDED_FILE_URL.getKey(), "/file")
                                                    .with(NotificationConstants.AVATAR_URL.getKey(), "/avatar")
                                                    .with(NotificationConstants.CALL_OWNER.getKey(), "space");
    NotificationContext ctx = mock(NotificationContext.class);
    when(ctx.getNotificationInfo()).thenReturn(notification);
    MailTemplateProvider provider = new MailTemplateProvider(new InitParams());
    try (MockedStatic<TemplateContext> contexts = mockStatic(TemplateContext.class);
         MockedStatic<TemplateUtils> utils = mockStatic(TemplateUtils.class);
         MockedStatic<LinkProviderUtils> links = mockStatic(LinkProviderUtils.class);
         MockedStatic<NotificationPluginUtils> plugins = mockStatic(NotificationPluginUtils.class);
         MockedStatic<MailPalette> palette = mockStatic(MailPalette.class)) {
      contexts.when(() -> TemplateContext.newChannelInstance(any(), anyString(), any())).thenReturn(templateContext);
      plugins.when(() -> NotificationPluginUtils.getLanguage("root")).thenReturn("en");
      links.when(LinkProviderUtils::getBaseUrl).thenReturn("https://exo.test");

      provider.getTemplateBuilder().get(PluginKey.key(CallRecordingPlugin.ID)).buildMessage(ctx);

      palette.verify(() -> MailPalette.bind(templateContext, "root"));
    }
  }

  @Test
  void everyMailTemplateKeepsNoPaletteColourAsALiteral() throws Exception {
    for (String name : new String[] { "MissedCallPlugin", "DeclinedCallPlugin", "CallRecordingPlugin" }) {
      String text = read(name);
      assertFalse(PALETTE_LITERAL.matcher(text).find(), name + " keeps a palette colour as a literal");
      assertTrue(text.contains("MailPalette.of(_templateContext)"), name + " reads the palette");
    }
  }

  @Test
  void theBoundPaletteReachesEveryRenderedMail() throws Exception {
    Map<String, String> palette = new HashMap<>();
    palette.put("surface", "#1E2328");
    palette.put("background", "#14181B");
    palette.put("text", "#E6EAEE");
    palette.put("secondaryText", "#9AA4AD");
    palette.put("link", "#5FA9AC");
    palette.put("border", "#3A4148");
    palette.put("primary", "#3F8487");
    palette.put(MailPalette.MODE, "dark");
    for (String name : new String[] { "MissedCallPlugin", "DeclinedCallPlugin", "CallRecordingPlugin" }) {
      String html = render(name, palette);
      assertTrue(html.contains("bgcolor=\"#1E2328\""), name + ": " + html);
      assertTrue(html.contains("color:#E6EAEE"), name + ": " + html);
      assertTrue(html.contains("class=\"mail-surface mail-text\""), name + ": " + html);
      assertFalse(html.contains("#ffffff"), name + ": " + html);
    }
  }

  @Test
  void aContextWithoutPaletteRendersInTheLightColours() throws Exception {
    for (String name : new String[] { "MissedCallPlugin", "DeclinedCallPlugin", "CallRecordingPlugin" }) {
      String html = render(name, null).toLowerCase();
      assertTrue(html.contains("bgcolor=\"#ffffff\""), name + ": " + html);
      assertFalse(html.contains("null"), name + ": " + html);
    }
  }

  private String read(String name) throws Exception {
    Path file = TEMPLATES.resolve(name + ".gtmpl");
    assumeTrue(Files.exists(file), "The mail templates of the webapp module are not reachable from this module");
    return Files.readString(file, StandardCharsets.UTF_8);
  }

  private String render(String name, Map<String, String> palette) throws Exception {
    Map<String, Object> templateContext = new HashMap<>();
    if (palette != null) {
      templateContext.put(MailPalette.CONTEXT_KEY, palette);
    }
    Map<String, Object> binding = new HashMap<>();
    binding.put("_ctx", new TemplateContextStub());
    binding.put("_templateContext", templateContext);
    binding.put("USER", "Alice");
    binding.put("CALLER_NAME", "John");
    binding.put("CALLER_AVATAR", "/avatar");
    binding.put("IS_GROUP", "false");
    binding.put("IS_ENDED", "false");
    binding.put("CALL_OWNER", "Space");
    binding.put("CALL_LINK", "https://exo.test/call");
    binding.put("HAS_CALL_PAGE", "true");
    binding.put("DECLINER_NAME", "Mary");
    binding.put("DECLINER_AVATAR", "/avatar");
    binding.put("DECLINERS_COUNT", "1");
    binding.put("RECORDING_STATUS", "ok");
    binding.put("FILE_URL", "https://exo.test/file");
    binding.put("FILE_NAME", "recording.mp4");
    binding.put("AVATAR_URL", "/avatar");
    // the recording mail's meeting row, rendered as its provider binds it
    binding.put("CALL_TITLE", "Demo ACME");
    binding.put("CALL_START_DATE", "");
    return new GroovyTemplate(name, name, read(name)).render(binding);
  }

  /** Stands for the notification context: labels echo their key, includes are not part of what is pinned. */
  public static class TemplateContextStub {

    public String appRes(String key, Object... args) {
      return "[" + key + "]";
    }

    public String escapeHTML(String text) {
      return text;
    }

    public void include(String path, Object templateContext) {
      // the shared header and footer hold nothing this test looks at
    }
  }
}
