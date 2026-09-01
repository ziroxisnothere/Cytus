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

package io.github.zirox.cytus.modules;

import com.electronwill.nightconfig.core.CommentedConfig;
import org.apache.logging.log4j.Logger;

/**
 * InvalidSelectBundleModule checks Select Bundle packets (used for server links
 * / chat-bundle selections) for valid bounds.
 */
public class InvalidSelectBundleModule extends ViolationsModule {

  private final Logger logger;

  public InvalidSelectBundleModule(Logger logger) {
    super(logger);
    this.logger = logger;
  }

  @Override
  public String getName() {
    return "InvalidSelectBundle";
  }

  @Override
  public void reload(CommentedConfig configYml) {
    if (configYml == null) {
      return;
    }
    this.enabled = configYml.getOrElse("enabled", true);
    this.vls = configYml.getOrElse("vls", 100.0);
  }

  @Override
  public boolean isEnabled() {
    return this.enabled;
  }

  public double getVls() {
    return this.vls;
  }

  /**
   * Checks if a select bundle index is valid against the bundle size.
   *
   * @param bundleIndex the index from the packet
   * @param bundleSize  the actual size of the bundle (max 100 as per Mojang spec)
   * @return true if the bundle index is valid
   */
  public boolean isValidBundleIndex(int bundleIndex, int bundleSize) {
    if (!this.enabled) return true;
    // Mojang spec: bundles have up to 100 entries, indices are 0-based
    return bundleIndex >= 0 && bundleIndex < bundleSize && bundleSize <= 100;
  }

  /**
   * Convenience check for typical cases.
   */
  public boolean isValidBundleIndex(int bundleIndex) {
    return isValidBundleIndex(bundleIndex, 100);
  }
}