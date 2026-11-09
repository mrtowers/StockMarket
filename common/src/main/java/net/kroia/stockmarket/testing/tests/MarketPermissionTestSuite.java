package net.kroia.stockmarket.testing.tests;

import net.kroia.banksystem.util.ItemID;
import net.kroia.modutilities.testing.TestCategory;
import net.kroia.modutilities.testing.TestResult;
import net.kroia.modutilities.testing.TestSuite;
import net.kroia.stockmarket.api.marketmanager.MarketActionScope;
import net.kroia.stockmarket.api.marketmanager.PlayerMarketPermissions;
import net.kroia.stockmarket.stockmarket.market.ServerMarket;
import net.kroia.stockmarket.stockmarket.marketmanager.User;
import net.kroia.stockmarket.testing.StockMarketTestCategories;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/**
 * Issue-31 market permission tests: the per-player scope matrix, the User NBT
 * round-trip, and ServerMarket owner persistence. Pure logic — no world or
 * backend mutation, so this suite is safe to run anywhere.
 */
public class MarketPermissionTestSuite extends TestSuite {

    @Override
    public TestCategory getCategory() {
        return StockMarketTestCategories.MARKET_PERMISSIONS;
    }

    @Override
    public void registerTests() {
        addTest("none_denies_everything", this::test_none_denies_everything);
        addTest("create_only", this::test_create_only);
        addTest("delete_own_scope", this::test_delete_own_scope);
        addTest("delete_any_scope", this::test_delete_any_scope);
        addTest("manage_own_scope", this::test_manage_own_scope);
        addTest("manage_any_scope", this::test_manage_any_scope);
        addTest("admin_bypasses_scopes", this::test_admin_bypasses_scopes);
        addTest("ownerless_market_requires_any", this::test_ownerless_market_requires_any);
        addTest("user_nbt_roundtrip", this::test_user_nbt_roundtrip);
        addTest("rename_preserves_permissions", this::test_rename_preserves_permissions);
        addTest("server_market_owner_nbt_roundtrip", this::test_server_market_owner_nbt_roundtrip);
    }

    private TestResult test_none_denies_everything() {
        UUID player = UUID.randomUUID();
        PlayerMarketPermissions perms = PlayerMarketPermissions.NONE;
        TestResult r = assertFalse("NONE must deny create", perms.canCreate());
        if (!r.passed()) return r;
        r = assertFalse("NONE must deny delete on own market", perms.canDelete(player, player));
        if (!r.passed()) return r;
        r = assertFalse("NONE must deny manage on own market", perms.canManage(player, player));
        if (!r.passed()) return r;
        r = assertFalse("NONE must not open the management screen", perms.canOpenManagement());
        if (!r.passed()) return r;
        return pass("NONE denies every market action");
    }

    private TestResult test_create_only() {
        UUID player = UUID.randomUUID();
        PlayerMarketPermissions perms = new PlayerMarketPermissions(
                false, true, MarketActionScope.NONE, MarketActionScope.NONE);
        TestResult r = assertTrue("CREATE must allow create", perms.canCreate());
        if (!r.passed()) return r;
        r = assertFalse("CREATE alone must not allow delete", perms.canDelete(player, player));
        if (!r.passed()) return r;
        r = assertFalse("CREATE alone must not allow manage", perms.canManage(player, player));
        if (!r.passed()) return r;
        r = assertTrue("CREATE must open the player screen", perms.canOpenManagement());
        if (!r.passed()) return r;
        return pass("CREATE allows creation only");
    }

    private TestResult test_delete_own_scope() {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        PlayerMarketPermissions perms = new PlayerMarketPermissions(
                false, false, MarketActionScope.OWN, MarketActionScope.NONE);
        TestResult r = assertTrue("OWN must allow deleting own market", perms.canDelete(owner, owner));
        if (!r.passed()) return r;
        r = assertFalse("OWN must deny deleting another player's market", perms.canDelete(owner, other));
        if (!r.passed()) return r;
        r = assertFalse("OWN must deny managing another market", perms.canManage(owner, other));
        if (!r.passed()) return r;
        r = assertTrue("DELETE OWN must open the player screen", perms.canOpenManagement());
        if (!r.passed()) return r;
        return pass("DELETE OWN is owner-scoped");
    }

    private TestResult test_delete_any_scope() {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        PlayerMarketPermissions perms = new PlayerMarketPermissions(
                false, false, MarketActionScope.ANY, MarketActionScope.NONE);
        TestResult r = assertTrue("ANY must allow deleting another player's market", perms.canDelete(owner, other));
        if (!r.passed()) return r;
        r = assertTrue("ANY must allow deleting own market", perms.canDelete(owner, owner));
        if (!r.passed()) return r;
        r = assertFalse("DELETE ANY alone must not allow manage", perms.canManage(owner, other));
        if (!r.passed()) return r;
        return pass("DELETE ANY covers every market");
    }

