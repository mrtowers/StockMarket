package net.kroia.stockmarket.api.marketmanager;

import net.kroia.banksystem.util.ItemID;
import net.kroia.modutilities.networking.ExtraCodecUtils;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Lightweight ownership snapshot for one market, shipped to the management GUI
 * so it can decide which per-market action buttons to show. Part of issue 31.
 *
 * @param marketID  the market's ItemID
 * @param ownerUUID the creator, or {@code null} for server-owned markets
 * @param ownerName the creator's last known name, or an empty string for server-owned markets
 */
public record MarketOwnerInfo(@NotNull ItemID marketID,
                              @Nullable UUID ownerUUID,
                              @NotNull String ownerName) {

    public static final StreamCodec<RegistryFriendlyByteBuf, MarketOwnerInfo> STREAM_CODEC =
            StreamCodec.composite(
                    ItemID.STREAM_CODEC, MarketOwnerInfo::marketID,
                    ExtraCodecUtils.nullable(UUIDUtil.STREAM_CODEC), MarketOwnerInfo::ownerUUID,
                    ByteBufCodecs.STRING_UTF8, MarketOwnerInfo::ownerName,
                    MarketOwnerInfo::new);
}
