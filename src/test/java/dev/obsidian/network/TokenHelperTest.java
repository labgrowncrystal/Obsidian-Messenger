package dev.obsidian.network;

import dev.obsidian.crypto.ECDHHelper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TokenHelperTest {

    @Test
    public void testTokenGenerationAndParsingWithCRC() throws Exception {
        ECDHHelper.ECDHKeyPair pair = ECDHHelper.generateKeyPair();
        String token = TokenHelper.generateToken("1.2.3.4", "192.168.1.50", 49156, 24, 10, pair.publicKeyBase64);

        assertTrue(token.startsWith("OM-"));

        TokenHelper.SessionTokenData data = TokenHelper.parseToken(token);
        assertEquals("1.2.3.4", data.publicIp);
        assertEquals("192.168.1.50", data.lanIp);
        assertEquals(49156, data.port);
        assertEquals(10, data.maxPlayers);
        assertEquals(pair.publicKeyBase64, data.hostPubKey);
    }

    @Test
    public void testCorruptedTokenFailsCRC() throws Exception {
        ECDHHelper.ECDHKeyPair pair = ECDHHelper.generateKeyPair();
        String token = TokenHelper.generateToken("1.2.3.4", "192.168.1.50", 49156, 24, 10, pair.publicKeyBase64);
        
        // Corrupt token string
        String corruptedToken = token.substring(0, token.length() - 5) + "XXXXX";
        
        assertThrows(Exception.class, () -> {
            TokenHelper.parseToken(corruptedToken);
        });
    }
}
