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

import org.apache.logging.log4j.Logger;

/**
 * Simple logger wrapper that prefixes all messages with "[Cytus]".
 */
public class CytusLogger {

  private static final String PREFIX = "[Cytus] ";
  private final Logger logger;

  public CytusLogger(Logger logger) {
    this.logger = logger;
  }

  public void info(String message) {
    logger.info(PREFIX + message);
  }

  public void warn(String message) {
    logger.warn(PREFIX + message);
  }

  public void warn(String message, Throwable t) {
    logger.warn(PREFIX + message, t);
  }

  public void error(String message) {
    logger.error(PREFIX + message);
  }

  public void error(String message, Throwable t) {
    logger.error(PREFIX + message, t);
  }

  public void debug(String message) {
    logger.debug(PREFIX + message);
  }

  /**
   * Generic log method - logs at info level.
   */
  public void log(String message) {
    info(message);
  }

  /**
   * Logs a packet rejection with the player and reason.
   */
  public void logRejection(String playerName, String reason) {
    warn("Rejected packet from " + playerName + ": " + reason);
  }
}
