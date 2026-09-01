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

/**
 * Result of an item/packet check.
 */
public enum CheckItemResult {
  VALID_ITEM,
  LONG_ITEM_NAME,
  LONG_ITEM_LORE,
  LONG_BOOK_TITLE,
  LONG_BOOK_AUTHOR,
  TOO_MANY_BOOK_PAGES,
  LONG_BOOK_CONTENT,
  INVALID_FIREWORK_POWER,
  TOO_MANY_FIREWORK_EFFECTS,
  INVALID_FIREWORK_COLORS,
  INVALID_FIREWORK_FADE_COLORS,
  INVALID_ITEM,
  INVALID_SLOT,
  INVALID_BUTTON_ID,
  OUT_OF_BOUNDS_MOVE,
  TOO_MANY_FLAGS,
  TAB_COMPLETE_LIMIT_EXCEEDED,
  STRING_TOO_LONG,
  INVALID_SIGN_SIZE
}
