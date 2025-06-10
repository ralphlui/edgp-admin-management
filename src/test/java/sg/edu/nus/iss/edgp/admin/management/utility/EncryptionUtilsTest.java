package sg.edu.nus.iss.edgp.admin.management.utility;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilsTest {

    private EncryptionUtils encryptionUtils;

    // Example 16-byte (128-bit) AES key in hex (must be 32 hex characters for 128-bit key)
    private static final String TEST_KEY = "00112233445566778899AABBCCDDEEFF";

    @BeforeEach
    void setUp() throws Exception {
        encryptionUtils = new EncryptionUtils();
        Field aesSecretKeyField = EncryptionUtils.class.getDeclaredField("aesSecretKey");
        aesSecretKeyField.setAccessible(true);
        aesSecretKeyField.set(encryptionUtils, TEST_KEY);
    }

    @Test
    void testEncryptionAndDecryption() throws Exception {
        String originalText = "HelloWorld123!";
        String encryptedText = encryptionUtils.encrypt(originalText);
        assertNotNull(encryptedText);
        assertNotEquals(originalText, encryptedText);

        String decryptedText = encryptionUtils.decrypt(encryptedText);
        assertEquals(originalText, decryptedText);
    }

    @Test
    void testDecryptWithInvalidData() {
        String invalidEncryptedText = "00AABB"; // not valid encrypted format
        assertThrows(Exception.class, () -> {
            encryptionUtils.decrypt(invalidEncryptedText);
        });
    }

    @Test
    void testEncryptNullValue() {
        assertThrows(NullPointerException.class, () -> {
            encryptionUtils.encrypt(null);
        });
    }
}
