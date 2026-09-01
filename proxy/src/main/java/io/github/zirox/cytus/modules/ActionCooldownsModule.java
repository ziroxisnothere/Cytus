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
import com.velocitypowered.api.proxy.player.Player;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.Logger;

/**
 * Base module for packet action/cooldown tracking per player.
 */
public abstract class ActionCooldownsModule extends ViolationsModule {

  protected final Logger logger;
  // Map of player name -> packet name -> violation count
  private final Map<String, Map<String, ViolationEntry>> violations = new ConcurrentHashMap<>();

  public ActionCooldownsModule(Logger logger) {
    super(logger);
    this.logger = logger;
  }

  @Override
  public void reload(CommentedConfig config) {
    // Subclasses should call super.reload(config) after loading their own settings
    // This clears violations on reload
    violations.clear();
  }

  /**
   * Records a violation for a player/packet combination.
   */
  public void recordViolation(Player player, String packetName) {
    violations
        .computeIfAbsent(player.getUsername(), k -> new ConcurrentHashMap<>())
        .computeIfAbsent(packetName, k -> new ViolationEntry())
        .increment();
  }

  /**
   * Gets the current violation count for a player/packet.
   */
  public int getViolationCount(Player player, String packetName) {
    Map<String, ViolationEntry> playerViolations = violations.get(player.getUsername());
    if (playerViolations == null) {
      return 0;
    }
    ViolationEntry entry = playerViolations.get(packetName);
    return entry == null ? 0 : entry.getCount();
  }

  /**
   * Clears all violations for a player.
   */
  public void clearViolations(Player player) {
    violations.remove(player.getUsername());
  }

  /**
   * Clears violations for a specific packet for a player.
   */
  public void clearViolation(Player player, String packetName) {
    Map<String, ViolationEntry> playerViolations = violations.get(player.getUsername());
    if (playerViolations != null) {
      playerViolations.remove(packetName);
    }
  }

  /**
   * Tracks violation entry with timestamp for decay.
   */
  public static class ViolationEntry {
    private int count;
    private long lastUpdate;

    public ViolationEntry() {
      this.count = 0;
      this.lastUpdate = System.currentTimeMillis();
    }

    public void increment() {
      this.count++;
      this.lastUpdate = System.currentTimeMillis();
    }

    public int getCount() {
      return count;
    }

    public long getLastUpdate() {
      return lastUpdate;
    }
  }
}
