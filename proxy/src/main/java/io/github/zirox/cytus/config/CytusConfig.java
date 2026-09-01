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

package io.github.zirox.cytus.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import io.github.zirox.cytus.modules.InvalidPayloadModule;
import io.github.zirox.cytus.modules.InvalidRecipeIDModule;
import io.github.zirox.cytus.modules.InvalidSelectBundleModule;
import io.github.zirox.cytus.modules.PacketFilterModule;
import io.github.zirox.cytus.modules.PacketFunnelModule;
import io.github.zirox.cytus.modules.PacketLimiterModule;
import io.github.zirox.cytus.modules.ViolationsModule;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.apache.logging.log4j.Logger;

/**
 * CytusConfig loads and reloads all module configs from the data directory.
 */
public class CytusConfig {

  private final Logger logger;
  private final Path dataDirectory;

  private final PacketLimiterModule packetLimiter;
  private final PacketFunnelModule packetFunnel;
  private final PacketFilterModule packetFilter;
  private final InvalidPayloadModule invalidPayload;
  private final InvalidRecipeIDModule invalidRecipeID;
  private final InvalidSelectBundleModule invalidSelectBundle;

  public CytusConfig(
      Logger logger,
      Path dataDirectory,
      PacketLimiterModule packetLimiter,
      PacketFunnelModule packetFunnel,
      PacketFilterModule packetFilter,
      InvalidPayloadModule invalidPayload,
      InvalidRecipeIDModule invalidRecipeID,
      InvalidSelectBundleModule invalidSelectBundle
  ) {
    this.logger = logger;
    this.dataDirectory = dataDirectory;
    this.packetLimiter = packetLimiter;
    this.packetFunnel = packetFunnel;
    this.packetFilter = packetFilter;
    this.invalidPayload = invalidPayload;
    this.invalidRecipeID = invalidRecipeID;
    this.invalidSelectBundle = invalidSelectBundle;
  }

  /**
   * Loads/reloads all Cytus module configs.
   *
   * @return true if reload was successful, false otherwise
   */
  public boolean reloadAll() {
    int loaded = 0;
    int failed = 0;

    if (reload(packetLimiter, "packet-limiter")) {
      loaded++;
    } else {
      failed++;
    }
    if (reload(packetFunnel, "packet-funnel")) {
      loaded++;
    } else {
      failed++;
    }
    if (reload(packetFilter, "packet-filter")) {
      loaded++;
    } else {
      failed++;
    }
    if (reload(invalidPayload, "invalid-payload")) {
      loaded++;
    } else {
      failed++;
    }
    if (reload(invalidRecipeID, "invalid-recipe-id")) {
      loaded++;
    } else {
      failed++;
    }
    if (reload(invalidSelectBundle, "invalid-select-bundle")) {
      loaded++;
    } else {
      failed++;
    }

    logger.info("Cytus config reloaded: " + loaded + " modules loaded, " + failed + " failed");
    return failed == 0;
  }

  /**
   * Reloads a single module config from a TOML file.
   */
  private boolean reload(ViolationsModule module, String configName) {
    try {
      Path configPath = dataDirectory.resolve(configName + ".toml");

      // Create default config from jar resources if it doesn't exist
      if (!Files.exists(configPath)) {
        Files.createDirectories(dataDirectory);
        InputStream defaultStream = CytusConfig.class.getClassLoader()
            .getResourceAsStream(configName + ".toml");
        if (defaultStream != null) {
          Files.copy(defaultStream, configPath, StandardCopyOption.REPLACE_EXISTING);
          logger.info("Created default config: " + configPath);
        }
      }

      try (CommentedFileConfig config = CommentedFileConfig.builder(configPath).build()) {
        config.load();
        module.reload(config);
        logger.debug("Reloaded config: " + configName);
        return true;
      }
    } catch (Exception e) {
      logger.error("Failed to reload config: " + configName, e);
      return false;
    }
  }

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
}
