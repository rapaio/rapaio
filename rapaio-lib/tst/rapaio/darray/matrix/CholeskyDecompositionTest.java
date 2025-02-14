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

package rapaio.darray.matrix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import rapaio.darray.DArray;
import rapaio.darray.DArrayManager;
import rapaio.darray.DType;
import rapaio.darray.Order;
import rapaio.darray.Shape;

public class CholeskyDecompositionTest {

    private static final int TIMES = 20;

    private Random random;

    @BeforeEach
    void beforeEach() {
        random = new Random(42);
    }

    @Test
    void symmetricPositiveDefiniteTest() {
        testType(DArrayManager.base(), DType.DOUBLE, true, 1e-8);
        testType(DArrayManager.base(), DType.FLOAT, true, 1e-0);
    }

    private <N extends Number> void testType(DArrayManager dm, DType<N> dt, boolean rightFlag, double TOL) {
        for (int i = 0; i < TIMES; i++) {
            var m = dm.random(dt, Shape.of(10, 10), random, Order.C);

            testSolve(m.t().mm(m), dm.full(dt, Shape.of(10, 1), 1d), rightFlag, TOL);
            testSolve(dm.eye(dt, 100, Order.C), dm.full(dt, Shape.of(100), 1d), rightFlag, TOL);

            testNonSPD(dm.random(dt, Shape.of(10, 10), random), dm.random(dt, Shape.of(10, 3), random));
        }
    }

    private <N extends Number> void testSolve(DArray<N> A, DArray<N> b, boolean rightFlag, double TOL) {
        var chol = A.cholesky(rightFlag);
        assertTrue(chol.isSPD());
        assertEquals(rightFlag, chol.hasRightFlag());

        if (rightFlag) {
            assertNotNull(chol.r());
            assertNull(chol.l());
        } else {
            assertNull(chol.r());
            assertNotNull(chol.l());
        }

        DArray<N> solution = chol.solve(b);
        assertTrue(A.mv(solution.isMatrix() ? solution.squeeze(1) : solution).deepEquals(b.isMatrix() ? b.squeeze(1) : b, TOL));
        assertTrue(A.deepEquals(A.cholesky(false).l().mm(A.cholesky(true).r()), TOL));
    }

    @Test
    void testIntegerTypes() {
        var ti = DArrayManager.base().eye(DType.INTEGER, 10);
        var ei = assertThrows(IllegalArgumentException.class, ti::cholesky);
        assertEquals("Cannot compute decomposition for integer types (dtype: INTEGER)", ei.getMessage());

        var tb = DArrayManager.base().eye(DType.BYTE, 10);
        var eb = assertThrows(IllegalArgumentException.class, tb::cholesky);
        assertEquals("Cannot compute decomposition for integer types (dtype: BYTE)", eb.getMessage());
    }

    <N extends Number> void testNonSPD(DArray<N> a, DArray<N> b) {
        var chol = a.cholesky();
        assertFalse(chol.isSPD());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, chol::inv);
        assertEquals("Matrix is not symmetric positive definite.", ex.getMessage());

        ex = assertThrows(IllegalArgumentException.class, b::cholesky);
        assertEquals("Only square matrices can have Cholesky decomposition.", ex.getMessage());
    }
}
