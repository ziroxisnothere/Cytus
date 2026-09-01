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

package io.github.zirox.velocity.modules;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.player.Player;
import com.velocitypowered.proxy.VelocityServer;
import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * PacketLimiterModule filters packets based on configurable size and content limits,
 * similar to FireGuard's packet filtering approach.
 */
public class PacketLimiterModule extends ViolationsModule {

  private final VelocityServer server;
  private double vls;
  private long maxSizeBytes;
  private int displayNameLimit;
  private int loreLimit;
  private int bookTitleLimit;
  private int bookAuthorLimit;
  private int bookPagesLimit;
  private int bookContentLimit;
  private int maxSignSize;
  private int tabCompleteLimit;
  private int fireworkEffectLimit;
  private int fireworkPowerLimit;
  private boolean maxFlagsEnabled;
  private Map<String, Integer> maxFlagsLimits = new HashMap<>();
  private int maxPPS;
  private long maxBPS;
  private int maxNbtStringLength;
  private int maxSlotId;
  private int maxButtonId;
  private int pickItemSlotMin;
  private int pickItemSlotMax;
  private double moveMaxCoordinate;
  private boolean moveRejectInfinite;

  @Override
  public String getName() {
    return "PacketLimiter";
  }

  public PacketLimiterModule(Logger logger, VelocityServer server) {
    super(logger);
    this.server = server;
  }

  @Override
  public void reload(@Nullable CommentedConfig configYml) {
    if (configYml == null) {
      return;
    }

    this.enabled = configYml.getBoolean("enabled", true);
    this.vls = configYml.getDouble("vls", 50.0);

    String maxSize = configYml.getString("max_size", "32kb");
    this.maxSizeBytes = parseSizeToBytes(maxSize);

    this.tabCompleteLimit = configYml.getInt("tab_complete_limit", 128);
    this.fireworkEffectLimit = configYml.getInt("firework_effect_limit", 8);
    this.fireworkPowerLimit = configYml.getInt("firework_power_limit", 3);

    CommentedConfig itemLimits = configYml.get("item_limits");
    if (itemLimits != null) {
      this.displayNameLimit = itemLimits.getInt("displayname", 256);
      this.loreLimit = itemLimits.getInt("lore", 128);
    }

    CommentedConfig bookLimits = configYml.get("book_limits");
    if (bookLimits != null) {
      this.bookTitleLimit = bookLimits.getInt("title", 64);
      this.bookAuthorLimit = bookLimits.getInt("author", 64);
      this.bookPagesLimit = bookLimits.getInt("pages", 50);
      this.bookContentLimit = bookLimits.getInt("content", 512);
    }

    this.maxSignSize = configYml.getInt("max_size_sign", 128);

    CommentedConfig maxFlags = configYml.get("max_flags");
    this.maxFlagsLimits.clear();
    if (maxFlags != null) {
      this.maxFlagsEnabled = maxFlags.getBoolean("enabled", true);

      for (String key : maxFlags.entrySet().keySet()) {
        int limit = maxFlags.getInt(key);
        if (!key.equals("enabled")) {
          this.addFlagLimit(key, limit);
        }
      }
    }

    this.maxPPS = configYml.getInt("max_pps", 2048);
    String maxBPStr = configYml.getString("max_bps", "32kb");
    this.maxBPS = parseSizeToBytes(maxBPStr);

    this.maxNbtStringLength = configYml.getInt("max_nbt_string_length", 128);
    this.maxSlotId = configYml.getInt("max_slot_id", 54);
    this.maxButtonId = configYml.getInt("max_button_id", 8);
    this.pickItemSlotMin = configYml.getInt("pick_item_slot_min", 0);
    this.pickItemSlotMax = configYml.getInt("pick_item_slot_max", 44);

    CommentedConfig movePacket = configYml.get("move_packet");
    if (movePacket != null) {
      this.moveMaxCoordinate = movePacket.getDouble("max_coordinate", 30000000.0);
      this.moveRejectInfinite = movePacket.getBoolean("reject_infinite", true);
    } else {
      this.moveMaxCoordinate = 30000000.0;
      this.moveRejectInfinite = true;
    }
  }

