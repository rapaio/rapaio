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

package rapaio.math.tensor.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import rapaio.math.tensor.DType;
import rapaio.math.tensor.TensorEngine;
import rapaio.math.tensor.Order;
import rapaio.math.tensor.Shape;
import rapaio.math.tensor.Tensor;
import rapaio.math.tensor.engine.basearray.BaseArrayTensorEngine;

public class TensorEngineTest {

    private Random random;

    @BeforeEach
    void beforeEach() {
        random = new Random(42);
    }

    @Test
    void mainTestLoop() {

        BaseArrayTensorEngine baseArrayEngine = new BaseArrayTensorEngine();
        testManager(baseArrayEngine, baseArrayEngine.ofType(DType.DOUBLE));
        testManager(baseArrayEngine, baseArrayEngine.ofType(DType.FLOAT));
    }

    <N extends Number, T extends Tensor<N, T>> void testManager(TensorEngine tensorEngine, TensorEngine.OfType<N, T> ofType) {
        testZeros(tensorEngine, ofType);
        testEye(tensorEngine, ofType);
        testFull(tensorEngine, ofType);
        testSeq(tensorEngine, ofType);
        testRandom(tensorEngine, ofType);
        testStride(tensorEngine, ofType);
        testWrap(tensorEngine, ofType);
        testConcatenate(tensorEngine, ofType);
        testStack(tensorEngine, ofType);
    }

    <N extends Number, T extends Tensor<N, T>> void testZeros(TensorEngine tensorEngine, TensorEngine.OfType<N, T> ofType) {
        var t = ofType.zeros(Shape.of(10, 20));
        assertEquals(tensorEngine, t.engine());
        assertEquals(t.shape(), Shape.of(10, 20));
        var it = t.pointerIterator();
        while (it.hasNext()) {
            assertEquals(ofType.dtype().castValue(0), t.getAt(it.nextInt()));
        }
    }

    <N extends Number, T extends Tensor<N, T>> void testEye(TensorEngine tensorEngine, TensorEngine.OfType<N, T> ofType) {
        var t = ofType.eye(3);
        assertEquals(2, t.rank());
        assertEquals(9, t.size());

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                double value = i == j ? 1 : 0;
                assertEquals(ofType.dtype().castValue(value), t.get(i, j));
            }
        }
    }

    <N extends Number, T extends Tensor<N, T>>void testFull(TensorEngine tensorEngine, TensorEngine.OfType<N, T> ofType) {
        var t = ofType.full(Shape.of(2, 3), ofType.dtype().castValue(5));
        assertEquals(2, t.rank());
        assertEquals(6, t.size());

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 3; j++) {
                assertEquals(ofType.dtype().castValue(5), t.get(i, j));
            }
        }
    }

    <N extends Number, T extends Tensor<N, T>> void testSeq(TensorEngine tensorEngine, TensorEngine.OfType<N, T> ofType) {
        var t = ofType.seq(Shape.of(2, 3, 4));
        var it = t.pointerIterator(Order.C);
        int i = 0;
        while (it.hasNext()) {
            assertEquals(ofType.dtype().castValue(i++), t.getAt(it.nextInt()));
        }
    }

    <N extends Number, T extends Tensor<N, T>> void testRandom(TensorEngine tensorEngine, TensorEngine.OfType<N, T> ofType) {
        var t = ofType.random(Shape.of(3, 4), random);
        var it = t.pointerIterator(Order.C);
        N last = t.getAt(it.nextInt());
        while (it.hasNext()) {
            N next = t.getAt(it.nextInt());
            assertNotEquals(last, next);
            last = next;
        }
    }

    <N extends Number, T extends Tensor<N, T>> void testStride(TensorEngine tensorEngine, TensorEngine.OfType<N, T> ofType) {
        double[] seq = new double[12];
        for (int i = 0; i < seq.length; i++) {
            seq[i] = i;
        }
        var t = ofType.stride(Shape.of(4, 3), 0, new int[] {1, 4}, seq);
        int i = 0;
        var it = t.pointerIterator(Order.F);
        while (it.hasNext()) {
            assertEquals(ofType.dtype().castValue(i++), t.getAt(it.nextInt()));
        }
    }

    <N extends Number, T extends Tensor<N, T>> void testWrap(TensorEngine tensorEngine, TensorEngine.OfType<N, T> ofType) {
        var t = ofType.stride(Shape.of(2, 3), Order.C, new double[] {1., 2, 3, 4, 5, 6});
        int i = 1;
        var it = t.pointerIterator(Order.C);
        while (it.hasNext()) {
            assertEquals(ofType.dtype().castValue(i++), t.getAt(it.nextInt()));
        }
    }

    <N extends Number, T extends Tensor<N, T>> void testConcatenate(TensorEngine tensorEngine, TensorEngine.OfType<N, T> ofType) {
        var t1 = ofType.stride(Shape.of(2, 3), Order.C, new double[] {1., 2, 3, 4, 5, 6});
        var t2 = ofType.stride(Shape.of(1, 3), Order.C, new float[] {7.f, 8, 9});
        var t3 = ofType.stride(Shape.of(3, 3), Order.C, new double[] {10., 11, 12, 13, 14, 15, 16, 17, 18});
        var t4 = ofType.stride(Shape.of(1, 2), Order.C, new double[] {1., 2, 3, 4});


        var t = tensorEngine.concat(0, List.of(t1, t2, t3));
        int i = 1;
        var it = t.pointerIterator(Order.C);
        while (it.hasNext()) {
            assertEquals(ofType.dtype().castValue(i++), t.getAt(it.nextInt()));
        }

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> tensorEngine.concat(0, List.of(t1, t4)));
        assertEquals("Tensors are not valid for concatenation", ex.getMessage());
    }

    <N extends Number, T extends Tensor<N, T>> void testStack(TensorEngine tensorEngine, TensorEngine.OfType<N, T> ofType) {
        var t1 = ofType.stride(Shape.of(2, 3), Order.C, new double[] {1., 2, 3, 4, 5, 6});
        var t2 = ofType.stride(Shape.of(2, 3), Order.C, new float[] {7.f, 8, 9, 10, 11, 12});
        var t3 = ofType.stride(Shape.of(2, 3), Order.C, new double[] {13., 14, 15, 16, 17, 18});

        double[][] expected = new double[][] {
                {1., 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18},
                {1., 2, 3, 7, 8, 9, 13, 14, 15, 4, 5, 6, 10, 11, 12, 16, 17, 18},
                {1., 7, 13, 2, 8, 14, 3, 9, 15, 4, 10, 16, 5, 11, 17, 6, 12, 18}
        };
        for (int s = 0; s < 3; s++) {
            var t = tensorEngine.stack(s, List.of(t1, t2, t3));
            int i = 0;
            var it = t.pointerIterator(Order.defaultOrder());
            while (it.hasNext()) {
                assertEquals(ofType.dtype().castValue(expected[s][i++]), t.getAt(it.nextInt()));
            }
        }

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> tensorEngine.stack(0, List.of(t1, t2.t())));
        assertEquals("Tensors are not valid for stack, they have to have the same dimensions.", ex.getMessage());
    }
}