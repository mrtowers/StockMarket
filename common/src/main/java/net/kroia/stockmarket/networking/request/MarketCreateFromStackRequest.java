package net.kroia.stockmarket.networking.request;

import net.kroia.banksystem.util.ItemID;
import net.kroia.stockmarket.api.marketmanager.PlayerMarketPermissions;
import net.kroia.stockmarket.networking.NetworkGate;
import net.kroia.stockmarket.util.MultiServerUtils;
import net.kroia.stockmarket.util.StockMarketGenericRequest;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Creates a market from a concrete {@link ItemStack} (issue 31). This is the
 * server-authoritative path for arbitrary items chosen from the registry or
 * the player's inventory: the server resolves/registers the BankSystem
 * {@link ItemID} itself instead of trusting a client-side id.
 * <p>
 * Permission: the sender must hold the market CREATE permission (admins
 * bypass). Server-internal callers with a {@code null} player UUID are trusted.
 */
public class MarketCreateFromStackRequest extends StockMarketGenericRequest<ItemStack, Boolean> {

    @Override
    public String getRequestTypeID() {
        return MarketCreateFromStackRequest.class.getName();
    }

    @Override
    protected Boolean getDefaultResponse() {
        return false;
    }

    @Override
    public CompletableFuture<Boolean> handleOnMasterServer(ItemStack stack, String slaveID,
                                                           @Nullable UUID playerSender) {
        // T-123: mutating call — reject when forwarded by an untrusted slave.
        if (!NetworkGate.isMutatingCallAllowed(slaveID, "MarketCreateFromStackRequest"))
            return CompletableFuture.completedFuture(false);
        if (needsRoutingToMaster() && !MultiServerUtils.canInteractWithStockMarket(playerSender))
            return CompletableFuture.completedFuture(false);
        if (stack == null || stack.isEmpty())
            return CompletableFuture.completedFuture(false);

        if (playerSender != null) {
            PlayerMarketPermissions perms = getServerMarketManager().getPlayerMarketPermissions(playerSender);
            if (!perms.canCreate()) {
                warn("Rejected CreateMarketFromStack from player " + playerSender + " (no create permission)");
                return CompletableFuture.completedFuture(false);
            }
        }

        ItemID id = ItemID.getOrRegisterFromItemStackServerSide_direct(stack);
        if (id == null || !id.isValid())
            return CompletableFuture.completedFuture(false);
        return CompletableFuture.completedFuture(
                getServerMarketManager().createMarket(id, playerSender) != null);
    }

    @Override
    public void encodeInput(RegistryFriendlyByteBuf buf, ItemStack input) {
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, input);
    }

    @Override
    public void encodeOutput(RegistryFriendlyByteBuf buf, Boolean output) {
        ByteBufCodecs.BOOL.encode(buf, output);
    }

    @Override
    public ItemStack decodeInput(RegistryFriendlyByteBuf buf) {
        return ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
    }

    @Override
    public Boolean decodeOutput(RegistryFriendlyByteBuf buf) {
        return ByteBufCodecs.BOOL.decode(buf);
    }
}
