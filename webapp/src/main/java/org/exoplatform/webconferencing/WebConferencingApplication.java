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

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

import io.meeds.spring.AvailableIntegration;
import io.meeds.spring.kernel.PortalApplicationContextInitializer;

/**
 * Spring Boot bootstrap of the Web Conferencing webapp.
 * <p>
 * Web Conferencing has historically been a pure eXo Kernel add-on with no
 * Spring context at all. It gets one here for a single reason: contributing an
 * Application Center badge means contributing a Spring bean, and a bean needs a
 * context to live in. Extending {@link PortalApplicationContextInitializer}
 * registers this WAR's context with the Kernel before the portal container
 * boots, so beans of this add-on and of the platform become mutually
 * injectable — which is how {@code VisioApplicationBadgePlugin} can
 * {@code @Autowired} the Kernel-side {@link WebConferencingService}.
 * <p>
 * The scan is deliberately narrow. Only {@link #MODULE_NAME} is scanned, and
 * every class currently shipped under that package by this add-on, by
 * {@code web-conferencing-jitsi} and by {@code external-visio-connector}
 * carries no Spring annotation whatsoever — so this context registers the badge
 * plugin and its listener, and nothing else. No JPA and no Liquibase
 * integration is declared: this add-on keeps managing its own schema through
 * the Kernel, and pulling those in would put a second, competing persistence
 * bootstrap in front of it.
 * <p>
 * {@code AvailableIntegration.WEB_MODULE} is kept even though no Spring MVC
 * endpoint is exposed: it carries the platform's own web security, locale and
 * transaction filter configuration, and every legacy Kernel WAR that took a
 * Spring context before this one ({@code agenda}, {@code email-connector})
 * declares it. Dropping it would leave Spring Boot's default security
 * auto-configuration unadjusted over this WAR's existing url space.
 */
@SpringBootApplication(scanBasePackages = { WebConferencingApplication.MODULE_NAME, AvailableIntegration.KERNEL_MODULE,
    AvailableIntegration.WEB_MODULE })
@PropertySource("classpath:application.properties")
@PropertySource("classpath:application-common.properties")
public class WebConferencingApplication extends PortalApplicationContextInitializer {

  /**
   * Base package of the add-on, and the only root this context scans.
   */
  public static final String MODULE_NAME = "org.exoplatform.webconferencing";

}
