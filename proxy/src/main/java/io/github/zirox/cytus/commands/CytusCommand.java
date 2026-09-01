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

package io.github.zirox.cytus.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import io.github.zirox.cytus.config.CytusConfig;
import java.util.List;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * /cytus command - displays Cytus info and handles subcommands.
 */
public class CytusCommand implements SimpleCommand {

  private static final String VERSION = "V1";
  private static final String AUTHOR = "Zirox";

  private final Logger logger;
  private CytusConfig cytusConfig;

  public CytusCommand(Logger logger) {
    this.logger = logger;
  }

  public void setCytusConfig(CytusConfig cytusConfig) {
    this.cytusConfig = cytusConfig;
  }

  @Override
  public void execute(Invocation invocation) {
    String[] args = invocation.args();
    CommandSource source = invocation.source();

    if (args.length == 0) {
      showCytusInfo(source);
      return;
    }

    String subCommand = args[0].toLowerCase();

    switch (subCommand) {
      case "reload" -> handleReload(source);
      case "info" -> showCytusInfo(source);
      default -> source.sendMessage(Component.text("Usage: /cytus [reload|info]", NamedTextColor.RED));
    }
  }

  private void handleReload(CommandSource source) {
    if (!source.hasPermission("cytus.command.reload")) {
      source.sendMessage(Component.text("You don't have permission to reload Cytus config.", NamedTextColor.RED));
      return;
    }

    if (cytusConfig == null) {
      source.sendMessage(Component.text("Cytus config not initialized.", NamedTextColor.RED));
      return;
    }

    boolean success = cytusConfig.reloadAll();
    if (success) {
      source.sendMessage(Component.text("Cytus config reloaded successfully!", NamedTextColor.GREEN));
      logger.info("Cytus config reloaded by " + source);
    } else {
      source.sendMessage(Component.text("Failed to reload some Cytus configs. Check console for details.", NamedTextColor.YELLOW));
    }
  }

  @Override
  public boolean hasPermission(Invocation invocation) {
    String[] args = invocation.args();
    if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
      return invocation.source().hasPermission("cytus.command.reload");
    }
    return invocation.source().hasPermission("cytus.command");
  }

  @Override
  public List<String> suggest(Invocation invocation) {
    String[] args = invocation.args();
    if (args.length == 1) {
      return List.of("reload", "info");
    }
    return List.of();
  }

  /**
   * Displays the Cytus plugin info to the command source.
   */
  public static void showCytusInfo(CommandSource source) {
    source.sendMessage(Component.empty()
        .append(Component.text("╔══════════════════════════════════════╗", NamedTextColor.DARK_GRAY))
    );
    source.sendMessage(Component.empty()
        .append(Component.text(" Cytus ", NamedTextColor.AQUA, TextDecoration.BOLD))
        .append(Component.text(VERSION, NamedTextColor.WHITE))
        .append(Component.text(" By ", NamedTextColor.GRAY))
        .append(Component.text(AUTHOR, NamedTextColor.GOLD))
    );
    source.sendMessage(Component.text(" Comprehensive packet protection for Velocity", NamedTextColor.GRAY));
    source.sendMessage(Component.empty()
        .append(Component.text("╚══════════════════════════════════════╝", NamedTextColor.DARK_GRAY))
    );
  }
}
