package com.alvarium.hash;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class HashProviderTest {
  private final String[] testCases = {"alvarium is the best", "foo bar"};
  private final String[] sha256Hashes = {
      "DD22AA6A8CF771E812349DA134CB266471DEBB549B0E2908091C61B1C47FA853",
      "FBC1A9F858EA9E177916964BD88C3D37B91A1E84412765E29950777F265C4B75"};
  private final String[] md5Hashes = { "8F907CCF83C58F0D8E84BABA9DF0DBB7", 
      "327B6F07435811239BC47E1544353273" };

  @Test
  public void noneProviderShouldReturnTheSameString() throws HashTypeException {
    HashProviderFactory hashProviderFactory = new HashProviderFactory();
    HashProvider sut = hashProviderFactory.getProvider(HashType.NoHash);
    for (int i = 0; i < 10; i++) {
      String randomString = this.generateRandomString(64);
      System.out.println(String.format("test string: %s", randomString));
      String result = sut.derive(randomString.getBytes());
      assertEquals(randomString, result);
    }
  }

  @Test
  public void sha256ProviderShouldGenerateAppropriateHashes() throws HashTypeException {
    HashProviderFactory hashProviderFactory = new HashProviderFactory();
    HashProvider sut = hashProviderFactory.getProvider(HashType.SHA256Hash);

    for (int i = 0; i < testCases.length; i++) {
      final String resultedHash = sut.derive(testCases[i].getBytes());
      assertEquals(sha256Hashes[i], resultedHash);
    }
  }

  @Test
  public void md5ProviderShouldGenerateAppropriateHashes() throws HashTypeException {
    HashProviderFactory hashProviderFactory = new HashProviderFactory();
    HashProvider sut = hashProviderFactory.getProvider(HashType.MD5Hash);
    
    for (int i = 0; i < testCases.length; i++) {
      final String result = sut.derive(testCases[i].getBytes()); 
      assertEquals(md5Hashes[i], result);
    }
  }

  String generateRandomString(int length) {
    int lowerLimit = 97;
    int upperLimit = 122;
    Random random = new Random();
    String generatedString = random.ints(lowerLimit, upperLimit + 1).limit(length)
        .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
        .toString();
    return generatedString;
  }
}
