/*
 * Copyright (c) 2021, Red Hat, Inc. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */


import java.util.Random;
import jdk.test.lib.Utils;

public abstract class AbstractStressArrayCopy {
    static final int MAX_SIZE = 1024*1024 + 1;
    static final int EXHAUSTIVE_SIZES = Integer.getInteger("exhaustiveSizes", 192);
    static final int FUZZ_COUNT = Integer.getInteger("fuzzCount", 300);

    public static void throwSeedError(int len, int pos) {
        throw new RuntimeException("Error after seed: " +
            len + " elements, at pos " + pos);
    }

    public static void throwContentsError(int l, int r, int len, int pos) {
        throwError("in contents", l, r, len, pos);
    }

    public static void throwHeadError(int l, int r, int len, int pos) {
        throwError("in head", l, r, len, pos);
    }

    public static void throwTailError(int l, int r, int len, int pos) {
        throwError("in tail", l, r, len, pos);
    }

    private static void throwError(String phase, int l, int r, int len, int pos) {
        throw new RuntimeException("Error " + phase + ": " +
            len + " elements, " +
            "[" + l + ", " + (l+len) + ") -> " +
            "[" + r + ", " + (r+len) + "), " +
            "at pos " + pos);
    }

    protected abstract void testWith(int size, int l, int r, int len);

    public void exhaustiveWith(int size) {
        for (int l = 0; l < size; l++) {
            for (int r = 0; r < size; r++) {
                int maxLen = Math.min(size - l, size - r);
                for (int len = 0; len <= maxLen; len++) {
                    testWith(size, l, r, len);
                }
            }
        }
    }

    public void fuzzWith(Random rand, int size) {
        // Some basic checks first
        testWith(size, 0, 1, 1);
        testWith(size, 0, 1, size-1);

        // Test disjoint:
        for (int c = 0; c < FUZZ_COUNT; c++) {
            int l = rand.nextInt(size / 2);
            int len = rand.nextInt((size - l) / 2);
            int r = (l + len) + rand.nextInt(size - 2*len - l);

//             if (l >= size)      throw new IllegalStateException("l is out of bounds");
//             if (l + len > size) throw new IllegalStateException("l+len is out of bounds");
//             if (r >= size)      throw new IllegalStateException("r is out of bounds");
//             if (r + len > size) throw new IllegalStateException("r+len is out of bounds: " + l + " " + r + " " + len + " " + size);
//             if (l + len > r)    throw new IllegalStateException("Not disjoint");

            testWith(size, l, r, len);
            testWith(size, r, l, len);
        }

        // Test conjoint:
        for (int c = 0; c < FUZZ_COUNT; c++) {
            int l = rand.nextInt(size);
            int len = rand.nextInt(size - l);
            int r = Math.min(l + (len > 0 ? rand.nextInt(len) : 0), size - len);

//             if (l >= size)      throw new IllegalStateException("l is out of bounds");
//             if (l + len > size) throw new IllegalStateException("l+len is out of bounds");
//             if (r >= size)      throw new IllegalStateException("r is out of bounds");
//             if (r + len > size) throw new IllegalStateException("r+len is out of bounds: " + l + " " + r + " " + len + " " + size);
//             if (l + len < r)    throw new IllegalStateException("Not conjoint");

            testWith(size, l, r, len);
            testWith(size, r, l, len);
        }
    }

    public void run(Random rand) {
        // Exhaustive on all small arrays
        for (int size = 1; size <= EXHAUSTIVE_SIZES; size++) {
            exhaustiveWith(size);
        }

        // Fuzz powers of ten
        for (int size = 10; size <= MAX_SIZE; size *= 10) {
            if (size <= EXHAUSTIVE_SIZES) continue;
            fuzzWith(rand, size - 1);
            fuzzWith(rand, size);
            fuzzWith(rand, size + 1);
        }

        // Fuzz powers of two
        for (int size = 2; size <= MAX_SIZE; size *= 2) {
            if (size <= EXHAUSTIVE_SIZES) continue;
            fuzzWith(rand, size - 1);
            fuzzWith(rand, size);
            fuzzWith(rand, size + 1);
        }
    }

}