  /**
   * Parses a size string like "32kb" or "1mb" to bytes.
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
      return 32768; // Default 32kb
    }
  }

  private void addFlagLimit(String key, int limit) {
    this.maxFlagsLimits.put(key.toLowerCase(), limit);
  }

  @Override
  public boolean isEnabled() {
    return this.enabled;
  }

  public double getVls() {
    return this.vls;
  }

  public long getMaxSizeBytes() {
    return this.maxSizeBytes;
  }

  public int getDisplayNameLimit() {
    return this.displayNameLimit;
  }

  public int getLoreLimit() {
    return this.loreLimit;
  }

  public int getBookTitleLimit() {
    return this.bookTitleLimit;
  }

  public int getBookAuthorLimit() {
    return this.bookAuthorLimit;
  }

  public int getBookPagesLimit() {
    return this.bookPagesLimit;
  }

  public int getBookContentLimit() {
    return this.bookContentLimit;
  }

  public int getMaxSignSize() {
    return this.maxSignSize;
  }

  public boolean isMaxFlagsEnabled() {
    return this.maxFlagsEnabled;
  }

  public int getTabCompleteLimit() {
    return this.tabCompleteLimit;
  }

  public int getFireworkEffectLimit() {
    return this.fireworkEffectLimit;
  }

  public Map<String, Integer> getMaxFlagsLimits() {
    return this.maxFlagsLimits;
  }

  public int getMaxFlagLimit(String flagType, int def) {
    return this.maxFlagsLimits.getOrDefault(flagType.toLowerCase(), def);
  }

  public int getMaxFlagLimit(String flagType) {
    return this.getMaxFlagLimit(flagType, this.getMaxFlagLimit("default", 32));
  }

  public int getMaxPPS() {
    return this.maxPPS;
  }

  public long getMaxBPS() {
    return this.maxBPS;
  }

  public int getMaxNbtStringLength() {
    return this.maxNbtStringLength;
  }

  public int getMaxSlotId() {
    return this.maxSlotId;
  }

  public int getMaxButtonId() {
    return this.maxButtonId;
  }

  public int getPickItemSlotMin() {
    return this.pickItemSlotMin;
  }

  public int getPickItemSlotMax() {
    return this.pickItemSlotMax;
  }

  public double getMoveMaxCoordinate() {
    return this.moveMaxCoordinate;
  }

  public boolean isMoveRejectInfinite() {
    return this.moveRejectInfinite;
  }

  /**
   * Checks if a packet's total byte size exceeds the limit.
   */
  public boolean checkPacketSize(ByteBuf buf) {
    return buf.readableBytes() > this.maxSizeBytes;
  }

  /**
   * Checks if a string's byte length exceeds the NBT string limit.
   */
  public CheckItemResult checkNbtString(String str) {
    if (str == null) {
      return CheckItemResult.VALID_ITEM;
    }
    int byteLength = str.getBytes(StandardCharsets.UTF_8).length;
    if (byteLength > this.maxNbtStringLength) {
      return CheckItemResult.STRING_TOO_LONG;
    }
    return CheckItemResult.VALID_ITEM;
  }

  /**
   * Checks a sign's text content.
   */
  public CheckItemResult checkSign(Component[] lines) {
    for (Component line : lines) {
      if (line == null) {
        continue;
      }
      String text = Component.textAndArgsToString(line);
      if (text.length() > this.maxSignSize) {
        return CheckItemResult.INVALID_SIGN_SIZE;
      }
    }
    return CheckItemResult.VALID_ITEM;
  }

  /**
   * Checks if a slot ID is within valid bounds.
   */
  public CheckItemResult checkSlotId(int slotId) {
    if (slotId < 0 || slotId > this.maxSlotId) {
      return CheckItemResult.INVALID_SLOT;
    }
    return CheckItemResult.VALID_ITEM;
  }

  /**
   * Checks if a button ID is within valid bounds.
   */
  public CheckItemResult checkButtonId(int buttonId) {
    if (buttonId < 0 || buttonId > this.maxButtonId) {
      return CheckItemResult.INVALID_BUTTON_ID;
    }
    return CheckItemResult.VALID_ITEM;
  }

  /**
   * Checks if a pick item slot is within valid bounds.
   */
  public CheckItemResult checkPickItemSlot(int slot) {
    if (slot < this.pickItemSlotMin || slot > this.pickItemSlotMax) {
      return CheckItemResult.INVALID_SLOT;
    }
    return CheckItemResult.VALID_ITEM;
  }

  /**
   * Checks a player's position for out-of-bounds movement.
   */
  public CheckItemResult checkMovePosition(double x, double y, double z) {
    if (this.moveRejectInfinite) {
      if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
        return CheckItemResult.OUT_OF_BOUNDS_MOVE;
      }
    }

    if (Math.abs(x) > this.moveMaxCoordinate
        || Math.abs(y) > this.moveMaxCoordinate
        || Math.abs(z) > this.moveMaxCoordinate) {
      return CheckItemResult.OUT_OF_BOUNDS_MOVE;
    }

    return CheckItemResult.VALID_ITEM;
  }

  /**
   * Checks the number of flags in a packet.
   */
  public CheckItemResult checkFlagCount(String flagType, int count) {
    if (!this.maxFlagsEnabled) {
      return CheckItemResult.VALID_ITEM;
    }
    int limit = this.getMaxFlagLimit(flagType, 32);
    if (count > limit) {
      return CheckItemResult.TOO_MANY_FLAGS;
    }
    return CheckItemResult.VALID_ITEM;
  }

  /**
   * Checks if tab complete request is within limits.
   */
  public CheckItemResult checkTabComplete(String[] args) {
    if (args != null && args.length > this.tabCompleteLimit) {
      return CheckItemResult.TAB_COMPLETE_LIMIT_EXCEEDED;
    }
    return CheckItemResult.VALID_ITEM;
  }

  /**
   * Main packet filter method - call this from your packet handler.
   *
   * @param packetType the type of packet being checked
   * @param buf        the packet buffer
   * @param player     the player who sent the packet (can be null)
   * @return the check result
   */
  public CheckItemResult filterPacket(String packetType, ByteBuf buf, @Nullable Player player) {
    // Check overall packet size
    if (checkPacketSize(buf)) {
      return CheckItemResult.INVALID_ITEM;
    }

    // Packet-type specific checks can be added here
    // The actual implementation would depend on the packet type

    return CheckItemResult.VALID_ITEM;
  }
}
