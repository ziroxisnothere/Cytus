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
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Example plugin showing how to integrate PacketLimiterModule with Velocity's packet pipeline.
 * This demonstrates filtering packets using the PacketLimiterModule similar to FireGuard's approach.
 */
@Plugin(
    name = "packetlimiter",
    id = "packetlimiter",
    version = "1.0.0",
    description = "Packet limiting and filtering module for Velocity",
    authors = {"zirox"}
)
public class PacketLimiterModuleExample extends ChannelInboundHandlerAdapter {

  private final Logger logger;
  private final VelocityServer server;
  private final PacketLimiterModule packetLimiter;
  private final Path dataDirectory;

  public PacketLimiterModuleExample(
      Logger logger,
      VelocityServer server,
      @DataDirectory Path dataDirectory
  ) {
    this.logger = logger;
    this.server = server;
    this.dataDirectory = dataDirectory;
    this.packetLimiter = new PacketLimiterModule(logger, server);
    logger.info("PacketLimiterModule initialized");
  }

  @Subscribe
  public void onProxyInitialize(ProxyInitializeEvent event) {
    logger.info("PacketLimiter module enabled!");
  }

  @Subscribe
  public void onProxyShutdown(ProxyShutdownEvent event) {
    logger.info("PacketLimiter module disabled!");
  }

  @Subscribe
  public void onPlayerLogin(LoginEvent event) {
    logger.info("Player {} is logging in", event.getPlayer().getUsername());
  }

  @Subscribe
  public void onPlayerDisconnect(DisconnectEvent event) {
    logger.info("Player {} disconnected: {}",
        event.getPlayer().getUsername(),
        event.getReason().orElse("Unknown"));
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof MinecraftPacket packet) {
      // Check packet size before processing
      CheckItemResult result = packetLimiter.filterPacket(
          packet.getClass().getSimpleName(),
          null,  // Buffer obtained from decode context in real usage
          null   // Player from connection in real usage
      );

      if (result != CheckItemResult.VALID_ITEM) {
        logger.warning("Packet {} rejected: {}", packet.getClass().getSimpleName(), result);
        return;
      }
    }
    super.channelRead(ctx, msg);
  }
}
