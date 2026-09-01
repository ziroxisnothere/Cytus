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
import com.velocitypowered.proxy.VelocityServer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.Logger;

/**
 * PacketFunnelModule manages data throughput limits with multipliers per packet type,
 * supports Floodgate (Bedrock) compensation, and maintains a whitelist for certain packets.
 */
public class PacketFunnelModule extends ViolationsModule {

  private final Logger logger;
  private final VelocityServer server;
  private long dataMultiplier;
  private Map<String, Double> packetMultipliers;
  private Map<String, Double> floodgateCompensations;
  private Set<String> whitelist = new HashSet<>();
  private int maxWhitelistCapacity = 10;

  @Override
  public String getName() {
    return "PacketFunnel";
  }

  public PacketFunnelModule(Logger logger, VelocityServer server) {
    super(logger);
    this.logger = logger;
    this.server = server;
  }

  @Override
  public void reload(CommentedConfig config) {
    if (config == null) {
      return;
    }

    this.enabled = config.getBoolean("enabled", true);
    this.vls = config.getDoubleOrElse("vls", 50.0);

    String multiplierStr = config.getString("data_multiplier", "500");
    this.dataMultiplier = parseSizeToBytes(multiplierStr);

    this.packetMultipliers = new HashMap<>();
    CommentedConfig multipliersSection = config.get("packet_multipliers");
    if (multipliersSection != null) {
      for (Map.Entry<String, Object> entry : multipliersSection.entrySet()) {
        if (entry.getValue() instanceof Number) {
          this.packetMultipliers.put(entry.getKey(), ((Number) entry.getValue()).doubleValue());
        }
      }
    }

    this.packetMultipliers.putIfAbsent("Default", 0.01);

    this.floodgateCompensations = new HashMap<>();
    CommentedConfig floodgateSection = config.get("floodgate.compensations");
    if (floodgateSection != null) {
      for (Map.Entry<String, Object> entry : floodgateSection.entrySet()) {
        if (entry.getValue() instanceof Number) {
          double compensation = ((Number) entry.getValue()).doubleValue();
          if (compensation > 0.0) {
            this.floodgateCompensations.put(entry.getKey(), compensation);
          }
        }
      }
    }

    this.whitelist.clear();
    List<String> loadedWhitelist = config.get("whitelist");
    if (loadedWhitelist != null) {
      this.whitelist.addAll(loadedWhitelist);
    }

    this.maxWhitelistCapacity = config.getInt("max-whitelist-capacity", 10);
  }

  /**
   * Parses a size string like "500" to bytes.
   */
  private long parseSizeToBytes(String size) {
    size = size.toLowerCase().trim();
    try {
      if (size.endsWith("kb")) {
        return Long.parseLong(size.replace("kb", "").trim()) * 1024;
      } else if (size.endsWith("mb")) {
        return Long.parseLong(size.replace("mb", "").trim()) * 1024 * 1024;
      } else if (size.endsWith("gb")) {
        return Long.parseLong(size.replace("gb", "").trim()) * 1024 * 1024 * 1024;
      } else if (size.endsWith("b")) {
        return Long.parseLong(size.replace("b", "").trim());
      }
      return Long.parseLong(size);
    } catch (NumberFormatException e) {
      return 500;
    }
  }

  @Override
  public boolean isEnabled() {
    return this.enabled;
  }

  @Override
  public boolean shouldCancel(Player player, String packetName, int capacity) {
    if (super.shouldCancel(player, packetName, capacity)) {
      if (packetName != null && this.whitelist.contains(packetName)) {
        if (capacity <= this.maxWhitelistCapacity) {
          logger.debug("Packet {} is whitelisted but capacity {} <= max {}",
              packetName, capacity, this.maxWhitelistCapacity);
          return false;
        }
        logger.debug("Packet {} is whitelisted but capacity {} > max {}",
            packetName, capacity, this.maxWhitelistCapacity);
      }
      return true;
    }
    return false;
  }

  /**
   * Checks if a player is connected via Floodgate (Bedrock).
   * This is a placeholder - actual implementation depends on Velocity's Floodgate integration.
   */
  public boolean isFloodgatePlayer(Player player) {
    // In a real implementation, check if player is a Floodgate player
    // This could be done via player info, bridge plugin message, etc.
    return false;
  }

  public long getDataMultiplier() {
    return this.dataMultiplier;
  }

  public double getPacketMultiplier(Player player, String packetName) {
    double vls = this.packetMultipliers.getOrDefault(packetName,
        this.packetMultipliers.getOrDefault("Default", 0.01));

    if (isFloodgatePlayer(player)) {
      double compensation = this.floodgateCompensations.getOrDefault(packetName, 1.0);
      if (compensation > 1.0) {
        vls /= compensation;
      }
    }

    return vls;
  }

  public double getPacketMultiplier(String packetName, double def) {
    return this.packetMultipliers.getOrDefault(packetName, def);
  }

  public double getVlsForPacketSize(int packetSize) {
    return packetSize * (1.0 / this.dataMultiplier);
  }

  public boolean isWhitelisted(String packetName) {
    return this.whitelist.contains(packetName);
  }

  public int getMaxWhitelistCapacity() {
    return this.maxWhitelistCapacity;
  }
}
