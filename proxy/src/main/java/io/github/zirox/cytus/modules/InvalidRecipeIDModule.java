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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

/**
 * InvalidRecipeIDModule validates that recipe IDs in recipe packets are
 * within the bounds of known recipe displays. This prevents crashes from
 * invalid recipe IDs sent by malicious clients.
 */
public class InvalidRecipeIDModule extends ViolationsModule {

  private final Logger logger;
  private double vls;
  private List<?> allRecipeDisplays;
  private final Collection<String> failedToCheck = new HashSet<>();

  public InvalidRecipeIDModule(Logger logger) {
    super(logger);
    this.logger = logger;
  }

  @Override
  public String getName() {
    return "InvalidRecipeID";
  }

  @Override
  public void reload(CommentedConfig configYml) {
    if (configYml == null) {
      return;
    }
    this.enabled = configYml.getBoolean("enabled", true);
    this.vls = configYml.getDouble("vls", 100.0);
    // In a real implementation, this would fetch recipe displays from the server
    // For Velocity proxy, we'd need to get this from connected game servers
    this.allRecipeDisplays = getAllRecipeDisplays();
    if (this.allRecipeDisplays != null && !this.allRecipeDisplays.isEmpty()) {
      this.logger.info("Invalid Recipe ID module initialized successfully");
    } else {
      this.logger.warning("Unable to load recipe displays - Invalid Recipe ID module disabled");
      this.enabled = false;
    }
  }

  /**
   * Placeholder method - in a real implementation, this would query
   * connected Minecraft servers for their recipe displays.
   */
  @SuppressWarnings("unchecked")
  private List<?> getAllRecipeDisplays() {
    // Return empty list for now - would be populated from server data
    return java.util.Collections.emptyList();
  }

  /**
   * Checks a recipe packet for invalid recipe IDs.
   *
   * @param event    the cancellable event
   * @param packet   the packet wrapper (would contain recipe data)
   * @param player   the player who sent the packet
   */
  public void checkRecipePacket(java.util.function.Consumer<Runnable> event,
                                Object packet,
                                Player player) {
    if (!this.enabled || this.allRecipeDisplays == null) {
      return;
    }

    // In a real implementation, extract packet name and recipe ID from packet
    String packetName = getPacketName(packet);
    if (packetName == null || !packetName.contains("Recipe")) {
      return;
    }

    if (this.failedToCheck.contains(packetName)) {
      return;
    }

    try {
      int recipeIndex = getRecipeIdFromPacket(packet);
      // Check if player has bypass permission (placeholder)
      boolean hasBypass = hasBypassPermission(player);
      if (hasBypass) {
        return;
      }

      if (recipeIndex != -1 &&
          (recipeIndex < 0 || recipeIndex >= this.allRecipeDisplays.size())) {
        // Cancel the event
        String reason = "Invalid recipe: " + recipeIndex + "/" + this.allRecipeDisplays.size();
        this.logger.debug("{}", reason);
        if (this.vls > 0.0) {
          addViolations(player, event, this.vls, reason);
        }
      }
    } catch (Exception e) {
      this.failedToCheck.add(packetName);
      this.logger.debug("[" + packetName + "] error reading recipe index!", e);
    }
  }

  private String getPacketName(Object packet) {
    if (packet == null) return null;
    // Simplified - would get actual packet class name
    return packet.getClass().getSimpleName();
  }

  private int getRecipeIdFromPacket(Object packet) {
    // Placeholder - would extract actual recipe ID from packet data
    return -1;
  }

  private boolean hasBypassPermission(Player player) {
    // Placeholder - would check actual permissions
    return false;
  }

  /**
   * Adds violations to a player's record (delegates to ActionCooldownsModule if applicable).
   */
  protected void addViolations(Player player,
                               java.util.function.Consumer<Runnable> event,
                               double vls,
                               String reason) {
    // If this class extended ActionCooldownsModule, we'd call super.recordViolation
    // For now, just log
    this.logger.debug("Violation for {}: {} VLS", player.getUsername(), vls);
  }

  @Override
  public boolean isEnabled() {
    return this.enabled;
  }

  public double getVls() {
    return this.vls;
  }
}