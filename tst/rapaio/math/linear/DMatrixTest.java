/*
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/
 *
 *    Copyright 2013 Aurelian Tutuianu
 *    Copyright 2014 Aurelian Tutuianu
 *    Copyright 2015 Aurelian Tutuianu
 *    Copyright 2016 Aurelian Tutuianu
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

package rapaio.math.linear;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rapaio.core.RandomSource;
import rapaio.math.linear.dense.SolidDMatrix;

import static org.junit.jupiter.api.Assertions.*;

public class DMatrixTest {

    private static final double TOL = 1e-20;

    @BeforeEach
    void beforeEach() {
        RandomSource.setSeed(1234);
    }

    @Test
    void plusMinusTest() {
        DMatrix A1 = SolidDMatrix.identity(3);
        A1.plus(2);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                assertEquals(i == j ? 3 : 2, A1.get(i, j), TOL);
            }
        }

        DMatrix A2 = SolidDMatrix.identity(3);
        DMatrix A3 = A2.copy().plus(A2);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                assertEquals(i == j ? 2 : 0, A3.get(i, j), TOL);
            }
        }

        DMatrix A4 = A3.minus(A2);
        assertTrue(A4.isEqual(A2));


        DMatrix A5 = SolidDMatrix.identity(10);
        DMatrix A6 = A5.copy().plus(10).minus(10);
        assertTrue(A5.isEqual(A6));
    }

    @Test
    void plusNonConformantTest() {
        DMatrix A = SolidDMatrix.identity(3);
        DMatrix B = SolidDMatrix.identity(4);
        assertThrows(IllegalArgumentException.class, () -> A.plus(B));
    }

    @Test
    void minusNonConformantTest() {
        DMatrix A = SolidDMatrix.identity(3);
        DMatrix B = SolidDMatrix.identity(4);
        assertThrows(IllegalArgumentException.class, () -> A.minus(B));
    }


    @Test
    void rowMappingTest() {
        DMatrix a1 = SolidDMatrix.random(5, 5);
        DMatrix a2 = a1.mapRows(1, 3);
        for (int i = 0; i < a1.colCount(); i++) {
            assertEquals(a1.get(1, i), a2.get(0, i), TOL);
            assertEquals(a1.get(3, i), a2.get(1, i), TOL);
        }

        DMatrix a3 = a1.removeRows(0, 2, 4);
        assertTrue(a3.isEqual(a2));

        DMatrix a4 = a1.rangeRows(1, 4).removeRows(1);
        assertTrue(a4.isEqual(a2));
    }

    @Test
    void colMappingTest() {
        DMatrix a1 = SolidDMatrix.random(5, 5);

        DMatrix a2 = a1.mapCols(1, 3);
        for (int i = 0; i < a1.rowCount(); i++) {
            assertEquals(a1.get(i, 1), a2.get(i, 0), TOL);
            assertEquals(a1.get(i, 3), a2.get(i, 1), TOL);
        }

        DMatrix a3 = a1.removeCols(0, 2, 4);
        assertTrue(a3.isEqual(a2));

        DMatrix a4 = a1.rangeCols(1, 4).removeCols(1);
        assertTrue(a4.isEqual(a2));
    }

    @Test
    void dotTest() {

        DMatrix a1 = SolidDMatrix.random(10, 10);
        DMatrix a2 = a1.copy().times(2);
        DMatrix a3 = a1.copy().plus(a1);

        assertTrue(a2.isEqual(a3, TOL));

        DMatrix i10 = SolidDMatrix.identity(10);
        assertTrue(a1.isEqual(a1.dot(i10), TOL));
    }

    @Test
    void testDiagonal() {
        DMatrix a1 = SolidDMatrix.identity(10);
        assertEquals(10, a1.diag().valueStream().sum(), TOL);

        DMatrix a2 = SolidDMatrix.random(10, 10);
        DVector d2 = a2.diag();
        for (int i = 0; i < 10; i++) {
            assertEquals(a2.get(i, i), d2.get(i), TOL);
        }
    }

    @Test
    void scatter() {
        DMatrix a1 = SolidDMatrix.identity(4);
        DMatrix s1 = a1.scatter();
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                assertEquals(i == j ? 0.75 : -0.25, s1.get(i, j), TOL);
            }
        }

        // reference for this test is found at: http://www.itl.nist.gov/div898/handbook/pmc/section5/pmc541.htm

        DMatrix a2 = SolidDMatrix.wrap(new double[][]{
                {4, 2, 0.6},
                {4.2, 2.1, 0.59},
                {3.9, 2.0, 0.58},
                {4.3, 2.1, 0.62},
                {4.1, 2.2, 0.63}
        });


        // reference for this test is found at: https://gist.github.com/nok/73d07cc644a390fad9e9

        DMatrix a3 = SolidDMatrix.wrap(new double[][]{
                {90, 60, 90},
                {90, 90, 30},
                {60, 60, 60},
                {60, 60, 90},
                {30, 30, 30}
        });
        DMatrix s3 = SolidDMatrix.wrap(new double[][]{
                {504.0, 360.0, 180.0},
                {360.0, 360.0, 0.0},
                {180.0, 0.0, 720.0}
        });
        assertTrue(s3.isEqual(a3.scatter().times(1.0 / a3.rowCount())));
    }
}
