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

package io.github.zirox.velocity.modules;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.velocitypowered.api.proxy.player.Player;
import org.apache.logging.log4j.Logger;

/**
 * Base class for packet-limiting violation modules.
 */
public abstract class ViolationsModule {

  protected final Logger logger;
  protected boolean enabled;
  protected double vls = 50.0;

  public ViolationsModule(Logger logger) {
    this.logger = logger;
  }

  public abstract String getName();

  public abstract void reload(CommentedConfig config);

  public abstract boolean isEnabled();

  /**
   * Determines if a packet should be cancelled based on player, packet name, and capacity.
   * Override this in subclasses to implement custom cancellation logic.
   *
   * @param player     the player who sent the packet
   * @param packetName the name of the packet
   * @param capacity   the current capacity/buffer size
   * @return true if the packet should be cancelled, false otherwise
   */
  public boolean shouldCancel(Player player, String packetName, int capacity) {
    return false;
  }
}
