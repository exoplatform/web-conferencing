/*
 * Copyright (C) 2022 eXo Platform SAS.
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
package org.exoplatform.webconferencing.notification.utils;

import org.exoplatform.commons.api.notification.model.ArgumentLiteral;

import java.util.List;

public class NotificationConstants {

    public static final ArgumentLiteral<? extends List> CALL_PARTICIPANTS     = new ArgumentLiteral<>(List.class, "CALL_PARTICIPANTS");

    public static final ArgumentLiteral<String> FILE_NAME     = new ArgumentLiteral<>(String.class, "FILE_NAME");

    public static final ArgumentLiteral<String> RECORDED_FILE_URL         = new ArgumentLiteral<>(String.class, "RECORDED_FILE_URL");

    public static final ArgumentLiteral<String> CALL_TYPE         = new ArgumentLiteral<>(String.class, "CALL_TYPE");

    public static final ArgumentLiteral<String> RECORDING_STATUS         = new ArgumentLiteral<>(String.class, "RECORDING_STATUS");

    public static final ArgumentLiteral<String> AVATAR_URL     = new ArgumentLiteral<>(String.class, "AVATAR");

    public static final ArgumentLiteral<String> CALL_OWNER  = new ArgumentLiteral<>(String.class, "CALL_OWNER");

    private NotificationConstants() {
        throw new IllegalStateException("Utility class");
    }
}
