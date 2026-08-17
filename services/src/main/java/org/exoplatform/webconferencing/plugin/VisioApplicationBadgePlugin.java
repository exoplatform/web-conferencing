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

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webconferencing.CallState;
import org.exoplatform.webconferencing.WebConferencingService;

import io.meeds.appcenter.plugin.ApplicationBadgePlugin;
import io.meeds.appcenter.service.ApplicationBadgePluginRegistry;

import jakarta.annotation.PostConstruct;

/**
 * Reports how many visios are going on right now among the ones the user
 * belongs to, on the Visio drawer tile of the Application Center.
 * <p>
 * Carries no counting logic of its own: it reads
 * {@link WebConferencingService#getUserCalls(String)} — the same
 * participant-scoped view the Visio drawer and the platform's own call buttons
 * are built on — and keeps the rows whose state is
 * {@link CallState#STARTED}. Badge and drawer can therefore never disagree
 * about what is live.
 * <p>
 * <strong>Semantics: every started call the user belongs to</strong>, whether
 * or not they already joined it. The nicer "only the ones I have not joined
 * yet" variant needs one extra {@code getCall(id)} read per started call, and
 * App Center's {@code ApplicationBadgeCounter} enforces a ~2000 ms budget with
 * a 3-failure/60 s breaker that returns {@code 0} — a per-call fan-out inside
 * that budget risks a badge that silently zeroes under load. This is a product
 * decision, not a technical one, and it is one predicate either way.
 * <p>
 * Two known under-reports, both inherited from the underlying query and
 * documented rather than papered over:
 * <ul>
 * <li>a user who joins a space <em>while</em> a call is already running gets no
 * participant row, because participants are only ever synced when a call
 * starts — so that call never appears in their count;</li>
 * <li>a scheduled visio nobody has joined yet has no participant rows at all,
 * which is intended: the badge reports what is live, not what is planned.</li>
 * </ul>
 */
@Component
@ConditionalOnClass(ApplicationBadgePlugin.class)
public class VisioApplicationBadgePlugin implements ApplicationBadgePlugin {

  /** Logger of this plugin. */
  private static final Log               LOG        = ExoLogger.getLogger(VisioApplicationBadgePlugin.class);

  /** Stable identifier of the badge, on the WebSocket frame and in the administration form. */
  public static final String             BADGE_NAME = "visioOngoing";

  /**
   * Optional on purpose: the badge is a nicety, not something Web Conferencing
   * depends on. When the Application Center registry is absent — a deployment
   * without the add-on, or this module's own Spring test context — the plugin
   * simply does not register instead of failing the whole context.
   */
  @Autowired(required = false)
  private ApplicationBadgePluginRegistry applicationBadgePluginRegistry;

  /**
   * The add-on's own Service, injected through the Kernel-to-Spring bridge. It
   * is the only source the count is derived from.
   */
  @Autowired
  private WebConferencingService         webConferencingService;

  /**
   * The urls of the Application Center catalog entries pointing at the Visio
   * drawer. Comma-separated and configurable, so a deployment that renamed or
   * added an entry can rebind it without an administrator having to set the
   * binding by hand. The default matches the {@code url} of the {@code visio}
   * descriptor this add-on ships in its {@code applications.json}.
   */
  @Value("${webconferencing.badge.drawerNames:visio}")
  private List<String>                   drawerNames;

  /**
   * Registers this plugin with the Application Center, unless the add-on is not
   * deployed. Self-registration from {@code @PostConstruct} is what makes the
   * contribution independent of the order in which the WARs boot.
   */
  @PostConstruct
  public void init() {
    if (applicationBadgePluginRegistry == null) {
      LOG.debug("Application Center badge registry not available, Visio badge not registered");
      return;
    }
    applicationBadgePluginRegistry.addPlugin(this);
  }

  /**
   * @return the stable identifier of this badge
   */
  @Override
  public String getName() {
    return BADGE_NAME;
  }

  /**
   * @return the {@code url} of every DRAWER catalog entry this badge binds to
   */
  @Override
  public List<String> getDrawerNames() {
    return drawerNames;
  }

  /**
   * Counts the visios currently going on among the ones that user belongs to.
   * <p>
   * The underlying query joins the participants table, so it is already scoped
   * to the queried user and returns no call they are not part of. It returns
   * every state though — participant rows survive a call being stopped — hence
   * the explicit {@link CallState#STARTED} predicate. It also spans every kind
   * of call: one-to-one, space, chat room and agenda-event visios alike, which
   * is exactly the set the drawer lists.
   *
   * @param  username the user the count is computed for
   * @return          the number of ongoing visios, {@code 0} when there is none
   *                    or when the count could not be established
   */
  @Override
  public long countBadge(String username) {
    if (StringUtils.isBlank(username)) {
      return 0;
    }
    try {
      return Arrays.stream(webConferencingService.getUserCalls(username))
                   .filter(callState -> CallState.STARTED.equals(callState.getState()))
                   .count();
    } catch (Exception e) {
      LOG.warn("Error counting ongoing visios of user {}", username, e);
      return 0;
    }
  }

  /**
   * @param  username the user to test
   * @return          {@code true} for any identified user: every user can be
   *                    invited to a visio, so none is opted out
   */
  @Override
  public boolean isEnabled(String username) {
    return StringUtils.isNotBlank(username);
  }

}
