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

package rapaio.math.tensor.factories;

import java.util.Random;

import rapaio.math.tensor.ITensor;
import rapaio.math.tensor.Order;
import rapaio.math.tensor.Shape;
import rapaio.math.tensor.TensorMill;
import rapaio.util.collection.IntArrays;

public final class IntegerDenseStride extends IntegerDense {

    public IntegerDenseStride(TensorMill manager) {
        super(manager);
    }

    @Override
    public ITensor seq(Shape shape) {
        int[] strides = IntArrays.newFill(shape.rank(), 1);
        int[] ordering = IntArrays.newSeq(0, shape.rank());
        IntArrays.shuffle(ordering, new Random(42));
        for (int i = 1; i < shape.rank(); i++) {
            int next = -1;
            int prev = -1;
            for (int j = 0; j < ordering.length; j++) {
                if (ordering[j] == i) {
                    next = j;
                    break;
                }
            }
            for (int j = 0; j < ordering.length; j++) {
                if (ordering[j] == i - 1) {
                    prev = j;
                    break;
                }
            }
            strides[next] = strides[prev] * shape.dim(prev);
        }

        int offset = 10;
        var t = ofType.stride(shape, offset, strides, new int[offset + shape.size()]);

        t.apply(Order.C, (i, p) -> i);

        return t;
    }

    @Override
    public ITensor zeros(Shape shape) {
        int offset = 10;
        int[] strides = IntArrays.newFill(shape.rank(), 1);
        int[] ordering = IntArrays.newSeq(0, shape.rank());
        IntArrays.shuffle(ordering, new Random(42));

        for (int i = 1; i < shape.rank(); i++) {
            int next = -1;
            int prev = -1;
            for (int j = 0; j < ordering.length; j++) {
                if (ordering[j] == i) {
                    next = j;
                    break;
                }
            }
            for (int j = 0; j < ordering.length; j++) {
                if (ordering[j] == i - 1) {
                    prev = j;
                    break;
                }
            }
            strides[next] = strides[prev] * shape.dim(prev);
        }

        return ofType.stride(shape, offset, strides, new int[offset + shape.size()]);
    }

    @Override
    public ITensor random(Shape shape) {
        int offset = 10;
        int[] strides = IntArrays.newFill(shape.rank(), 1);
        int[] ordering = IntArrays.newSeq(0, shape.rank());
        IntArrays.shuffle(ordering, new Random(42));

        for (int i = 1; i < shape.rank(); i++) {
            int next = -1;
            int prev = -1;
            for (int j = 0; j < ordering.length; j++) {
                if (ordering[j] == i) {
                    next = j;
                    break;
                }
            }
            for (int j = 0; j < ordering.length; j++) {
                if (ordering[j] == i - 1) {
                    prev = j;
                    break;
                }
            }
            strides[next] = strides[prev] * shape.dim(prev);
        }

        int[] array = new int[offset + shape.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = random.nextInt();
        }
        return ofType.stride(shape, offset, strides, array);
    }
}
