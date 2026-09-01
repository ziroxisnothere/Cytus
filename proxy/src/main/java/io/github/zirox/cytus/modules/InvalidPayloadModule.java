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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.apache.logging.log4j.Logger;

/**
 * InvalidPayloadModule blocks known exploit channels and oversized plugin message payloads.
 * Provides helpers for packing/unpacking block positions used for exploit detection.
 */
public class InvalidPayloadModule extends ViolationsModule {

  private static final int SIZE_BITS_X = 26;
  private static final int SIZE_BITS_Z = 26;
  private static final int SIZE_BITS_Y = 12;
  private static final int BIT_SHIFT_Z = 12;
  private static final int BIT_SHIFT_X = 38;
  private static final Set<String> EXPLOIT_CHANNELS = new HashSet<>(Arrays.asList(
      "viaver-print-exploit",
      "viaver-logfucker-exploit",
      "viaver-null-exploit",
      "plhidepro:tab"
  ));
  private static final Set<String> OVERSIZED_PAYLOAD_CHANNELS = new HashSet<>(Arrays.asList(
      "luckperms:update"
  ));

  private final Logger logger;
  private boolean beehiveCrash;
  private boolean logsExploit;
  private int maxPayloadSize;

  @Override
  public String getName() {
    return "InvalidPayload";
  }

  public InvalidPayloadModule(Logger logger) {
    super(logger);
    this.logger = logger;
  }

  @Override
  public void reload(CommentedConfig configYml) {
    if (configYml == null) {
      return;
    }
    this.enabled = configYml.getOrElse("enabled", true);
    this.vls = configYml.getOrElse("vls", 100.0);
    this.beehiveCrash = configYml.getOrElse("beehive_crash", true);
    this.logsExploit = configYml.getOrElse("logs_exploit", true);
    this.maxPayloadSize = configYml.getIntOrElse("max_payload_size", 1024);
  }

  @Override
  public boolean isEnabled() {
    return this.enabled;
  }

  public boolean isBeehiveCrashEnabled() {
    return this.beehiveCrash;
  }

  public boolean isLogsExploitEnabled() {
    return this.logsExploit;
  }

  public int getMaxPayloadSize() {
    return this.maxPayloadSize;
  }

  /**
   * Checks if a channel is a known exploit channel.
   */
  public boolean isExploitChannel(String channel) {
    if (channel == null) return false;
    String lower = channel.toLowerCase(Locale.ROOT);
    return EXPLOIT_CHANNELS.contains(lower);
  }

  /**
   * Checks if a channel is known to send oversized payloads.
   */
  public boolean isOversizedPayloadChannel(String channel) {
    if (channel == null) return false;
    String lower = channel.toLowerCase(Locale.ROOT);
    return OVERSIZED_PAYLOAD_CHANNELS.contains(lower);
  }

  /**
   * Validates a plugin message payload against the configured size limit.
   */
  public boolean isPayloadOversized(String channel, int payloadSize) {
    int limit = isOversizedPayloadChannel(channel)
        ? maxPayloadSize
        : (maxPayloadSize * 4);
    return payloadSize > limit;
  }

  /**
   * Checks if a position packed via the standard Mojang encoding has
   * invalid bits set (used to detect out-of-bounds beehive crash exploit).
   */
  public boolean hasInvalidBlockBits(long packedPos) {
    int x = unpackLongX(packedPos);
    int y = unpackLongY(packedPos);
    int z = unpackLongZ(packedPos);
    // The exploit packs negative shifts which produce bit garbage above the valid range
    return x < -33554432 || x > 33554431
        || y < -2048 || y > 2047
        || z < -33554432 || z > 33554431;
  }

  public int unpackLongX(long packedPos) {
    return (int) (packedPos << 0 >> BIT_SHIFT_X);
  }

  public int unpackLongY(long packedPos) {
    return (int) (packedPos << 52 >> 52);
  }

  public int unpackLongZ(long packedPos) {
    return (int) (packedPos << 26 >> 38);
  }

  /**
   * Returns true if a custom payload (plugin message) should be rejected.
   */
  public boolean shouldRejectPayload(String channel, byte[] data) {
    if (!enabled) return false;
    if (isExploitChannel(channel)) return true;
    if (isPayloadOversized(channel, data == null ? 0 : data.length)) return true;
    return false;
  }
}