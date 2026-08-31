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
package org.exoplatform.webconferencing;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
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
 * <p>
 * {@code LiquibaseAutoConfiguration} is excluded, and the exclusion is what
 * actually enforces the "no second schema bootstrap" intent. Declining to list
 * {@code LIQUIBASE_MODULE} in the scan is not enough: Spring Boot
 * auto-configuration triggers on what is present on the <em>classpath</em>, not
 * on what is scanned, and Liquibase is on every platform's classpath. Left
 * alone it looks for its default {@code db/changelog/db.changelog-master.yaml},
 * does not find one here, and fails the {@code liquibase} bean — which fails
 * {@code entityManagerFactory} behind it, stops this context, and through the
 * Kernel/Spring bridge takes the portal itself to a 500. Observed on a running
 * server, not theorised. {@code agenda} excludes the same configuration.
 * <p>
 * The JPA auto-configuration is deliberately <em>not</em> excluded, even though
 * this add-on owns no Spring-managed entity. Removing it was tried and broke
 * the boot differently: the platform modules scanned above expect an
 * {@code entityManagerFactory} to exist ({@code jpaSharedEM_entityManagerFactory}
 * resolves against it), so denying them one merely swaps a Liquibase failure
 * for a missing-bean one. The add-on continues to manage its own schema through
 * the Kernel regardless; the factory simply goes unused here.
 */
@SpringBootApplication(scanBasePackages = { WebConferencingApplication.MODULE_NAME, AvailableIntegration.KERNEL_MODULE,
    AvailableIntegration.WEB_MODULE }, exclude = { LiquibaseAutoConfiguration.class })
@PropertySource("classpath:application.properties")
@PropertySource("classpath:application-common.properties")
public class WebConferencingApplication extends PortalApplicationContextInitializer {

  /**
   * Base package of the add-on, and the only root this context scans.
   */
  public static final String MODULE_NAME = "org.exoplatform.webconferencing";

}
