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
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.BundleDelimiterPacket;
import com.velocitypowered.proxy.protocol.packet.ClientSettingsPacket;
import com.velocitypowered.proxy.protocol.packet.PluginMessagePacket;
import io.github.zirox.cytus.modules.CheckItemResult;
import io.github.zirox.cytus.modules.InvalidPayloadModule;
import io.github.zirox.cytus.modules.InvalidRecipeIDModule;
import io.github.zirox.cytus.modules.InvalidSelectBundleModule;
import io.github.zirox.cytus.modules.PacketFilterModule;
import io.github.zirox.cytus.modules.PacketFunnelModule;
import io.github.zirox.cytus.modules.PacketLimiterModule;
import io.github.zirox.cytus.util.CytusLogger;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;

/**
 * PacketInterceptor hooks into Velocity's event bus to intercept packets
 * and applies filtering via Cytus modules.
 *
 * Velocity exposes packet data via:
 *  - PluginMessageEvent for plugin messages
 *  - ServerConnection events for raw packet intercept (via custom handlers)
 *  - PlayerChatEvent for chat
 *
 * For raw serverbound packets, you'd typically inject a Netty handler
 * into MinecraftConnection's pipeline.
 */
public class PacketInterceptor {

  private final PacketLimiterModule packetLimiter;
  private final PacketFunnelModule packetFunnel;
  private final PacketFilterModule packetFilter;
  private final InvalidPayloadModule invalidPayload;
  private final InvalidRecipeIDModule invalidRecipeID;
  private final InvalidSelectBundleModule invalidSelectBundle;
  private final CytusLogger cytusLogger;
  private final Logger logger;

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
   * Velocity event handler for PluginMessageEvent (custom payload).
   * This is the primary event Velocity exposes for plugin message interception.
   */
  @Subscribe
  public void onPluginMessage(PluginMessageEvent event) {
    if (!packetFilter.isEnabled() && !invalidPayload.isEnabled()) {
      return;
    }

    String channel = event.getIdentifier();
    byte[] data = event.getData();

    // Exploit channel check
    if (invalidPayload.isEnabled() && invalidPayload.isExploitChannel(channel)) {
      event.setResult(PluginMessageEvent.ForwardResult.handled());
      cytusLogger.warn("REJECTED exploit channel: " + channel);
      return;
    }

    // Oversized payload check
    if (invalidPayload.isEnabled() && data != null &&
        invalidPayload.isPayloadOversized(channel, data.length)) {
      event.setResult(PluginMessageEvent.ForwardResult.handled());
      cytusLogger.warn("REJECTED oversized payload on channel: " + channel + " (size: " + data.length + ")");
      return;
    }

    // Blacklist check
    if (packetFilter.isEnabled() && packetFilter.isPacketBlacklisted("PluginMessage:" + channel)) {
      event.setResult(PluginMessageEvent.ForwardResult.handled());
      cytusLogger.warn("REJECTED blacklisted channel: " + channel);
    }
  }

  /**
   * Handles a raw PluginMessagePacket from the server side.
   * This is what you would call from a Netty handler in the pipeline.
   */
  public CheckItemResult handlePluginMessage(PluginMessagePacket packet) {
    if (packet == null) {
      return CheckItemResult.VALID_ITEM;
    }

    String channel = packet.getChannel();

    // Exploit channel check
    if (invalidPayload.isExploitChannel(channel)) {
      cytusLogger.warn("REJECTED exploit channel in raw packet: " + channel);
      return CheckItemResult.INVALID_ITEM;
    }

    // Oversized payload check
    ByteBuf data = packet.content();
    if (data != null && invalidPayload.isPayloadOversized(channel, data.readableBytes())) {
      cytusLogger.warn("REJECTED oversized payload (raw) on channel: " + channel);
      return CheckItemResult.STRING_TOO_LONG;
    }

    // Blacklist check
    if (packetFilter.isPacketBlacklisted("PluginMessage:" + channel)) {
      cytusLogger.warn("REJECTED blacklisted channel in raw packet: " + channel);
      return CheckItemResult.INVALID_ITEM;
    }

    return CheckItemResult.VALID_ITEM;
  }

