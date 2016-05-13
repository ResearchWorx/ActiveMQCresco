package com.researchworx.cresco.controller.netdiscovery;



import org.apache.commons.net.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * Created by cody on 5/12/16.
 */
public class DiscoveryCrypto {

    private static final Logger logger = LoggerFactory.getLogger(DiscoveryCrypto.class);

    private static final String ALGORITHM = "AES";

    public DiscoveryCrypto() {}


    private Key generateKeyFromString(final String secKey) throws Exception {
        String SALT = "MrSaltyManBaby";
        byte[] keyVal = (SALT + secKey).getBytes("UTF-8");
        MessageDigest sha = MessageDigest.getInstance("SHA-1");
        keyVal = sha.digest(keyVal);
        keyVal = Arrays.copyOf(keyVal, 16); // use only first 128 bit
        final Key key = new SecretKeySpec(keyVal, ALGORITHM);
        return key;
    }

    public String encrypt(final String valueEnc, final String secKey) {

        String encryptedValue = null;

        try {
            final Key key = generateKeyFromString(secKey);
            final Cipher c = Cipher.getInstance(ALGORITHM);
            c.init(Cipher.ENCRYPT_MODE, key);
            final byte[] encValue = c.doFinal(valueEnc.getBytes());
            encryptedValue = new BASE64Encoder().encode(encValue);
            //encryptedValue = Base64.encodeBase64String(encValue);
        } catch(Exception ex) {
            logger.error(ex.getMessage());
            ex.printStackTrace();
        }

        return encryptedValue;
    }

    public String decrypt(final String encryptedValue, final String secretKey) {

        String decryptedValue = null;

        try {

            final Key key = generateKeyFromString(secretKey);
            final Cipher c = Cipher.getInstance(ALGORITHM);
            c.init(Cipher.DECRYPT_MODE, key);
            final byte[] decorVal = new BASE64Decoder().decodeBuffer(encryptedValue);
            //final byte[] decorVal = Base64.decodeBase64(encryptedValue);
            //byte[] valueDecoded= Base64.decodeBase64(bytesEncoded );
            final byte[] decValue = c.doFinal(decorVal);
            decryptedValue = new String(decValue);
        } catch(Exception ex) {
            logger.error(ex.getMessage());
            ex.printStackTrace();
        }

        return decryptedValue;
    }
}
