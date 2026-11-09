package net.kroia.stockmarket.api.marketmanager;

import net.kroia.modutilities.networking.ExtraCodecUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Per-player market management permissions, persisted on the master server in
 * {@code User} NBT and sent to the client when the player market screen opens.
 * <p>
 * StockMarket admins implicitly hold every permission: {@link #adminPermissions()} returns
 * the full set, and the {@code can*} methods short-circuit on {@link #admin()}.
 * <p>
 * The issue-31 permission matrix maps directly:
 * <ul>
 *   <li><b>Creating markets</b> — {@link #create()}</li>
 *   <li><b>Deleting markets</b> — {@code deleteScope == ANY}</li>
 *   <li><b>Deleting only self created markets</b> — {@code deleteScope == OWN}</li>
 *   <li><b>Managing markets</b> — {@code manageScope == ANY}</li>
 *   <li><b>Managing only self created markets</b> — {@code manageScope == OWN}</li>
 * </ul>
 *
 * @param admin       true for StockMarket admins (implicit full access)
 * @param create      true when the player may create markets
 * @param deleteScope delete scope (NONE/OWN/ANY)
 * @param manageScope manage scope (NONE/OWN/ANY)
 */
public record PlayerMarketPermissions(
        boolean admin,
        boolean create,
        MarketActionScope deleteScope,
        MarketActionScope manageScope) {

    /** Hand-rolled codec is unnecessary here: four fields fit StreamCodec.composite. */
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerMarketPermissions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, PlayerMarketPermissions::admin,
                    ByteBufCodecs.BOOL, PlayerMarketPermissions::create,
                    ExtraCodecUtils.enumStreamCodec(MarketActionScope.class), PlayerMarketPermissions::deleteScope,
                    ExtraCodecUtils.enumStreamCodec(MarketActionScope.class), PlayerMarketPermissions::manageScope,
                    PlayerMarketPermissions::new);

    /** No permissions at all (new players, unknown UUIDs, offline lookups). */
    public static final PlayerMarketPermissions NONE =
            new PlayerMarketPermissions(false, false, MarketActionScope.NONE, MarketActionScope.NONE);

    /** Full admin permissions. */
    public static PlayerMarketPermissions adminPermissions() {
        return new PlayerMarketPermissions(true, true, MarketActionScope.ANY, MarketActionScope.ANY);
    }

    /** @return true when this player may create markets */
    public boolean canCreate() {
        return admin || create;
    }

    /**
     * @param ownerUUID  the market's creator, or {@code null} for server-owned markets
     * @param playerUUID the player requesting the action
     * @return true when the player may delete the market
     */
    public boolean canDelete(@Nullable UUID ownerUUID, @Nullable UUID playerUUID) {
        return admin || deleteScope.allows(ownerUUID, playerUUID);
    }

    /**
     * @param ownerUUID  the market's creator, or {@code null} for server-owned markets
     * @param playerUUID the player requesting the action
     * @return true when the player may change the market's settings/maintenance/hours
     */
    public boolean canManage(@Nullable UUID ownerUUID, @Nullable UUID playerUUID) {
        return admin || manageScope.allows(ownerUUID, playerUUID);
    }

    /** @return true when the player has any reason to open the player market screen */
    public boolean canOpenManagement() {
        return admin || create || deleteScope != MarketActionScope.NONE || manageScope != MarketActionScope.NONE;
    }
}
