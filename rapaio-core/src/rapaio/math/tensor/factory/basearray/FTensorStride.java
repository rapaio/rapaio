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

package rapaio.math.tensor.factory.basearray;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import rapaio.math.tensor.FTensor;
import rapaio.math.tensor.Order;
import rapaio.math.tensor.Shape;
import rapaio.math.tensor.TensorFactory;
import rapaio.math.tensor.factory.AbstractTensor;
import rapaio.math.tensor.iterators.ChunkIterator;
import rapaio.math.tensor.iterators.DensePointerIterator;
import rapaio.math.tensor.iterators.PointerIterator;
import rapaio.math.tensor.iterators.ScalarChunkIterator;
import rapaio.math.tensor.iterators.StrideChunkIterator;
import rapaio.math.tensor.iterators.StridePointerIterator;
import rapaio.math.tensor.layout.StrideLayout;
import rapaio.util.collection.IntArrays;
import rapaio.util.function.IntIntBiFunction;

public sealed class FTensorStride extends AbstractTensor<Float, FTensor>
        implements FTensor permits rapaio.math.tensor.factory.parallelarray.FTensorStride {

    protected final StrideLayout layout;
    protected final TensorFactory factory;
    protected final float[] array;

    public FTensorStride(TensorFactory factory, StrideLayout layout, float[] array) {
        this.layout = layout;
        this.factory = factory;
        this.array = array;
    }

    public FTensorStride(TensorFactory factory, Shape shape, int offset, int[] strides, float[] array) {
        this(factory, StrideLayout.of(shape, offset, strides), array);
    }

    public FTensorStride(TensorFactory factory, Shape shape, int offset, Order order, float[] array) {
        this(factory, StrideLayout.ofDense(shape, offset, order), array);
    }

    @Override
    public TensorFactory factory() {
        return factory;
    }

    public float[] array() {
        return array;
    }

    @Override
    public StrideLayout layout() {
        return layout;
    }

    @Override
    public float get(int... indexes) {
        return array[layout.pointer(indexes)];
    }

    @Override
    public void set(float value, int... indexes) {
        array[layout.pointer(indexes)] = value;
    }

    @Override
    public float ptrGet(int ptr) {
        return array[ptr];
    }

    @Override
    public void ptrSet(int ptr, float value) {
        array[ptr] = value;
    }

    @Override
    public FTensor abs_() {
        var it = pointerIterator(Order.A);
        while (it.hasNext()) {
            int pos = it.nextInt();
            array[pos] = Math.abs(array[pos]);
        }
        return this;
    }

    @Override
    public FTensor neg_() {
        var it = pointerIterator(Order.A);
        while (it.hasNext()) {
            int pos = it.nextInt();
            array[pos] = -array[pos];
        }
        return this;
    }

    @Override
    public FTensor log_() {
        var it = pointerIterator(Order.A);
        while (it.hasNext()) {
            int pos = it.nextInt();
            array[pos] = (float) Math.log(array[pos]);
        }
        return this;
    }

    @Override
    public FTensor log1p_() {
        var it = pointerIterator(Order.A);
        while (it.hasNext()) {
            int pos = it.nextInt();
            array[pos] = (float) Math.log1p(array[pos]);
        }
        return this;
    }

    @Override
    public FTensor exp_() {
        var it = pointerIterator(Order.A);
        while (it.hasNext()) {
            int pos = it.nextInt();
            array[pos] = (float) Math.exp(array[pos]);
        }
        return this;
    }

    @Override
    public FTensor expm1_() {
        var it = pointerIterator(Order.A);
        while (it.hasNext()) {
            int pos = it.nextInt();
            array[pos] = (float) Math.expm1(array[pos]);
        }
        return this;
    }

    @Override
    public FTensor sin_() {
        var it = pointerIterator(Order.A);
        while (it.hasNext()) {
            int pos = it.nextInt();
            array[pos] = (float) Math.sin(array[pos]);
        }
        return this;
    }

    @Override
    public FTensor asin_() {
        var it = pointerIterator(Order.A);
        while (it.hasNext()) {
            int pos = it.nextInt();
            array[pos] = (float) Math.sin(array[pos]);
        }
        return this;
    }

    @Override
    public FTensor sinh_() {
        var it = pointerIterator(Order.A);
        while (it.hasNext()) {
            int pos = it.nextInt();
            array[pos] = (float) Math.sinh(array[pos]);
        }
        return this;
    }

    @Override
    public FTensor cos_() {
        var it = pointerIterator(Order.A);
        while (it.hasNext()) {
            int pos = it.nextInt();
            array[pos] = (float) Math.cos(array[pos]);
        }
        return this;
    }

    @Override
    public FTensor acos_() {
        var it = pointerIterator(Order.A);
        while (it.hasNext()) {
            int pos = it.nextInt();
            array[pos] = (float) Math.acos(array[pos]);
        }
        return this;
    }

    @Override
    public FTensor cosh_() {
        var it = pointerIterator(Order.A);
        while (it.hasNext()) {
            int pos = it.nextInt();
            array[pos] = (float) Math.cosh(array[pos]);
        }
        return this;
    }

    @Override
    public FTensor tan_() {
        var it = pointerIterator(Order.A);
        while (it.hasNext()) {
            int pos = it.nextInt();
            array[pos] = (float) Math.tan(array[pos]);
        }
        return this;
    }

    @Override
    public FTensor atan_() {
        var it = pointerIterator(Order.A);
        while (it.hasNext()) {
            int pos = it.nextInt();
            array[pos] = (float) Math.atan(array[pos]);
        }
        return this;
    }

    @Override
    public FTensor tanh_() {
        var it = pointerIterator(Order.A);
        while (it.hasNext()) {
            int pos = it.nextInt();
            array[pos] = (float) Math.tanh(array[pos]);
        }
        return this;
    }

    private void validateSameShape(FTensor tensor) {
        if (!shape().equals(tensor.shape())) {
            throw new IllegalArgumentException("Shapes does not match.");
        }
    }

    @Override
    public FTensor add_(FTensor tensor) {
        validateSameShape(tensor);

        var order = layout.storageFastOrder();
        order = order == Order.C || order == Order.F ? order : Order.defaultOrder();

        var it = pointerIterator(order);
        var refIt = tensor.pointerIterator(order);
        while (it.hasNext()) {
            array[it.nextInt()] += tensor.ptrGetValue(refIt.nextInt());
        }
        return this;
    }

    @Override
    public FTensor sub_(FTensor tensor) {
        validateSameShape(tensor);

        var order = layout.storageFastOrder();
        order = order == Order.C || order == Order.F ? order : Order.defaultOrder();

        var it = pointerIterator(order);
        var refIt = tensor.pointerIterator(order);
        while (it.hasNext()) {
            array[it.nextInt()] -= tensor.ptrGetValue(refIt.nextInt());
        }
        return this;
    }

    @Override
    public FTensor mul_(FTensor tensor) {
        validateSameShape(tensor);

        var order = layout.storageFastOrder();
        order = order == Order.C || order == Order.F ? order : Order.defaultOrder();

        var it = pointerIterator(order);
        var refIt = tensor.pointerIterator(order);
        while (it.hasNext()) {
            array[it.nextInt()] *= tensor.ptrGetValue(refIt.nextInt());
        }
        return this;
    }

    @Override
    public FTensor div_(FTensor tensor) {
        validateSameShape(tensor);

        var order = layout.storageFastOrder();
        order = order == Order.C || order == Order.F ? order : Order.defaultOrder();

        var it = pointerIterator(order);
        var refIt = tensor.pointerIterator(order);
        while (it.hasNext()) {
            array[it.nextInt()] /= tensor.ptrGetValue(refIt.nextInt());
        }
        return this;
    }

    @Override
    public FTensor add_(float value) {
        var it = pointerIterator(layout.storageFastOrder());
        while (it.hasNext()) {
            array[it.nextInt()] += value;
        }
        return this;
    }

    @Override
    public FTensor sub_(float value) {
        var it = pointerIterator(layout.storageFastOrder());
        while (it.hasNext()) {
            array[it.nextInt()] -= value;
        }
        return this;
    }

    @Override
    public FTensor mul_(float value) {
        var it = pointerIterator(layout.storageFastOrder());
        while (it.hasNext()) {
            array[it.nextInt()] *= value;
        }
        return this;
    }

    @Override
    public FTensor div_(float value) {
        var it = pointerIterator(layout.storageFastOrder());
        while (it.hasNext()) {
            array[it.nextInt()] /= value;
        }
        return this;
    }

    @Override
    public FTensor mv(FTensor tensor) {
        if (shape().rank() != 2 || tensor.shape().rank() != 1 || shape().dim(1) != tensor.shape().dim(0)) {
            throw new RuntimeException("Operands are not valid for matrix-vector multiplication "
                    + "(m = %s, v = %s).".formatted(shape().toString(), tensor.shape().toString()));
        }
        float[] result = new float[shape().dim(0)];
        var it = pointerIterator(Order.C);
        for (int i = 0; i < shape().dim(0); i++) {
            var innerIt = tensor.pointerIterator(Order.C);
            float sum = 0;
            for (int j = 0; j < shape().dim(1); j++) {
                sum += ptrGet(it.nextInt()) * tensor.ptrGet(innerIt.nextInt());
            }
            result[i] = sum;
        }
        StrideLayout layout = StrideLayout.ofDense(Shape.of(shape().dim(0)), 0, Order.C);
        return factory.ofFloat().stride(layout, result);
    }

    @Override
    public FTensor mm(FTensor tensor) {
        if (shape().rank() != 2 || tensor.shape().rank() != 2 || shape().dim(1) != tensor.shape().dim(0)) {
            throw new RuntimeException("Operands are not valid for matrix-matrix multiplication "
                    + "(m = %s, v = %s).".formatted(shape().toString(), tensor.shape().toString()));
        }
        float[] result = new float[shape().dim(0) * tensor.shape().dim(1)];

        List<FTensor> rows = slice(0, 1);
        List<FTensor> cols = tensor.slice(1, 1);

        for (int i = 0; i < rows.size(); i++) {
            for (int j = 0; j < cols.size(); j++) {
                var it1 = rows.get(i).squeeze().iterator();
                var it2 = cols.get(j).squeeze().iterator();
                float sum = 0;
                while (it1.hasNext() && it2.hasNext()) {
                    sum += it1.next() * it2.next();
                }
                result[i * cols.size() + j] = sum;
            }
        }
        StrideLayout layout = StrideLayout.ofDense(Shape.of(shape().dim(0), tensor.shape().dim(1)), 0, Order.C);
        return factory.ofFloat().stride(layout, result);
    }

    @Override
    public FTensor matmul(FTensor tensor) {
        // TODO: implement
        return null;
    }

    @Override
    public Iterator<Float> iterator() {
        return iterator(Order.A);
    }

    @Override
    public Iterator<Float> iterator(Order askOrder) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(pointerIterator(askOrder), Spliterator.ORDERED), false)
                .map(i -> array[i]).iterator();
    }

    @Override
    public FTensorStride iteratorApply(Order askOrder, IntIntBiFunction<Float> apply) {
        var it = pointerIterator(askOrder);
        int i = 0;
        while (it.hasNext()) {
            int p = it.nextInt();
            array[p] = apply.applyAsInt(i++, p);
        }
        return this;
    }

    @Override
    public PointerIterator pointerIterator(Order askOrder) {
        if (layout.isCOrdered() && askOrder == Order.C) {
            return new DensePointerIterator(layout.shape(), layout.offset(), layout.stride(-1));
        }
        if (layout.isFOrdered() && askOrder == Order.F) {
            return new DensePointerIterator(layout.shape(), layout.offset(), layout.stride(0));
        }
        return new StridePointerIterator(layout, askOrder);
    }

    @Override
    public ChunkIterator chunkIterator(Order askOrder) {
        if (layout.rank() == 0) {
            return new ScalarChunkIterator(layout.offset());
        }
        return new StrideChunkIterator(layout, askOrder);
    }

    @Override
    public FTensor reshape(Shape askShape, Order askOrder) {
        if (layout.shape().size() != askShape.size()) {
            throw new IllegalArgumentException("Incompatible shape size.");
        }

        Order cmpOrder = askOrder == Order.C ? Order.C : Order.F;
        var baseLayout = layout.computeFortranLayout(cmpOrder, true);
        var compact = StrideLayout.ofDense(shape(), layout.offset(), cmpOrder).computeFortranLayout(cmpOrder, true);

        if (baseLayout.equals(compact)) {
            // we can create a view over tensor
            int newOffset = layout.offset();
            int[] newStrides = new int[askShape.rank()];
            for (int i = 0; i < askShape.rank(); i++) {
                int[] ione = new int[askShape.rank()];
                ione[i] = 1;
                int pos2 = askShape.position(cmpOrder, ione);
                int[] v1 = layout.shape().index(cmpOrder, pos2);
                int pointer2 = layout.pointer(v1);
                newStrides[i] = pointer2 - newOffset;
            }
            if (askOrder == Order.C) {
                IntArrays.reverse(newStrides);
            }
        }
        var it = new StridePointerIterator(layout, askOrder);
        FTensor copy = factory.ofFloat().zeros(askShape, askOrder);
        var copyIt = copy.pointerIterator(Order.C);
        while (it.hasNext()) {
            copy.ptrSet(copyIt.nextInt(), array[it.nextInt()]);
        }
        return copy;
    }

    @Override
    public FTensor ravel(Order askOrder) {
        var compact = layout.computeFortranLayout(askOrder, true);
        if (compact.shape().rank() == 1) {
            return factory.ofFloat().stride(compact, array);
        }
        return flatten(askOrder);
    }

    @Override
    public FTensor flatten(Order askOrder) {
        askOrder = Order.autoFC(askOrder);
        var out = new float[layout.size()];
        int p = 0;
        var it = chunkIterator(askOrder);
        while (it.hasNext()) {
            int pointer = it.nextInt();
            for (int i = pointer; i < pointer + it.loopBound(); i += it.loopStep()) {
                out[p++] = array[i];
            }
        }
        return factory.ofFloat().stride(Shape.of(layout.size()), 0, new int[] {1}, out);
    }

    @Override
    public FTensor squeeze() {
        return layout.shape().unitDimCount() == 0 ? this : factory.ofFloat().stride(layout.squeeze(), array);
    }

    @Override
    public FTensor unsqueeze(int axis) {
        return factory.ofFloat().stride(layout().unsqueeze(axis), array);
    }

    @Override
    public FTensor t() {
        return factory.ofFloat().stride(layout.revert(), array);
    }

    @Override
    public FTensor moveAxis(int src, int dst) {
        return factory.ofFloat().stride(layout.moveAxis(src, dst), array);
    }

    @Override
    public FTensor swapAxis(int src, int dst) {
        return factory.ofFloat().stride(layout.swapAxis(src, dst), array);
    }

    @Override
    public FTensor truncate(int axis, int start, int end) {
        if (axis < 0 || axis >= layout.rank()) {
            throw new IllegalArgumentException("Axis is out of bounds.");
        }
        int[] newDims = Arrays.copyOf(shape().dims(), shape().rank());
        newDims[axis] = end - start;
        int newOffset = layout().offset() + start * layout.stride(axis);
        int[] newStrides = Arrays.copyOf(layout.strides(), layout.rank());

        StrideLayout copyLayout = StrideLayout.of(Shape.of(newDims), newOffset, newStrides);
        return factory.ofFloat().stride(copyLayout, array);
    }

    @Override
    public List<FTensor> split(int axis, int... indexes) {
        return IntStream
                .range(0, indexes.length)
                .mapToObj(i -> truncate(axis, indexes[i], i < indexes.length - 1 ? indexes[i + 1] : shape().dim(axis)))
                .collect(Collectors.toList());
    }

    @Override
    public FTensor repeat(int axis, int repeat, boolean stack) {
        FTensor[] copies = new FTensor[repeat];
        Arrays.fill(copies, this);
        if (stack) {
            return factory.ofFloat().stack(axis, copies);
        } else {
            return factory.ofFloat().concatenate(axis, copies);
        }
    }

    @Override
    public FTensor copy(Order askOrder) {
        askOrder = Order.autoFC(askOrder);

        var copy = factory.ofFloat().zeros(shape(), askOrder);
        var it1 = chunkIterator(askOrder);
        var it2 = copy.pointerIterator(askOrder);
        while (it1.hasNext()) {
            int pointer = it1.nextInt();
            for (int i = pointer; i < pointer + it1.loopBound(); i += it1.loopStep()) {
                copy.ptrSet(it2.nextInt(), ptrGet(i));
            }
        }
        return copy;
    }
}
