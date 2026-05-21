package com.tasknotes.shared.util;

import java.security.SecureRandom;
import java.util.UUID;

public final class UuidV7Generator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidV7Generator() {}

    public static String generate() {
        long ts = System.currentTimeMillis();

        // MSB: [48 bits timestamp][4 bits version=7][12 bits rand_a]
        long randA = RANDOM.nextLong() & 0x0FFFL;
        long msb = (ts << 16) | (7L << 12) | randA;

        // LSB: [2 bits variant=10][62 bits rand_b]
        long randB = RANDOM.nextLong();
        long lsb = (randB & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;

        return new UUID(msb, lsb).toString();
    }
}
