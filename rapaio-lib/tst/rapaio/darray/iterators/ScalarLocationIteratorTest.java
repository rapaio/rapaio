/*
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/
 *
 *    Copyright 2013 - 2025 Aurelian Tutuianu
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

package rapaio.darray.iterators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import rapaio.darray.Order;
import rapaio.darray.Shape;
import rapaio.darray.layout.StrideLayout;

public class ScalarLocationIteratorTest {

    @Test
    void testScalarIterator() {
        var it = new ScalarPointerIterator(10);
        testScalar(it, 10);
    }

    @Test
    void testDense() {
        var it = new DensePointerIterator(Shape.of(), 0, 1);
        testScalar(it, 0);
    }

    @Test
    void testCIterator() {
        var it = new StridePointerIterator(StrideLayout.of(Shape.of(), 10, new int[0]), Order.C);
        testScalar(it, 10);
    }

    @Test
    void testFIterator() {
        var it = new StridePointerIterator(StrideLayout.of(Shape.of(), 10, new int[0]), Order.F);
        testScalar(it, 10);
    }

    @Test
    void testSIterator() {
        var it = new StridePointerIterator(StrideLayout.of(Shape.of(), 10, new int[0]), Order.S);
        testScalar(it, 10);
    }

    void testScalar(PointerIterator it, int pointer) {
        assertTrue(it.hasNext());
        assertEquals(pointer, it.nextInt());
        assertEquals(0, it.position());
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::nextInt);
    }
}
