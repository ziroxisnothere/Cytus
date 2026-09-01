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
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.Logger;

/**
 * PacketFilterModule allows blocking specific packets by name (blacklist).
 * It extends ActionCooldownsModule for violation tracking.
 */
public class PacketFilterModule extends ActionCooldownsModule {

  private boolean offlinePackets;
  private final Set<String> blacklist = ConcurrentHashMap.newKeySet();

  public PacketFilterModule(Logger logger) {
    super(logger);
  }

  @Override
  public void reload(CommentedConfig config) {
    if (config == null) {
      return;
    }

    super.reload(config);

    this.enabled = config.getOrElse("enabled", true);
    this.vls = config.getOrElse("vls", 50.0);
    this.offlinePackets = config.getOrElse("offline_packets", true);

    this.blacklist.clear();
    List<String> bl = config.get("blacklist");
    if (bl != null) {
      this.blacklist.addAll(bl);
    }
  }

  @Override
  public String getName() {
    return "PacketFilter";
  }

  @Override
  public boolean isEnabled() {
    return this.enabled;
  }

  /**
   * Checks if packets should still be processed when the player is offline.
   */
  public boolean isOfflinePacketsEnabled() {
    return this.offlinePackets;
  }

  /**
   * Checks if a packet type is blacklisted.
   *
   * @param packet the packet name to check
   * @return true if the packet is blacklisted
   */
  public boolean isPacketBlacklisted(String packet) {
    return this.blacklist.contains(packet);
  }

  /**
   * Adds a packet type to the blacklist.
   */
  public void addToBlacklist(String packet) {
    this.blacklist.add(packet);
  }

  /**
   * Removes a packet type from the blacklist.
   */
  public void removeFromBlacklist(String packet) {
    this.blacklist.remove(packet);
  }

  /**
   * Gets all blacklisted packet names.
   */
  public Set<String> getBlacklist() {
    return Set.copyOf(this.blacklist);
  }
}
