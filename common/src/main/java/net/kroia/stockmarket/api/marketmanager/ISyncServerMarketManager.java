package net.kroia.stockmarket.api.marketmanager;

import net.kroia.banksystem.util.ItemID;
import net.kroia.stockmarket.api.integration.MarketConfig;
import net.kroia.stockmarket.api.market.IServerMarket;
import net.kroia.stockmarket.data.table.record.MarketPriceStruct;
import net.kroia.stockmarket.stockmarket.marketmanager.PlayerPreferences;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public interface ISyncServerMarketManager {

    ItemID getTradingCurrencyID();
    List<ItemID> getAvailableMarketIDs();
    boolean marketExists(@NotNull ItemID marketID);

    @Nullable IServerMarket createMarket(@NotNull ItemID marketID);

    /**
     * Owner-aware counterpart to {@link #createMarket(ItemID)}. The default
     * implementation delegates to the owner-less method so third-party
     * implementations stay source-compatible; {@code ServerMarketManager}
     * overrides it and records the creator.
     *
     * @param marketID the ItemID to create a market for (never null)
     * @param owner    the creating player, or {@code null} for server-owned markets
     * @return the market, or {@code null} when creation was rejected
     */
    default @Nullable IServerMarket createMarket(@NotNull ItemID marketID, @Nullable UUID owner) {
        return createMarket(marketID);
    }

    /**
     * Creates a market from an explicit {@link MarketConfig}, bypassing the
     * preset system. Mirrors {@link #createMarket(ItemID)} but sources initial
     * price, natural abundance, and market flags from the passed config
     * instead of any matching preset. Blacklist check, plugin auto-subscribe
     * (unless the config opts out), and BankSystem {@code allowItemID} run the
     * same way.
     * <p>
     * Returns {@code null} when a market for {@code marketID} already exists;
     * callers must distinguish this from other failures with a
     * {@link #marketExists(ItemID)} pre-check if the distinction matters.
     * Server-thread only.
     *
     * @param marketID the ItemID to create a market for (never null)
     * @param cfg      the configuration to apply to the new market (never null)
     * @return the newly created market, or {@code null} if one already existed
     *         or creation was rejected (e.g. blacklisted item)
     */
    @Nullable IServerMarket createMarketWithConfig(@NotNull ItemID marketID, @NotNull MarketConfig cfg);

    /**
     * Owner-aware counterpart to {@link #createMarketWithConfig(ItemID, MarketConfig)}.
     * The default delegates to the owner-less method; {@code ServerMarketManager}
     * records the creator on the new market.
     *
     * @param marketID the ItemID to create a market for (never null)
     * @param cfg      the configuration to apply (never null)
     * @param owner    the creating player, or {@code null} for server-owned markets
     * @return the newly created market, or {@code null} if one already existed/rejected
     */
    default @Nullable IServerMarket createMarketWithConfig(@NotNull ItemID marketID, @NotNull MarketConfig cfg,
                                                           @Nullable UUID owner) {
        return createMarketWithConfig(marketID, cfg);
    }

    boolean deleteMarket(@NotNull ItemID marketID);
    @Nullable IServerMarket getMarket(@NotNull ItemID marketID);

    @Nullable UUID getPlayerUUID(String playerName);

    /**
     * @param playerUUID the player to resolve
     * @return the last known player name, or {@code null} when the UUID is unknown
     */
    default @Nullable String getPlayerName(UUID playerUUID) {
        return null;
    }

    boolean setStockmarketAdminMode(UUID playerUUID, boolean isAdmin);
    boolean isStockmarketAdmin(UUID playerUUID);

    /**
     * Returns the player's per-market permissions. Unknown players and
     * server-side callers without a user record get {@link PlayerMarketPermissions#NONE}.
     * StockMarket admins get {@link PlayerMarketPermissions#adminPermissions()}.
     *
     * @param playerUUID the player to query
     * @return the effective permissions, never null
     */
    default @NotNull PlayerMarketPermissions getPlayerMarketPermissions(UUID playerUUID) {
        return PlayerMarketPermissions.NONE;
    }

    /**
     * Grants or revokes the player's market-creation permission.
     *
     * @return true when the player record exists and was updated
     */
    default boolean setCanCreateMarkets(UUID playerUUID, boolean canCreate) {
        return false;
    }

    /**
     * Sets the player's market-delete scope.
     *
     * @return true when the player record exists and was updated
     */
    default boolean setDeleteScope(UUID playerUUID, MarketActionScope scope) {
        return false;
    }

    /**
     * Sets the player's market-manage scope.
     *
     * @return true when the player record exists and was updated
     */
    default boolean setManageScope(UUID playerUUID, MarketActionScope scope) {
        return false;
    }

    /**
     * Gets the player's trading preferences (favorites, last market, etc.)
     * @param playerUUID the player to get preferences for
     * @return the preferences, or a new empty preferences if the player is unknown
     */
    @NotNull PlayerPreferences getPlayerPreferences(UUID playerUUID);

    /**
     * Updates the player's trading preferences.
     * @param playerUUID the player to update
     * @param preferences the new preferences
     * @return true if the player exists and preferences were saved
     */
    boolean updatePlayerPreferences(UUID playerUUID, PlayerPreferences preferences);

    /**
     * Call this function when a player joins the server to setup its bank account
     * @param playerUUID
     * @param playerName
     */
    void onPlayerJoin(UUID playerUUID, String playerName);

    List<MarketPriceStruct>  getCurrentMarketPricesAndStartNewCandle();
    void update();
}
