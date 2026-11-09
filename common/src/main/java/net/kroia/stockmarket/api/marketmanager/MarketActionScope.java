package net.kroia.stockmarket.api.marketmanager;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Scope of a player-granted market action (delete or manage).
 * <p>
 * {@link #OWN} allows the action only on markets the player created,
 * {@link #ANY} allows it on every market (including ownerless/server-created
 * markets). {@link #NONE} denies the action entirely.
 * <p>
 * StockMarket admins bypass these scopes and always hold {@link #ANY} rights.
 */
public enum MarketActionScope {
    NONE,
    OWN,
    ANY;

    /**
     * @param ownerUUID  the market's creator, or {@code null} for server-owned markets
     * @param playerUUID the player requesting the action
     * @return true when this scope permits the action on the given market
     */
    public boolean allows(@Nullable UUID ownerUUID, @Nullable UUID playerUUID) {
        if (this == ANY) return true;
        if (this == OWN && ownerUUID != null && playerUUID != null)
            return ownerUUID.equals(playerUUID);
        return false;
    }
}
