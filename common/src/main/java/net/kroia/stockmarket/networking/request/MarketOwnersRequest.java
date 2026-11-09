package net.kroia.stockmarket.networking.request;

import net.kroia.banksystem.util.ItemID;
import net.kroia.modutilities.networking.ExtraCodecUtils;
import net.kroia.stockmarket.api.market.IServerMarket;
import net.kroia.stockmarket.api.marketmanager.MarketOwnerInfo;
import net.kroia.stockmarket.api.marketmanager.PlayerMarketPermissions;
import net.kroia.stockmarket.networking.NetworkGate;
import net.kroia.stockmarket.util.MultiServerUtils;
import net.kroia.stockmarket.util.StockMarketGenericRequest;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Returns ownership information for every market (issue 31). Used by the
 * management GUI to show "Owner: ..." and to decide whether the current player
 * may delete/manage a selected market.
 * <p>
 * Only players with some market-management permission (or admins) receive the
 * list; everyone else gets an empty response. The client still re-checks the
 * server on every mutation — this request is UI metadata, not a permission
 * grant.
 */
public class MarketOwnersRequest extends StockMarketGenericRequest<Integer, List<MarketOwnerInfo>> {

    @Override
    public String getRequestTypeID() {
        return MarketOwnersRequest.class.getName();
    }

    @Override
    protected List<MarketOwnerInfo> getDefaultResponse() {
        return List.of();
    }

    @Override
    public CompletableFuture<List<MarketOwnerInfo>> handleOnMasterServer(Integer input, String slaveID,
                                                                         @Nullable UUID playerSender) {
        // T-129: management-data read — do not expose ownership to an untrusted slave.
        if (!NetworkGate.isManagementReadAllowed(slaveID, "MarketOwnersRequest"))
            return CompletableFuture.completedFuture(List.of());
        if (needsRoutingToMaster() && !MultiServerUtils.canInteractWithStockMarket(playerSender))
            return CompletableFuture.completedFuture(List.of());

        // Server-internal callers are trusted; client callers must have at least
        // one management permission to enumerate ownership.
        PlayerMarketPermissions perms = playerSender == null
                ? PlayerMarketPermissions.adminPermissions()
                : getServerMarketManager().getPlayerMarketPermissions(playerSender);
        if (!perms.canOpenManagement())
            return CompletableFuture.completedFuture(List.of());

        List<MarketOwnerInfo> result = new ArrayList<>();
        for (ItemID marketID : getServerMarketManager().getAvailableMarketIDs()) {
            IServerMarket market = getServerMarketManager().getMarket(marketID);
            if (market == null) continue;
            UUID ownerUUID = market.getOwnerUUID();
            String ownerName = ownerUUID == null ? "" : getServerMarketManager().getPlayerName(ownerUUID);
            result.add(new MarketOwnerInfo(marketID, ownerUUID, ownerName == null ? "" : ownerName));
        }
        return CompletableFuture.completedFuture(result);
    }

    @Override
    public void encodeInput(RegistryFriendlyByteBuf buf, Integer input) {
        ByteBufCodecs.INT.encode(buf, input);
    }

    @Override
    public void encodeOutput(RegistryFriendlyByteBuf buf, List<MarketOwnerInfo> output) {
        ExtraCodecUtils.listStreamCodec(MarketOwnerInfo.STREAM_CODEC).encode(buf, output);
    }

    @Override
    public Integer decodeInput(RegistryFriendlyByteBuf buf) {
        return ByteBufCodecs.INT.decode(buf);
    }

    @Override
    public List<MarketOwnerInfo> decodeOutput(RegistryFriendlyByteBuf buf) {
        return ExtraCodecUtils.listStreamCodec(MarketOwnerInfo.STREAM_CODEC).decode(buf);
    }
}
