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

package io.github.zirox.velocity.util;

import org.slf4j.Logger;

/**
 * Logger wrapper for Cytus plugin.
 */
public class CytusLogger {

  private final Logger logger;

  public CytusLogger(Logger logger) {
    this.logger = logger;
  }

  public void warn(String message) {
    logger.warn("[Cytus] " + message);
  }

  public void info(String message) {
    logger.info("[Cytus] " + message);
  }

  public void debug(String message) {
    logger.debug("[Cytus] " + message);
  }

  public void error(String message) {
    logger.error("[Cytus] " + message);
  }

  public void error(String message, Throwable t) {
    logger.error("[Cytus] " + message, t);
  }
}