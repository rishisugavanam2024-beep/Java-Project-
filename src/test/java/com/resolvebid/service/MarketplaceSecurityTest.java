package com.resolvebid.service;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.resolvebid.dao.DatabaseManager;
import com.resolvebid.exception.AuthorizationException;
import com.resolvebid.model.Role;
import com.resolvebid.model.UserAccount;
import com.resolvebid.util.FileService;

class MarketplaceSecurityTest {
    @TempDir Path temp;
    @Test void rolePermissionsAndCollectionsAreEnforced() throws Exception {
        DatabaseManager database = new DatabaseManager(temp.resolve("market")); database.initialize();
        MarketplaceService service = new MarketplaceService(database, new FileService(temp), new ValueRankingStrategy());
        UserAccount provider = new UserAccount(100, "provider", "Provider", Role.PROVIDER);
        assertThrows(AuthorizationException.class,
                () -> service.postRequest(provider, "Electrical", "Power trips every evening", "This week", "2500"));
        assertArrayEquals(new String[]{"Home Appliances", "Computer Repair", "Electrical", "Plumbing", "Vehicle Service", "Other"}, MarketplaceService.CATEGORIES);
        assertTrue(service.supportedCategories().contains("Electrical"));
    }
}
