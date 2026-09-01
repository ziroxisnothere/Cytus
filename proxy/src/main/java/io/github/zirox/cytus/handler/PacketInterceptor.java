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

package io.github.zirox.cytus.handler;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.netty.MinecraftDecoder;
import io.github.zirox.cytus.modules.CheckItemResult;
import io.github.zirox.cytus.modules.InvalidPayloadModule;
import io.github.zirox.cytus.modules.InvalidRecipeIDModule;
import io.github.zirox.cytus.modules.InvalidSelectBundleModule;
import io.github.zirox.cytus.modules.PacketFilterModule;
import io.github.zirox.cytus.modules.PacketFunnelModule;
import io.github.zirox.cytus.modules.PacketLimiterModule;
import io.github.zirox.cytus.util.CytusLogger;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;

/**
 * PacketInterceptor is a Netty handler that intercepts serverbound packets
 * BEFORE they are decoded by Velocity's MinecraftDecoder. This allows
 * raw-byte inspection with full protocol version awareness.
 *
 * Position in pipeline:
 *   [frame_decoder] -> [cytus_packet_interceptor] -> [minecraft_decoder] -> [velocity_handler]
 *
 * The interceptor reads the packet ID (VarInt) and the protocol state, then
 * applies the appropriate Cytus module checks before passing the packet on.
 */
public class PacketInterceptor extends ChannelInboundHandlerAdapter {

  private final PacketLimiterModule packetLimiter;
  private final PacketFunnelModule packetFunnel;
  private final PacketFilterModule packetFilter;
  private final InvalidPayloadModule invalidPayload;
  private final InvalidRecipeIDModule invalidRecipeID;
  private final InvalidSelectBundleModule invalidSelectBundle;
  private final CytusLogger cytusLogger;
  private final Logger logger;

  // Per-connection packet counters
  private final AtomicLong packetCount = new AtomicLong();

  // Track player (populated when channel is associated)
  private ConnectedPlayer player;
  // Track protocol state
  private StateRegistry state = StateRegistry.HANDSHAKE;

  public PacketInterceptor(
      PacketLimiterModule packetLimiter,
      PacketFunnelModule packetFunnel,
      PacketFilterModule packetFilter,
      InvalidPayloadModule invalidPayload,
      InvalidRecipeIDModule invalidRecipeID,
      InvalidSelectBundleModule invalidSelectBundle,
      Logger logger
  ) {
    this.packetLimiter = packetLimiter;
    this.packetFunnel = packetFunnel;
    this.packetFilter = packetFilter;
    this.invalidPayload = invalidPayload;
    this.invalidRecipeID = invalidRecipeID;
    this.invalidSelectBundle = invalidSelectBundle;
    this.logger = logger;
    this.cytusLogger = new CytusLogger(logger);
  }

  /**
   * Sets the connected player for this interceptor.
   */
  public void setPlayer(ConnectedPlayer player) {
    this.player = player;
  }

