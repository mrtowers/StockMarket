package net.kroia.stockmarket.networking.packet;

import dev.architectury.networking.NetworkManager;
import net.kroia.modutilities.networking.ExtraCodecUtils;
import net.kroia.stockmarket.StockMarketMod;
import net.kroia.stockmarket.api.marketmanager.PlayerMarketPermissions;
import net.kroia.stockmarket.util.StockMarketClientHooks;
import net.kroia.stockmarket.util.StockMarketNetworkPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation; // mc<=1.21.10
//import net.minecraft.resources.Identifier; // mc>=1.21.11
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public class OpenUIPacket extends StockMarketNetworkPacket {

    public static final Type<OpenUIPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(StockMarketMod.MOD_ID, "open_ui_packet")); // mc<=1.21.10
    //public static final Type<OpenUIPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(StockMarketMod.MOD_ID, "open_ui_packet")); // mc>=1.21.11
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenUIPacket> STREAM_CODEC = StreamCodec.composite(
            ExtraCodecUtils.enumStreamCodec(OpenUIPacket.GUIType.class), p -> p.guiType,
            PlayerMarketPermissions.STREAM_CODEC, p -> p.permissions,
            OpenUIPacket::new
    );
    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum GUIType
    {
        DEVELOPMENT,
        MANAGEMENT,
        EXPORT_RECIPES,
        /** Newspaper screen — opened by the player-level {@code /stockmarket news} command (T-076). */
        NEWS,
        /** Player-facing market management screen (issue 31) — restricted tabs/actions. */
        PLAYER_MANAGEMENT,
    }


    private final GUIType  guiType;
    /**
     * Permissions shipped to the player-facing management screen. Unused by
     * the other GUI types (kept as {@link PlayerMarketPermissions#NONE}).
     */
    private final PlayerMarketPermissions permissions;

    public OpenUIPacket(GUIType guiType)
    {
        this(guiType, PlayerMarketPermissions.NONE);
    }

    public OpenUIPacket(GUIType guiType, PlayerMarketPermissions permissions)
    {
        this.guiType = guiType;
        this.permissions = permissions == null ? PlayerMarketPermissions.NONE : permissions;
    }


    public static void sendToClient(ServerPlayer player, GUIType guiType)
    {
        OpenUIPacket packet = new OpenUIPacket(guiType);
        packet.sendToClient(player);
    }

    public static void sendToClient(ServerPlayer player, GUIType guiType, PlayerMarketPermissions permissions)
    {
        OpenUIPacket packet = new OpenUIPacket(guiType, permissions);
        packet.sendToClient(player);
    }





    @Override
    protected void handleOnClient(NetworkManager.PacketContext context)
    {
        StockMarketClientHooks.openGUI(guiType, permissions);
    }
}
