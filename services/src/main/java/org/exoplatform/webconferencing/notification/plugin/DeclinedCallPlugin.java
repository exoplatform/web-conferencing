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
package org.exoplatform.webconferencing.notification.plugin;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.NotificationMessageUtils;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.api.notification.model.UserSetting;
import org.exoplatform.commons.api.notification.model.WebNotificationFilter;
import org.exoplatform.commons.api.notification.plugin.BaseNotificationPlugin;
import org.exoplatform.commons.api.notification.service.WebNotificationService;
import org.exoplatform.commons.api.notification.service.setting.PluginSettingService;
import org.exoplatform.commons.api.notification.service.setting.UserSettingService;
import org.exoplatform.commons.notification.channel.WebChannel;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webconferencing.notification.utils.DeclinedCallNotificationUtils;
import org.exoplatform.webconferencing.notification.utils.NotificationConstants;

import io.meeds.pwa.service.PwaNotificationService;

/**
 * Tells the caller that participants declined the call: one web notification per call start, whatever the number
 * of declines, created at the first one and updated at each next one, its push sent again; and one email once the
 * incoming call alert closed, grouping every decline. The web notification is stored here, not by the web channel:
 * the notification this plugin returns for a decline is marked read, so that the web channel stores nothing, and
 * the mail channel sends it only once the declines are closed (see
 * {@link org.exoplatform.webconferencing.notification.provider.DeclinedCallTemplateBuilder}). Declines processed
 * at the same time on this server are grouped one after the other: the platform's notification threads, which run
 * this plugin, wait for each other only for the lookup and the save of a declined call notification. On two
 * servers of a cluster, the first declines of a call start processed at the same time can each create a web
 * notification.
 */
public class DeclinedCallPlugin extends BaseNotificationPlugin {

  public static final String  ID          = "DeclinedCallPlugin";

  private static final Log    LOG         = ExoLogger.getLogger(DeclinedCallPlugin.class);

  private static final Object WEB_STORAGE = new Object();

