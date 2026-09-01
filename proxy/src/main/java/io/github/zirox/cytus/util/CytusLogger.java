/*
 * Copyright (C) 2025 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.zirox.cytus.util;

import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * CytusLogger provides logging utilities with Cytus branding.
 */
public class CytusLogger {

  private static final String PREFIX = "[Cytus] ";
  private final Logger logger;

  public CytusLogger(Logger logger) {
    this.logger = logger;
  }

  /**
   * Logs a message with Cytus prefix.
   */
  public void log(String message) {
    logger.info(PREFIX + message);
  }

  /**
   * Logs a warning with Cytus prefix.
   */
  public void warn(String message) {
    logger.warning(PREFIX + message);
  }

  /**
   * Logs a debug message with Cytus prefix.
   */
  public void debug(String message) {
    logger.fine(PREFIX + message);
  }

  /**
   * Logs an error with Cytus prefix.
   */
  public void error(String message) {
    logger.severe(PREFIX + message);
  }

  /**
   * Logs a rejection event.
   */
  public void logRejection(String player, String reason) {
    logger.info(PREFIX + "REJECTED [" + player + "] " + reason);
  }

  /**
   * Formats a rejection message as an Adventure Component.
   */
  public static Component rejectionMessage(String reason) {
    return Component.text()
        .append(Component.text("[Cytus] ", NamedTextColor.RED))
        .append(Component.text("Packet rejected: ", NamedTextColor.YELLOW))
        .append(Component.text(reason, NamedTextColor.GRAY))
        .build();
  }
}
