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

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Example plugin integrating all three packet limiting modules:
 * - PacketLimiterModule: size/content checks
 * - PacketFunnelModule: throughput with multipliers
 * - PacketFilterModule: blacklist-based filtering
 *
 * Usage from your Netty handler:
 *   if (packetFilter.isPacketBlacklisted(packetName)) { cancel; }
 *   if (packetLimiter.filterPacket(...) != VALID) { cancel; }
 *   if (packetFunnel.shouldCancel(player, packetName, capacity)) { cancel; }
 */
@Plugin(
    name = "packetguard",
    id = "packetguard",
    version = "1.0.0",
    description = "Comprehensive packet limiting and filtering for Velocity",
    authors = {"zirox"}
)
public class ModulesUsageExample extends ChannelInboundHandlerAdapter {

  private final Logger logger;
  private final VelocityServer server;
  private final Path dataDirectory;
  private final PacketLimiterModule packetLimiter;
  private final PacketFunnelModule packetFunnel;
  private final PacketFilterModule packetFilter;

  public ModulesUsageExample(
      Logger logger,
      VelocityServer server,
      @DataDirectory Path dataDirectory
  ) {
    this.logger = logger;
    this.server = server;
    this.dataDirectory = dataDirectory;
    this.packetLimiter = new PacketLimiterModule(logger, server);
    this.packetFunnel = new PacketFunnelModule(logger, server);
    this.packetFilter = new PacketFilterModule(logger);

    logger.info("PacketGuard modules initialized");
  }

  @Subscribe
  public void onProxyInitialize(ProxyInitializeEvent event) {
    logger.info("PacketGuard enabled!");
  }

  @Subscribe
  public void onProxyShutdown(ProxyShutdownEvent event) {
    logger.info("PacketGuard disabled!");
  }

  @Subscribe
  public void onPlayerLogin(LoginEvent event) {
    packetFilter.clearViolations(event.getPlayer());
    logger.info("Player {} logged in", event.getPlayer().getUsername());
  }

  @Subscribe
  public void onPlayerDisconnect(DisconnectEvent event) {
    packetFilter.clearViolations(event.getPlayer());
    logger.info("Player {} disconnected", event.getPlayer().getUsername());
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof MinecraftPacket packet) {
      String packetName = packet.getClass().getSimpleName();

      // 1) Blacklist check (PacketFilterModule)
      if (packetFilter.isEnabled() && packetFilter.isPacketBlacklisted(packetName)) {
        logger.fine("Packet {} is blacklisted, dropping", packetName);
        return;
      }

      // 2) Size/content check (PacketLimiterModule)
      CheckItemResult limitResult = packetLimiter.filterPacket(packetName, null, null);
      if (limitResult != CheckItemResult.VALID_ITEM) {
        logger.warning("Packet {} rejected by PacketLimiter: {}", packetName, limitResult);
        return;
      }

      // 3) Throughput check (PacketFunnelModule)
      int capacity = 0; // Calculate based on player state
      // In real usage, get player from channel/connection

      // Pass along if all checks pass
      super.channelRead(ctx, msg);
    } else {
      super.channelRead(ctx, msg);
    }
  }
}