  public DeclinedCallPlugin(InitParams initParams) {
    super(initParams);
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public boolean isValid(NotificationContext ctx) {
    return StringUtils.isNotBlank(ctx.value(NotificationConstants.CALLER_ID))
        && StringUtils.isNotBlank(ctx.value(NotificationConstants.CALL_ID))
        && StringUtils.isNotBlank(ctx.value(NotificationConstants.DECLINE_KEY))
        && StringUtils.isNotBlank(ctx.value(NotificationConstants.DECLINER_IDS));
  }

  @Override
  protected NotificationInfo makeNotification(NotificationContext ctx) {
    String callerId = ctx.value(NotificationConstants.CALLER_ID);
    String declinerIds = ctx.value(NotificationConstants.DECLINER_IDS);
    NotificationInfo notification = NotificationInfo.instance()
                                                    .setFrom(DeclinedCallNotificationUtils.split(declinerIds).get(0))
                                                    .to(List.of(callerId))
                                                    .with(NotificationConstants.CALL_ID.getKey(),
                                                          ctx.value(NotificationConstants.CALL_ID))
                                                    .with(NotificationConstants.PROVIDER_TYPE.getKey(),
                                                          ctx.value(NotificationConstants.PROVIDER_TYPE))
                                                    .with(NotificationConstants.CALLER_ID.getKey(), callerId)
                                                    .with(NotificationConstants.CALL_TYPE.getKey(),
                                                          ctx.value(NotificationConstants.CALL_TYPE))
                                                    .with(NotificationConstants.OWNER_ID.getKey(),
                                                          ctx.value(NotificationConstants.OWNER_ID))
                                                    .with(NotificationConstants.CALL_OWNER.getKey(),
                                                          ctx.value(NotificationConstants.CALL_OWNER))
                                                    .with(NotificationConstants.AVATAR_URL.getKey(),
                                                          ctx.value(NotificationConstants.AVATAR_URL))
                                                    .with(NotificationConstants.DECLINE_KEY.getKey(),
                                                          ctx.value(NotificationConstants.DECLINE_KEY))
                                                    .with(NotificationConstants.DECLINER_IDS.getKey(), declinerIds)
                                                    .with(NotificationConstants.DECLINES_CLOSED.getKey(),
                                                          ctx.value(NotificationConstants.DECLINES_CLOSED))
                                                    .key(getKey())
                                                    .end();
    if (!DeclinedCallNotificationUtils.isClosed(notification)) {
      storeWebNotification(notification);
    }
    // The web notification is never the web channel's to store: the email alone goes on
    notification.setOnPopOver(false);
    notification.setRead(true);
    return notification;
  }

  /**
   * Creates the web notification of the declines of a call start, or adds the decline to the one already
   * created, read or not, and sends its push again, when the caller receives this notification on the web.
   *
   * @param notification the notification of a decline, its last decliner first
   */
  protected void storeWebNotification(NotificationInfo notification) {
    String callerId = notification.getValueOwnerParameter(NotificationConstants.CALLER_ID.getKey());
    if (!isWebActive(callerId)) {
      return;
    }
    synchronized (WEB_STORAGE) {
      NotificationInfo previous = findWebNotification(callerId,
                                                      notification.getValueOwnerParameter(NotificationConstants.DECLINE_KEY.getKey()));
      if (previous == null) {
        NotificationInfo created = notification.clone(true).setTo(callerId).setLastModifiedDate(Calendar.getInstance());
        created.setOnPopOver(true);
        created.setRead(false);
        getWebNotificationService().save(created);
      } else {
        List<String> declinerIds = new ArrayList<>(DeclinedCallNotificationUtils.split(notification.getValueOwnerParameter(NotificationConstants.DECLINER_IDS.getKey())));
        for (String declinerId : DeclinedCallNotificationUtils.getDeclinerIds(previous)) {
          if (!declinerIds.contains(declinerId)) {
            declinerIds.add(declinerId);
          }
        }
        previous.with(NotificationConstants.DECLINER_IDS.getKey(), String.join(",", declinerIds));
        previous.setFrom(notification.getFrom());
        previous.setUpdate(true);
        previous.setRead(false);
        previous.setOnPopOver(true);
        // The storage reads both states from these parameters first, as they were when the notification was read
        previous.with(NotificationMessageUtils.READ_PORPERTY.getKey(), Boolean.FALSE.toString());
        previous.with(NotificationMessageUtils.SHOW_POPOVER_PROPERTY.getKey(), Boolean.TRUE.toString());
        previous.setResetOnBadge(false);
        previous.setLastModifiedDate(Calendar.getInstance());
        getWebNotificationService().update(previous, true);
        sendPushAgain(previous);
      }
    }
  }

  /**
   * Finds the web notification of the declines of a call start sent to its caller, read or not.
   *
   * @param callerId the user who started the call
   * @param declineKey the call start, see {@link NotificationConstants#DECLINE_KEY}
   * @return the notification, or <code>null</code> when none was sent yet
   */
  protected NotificationInfo findWebNotification(String callerId, String declineKey) {
    WebNotificationFilter filter = new WebNotificationFilter(callerId, List.of(PluginKey.key(ID)), false);
    filter.setParameter(NotificationConstants.DECLINE_KEY.getKey(), declineKey);
    List<NotificationInfo> notifications = getWebNotificationService().getNotificationInfos(filter, 0, 1);
    return notifications == null || notifications.isEmpty() ? null : notifications.get(0);
  }

  /**
   * Tells if the caller receives this notification on the web, as the web channel checks it: the web channel
   * enabled for this notification on the platform, and by the caller.
   *
   * @param callerId the user who started the call
   * @return <code>true</code> when the web notification is to be stored
   */
  protected boolean isWebActive(String callerId) {
    if (!getPluginSettingService().isActive(WebChannel.ID, ID)) {
      return false;
    }
    UserSetting setting = getUserSettingService().get(callerId);
    return setting != null && setting.isEnabled() && setting.isChannelGloballyActive(WebChannel.ID)
        && setting.isActive(WebChannel.ID, ID);
  }

  private void sendPushAgain(NotificationInfo notification) {
    // An updated web notification is not pushed by the platform, only a created one: the push has the same tag
    try {
      PwaNotificationService pwaNotificationService = getPwaNotificationService();
      if (pwaNotificationService != null) {
        pwaNotificationService.create(Long.parseLong(notification.getId()));
      }
    } catch (RuntimeException e) {
      LOG.warn("Cannot push again the declined call notification {}", notification.getId(), e);
    }
  }

  protected WebNotificationService getWebNotificationService() {
    return CommonsUtils.getService(WebNotificationService.class);
  }

  protected PluginSettingService getPluginSettingService() {
    return CommonsUtils.getService(PluginSettingService.class);
  }

  protected UserSettingService getUserSettingService() {
    return CommonsUtils.getService(UserSettingService.class);
  }

  protected PwaNotificationService getPwaNotificationService() {
    return CommonsUtils.getService(PwaNotificationService.class);
  }
}
