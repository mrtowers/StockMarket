# Player-Created Markets

Since 2.2.0, server admins can let trusted players create and manage their own markets without granting them full StockMarket admin rights. This page covers the permission model and the player-facing workflow.

## Permission model

Permissions are stored per player on the **master server** and are configured by an admin with op level 2 (or an existing StockMarket admin):

```text
/stockmarket permissions <player> list
/stockmarket permissions <player> create <true|false>
/stockmarket permissions <player> delete <none|own|any>
/stockmarket permissions <player> manage <none|own|any>
```

| Permission | Values | Meaning |
|---|---|---|
| Create | `true` / `false` | The player may create markets for items. |
| Delete | `none` / `own` / `any` | Which markets the player may delete permanently. `own` only covers markets they created. |
| Manage | `none` / `own` / `any` | Which markets the player may configure: price/abundance, maintenance mode, trading hours, virtual orderbook reset/clear and net-flow reset. |

**Any** includes the player's own markets and server-owned/legacy markets. StockMarket admins always have full access and do not need these flags.

Plugin management, preset editing, mod settings, news administration and the global "all markets" actions stay admin-only regardless of these scopes.

## How a player uses it

A player with any of the three permissions runs:

```text
/stockmarket markets
```

This opens a restricted management screen:

* **Create Markets** — browse preset categories, or use **Add Item...** / **Add from Inventory** to select arbitrary items. Selected items are created with **Create Selected**. Items with a matching preset use the preset's start price and abundance; other items use the default fallback values. Players cannot edit price/abundance at creation time.
* **Overview** — shown when the player has a delete or manage scope. It lists all markets and shows the owner. **Delete**, **Market Settings** and **Maintenance mode** only appear when the player's scope covers the selected market. A player with `own` scope will not see action buttons on other players' markets.

Selecting two items (for example two hotbar items via **Add from Inventory**) creates a market for each of them. Item-to-item trading between them then works automatically through the normal **Pair Mode** — no separate pair object is needed.

## What a player-created market gets

* The creator's UUID is stored as the market owner and survives server restarts.
* Initial price/abundance come from the matching preset, or from the default fallback when no preset exists.
* Normal plugin auto-subscription applies, exactly like a market created from the admin GUI.
* The item is allowed in the banking system (the same `allowItemID` call as an admin-created market). Blacklisted items are still rejected.

## Ownership and legacy markets

Markets created before this feature, or by commands / the integration API, are **server-owned** (no owner). Only admins and players with `delete = any` / `manage = any` can act on them.

If a player changes their name, the owner UUID stays the same and the GUI resolves the current name from the server's user records.
