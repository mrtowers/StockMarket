package net.kroia.stockmarket.stockmarket.marketmanager;

import net.kroia.modutilities.NbtCompat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kroia.modutilities.JsonUtilities;
import net.kroia.modutilities.persistence.ServerSaveable;
import net.kroia.stockmarket.StockMarketMod;
import net.kroia.stockmarket.api.marketmanager.MarketActionScope;
import net.kroia.stockmarket.api.marketmanager.PlayerMarketPermissions;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class User implements ServerSaveable {


    public static final StreamCodec<RegistryFriendlyByteBuf, User> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(RegistryFriendlyByteBuf buf, User user) {
            UUIDUtil.STREAM_CODEC.encode(buf, user.userUUID);
            ByteBufCodecs.STRING_UTF8.encode(buf, user.userName);
            ByteBufCodecs.BOOL.encode(buf, user.isStockMarketAdmin);
            PlayerPreferences.STREAM_CODEC.encode(buf, user.preferences);
            ByteBufCodecs.BOOL.encode(buf, user.canCreateMarkets);
            ByteBufCodecs.VAR_INT.encode(buf, user.deleteScope.ordinal());
            ByteBufCodecs.VAR_INT.encode(buf, user.manageScope.ordinal());
        }

        @Override
        public User decode(RegistryFriendlyByteBuf buf) {
            UUID uuid = UUIDUtil.STREAM_CODEC.decode(buf);
            String name = ByteBufCodecs.STRING_UTF8.decode(buf);
            boolean admin = ByteBufCodecs.BOOL.decode(buf);
            PlayerPreferences prefs = PlayerPreferences.STREAM_CODEC.decode(buf);
            User user = new User(uuid, name, admin, prefs);
            user.canCreateMarkets = ByteBufCodecs.BOOL.decode(buf);
            user.deleteScope = scopeFromOrdinal(ByteBufCodecs.VAR_INT.decode(buf));
            user.manageScope = scopeFromOrdinal(ByteBufCodecs.VAR_INT.decode(buf));
            return user;
        }
    };

    private UUID userUUID;
    private String userName;
    private boolean isStockMarketAdmin = false;
    private PlayerPreferences preferences = new PlayerPreferences();
    private boolean canCreateMarkets = false;
    private MarketActionScope deleteScope = MarketActionScope.NONE;
    private MarketActionScope manageScope = MarketActionScope.NONE;

    private User()
    {

    }
    private User(UUID userUUID, String userName, boolean isStockMarketAdmin, PlayerPreferences preferences)
    {
        this.userUUID = userUUID;
        this.userName = userName;
        this.isStockMarketAdmin = isStockMarketAdmin;
        this.preferences = preferences;
    }
    private User(UUID userUUID, String userName, boolean isStockMarketAdmin)
    {
        this(userUUID, userName, isStockMarketAdmin, new PlayerPreferences());
    }
    public User(UUID userUUID, String userName) {
        this.userUUID = userUUID;
        this.userName = userName;
    }

    /**
     * Ordinal lookup with a defensive fallback: an out-of-range value (corrupt
     * or future data) degrades to {@link MarketActionScope#NONE} instead of
     * throwing while a player joins.
     */
    private static MarketActionScope scopeFromOrdinal(int ordinal) {
        MarketActionScope[] values = MarketActionScope.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : MarketActionScope.NONE;
    }

    /**
     * Parses a persisted scope name, falling back to {@code fallback} for
     * missing/unknown values so old or hand-edited saves never crash load.
     */
    public static MarketActionScope parseScope(@Nullable String value, MarketActionScope fallback) {
        if (value == null) return fallback;
        for (MarketActionScope scope : MarketActionScope.values()) {
            if (scope.name().equalsIgnoreCase(value)) return scope;
        }
        return fallback;
    }

    /**
     * Creates a copy of the user with a changed name, preserving UUID, admin
     * status, preferences and market permissions.
     */
    public static User createWithChangedName(User oldUser, String newName)
    {
        User copy = new User(oldUser.userUUID, newName, oldUser.isStockMarketAdmin, oldUser.preferences);
        copy.canCreateMarkets = oldUser.canCreateMarkets;
        copy.deleteScope = oldUser.deleteScope;
        copy.manageScope = oldUser.manageScope;
        return copy;
    }
    public static @Nullable User createFromTag(CompoundTag tag)
    {
        User user = new User();
        if(!user.load(tag)) {
            return null; // Invalid data
        }
        return user;
    }


    public UUID getUUID() {
        return userUUID;
    }
    public String getName() {
        return userName;
    }
    public boolean isStockMarketAdmin() {
        return isStockMarketAdmin;
    }
    public void setStockMarketAdmin(boolean isBankModAdmin) {
        this.isStockMarketAdmin = isBankModAdmin;
    }
    public boolean canCreateMarkets() {
        return canCreateMarkets;
    }
    public void setCanCreateMarkets(boolean canCreateMarkets) {
        this.canCreateMarkets = canCreateMarkets;
    }
    public MarketActionScope getDeleteScope() {
        return deleteScope;
    }
    public void setDeleteScope(MarketActionScope deleteScope) {
        this.deleteScope = deleteScope == null ? MarketActionScope.NONE : deleteScope;
    }
    public MarketActionScope getManageScope() {
        return manageScope;
    }
    public void setManageScope(MarketActionScope manageScope) {
        this.manageScope = manageScope == null ? MarketActionScope.NONE : manageScope;
    }

    /**
     * @return the effective permissions for this user (admin flag included)
     */
    public PlayerMarketPermissions getMarketPermissions() {
        if (isStockMarketAdmin)
            return PlayerMarketPermissions.adminPermissions();
        return new PlayerMarketPermissions(false, canCreateMarkets, deleteScope, manageScope);
    }
    public PlayerPreferences getPreferences() {
        return preferences;
    }
    public void setPreferences(PlayerPreferences preferences) {
        this.preferences = preferences;
    }

    @Override
    public boolean save(CompoundTag tag) {
        NbtCompat.putUUID(tag, "userUUID", userUUID);
        tag.putString("userName", userName);
        tag.putBoolean("isStockMarketAdmin", isStockMarketAdmin);
        tag.putBoolean("canCreateMarkets", canCreateMarkets);
        tag.putString("deleteScope", deleteScope.name());
        tag.putString("manageScope", manageScope.name());

        // Save player preferences as a sub-tag
        CompoundTag prefsTag = new CompoundTag();
        preferences.save(prefsTag);
        tag.put("preferences", prefsTag);
        return true;
    }

    @Override
    public boolean load(CompoundTag tag) {
        if(!NbtCompat.hasUUID(tag, "userUUID") || !tag.contains("userName")) {
            StockMarketMod.LOGGER.error("User.load: missing required fields");
            return false; // Invalid data
        }
        this.userUUID = NbtCompat.getUUID(tag, "userUUID");
        this.userName = NbtCompat.getString(tag, "userName");
        if(tag.contains("isStockMarketAdmin"))
            this.isStockMarketAdmin = NbtCompat.getBoolean(tag, "isStockMarketAdmin");
        else
            this.isStockMarketAdmin = false;

        // Market permissions: missing keys (old saves) default to no access.
        this.canCreateMarkets = tag.contains("canCreateMarkets") && NbtCompat.getBoolean(tag, "canCreateMarkets");
        this.deleteScope = tag.contains("deleteScope")
                ? parseScope(NbtCompat.getString(tag, "deleteScope"), MarketActionScope.NONE)
                : MarketActionScope.NONE;
        this.manageScope = tag.contains("manageScope")
                ? parseScope(NbtCompat.getString(tag, "manageScope"), MarketActionScope.NONE)
                : MarketActionScope.NONE;

        // Load player preferences (backward compatible — defaults to empty if not present)
        if (tag.contains("preferences")) {
            CompoundTag prefsTag = NbtCompat.getCompound(tag, "preferences");
            preferences = new PlayerPreferences();
            preferences.load(prefsTag);
        } else {
            preferences = new PlayerPreferences();
        }
        return true;
    }

    public JsonElement toJson()
    {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("userUUID", userUUID.toString());
        jsonObject.addProperty("userName", userName);
        jsonObject.addProperty("isStockMarketAdmin", isStockMarketAdmin);
        jsonObject.addProperty("canCreateMarkets", canCreateMarkets);
        jsonObject.addProperty("deleteScope", deleteScope.name());
        jsonObject.addProperty("manageScope", manageScope.name());
        return jsonObject;
    }
    @Override
    public String toString()
    {
        return JsonUtilities.toPrettyString(toJson());
    }
}
