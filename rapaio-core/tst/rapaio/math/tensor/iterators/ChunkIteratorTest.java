/*
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/
 *
 *    Copyright 2013 Aurelian Tutuianu
 *    Copyright 2014 Aurelian Tutuianu
 *    Copyright 2015 Aurelian Tutuianu
 *    Copyright 2016 Aurelian Tutuianu
 *    Copyright 2017 Aurelian Tutuianu
 *    Copyright 2018 Aurelian Tutuianu
 *    Copyright 2019 Aurelian Tutuianu
 *    Copyright 2020 Aurelian Tutuianu
 *    Copyright 2021 Aurelian Tutuianu
 *    Copyright 2022 Aurelian Tutuianu
 *    Copyright 2023 Aurelian Tutuianu
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package rapaio.math.tensor.iterators;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import rapaio.math.tensor.Order;
import rapaio.math.tensor.Shape;

public class ChunkIteratorTest {

    @Test
    void testScalarIterator() {
        var it = new ScalarChunkIterator(3);
        testScalar(it, 3);
    }

    @Test
    void testStrideIterator() {
        var it = new StrideChunkIterator(Shape.of(), 10, new int[0], Order.S);
        testChunkDescriptor(it, Shape.of(), 10, new int[0], Order.S);
        testScalar(it, 10);

        // test stride in fixed c order
        it = new StrideChunkIterator(Shape.of(2, 3, 4), 10, new int[] {10, 4, 7}, Order.C);
        int[] chunkOffsets = it.computeChunkOffsets();
        int pos = 0;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 3; j++) {
                assertTrue(it.hasNext());
                assertEquals(chunkOffsets[pos++], it.nextInt());
                assertEquals(10 + j * 4 + i * 10, chunkOffsets[pos - 1]);
                assertEquals(4, it.loopSize());
                assertEquals(7, it.loopStep());
                assertEquals(4 * 7, it.loopBound());
                assertEquals(6, it.chunkCount());
            }
        }
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::nextInt);
        testChunkDescriptor(it, Shape.of(2,3,4), 10, new int[]{10,4,7}, Order.C);

        // test stride in fixed fortran order
        it = new StrideChunkIterator(Shape.of(2, 3, 4), 10, new int[] {10, 4, 7}, Order.F);
        chunkOffsets = it.computeChunkOffsets();
        pos = 0;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 3; j++) {
                assertTrue(it.hasNext());
                assertEquals(chunkOffsets[pos++], it.nextInt());
                assertEquals(10 + j * 4 + i * 7, chunkOffsets[pos - 1]);
                assertEquals(2, it.loopSize());
                assertEquals(10, it.loopStep());
                assertEquals(2 * 10, it.loopBound());
                assertEquals(12, it.chunkCount());
            }
        }
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::nextInt);
        testChunkDescriptor(it, Shape.of(2, 3, 4), 10, new int[] {10, 4, 7}, Order.F);

        // test stride no compaction
        it = new StrideChunkIterator(Shape.of(2, 3, 4), 10, new int[] {10, 4, 7}, Order.S);
        pos = 0;
        chunkOffsets = it.computeChunkOffsets();
        for (int i = 0; i < 2; i++) {
            for (int k = 0; k < 4; k++) {
                assertTrue(it.hasNext());
                assertEquals(chunkOffsets[pos++], it.nextInt());
                assertEquals(10 + i * 10 + k * 7, chunkOffsets[pos - 1]);
                assertEquals(3, it.loopSize());
                assertEquals(4, it.loopStep());
                assertEquals(3 * 4, it.loopBound());
                assertEquals(8, it.chunkCount());
            }
        }
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::nextInt);
        testChunkDescriptor(it, Shape.of(2, 3, 4), 10, new int[] {10, 4, 7}, Order.S);

        // test stride with compaction of first 3 levels
        it = new StrideChunkIterator(Shape.of(5, 3, 4, 3), 10, new int[] {1, 20, 5, 100}, Order.S);
        pos = 0;
        chunkOffsets = it.computeChunkOffsets();
        for (int i = 0; i < 3; i++) {
            assertTrue(it.hasNext());
            assertEquals(chunkOffsets[pos++], it.nextInt());
            assertEquals(10 + i * 100, chunkOffsets[pos-1]);
            assertEquals(60, it.loopSize());
            assertEquals(1, it.loopStep());
            assertEquals(3, it.chunkCount());
        }
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::nextInt);
        testChunkDescriptor(it, Shape.of(5, 3, 4, 3), 10, new int[] {1, 20, 5, 100}, Order.S);

        // test compaction with all levels
        it = new StrideChunkIterator(Shape.of(5, 3, 4, 3), 10, new int[] {1, 20, 5, 60}, Order.S);
        testChunkDescriptor(it, Shape.of(5, 3, 4, 3), 10, new int[] {1, 20, 5, 60}, Order.S);
        pos = 0;
        chunkOffsets = it.computeChunkOffsets();
        assertTrue(it.hasNext());
        assertEquals(chunkOffsets[pos++], it.nextInt());
        assertEquals(10, chunkOffsets[pos-1]);
        assertEquals(180, it.loopSize());
        assertEquals(1, it.loopStep());
        assertEquals(1, it.chunkCount());
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::nextInt);

        // test compaction with all levels and stride 2
        it = new StrideChunkIterator(Shape.of(5, 3, 4, 3), 10, new int[] {2, 40, 10, 120}, Order.S);
        assertTrue(it.hasNext());
        assertEquals(10, it.nextInt());
        assertEquals(2, it.loopStep());
        assertEquals(180, it.loopSize());
        assertEquals(1, it.chunkCount());
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::nextInt);
        testChunkDescriptor(it, Shape.of(5, 3, 4, 3), 10, new int[] {2, 40, 10, 120}, Order.S);
    }

    void testScalar(ChunkIterator it, int offset) {
        assertTrue(it.hasNext());
        assertEquals(offset, it.nextInt());
        assertArrayEquals(new int[]{offset}, it.computeChunkOffsets());
        assertEquals(1, it.loopStep());
        assertEquals(1, it.loopSize());
        assertEquals(1, it.chunkCount());
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::nextInt);

        assertArrayEquals(new int[] {offset}, it.computeChunkOffsets());
    }

    void testChunkDescriptor(StrideChunkIterator it, Shape shape, int offset, int[] strides, Order order) {
        var descriptor = new StrideChunkDescriptor(shape, offset, strides, order);
        assertEquals(it.loopStep(), descriptor.loopStep());
        assertEquals(it.loopSize(), descriptor.loopSize());
        assertEquals(it.chunkCount(), descriptor.chunkCount());
        assertArrayEquals(it.computeChunkOffsets(), descriptor.getChunkOffsets());
    }
}
