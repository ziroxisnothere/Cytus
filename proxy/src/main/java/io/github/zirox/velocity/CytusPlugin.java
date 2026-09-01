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

package io.github.zirox.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.proxy.VelocityServer;
import io.github.zirox.velocity.commands.CytusCommand;
import io.github.zirox.velocity.modules.InvalidPayloadModule;
import io.github.zirox.velocity.modules.InvalidRecipeIDModule;
import io.github.zirox.velocity.modules.InvalidSelectBundleModule;
import io.github.zirox.velocity.modules.PacketFilterModule;
import io.github.zirox.velocity.modules.PacketFunnelModule;
import io.github.zirox.velocity.modules.PacketLimiterModule;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Cytus - Comprehensive packet protection module for Velocity.
 */
@Plugin(
    id = "cytus",
    name = "Cytus",
    version = "V1",
    description = "Comprehensive packet protection and filtering for Velocity proxy",
    authors = {"Zirox"}
)
public class CytusPlugin {

  private final Logger logger;
  private final VelocityServer server;
  private final Path dataDirectory;

  private PacketLimiterModule packetLimiter;
  private PacketFunnelModule packetFunnel;
  private PacketFilterModule packetFilter;
  private InvalidPayloadModule invalidPayload;
  private InvalidRecipeIDModule invalidRecipeID;
  private InvalidSelectBundleModule invalidSelectBundle;

  public CytusPlugin(Logger logger, VelocityServer server, @DataDirectory Path dataDirectory) {
    this.logger = logger;
    this.server = server;
    this.dataDirectory = dataDirectory;
  }

  @Subscribe
  public void onProxyInitialize(ProxyInitializeEvent event) {
    logger.info("Cytus V1 By Zirox - Initializing...");

    // Initialize all modules
    this.packetLimiter = new PacketLimiterModule(logger, server);
    this.packetFunnel = new PacketFunnelModule(logger, server);
    this.packetFilter = new PacketFilterModule(logger);
    this.invalidPayload = new InvalidPayloadModule(logger);
    this.invalidRecipeID = new InvalidRecipeIDModule(logger);
    this.invalidSelectBundle = new InvalidSelectBundleModule(logger);

    // Register /cytus command
    CytusCommand cytusCommand = new CytusCommand(logger);
    server.getCommandManager().register("cytus", cytusCommand);

    logger.info("Cytus V1 By Zirox - Enabled!");
  }

  @Subscribe
  public void onProxyShutdown(ProxyShutdownEvent event) {
    logger.info("Cytus V1 By Zirox - Disabled!");
  }

  // Module getters
  public PacketLimiterModule getPacketLimiter() {
    return packetLimiter;
  }

  public PacketFunnelModule getPacketFunnel() {
    return packetFunnel;
  }

  public PacketFilterModule getPacketFilter() {
    return packetFilter;
  }

  public InvalidPayloadModule getInvalidPayload() {
    return invalidPayload;
  }

  public InvalidRecipeIDModule getInvalidRecipeID() {
    return invalidRecipeID;
  }

  public InvalidSelectBundleModule getInvalidSelectBundle() {
    return invalidSelectBundle;
  }

  public static String getVersion() {
    return "V1";
  }

  public static String getAuthor() {
    return "Zirox";
  }
}
