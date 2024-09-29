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

import java.util.Arrays;

import jdk.incubator.vector.VectorSpecies;
import rapaio.math.tensor.Layout;
import rapaio.math.tensor.Order;
import rapaio.math.tensor.Shape;
import rapaio.math.tensor.layout.StrideLayout;
import rapaio.util.collection.IntArrays;

/**
 * A loop stride descriptor contains the same information as a loop iterator, but in a precomputed form.
 * It is an alternative to loop iterator, when you want precomputed loop offsets.
 */
public final class LoopDescriptor<N extends Number> {

    public static <N extends Number> LoopDescriptor<N> of(Shape shape, int offset, int[] strides, Order askOrder, VectorSpecies<N> vs) {
        return new LoopDescriptor<>(shape, offset, strides, askOrder, vs);
    }

    public static <N extends Number> LoopDescriptor<N> of(Layout layout, Order askOrder, VectorSpecies<N> vs) {
        return new LoopDescriptor<>(layout, askOrder, vs);
    }

    public final int size;
    public final int step;
    public final int count;
    public final int[] offsets;
    public final VectorSpecies<N> vs;
    public final int simdLen;
    public final int simdBound;
    public final int[] simdOffsets;

    private LoopDescriptor(Shape shape, int offset, int[] strides, Order askOrder, VectorSpecies<N> vs) {
        this(StrideLayout.of(shape, offset, strides), askOrder, vs);
    }

    private LoopDescriptor(Layout layout, Order askOrder, VectorSpecies<N> vs) {
        if (!(layout instanceof StrideLayout sl)) {
            throw new IllegalArgumentException("Layout is not a stride layout.");
        }
        this.vs = vs;
        this.simdLen = vs.length();

        if (layout.shape().rank() == 0) {
            size = 1;
            step = 1;
            count = 1;
            simdBound = 1;
            offsets = new int[] {sl.offset()};
            simdOffsets = simdOffsets(1);
            return;
        }

        var compact = sl.computeFortranLayout(askOrder, true);
        size = compact.dim(0);
        step = compact.stride(0);
        simdBound = vs.loopBound(size);
        simdOffsets = simdOffsets(vs.length());

        if (compact.rank() == 1) {
            count = 1;
            offsets = new int[] {sl.offset()};
            return;
        }

        int[] outerDims = Arrays.copyOfRange(compact.shape().dims(), 1, compact.shape().rank());
        int[] outerStrides = Arrays.copyOfRange(compact.strides(), 1, compact.shape().rank());

        this.count = IntArrays.prod(outerDims, 0, outerDims.length);
        this.offsets = IntArrays.newFill(count, sl.offset());

        int inner = 1;
        for (int i = 0; i < outerDims.length; i++) {
            int dim = outerDims[i];
            int stride = outerStrides[i];
            int pos = 0;
            while (pos < offsets.length) {
                int value = 0;
                for (int k = 0; k < dim; k++) {
                    for (int j = 0; j < inner; j++) {
                        offsets[pos + j] += value;
                    }
                    value += stride;
                    pos += inner;
                }
            }
            inner *= dim;
        }
    }

    private int[] simdOffsets(int len) {
        int[] offsetMap = new int[len];
        for (int i = 0; i < len; i++) {
            offsetMap[i] = i * step;
        }
        return offsetMap;
    }
}