  /**
   * Handles ClientSettingsPacket (locale, view distance, etc.)
   */
  public CheckItemResult handleClientSettings(ClientSettingsPacket packet) {
    if (!packetLimiter.isEnabled()) {
      return CheckItemResult.VALID_ITEM;
    }

    String locale = packet.getLocale();
    if (locale != null && locale.length() > packetLimiter.getTabCompleteLimit()) {
      cytusLogger.warn("REJECTED oversized locale string: " + locale.length() + " bytes");
      return CheckItemResult.STRING_TOO_LONG;
    }

    return CheckItemResult.VALID_ITEM;
  }

  /**
   * Handles BundleDelimiterPacket (used for nested packet bundles).
   */
  public CheckItemResult handleBundleDelimiter(BundleDelimiterPacket packet) {
    if (!packetLimiter.isEnabled()) {
      return CheckItemResult.VALID_ITEM;
    }
    // Bundles themselves have no payload to validate, but tracked for stats
    return CheckItemResult.VALID_ITEM;
  }

  /**
   * Validates a select bundle index.
   */
  public CheckItemResult handleSelectBundle(int bundleIndex, int bundleSize) {
    if (!invalidSelectBundle.isEnabled()) {
      return CheckItemResult.VALID_ITEM;
    }

    if (!invalidSelectBundle.isValidBundleIndex(bundleIndex, bundleSize)) {
      cytusLogger.warn("REJECTED invalid bundle index: " + bundleIndex + " / " + bundleSize);
      return CheckItemResult.INVALID_ITEM;
    }

    return CheckItemResult.VALID_ITEM;
  }

  /**
   * Generic MinecraftPacket validation - used for raw Netty handler pipeline.
   * Call this BEFORE the packet is dispatched to MinecraftSessionHandler.
   *
   * @param packet the packet to validate
   * @param player the player who sent/receives this packet
   * @return CheckItemResult - VALID_ITEM if packet should proceed
   */
  public CheckItemResult handlePacket(MinecraftPacket packet, ConnectedPlayer player) {
    if (packet == null) {
      return CheckItemResult.VALID_ITEM;
    }

    String packetName = packet.getClass().getSimpleName();

    // 1) Blacklist check
    if (packetFilter.isEnabled() && packetFilter.isPacketBlacklisted(packetName)) {
      cytusLogger.warn("REJECTED blacklisted packet: " + packetName);
      return CheckItemResult.INVALID_ITEM;
    }

    // 2) Whitelist bypass via PacketFunnel
    if (packetFunnel.isEnabled() && packetFunnel.isWhitelisted(packetName)) {
      return CheckItemResult.VALID_ITEM;
    }

    // 3) Packet-type specific checks
    if (packet instanceof PluginMessagePacket pluginMessage) {
      return handlePluginMessage(pluginMessage);
    }
    if (packet instanceof ClientSettingsPacket clientSettings) {
      return handleClientSettings(clientSettings);
    }
    if (packet instanceof BundleDelimiterPacket) {
      return handleBundleDelimiter((BundleDelimiterPacket) packet);
    }

    return CheckItemResult.VALID_ITEM;
  }

  /**
   * Returns true if a player has accumulated enough violations to be disconnected.
   */
  public boolean shouldDisconnect(ConnectedPlayer player, String reason) {
    if (packetLimiter.getVls() > 0) {
      cytusLogger.warn("Disconnecting " + player.getUsername() + ": " + reason);
      return true;
    }
    return false;
  }

  /**
   * Registers this interceptor as an event listener.
   * Called from the main plugin's initialize method.
   */
  public void register(Object eventManager) {
    if (eventManager instanceof com.velocitypowered.api.event.EventManager em) {
      em.register(this, this);
    }
  }
}