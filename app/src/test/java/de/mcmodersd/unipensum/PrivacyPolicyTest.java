package de.mcmodersd.unipensum;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

public class PrivacyPolicyTest {

    private static final Path MANIFEST = Path.of("src", "main", "AndroidManifest.xml");
    private static final Path STRINGS = Path.of("src", "main", "res", "values", "strings.xml");
    private static final Path POLICY = Path.of("..", "pages", "privacy", "index.html");
    private static final Pattern PERMISSION = Pattern.compile("<uses-permission[^>]*android:name=\"android\\.permission\\.([A-Z_]+)\"");
    private static final Pattern PRIVACY_URL = Pattern.compile("<string name=\"privacy_url\"[^>]*>([^<]+)</string>");
    private static final Pattern CANONICAL = Pattern.compile("<link rel=\"canonical\" href=\"([^\"]+)\"");

    @Test
    public void everyPermissionOfTheAppIsNamedInThePrivacyPolicy() throws IOException {
        var policy = Files.readString(POLICY);
        var permissions = PERMISSION.matcher(Files.readString(MANIFEST));
        var found = 0;
        while (permissions.find()) {
            found++;
            var name = permissions.group(1);
            assertTrue("The privacy policy does not name " + name, policy.contains(name));
        }
        assertTrue("The manifest has no permissions to check", found > 0);
    }

    @Test
    public void theAppHasNoInternetAccessAsThePrivacyPolicySays() throws IOException {
        assertFalse(Files.readString(MANIFEST).contains("android.permission.INTERNET"));
    }

    @Test
    public void theLinkInTheAppIsTheAddressOfThePrivacyPolicy() throws IOException {
        var link = PRIVACY_URL.matcher(Files.readString(STRINGS));
        var canonical = CANONICAL.matcher(Files.readString(POLICY));
        assertTrue("The app has no privacy_url", link.find());
        assertTrue("The privacy policy has no canonical link", canonical.find());
        assertEquals(canonical.group(1), link.group(1).trim());
    }
}