    private TestResult test_manage_own_scope() {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        PlayerMarketPermissions perms = new PlayerMarketPermissions(
                false, false, MarketActionScope.NONE, MarketActionScope.OWN);
        TestResult r = assertTrue("MANAGE OWN must allow managing own market", perms.canManage(owner, owner));
        if (!r.passed()) return r;
        r = assertFalse("MANAGE OWN must deny managing another player's market", perms.canManage(owner, other));
        if (!r.passed()) return r;
        r = assertFalse("MANAGE OWN must not allow deleting", perms.canDelete(owner, owner));
        if (!r.passed()) return r;
        return pass("MANAGE OWN is owner-scoped");
    }

    private TestResult test_manage_any_scope() {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        PlayerMarketPermissions perms = new PlayerMarketPermissions(
                false, false, MarketActionScope.NONE, MarketActionScope.ANY);
        TestResult r = assertTrue("MANAGE ANY must allow managing another player's market", perms.canManage(owner, other));
        if (!r.passed()) return r;
        r = assertTrue("MANAGE ANY must allow managing own market", perms.canManage(owner, owner));
        if (!r.passed()) return r;
        return pass("MANAGE ANY covers every market");
    }

    private TestResult test_admin_bypasses_scopes() {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        PlayerMarketPermissions perms = PlayerMarketPermissions.adminPermissions();
        TestResult r = assertTrue("Admin must create", perms.canCreate());
        if (!r.passed()) return r;
        r = assertTrue("Admin must delete any market", perms.canDelete(owner, other));
        if (!r.passed()) return r;
        r = assertTrue("Admin must manage any market", perms.canManage(owner, other));
        if (!r.passed()) return r;
        return pass("Admin bypasses the scope matrix");
    }

    private TestResult test_ownerless_market_requires_any() {
        UUID player = UUID.randomUUID();
        PlayerMarketPermissions ownOnly = new PlayerMarketPermissions(
                false, false, MarketActionScope.OWN, MarketActionScope.OWN);
        PlayerMarketPermissions any = new PlayerMarketPermissions(
                false, false, MarketActionScope.ANY, MarketActionScope.ANY);
        TestResult r = assertFalse("OWN must not touch an ownerless market", ownOnly.canDelete(null, player));
        if (!r.passed()) return r;
        r = assertFalse("OWN must not manage an ownerless market", ownOnly.canManage(null, player));
        if (!r.passed()) return r;
        r = assertTrue("ANY must delete an ownerless market", any.canDelete(null, player));
        if (!r.passed()) return r;
        r = assertTrue("ANY must manage an ownerless market", any.canManage(null, player));
        if (!r.passed()) return r;
        return pass("Ownerless markets are ANY/admin only");
    }

    private TestResult test_user_nbt_roundtrip() {
        UUID uuid = UUID.randomUUID();
        User user = new User(uuid, "PermTestUser");
        user.setCanCreateMarkets(true);
        user.setDeleteScope(MarketActionScope.OWN);
        user.setManageScope(MarketActionScope.ANY);

        CompoundTag tag = new CompoundTag();
        TestResult r = assertTrue("User.save should succeed", user.save(tag));
        if (!r.passed()) return r;

        User loaded = User.createFromTag(tag);
        r = assertNotNull("User.createFromTag should load the user", loaded);
        if (!r.passed()) return r;
        r = assertTrue("create permission should survive the round-trip", loaded.canCreateMarkets());
        if (!r.passed()) return r;
        r = assertEquals("delete scope should survive the round-trip",
                MarketActionScope.OWN, loaded.getDeleteScope());
        if (!r.passed()) return r;
        r = assertEquals("manage scope should survive the round-trip",
                MarketActionScope.ANY, loaded.getManageScope());
        if (!r.passed()) return r;
        return pass("User permissions persist through NBT");
    }

    private TestResult test_rename_preserves_permissions() {
        User user = new User(UUID.randomUUID(), "OldName");
        user.setCanCreateMarkets(true);
        user.setDeleteScope(MarketActionScope.OWN);
        user.setManageScope(MarketActionScope.ANY);

        User renamed = User.createWithChangedName(user, "NewName");
        TestResult r = assertTrue("create permission should survive rename", renamed.canCreateMarkets());
        if (!r.passed()) return r;
        r = assertEquals("delete scope should survive rename",
                MarketActionScope.OWN, renamed.getDeleteScope());
        if (!r.passed()) return r;
        r = assertEquals("manage scope should survive rename",
                MarketActionScope.ANY, renamed.getManageScope());
        if (!r.passed()) return r;
        return pass("Renaming a player preserves market permissions");
    }

    private TestResult test_server_market_owner_nbt_roundtrip() {
        ItemID id = new ItemID((short) 777);
        UUID owner = UUID.randomUUID();
        ServerMarket market = new ServerMarket(id);
        market.setOwnerUUID(owner);

        CompoundTag tag = new CompoundTag();
        TestResult r = assertTrue("ServerMarket.save should succeed", market.save(tag));
        if (!r.passed()) return r;

        ServerMarket loaded = new ServerMarket(id);
        r = assertTrue("ServerMarket.load should succeed", loaded.load(tag));
        if (!r.passed()) return r;
        r = assertEquals("owner UUID should survive the round-trip", owner, loaded.getOwnerUUID());
        if (!r.passed()) return r;
        return pass("ServerMarket owner persists through NBT");
    }
}
