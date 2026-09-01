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

package io.github.zirox.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import java.util.List;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * /cytus command - displays Cytus version info.
 */
public class CytusCommand implements SimpleCommand {

  private static final String VERSION = "V1";
  private static final String AUTHOR = "Zirox";

  private final Logger logger;

  public CytusCommand(Logger logger) {
    this.logger = logger;
  }

  @Override
  public void execute(Invocation invocation) {
    CommandSource source = invocation.source();
    showCytusInfo(source);
  }

  @Override
  public boolean hasPermission(Invocation invocation) {
    return invocation.source().hasPermission("cytus.command");
  }

  @Override
  public List<String> suggest(Invocation invocation) {
    return List.of();
  }

  /**
   * Displays the Cytus plugin info to the command source.
   */
  public static void showCytusInfo(CommandSource source) {
    source.sendMessage(Component.empty()
        .append(Component.text("Cytus ", NamedTextColor.AQUA, TextDecoration.BOLD))
        .append(Component.text(VERSION, NamedTextColor.WHITE))
        .append(Component.text(" By ", NamedTextColor.GRAY))
        .append(Component.text(AUTHOR, NamedTextColor.GOLD))
    );
  }
}