  /**
   * Updates the protocol state (called when state changes).
   */
  public void setState(StateRegistry state) {
    this.state = state;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof ByteBuf buf) {
      // 1) Check raw packet size
      int size = buf.readableBytes();
      if (packetLimiter.isEnabled() && packetLimiter.checkPacketSizeBytes(size)) {
        cytusLogger.warn("Raw packet exceeded max size: " + size);
        buf.release();
        return;
      }

      // 2) Read packet ID with version awareness
      if (buf.readableBytes() > 0) {
        int packetId = ProtocolUtils.readVarInt(buf);
        buf.readerIndex(0); // Reset for downstream

        // 3) Resolve the packet class with the actual protocol version
        String packetName = resolvePacketName(packetId, ctx);

        // 4) Blacklist check (PacketFilterModule)
        if (packetFilter.isEnabled() && packetFilter.isPacketBlacklisted(packetName)) {
          cytusLogger.logRejection(
              player != null ? player.getUsername() : "?",
              "Blacklisted packet: " + packetName
          );
          buf.release();
          return;
        }

        // 5) Whitelist bypass (PacketFunnelModule)
        if (!packetFunnel.isEnabled() || !packetFunnel.isWhitelisted(packetName)) {
          // 6) Apply checks specific to packet type
          CheckItemResult result = applyPacketChecks(packetId, packetName, buf);
          if (result != CheckItemResult.VALID_ITEM) {
            cytusLogger.logRejection(
                player != null ? player.getUsername() : "?",
                packetName + ": " + result
            );
            buf.release();
            return;
          }
        }
      }

      packetCount.incrementAndGet();
      ctx.fireChannelRead(buf);
    } else {
      ctx.fireChannelRead(msg);
    }
  }

  /**
   * Resolves a packet class name using the protocol version of the connection.
   * Falls back to "Unknown{id=...}" if not registered.
   */
  private String resolvePacketName(int packetId, ChannelHandlerContext ctx) {
    try {
      MinecraftDecoder decoder = ctx.pipeline().get(MinecraftDecoder.class);
      if (decoder == null) {
        return "Unknown{id=" + packetId + "}";
      }
      com.velocitypowered.proxy.protocol.StateRegistry.PacketRegistry.ProtocolRegistry registry =
          decoder.getProtocolRegistry();
      Class<? extends MinecraftPacket> packetClass = registry.getPacketClass(packetId);
      return packetClass != null ? packetClass.getSimpleName() : "Unknown{id=" + packetId + "}";
    } catch (Exception e) {
      return "Unknown{id=" + packetId + "}";
    }
  }

  /**
   * Applies the relevant Cytus module checks for a given packet ID.
   */
  private CheckItemResult applyPacketChecks(int packetId, String packetName, ByteBuf buf) {
    // Check for plugin message packets
    if (invalidPayload.isEnabled() && packetName.contains("PluginMessage")) {
      return handlePluginMessageCheck(packetId, buf);
    }

    // Check for recipe packets
    if (invalidRecipeID.isEnabled() && packetName.contains("Recipe")) {
      return handleRecipeCheck(packetId, packetName);
    }

    // Check for select bundle packets
    if (invalidSelectBundle.isEnabled() && packetName.contains("SelectBundle")) {
      return handleSelectBundleCheck(packetId, buf);
    }

    // Check for move/position packets
    if (packetLimiter.isEnabled() && isMovePacket(packetName)) {
      return handleMovePacketCheck(packetId, buf);
    }

    return CheckItemResult.VALID_ITEM;
  }

  /**
   * Handles PluginMessage (custom payload) packet validation.
   */
  private CheckItemResult handlePluginMessageCheck(int packetId, ByteBuf buf) {
    try {
        int readerIndex = buf.readerIndex();
        // Skip the packet ID we already read
        int packetIdLen = ProtocolUtils.readVarInt(buf);
        readerIndex = buf.readerIndex();

        // Read channel name
        String channel = ProtocolUtils.readString(buf);
        // Reset reader index
        buf.readerIndex(readerIndex);

        if (invalidPayload.isExploitChannel(channel)) {
          return CheckItemResult.INVALID_ITEM;
        }

        // Read remaining bytes for payload size
        int payloadSize = buf.readableBytes() - packetIdLen;
        if (invalidPayload.isPayloadOversized(channel, payloadSize)) {
          return CheckItemResult.STRING_TOO_LONG;
        }

        if (packetFilter.isPacketBlacklisted("PluginMessage:" + channel)) {
          return CheckItemResult.INVALID_ITEM;
        }
      } catch (Exception e) {
        // Malformed packet - let downstream handle it
      }
    return CheckItemResult.VALID_ITEM;
  }

  /**
   * Handles Recipe packet validation.
   */
  private CheckItemResult handleRecipeCheck(int packetId, String packetName) {
    // We rely on Velocity's decoder to parse the recipe packet
    // For now, just return valid - more detailed validation requires decoded packet
    return CheckItemResult.VALID_ITEM;
  }

  /**
   * Handles SelectBundle packet validation.
   */
  private CheckItemResult handleSelectBundleCheck(int packetId, ByteBuf buf) {
    try {
      int readerIndex = buf.readerIndex();
      int packetIdLen = ProtocolUtils.readVarInt(buf);
      // SelectBundle has: int bundleIndex
      int bundleIndex = buf.readInt();
      buf.readerIndex(readerIndex);

      if (!invalidSelectBundle.isValidBundleIndex(bundleIndex)) {
        return CheckItemResult.INVALID_ITEM;
      }
    } catch (Exception e) {
      // Malformed packet - reject
      return CheckItemResult.INVALID_ITEM;
    }
    return CheckItemResult.VALID_ITEM;
  }

  /**
   * Handles move/position packet validation.
   */
  private CheckItemResult handleMovePacketCheck(int packetId, ByteBuf buf) {
    try {
      int readerIndex = buf.readerIndex();
      ProtocolUtils.readVarInt(buf); // Skip packet ID

      // Player position packets contain 3 doubles (x, y, z)
      // Use the readable bytes to estimate
      int remaining = buf.readableBytes();
      buf.readerIndex(readerIndex);

      if (remaining < 24) { // At least 3 doubles = 24 bytes
        return CheckItemResult.OUT_OF_BOUNDS_MOVE;
      }

      // Read position values
      buf.skipBytes(packetIdBytes(packetId));
      double x = buf.readDouble();
      double y = buf.readDouble();
      double z = buf.readDouble();
      buf.readerIndex(readerIndex);

      return packetLimiter.checkMovePosition(x, y, z);
    } catch (Exception e) {
      return CheckItemResult.OUT_OF_BOUNDS_MOVE;
    }
  }

  /**
   * Approximate bytes for packet ID (1-5 bytes).
   */
  private int packetIdBytes(int packetId) {
    if (packetId < 0x80) return 1;
    if (packetId < 0x4000) return 2;
    if (packetId < 0x200000) return 3;
    if (packetId < 0x10000000) return 4;
    return 5;
  }

  /**
   * Returns true if the packet name corresponds to a position/move packet.
   */
  private boolean isMovePacket(String packetName) {
    return packetName.contains("PlayerPosition")
        || packetName.contains("PlayerPositionLook")
        || packetName.contains("PlayerLook")
        || packetName.contains("VehicleMove");
  }

  /**
   * Velocity event handler for plugin messages (server-side filter).
   */
  @Subscribe
  public void onPluginMessage(PluginMessageEvent event) {
    if (!invalidPayload.isEnabled() && !packetFilter.isEnabled()) {
      return;
    }

    String channel = event.getIdentifier();
    byte[] data = event.getData();

    if (invalidPayload.isExploitChannel(channel)) {
      event.setResult(PluginMessageEvent.ForwardResult.handled());
      cytusLogger.log("REJECTED exploit channel: " + channel);
      return;
    }

    if (data != null && invalidPayload.isPayloadOversized(channel, data.length)) {
      event.setResult(PluginMessageEvent.ForwardResult.handled());
      cytusLogger.log("REJECTED oversized payload on channel: " + channel
          + " (size: " + data.length + ")");
      return;
    }

    if (packetFilter.isPacketBlacklisted("PluginMessage:" + channel)) {
      event.setResult(PluginMessageEvent.ForwardResult.handled());
      cytusLogger.log("REJECTED blacklisted channel: " + channel);
    }
  }

  /**
   * Gets the total number of packets processed by this interceptor.
   */
  public long getPacketCount() {
    return packetCount.get();
  }

  /**
   * Resets the packet counter.
   */
  public void resetPacketCount() {
    packetCount.set(0);
  }
}