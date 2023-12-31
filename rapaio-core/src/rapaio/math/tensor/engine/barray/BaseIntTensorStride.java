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

package rapaio.math.tensor.engine.barray;

import static java.lang.Math.ceil;
import static java.lang.Math.floor;
import static java.lang.Math.sqrt;

import static rapaio.util.Hardware.CORES;
import static rapaio.util.Hardware.L2_CACHE_SIZE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.Stack;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import rapaio.math.tensor.IntTensor;
import rapaio.math.tensor.Order;
import rapaio.math.tensor.Shape;
import rapaio.math.tensor.Statistics;
import rapaio.math.tensor.TensorEngine;
import rapaio.math.tensor.engine.AbstractTensor;
import rapaio.math.tensor.engine.varray.VectorizedIntTensorStride;
import rapaio.math.tensor.iterators.DensePointerIterator;
import rapaio.math.tensor.iterators.LoopIterator;
import rapaio.math.tensor.iterators.PointerIterator;
import rapaio.math.tensor.iterators.ScalarLoopIterator;
import rapaio.math.tensor.iterators.StrideLoopDescriptor;
import rapaio.math.tensor.iterators.StrideLoopIterator;
import rapaio.math.tensor.iterators.StridePointerIterator;
import rapaio.math.tensor.layout.StrideLayout;
import rapaio.math.tensor.operator.TensorAssociativeOp;
import rapaio.math.tensor.operator.TensorBinaryOp;
import rapaio.math.tensor.operator.TensorUnaryOp;
import rapaio.util.NotImplementedException;
import rapaio.util.collection.IntArrays;
import rapaio.util.function.IntIntBiFunction;

public sealed class BaseIntTensorStride extends AbstractTensor<Integer, IntTensor> implements IntTensor
        permits VectorizedIntTensorStride {

    protected final StrideLayout layout;
    protected final TensorEngine engine;
    protected final int[] array;
    protected final StrideLoopDescriptor loop;

    public BaseIntTensorStride(TensorEngine engine, Shape shape, int offset, int[] strides, int[] array) {
        this(engine, StrideLayout.of(shape, offset, strides), array);
    }

    public BaseIntTensorStride(TensorEngine engine, Shape shape, int offset, Order order, int[] array) {
        this(engine, StrideLayout.ofDense(shape, offset, order), array);
    }

    public BaseIntTensorStride(TensorEngine engine, StrideLayout layout, int[] array) {
        this.layout = layout;
        this.engine = engine;
        this.array = array;
        this.loop = StrideLoopDescriptor.of(layout, layout.storageFastOrder());
    }

    @Override
    public TensorEngine engine() {
        return engine;
    }

    @Override
    public StrideLayout layout() {
        return layout;
    }

    @Override
    public IntTensor reshape(Shape askShape, Order askOrder) {
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
        IntTensor copy = engine.ofInt().zeros(askShape, askOrder);
        var copyIt = copy.ptrIterator(Order.C);
        while (it.hasNext()) {
            copy.ptrSetInteger(copyIt.nextInt(), array[it.nextInt()]);
        }
        return copy;
    }

    @Override
    public IntTensor transpose() {
        return engine.ofInt().stride(layout.revert(), array);
    }

    @Override
    public IntTensor ravel(Order askOrder) {
        var compact = layout.computeFortranLayout(askOrder, true);
        if (compact.shape().rank() == 1) {
            return engine.ofInt().stride(compact, array);
        }
        return flatten(askOrder);
    }

    @Override
    public IntTensor flatten(Order askOrder) {
        askOrder = Order.autoFC(askOrder);
        var out = new int[layout.size()];
        int p = 0;
        var it = loopIterator(askOrder);
        while (it.hasNext()) {
            int pointer = it.nextInt();
            for (int i = pointer; i < pointer + it.bound(); i += it.step()) {
                out[p++] = array[i];
            }
        }
        return engine.ofInt().stride(Shape.of(layout.size()), 0, new int[] {1}, out);
    }

    @Override
    public IntTensor squeeze() {
        return layout.shape().unitDimCount() == 0 ? this : engine.ofInt().stride(layout.squeeze(), array);
    }

    @Override
    public IntTensor squeeze(int axis) {
        return layout.shape().dim(axis) != 1 ? this : engine.ofInt().stride(layout.squeeze(axis), array);
    }

    @Override
    public IntTensor unsqueeze(int axis) {
        return engine.ofInt().stride(layout().unsqueeze(axis), array);
    }

    @Override
    public IntTensor moveAxis(int src, int dst) {
        return engine.ofInt().stride(layout.moveAxis(src, dst), array);
    }

    @Override
    public IntTensor swapAxis(int src, int dst) {
        return engine.ofInt().stride(layout.swapAxis(src, dst), array);
    }

    @Override
    public IntTensor narrow(int axis, boolean keepdim, int start, int end) {
        return engine.ofInt().stride(layout.narrow(axis, keepdim, start, end), array);
    }

    @Override
    public IntTensor narrowAll(boolean keepdim, int[] starts, int[] ends) {
        return engine.ofInt().stride(layout.narrowAll(keepdim, starts, ends), array);
    }

    @Override
    public List<IntTensor> split(int axis, boolean keepdim, int... indexes) {
        List<IntTensor> result = new ArrayList<>(indexes.length);
        for (int i = 0; i < indexes.length; i++) {
            result.add(narrow(axis, keepdim, indexes[i], i < indexes.length - 1 ? indexes[i + 1] : shape().dim(axis)));
        }
        return result;
    }

    @Override
    public List<IntTensor> splitAll(boolean keepdim, int[][] indexes) {
        if (indexes.length != rank()) {
            throw new IllegalArgumentException(
                    "Indexes length of %d is not the same as shape rank %d.".formatted(indexes.length, rank()));
        }
        List<IntTensor> results = new ArrayList<>();
        int[] starts = new int[indexes.length];
        int[] ends = new int[indexes.length];
        splitAllRec(results, indexes, keepdim, starts, ends, 0);
        return results;
    }

    private void splitAllRec(List<IntTensor> results, int[][] indexes, boolean keepdim, int[] starts, int[] ends, int level) {
        if (level == indexes.length) {
            return;
        }
        for (int i = 0; i < indexes[level].length; i++) {
            starts[level] = indexes[level][i];
            ends[level] = i < indexes[level].length - 1 ? indexes[level][i + 1] : shape().dim(level);
            if (level == indexes.length - 1) {
                results.add(narrowAll(keepdim, starts, ends));
            } else {
                splitAllRec(results, indexes, keepdim, starts, ends, level + 1);
            }
        }
    }

    @Override
    public IntTensor repeat(int axis, int repeat, boolean stack) {
        IntTensor[] copies = new IntTensor[repeat];
        Arrays.fill(copies, this);
        if (stack) {
            return engine.stack(axis, Arrays.asList(copies));
        } else {
            return engine.concat(axis, Arrays.asList(copies));
        }
    }

    @Override
    public IntTensor tile(int[] repeats) {
        throw new NotImplementedException();
    }

    @Override
    public IntTensor expand(int axis, int dim) {
        if (layout.dim(axis) != 1) {
            throw new IllegalArgumentException(STR."Dimension \{axis} does not have dimension 1.");
        }
        if (dim < 1) {
            throw new IllegalArgumentException(STR."Dimension of the new axis \{dim} must be positive.");
        }
        int[] newDims = Arrays.copyOf(layout.dims(), layout.dims().length);
        int[] newStrides = Arrays.copyOf(layout.strides(), layout.strides().length);

        newDims[axis] = dim;
        newStrides[axis] = 0;
        return engine.ofInt().stride(StrideLayout.of(Shape.of(newDims), layout.offset(), newStrides), array);
    }

    @Override
    public IntTensor permute(int[] dims) {
        return engine.ofInt().stride(layout().permute(dims), array);
    }

    @Override
    public int getInt(int... indexes) {
        return array[layout.pointer(indexes)];
    }

    @Override
    public void setInt(int value, int... indexes) {
        array[layout.pointer(indexes)] = value;
    }

    @Override
    public int ptrGetInteger(int ptr) {
        return array[ptr];
    }

    @Override
    public void ptrSetInteger(int ptr, int value) {
        array[ptr] = value;
    }

    @Override
    public Iterator<Integer> iterator(Order askOrder) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(ptrIterator(askOrder), Spliterator.ORDERED), false)
                .map(i -> array[i]).iterator();
    }

    @Override
    public PointerIterator ptrIterator(Order askOrder) {
        if (layout.isCOrdered() && askOrder != Order.F) {
            return new DensePointerIterator(layout.shape(), layout.offset(), layout.stride(-1));
        }
        if (layout.isFOrdered() && askOrder != Order.C) {
            return new DensePointerIterator(layout.shape(), layout.offset(), layout.stride(0));
        }
        return new StridePointerIterator(layout, askOrder);
    }

    @Override
    public LoopIterator loopIterator(Order askOrder) {
        if (layout.rank() == 0) {
            return new ScalarLoopIterator(layout.offset());
        }
        return new StrideLoopIterator(layout, askOrder);
    }

    @Override
    public BaseIntTensorStride apply_(Order askOrder, IntIntBiFunction<Integer> apply) {
        var it = ptrIterator(askOrder);
        int i = 0;
        while (it.hasNext()) {
            int p = it.nextInt();
            array[p] = apply.applyAsInt(i++, p);
        }
        return this;
    }

    @Override
    public IntTensor apply_(Function<Integer, Integer> fun) {
        var ptrIter = ptrIterator(Order.S);
        while (ptrIter.hasNext()) {
            int ptr = ptrIter.nextInt();
            array[ptr] = fun.apply(array[ptr]);
        }
        return this;
    }

    @Override
    public IntTensor fill_(Integer value) {
        for (int offset : loop.offsets) {
            for (int i = offset; i < loop.bound + offset; i += loop.step) {
                array[i] = value;
            }
        }
        return this;
    }

    @Override
    public IntTensor fillNan_(Integer value) {
        for (int offset : loop.offsets) {
            for (int i = offset; i < loop.bound + offset; i += loop.step) {
                if (dtype().isNaN(array[i])) {
                    array[i] = value;
                }
            }
        }
        return this;
    }

    @Override
    public IntTensor clamp_(Integer min, Integer max) {
        for (int offset : loop.offsets) {
            for (int i = offset; i < loop.bound + offset; i += loop.step) {
                if (!dtype().isNaN(min) && array[i] < min) {
                    array[i] = min;
                }
                if (!dtype().isNaN(max) && array[i] > max) {
                    array[i] = max;
                }
            }
        }
        return this;
    }

    @Override
    public IntTensor take(Order order, int... indexes) {
        throw new NotImplementedException();
    }

    private void unaryOpStep(TensorUnaryOp op) {
        for (int off : loop.offsets) {
            for (int i = off; i < loop.bound + off; i += loop.step) {
                array[i] = op.applyInt(array[i]);
            }
        }
    }

    protected void unaryOp(TensorUnaryOp op) {
        if (op.isFloatOnly() && !dtype().isFloat()) {
            throw new IllegalArgumentException("This operation is available only for floating point tensors.");
        }
        unaryOpStep(op);
    }

    @Override
    public BaseIntTensorStride abs_() {
        unaryOp(TensorUnaryOp.ABS);
        return this;
    }

    @Override
    public BaseIntTensorStride negate_() {
        unaryOp(TensorUnaryOp.NEG);
        return this;
    }

    @Override
    public BaseIntTensorStride log_() {
        unaryOp(TensorUnaryOp.LOG);
        return this;
    }

    @Override
    public BaseIntTensorStride log1p_() {
        unaryOp(TensorUnaryOp.LOG1P);
        return this;
    }

    @Override
    public BaseIntTensorStride exp_() {
        unaryOp(TensorUnaryOp.EXP);
        return this;
    }

    @Override
    public BaseIntTensorStride expm1_() {
        unaryOp(TensorUnaryOp.EXPM1);
        return this;
    }

    @Override
    public BaseIntTensorStride sin_() {
        unaryOp(TensorUnaryOp.SIN);
        return this;
    }

    @Override
    public BaseIntTensorStride asin_() {
        unaryOp(TensorUnaryOp.ASIN);
        return this;
    }

    @Override
    public BaseIntTensorStride sinh_() {
        unaryOp(TensorUnaryOp.SINH);
        return this;
    }

    @Override
    public BaseIntTensorStride cos_() {
        unaryOp(TensorUnaryOp.COS);
        return this;
    }

    @Override
    public BaseIntTensorStride acos_() {
        unaryOp(TensorUnaryOp.ACOS);
        return this;
    }

    @Override
    public BaseIntTensorStride cosh_() {
        unaryOp(TensorUnaryOp.COSH);
        return this;
    }

    @Override
    public BaseIntTensorStride tan_() {
        unaryOp(TensorUnaryOp.TAN);
        return this;
    }

    @Override
    public BaseIntTensorStride atan_() {
        unaryOp(TensorUnaryOp.ATAN);
        return this;
    }

    @Override
    public BaseIntTensorStride tanh_() {
        unaryOp(TensorUnaryOp.TANH);
        return this;
    }

    protected void binaryVectorOp(TensorBinaryOp op, IntTensor b) {
        if (b.isScalar()) {
            binaryScalarOp(op, b.getInt());
            return;
        }
        if (!shape().equals(b.shape())) {
            throw new IllegalArgumentException("Tensors does not have the same shape.");
        }
        var order = layout.storageFastOrder();
        order = order == Order.S ? Order.defaultOrder() : order;

        var it = ptrIterator(order);
        var refIt = b.ptrIterator(order);
        while (it.hasNext()) {
            int next = it.nextInt();
            array[next] = op.applyInt(array[next], b.ptrGet(refIt.nextInt()));
        }
    }

    @Override
    public BaseIntTensorStride add_(IntTensor tensor) {
        binaryVectorOp(TensorBinaryOp.ADD, tensor);
        return this;
    }

    @Override
    public BaseIntTensorStride sub_(IntTensor tensor) {
        binaryVectorOp(TensorBinaryOp.SUB, tensor);
        return this;
    }

    @Override
    public BaseIntTensorStride mul_(IntTensor tensor) {
        binaryVectorOp(TensorBinaryOp.MUL, tensor);
        return this;
    }

    @Override
    public BaseIntTensorStride div_(IntTensor tensor) {
        binaryVectorOp(TensorBinaryOp.DIV, tensor);
        return this;
    }

    void binaryScalarOpStep(TensorBinaryOp op, int value) {
        for (int offset : loop.offsets) {
            for (int i = offset; i < loop.bound + offset; i += loop.step) {
                array[i] = op.applyInt(array[i], value);
            }
        }
    }

    protected void binaryScalarOp(TensorBinaryOp op, int value) {
        binaryScalarOpStep(op, value);
    }

    @Override
    public BaseIntTensorStride add_(Integer value) {
        binaryScalarOp(TensorBinaryOp.ADD, value);
        return this;
    }

    @Override
    public BaseIntTensorStride sub_(Integer value) {
        binaryScalarOp(TensorBinaryOp.SUB, value);
        return this;
    }

    @Override
    public BaseIntTensorStride mul_(Integer value) {
        binaryScalarOp(TensorBinaryOp.MUL, value);
        return this;
    }

    @Override
    public BaseIntTensorStride div_(Integer value) {
        binaryScalarOp(TensorBinaryOp.DIV, value);
        return this;
    }

    @Override
    public BaseIntTensorStride fma_(Integer a, IntTensor t) {
        int aVal = a;
        if (t.isScalar()) {
            var order = layout.storageFastOrder();
            var it = ptrIterator(order);
            int tVal = t.getInt(0);
            while (it.hasNext()) {
                int next = it.nextInt();
                array[next] = (int) Math.fma(array[next], aVal, tVal);
            }
            return this;
        }
        if (!shape().equals(t.shape())) {
            throw new IllegalArgumentException("Tensors does not have the same shape.");
        }
        var order = layout.storageFastOrder();
        order = order == Order.S ? Order.defaultOrder() : order;

        var it = ptrIterator(order);
        var refIt = t.ptrIterator(order);
        while (it.hasNext()) {
            int next = it.nextInt();
            array[next] = (int) Math.fma(array[next], aVal, t.ptrGet(refIt.nextInt()));
        }
        return this;
    }

    @Override
    public Integer vdot(IntTensor tensor) {
        return vdot(tensor, 0, shape().dim(0));
    }

    @Override
    public Integer vdot(IntTensor tensor, int start, int end) {
        if (shape().rank() != 1 || tensor.shape().rank() != 1 || shape().dim(0) != tensor.shape().dim(0)) {
            throw new IllegalArgumentException(
                    "Operands are not valid for vector dot product (v = %s, v = %s)."
                            .formatted(shape().toString(), tensor.shape().toString()));
        }
        if (start >= end || start < 0 || end > tensor.shape().dim(0)) {
            throw new IllegalArgumentException("Start and end indexes are invalid (start: %d, end: %s).".formatted(start, end));
        }
        BaseIntTensorStride dts = (BaseIntTensorStride) tensor;
        int step1 = layout.stride(0);
        int step2 = dts.layout.stride(0);

        int start1 = layout.offset() + start * step1;
        int end1 = layout.offset() + end * step1;
        int start2 = dts.layout.offset() + start * step2;

        int sum = 0;
        for (int i = start1; i < end1; i += step1) {
            sum += (int) (array[i] * dts.array[start2]);
            start2 += step2;
        }
        return sum;
    }

    @Override
    public IntTensor mv(IntTensor tensor) {
        if (shape().rank() != 2 || tensor.shape().rank() != 1 || shape().dim(1) != tensor.shape().dim(0)) {
            throw new IllegalArgumentException(
                    STR."Operands are not valid for matrix-vector multiplication \{"(m = %s, v = %s).".formatted(shape(),
                            tensor.shape())}");
        }
        int[] result = new int[shape().dim(0)];
        var it = ptrIterator(Order.C);
        for (int i = 0; i < shape().dim(0); i++) {
            var innerIt = tensor.ptrIterator(Order.C);
            int sum = 0;
            for (int j = 0; j < shape().dim(1); j++) {
                sum += (int) (ptrGetInteger(it.nextInt()) * tensor.ptrGetInteger(innerIt.nextInt()));
            }
            result[i] = sum;
        }
        StrideLayout layout = StrideLayout.ofDense(Shape.of(shape().dim(0)), 0, Order.C);
        return engine.ofInt().stride(layout, result);
    }

    @Override
    public IntTensor mm(IntTensor t, Order askOrder) {
        if (shape().rank() != 2 || t.shape().rank() != 2 || shape().dim(1) != t.shape().dim(0)) {
            throw new IllegalArgumentException(
                    STR."Operands are not valid for matrix-matrix multiplication \{"(m = %s, v = %s).".formatted(shape(), t.shape())}");
        }
        if (askOrder == Order.S) {
            throw new IllegalArgumentException("Illegal askOrder value, must be Order.C or Order.F");
        }
        int m = shape().dim(0);
        int n = shape().dim(1);
        int p = t.shape().dim(1);

        var result = new int[m * p];
        var ret = engine.ofInt().stride(StrideLayout.ofDense(Shape.of(m, p), 0, askOrder), result);

        List<IntTensor> rows = chunk(0, false, 1);
        List<IntTensor> cols = t.chunk(1, false, 1);

        int chunk = (int) floor(sqrt(L2_CACHE_SIZE / 2. / CORES / dtype().bytes()));
        chunk = chunk >= 8 ? chunk - chunk % 8 : chunk;

        int vectorChunk = chunk > 64 ? chunk * 4 : chunk;
        int innerChunk = chunk > 64 ? (int) ceil(sqrt(chunk / 4.)) : (int) ceil(sqrt(chunk));

        int iStride = ((StrideLayout) ret.layout()).stride(0);
        int jStride = ((StrideLayout) ret.layout()).stride(1);

        List<Future<?>> futures = new ArrayList<>();
        try (ExecutorService service = Executors.newFixedThreadPool(engine.cpuThreads())) {
            for (int r = 0; r < m; r += innerChunk) {
                int rs = r;
                int re = Math.min(m, r + innerChunk);

                futures.add(service.submit(() -> {
                    for (int c = 0; c < p; c += innerChunk) {
                        int ce = Math.min(p, c + innerChunk);

                        for (int k = 0; k < n; k += vectorChunk) {
                            int end = Math.min(n, k + vectorChunk);
                            for (int i = rs; i < re; i++) {
                                var krow = (BaseIntTensorStride) rows.get(i);
                                for (int j = c; j < ce; j++) {
                                    result[i * iStride + j * jStride] += krow.vdot(cols.get(j), k, end);
                                }
                            }
                        }
                    }
                    return null;
                }));
            }

            try {
                for (var future : futures) {
                    future.get();
                }
                service.shutdown();
                service.shutdownNow();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        return ret;
    }

    @Override
    public Statistics<Integer, IntTensor> stats() {
        if (!dtype().isFloat()) {
            throw new IllegalArgumentException("Operation available only for float tensors.");
        }

        int size = size();
        int nanSize = 0;
        int mean;
        int nanMean;
        int variance;
        int nanVariance;

        // first pass compute raw mean
        int sum = 0;
        int nanSum = 0;
        for (int offset : loop.offsets) {
            int i = offset;
            for (; i < loop.bound + offset; i += loop.step) {
                sum += array[i];
                if (!dtype().isNaN(array[i])) {
                    nanSum += array[i];
                    nanSize++;
                }
            }
        }
        mean = (int) (sum / size);
        nanMean = (int) (nanSum / nanSize);

        // second pass adjustments for mean
        sum = 0;
        nanSum = 0;
        for (int offset : loop.offsets) {
            int i = offset;
            for (; i < loop.bound + offset; i += loop.step) {
                sum += (int) (array[i] - mean);
                if (!dtype().isNaN(array[i])) {
                    nanSum += (int) (array[i] - nanMean);
                }
            }
        }
        mean += (int) (sum / size);
        nanMean += (int) (nanSum / nanSize);

        // third pass compute variance
        int sum2 = 0;
        int sum3 = 0;
        int nanSum2 = 0;
        int nanSum3 = 0;

        for (int offset : loop.offsets) {
            int i = offset;
            for (; i < loop.bound + offset; i += loop.step) {
                sum2 += (int) ((array[i] - mean) * (array[i] - mean));
                sum3 += (int) (array[i] - mean);
                if (!dtype().isNaN(array[i])) {
                    nanSum2 += (int) ((array[i] - nanMean) * (array[i] - nanMean));
                    nanSum3 += (int) (array[i] - nanMean);
                }
            }
        }
        variance = (int) ((sum2 - (sum3 * sum3) / size) / size);
        nanVariance = (int) ((nanSum2 - (nanSum3 * nanSum3) / nanSize) / nanSize);

        return new Statistics<>(dtype(), size, nanSize, mean, nanMean, variance, nanVariance);
    }

    @Override
    public Integer sum() {
        return associativeOp(TensorAssociativeOp.ADD);
    }

    @Override
    public Integer nanSum() {
        return nanAssociativeOp(TensorAssociativeOp.ADD);
    }

    @Override
    public Integer prod() {
        return associativeOp(TensorAssociativeOp.MUL);
    }

    @Override
    public Integer nanProd() {
        return nanAssociativeOp(TensorAssociativeOp.MUL);
    }

    @Override
    public Integer max() {
        return associativeOp(TensorAssociativeOp.MAX);
    }

    @Override
    public Integer nanMax() {
        return nanAssociativeOp(TensorAssociativeOp.MAX);
    }

    @Override
    public Integer min() {
        return associativeOp(TensorAssociativeOp.MIN);
    }

    @Override
    public Integer nanMin() {
        return nanAssociativeOp(TensorAssociativeOp.MIN);
    }

    @Override
    public int nanCount() {
        int count = 0;
        for (int offset : loop.offsets) {
            int i = offset;
            for (; i < loop.bound + offset; i += loop.step) {
                if (dtype().isNaN(array[i])) {
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    public int zeroCount() {
        int count = 0;
        for (int offset : loop.offsets) {
            for (int i = offset; i < loop.bound + offset; i += loop.step) {
                if (array[i] == 0) {
                    count++;
                }
            }
        }
        return count;
    }

    protected int associativeOp(TensorAssociativeOp op) {
        int agg = op.initialInt();
        for (int offset : loop.offsets) {
            for (int i = offset; i < loop.bound + offset; i += loop.step) {
                agg = op.applyInt(agg, array[i]);
            }
        }
        return agg;
    }

    protected int nanAssociativeOp(TensorAssociativeOp op) {
        int aggregate = op.initialInt();
        for (int offset : loop.offsets) {
            for (int i = offset; i < loop.bound + offset; i += loop.step) {
                if (!dtype().isNaN(array[i])) {
                    aggregate = op.applyInt(aggregate, array[i]);
                }
            }
        }
        return aggregate;
    }

    @Override
    public IntTensor copy(Order askOrder) {
        askOrder = Order.autoFC(askOrder);

        int[] copy = new int[size()];
        var dst = engine.ofInt().stride(StrideLayout.ofDense(shape(), 0, askOrder), copy);

        if (layout.storageFastOrder() == askOrder) {
            sameLayoutCopy(copy, askOrder);
        } else {
            copyTo(dst, askOrder);
        }
        return dst;
    }

    private void sameLayoutCopy(int[] copy, Order askOrder) {
        var chd = StrideLoopDescriptor.of(layout, askOrder);
        var last = 0;
        for (int ptr : chd.offsets) {
            for (int i = ptr; i < ptr + chd.bound; i += chd.step) {
                copy[last++] = array[i];
            }
        }
    }

    @Override
    public IntTensor copyTo(IntTensor to, Order askOrder) {

        if (to instanceof BaseIntTensorStride dst) {

            int limit = Math.floorDiv(L2_CACHE_SIZE, dtype().bytes() * 2 * engine.cpuThreads() * 8);

            if (layout.size() > limit) {

                int[] slices = Arrays.copyOf(layout.dims(), layout.rank());
                int size = IntArrays.prod(slices, 0, slices.length);
                while (size > limit) {
                    int axis = IntArrays.argmax(slices, 0, slices.length);
                    size = size * (slices[axis] / 2) / slices[axis];
                    slices[axis] = slices[axis] / 2;
                }

                int[] lens = new int[slices.length];
                for (int i = 0; i < lens.length; i++) {
                    lens[i] = Math.ceilDiv(layout().dim(i), slices[i]);
                }

                int[] starts = new int[slices.length];
                int[] ends = new int[slices.length];

                try (ExecutorService executor = Executors.newFixedThreadPool(engine.cpuThreads())) {
                    List<Future<?>> futures = new ArrayList<>();
                    Stack<Integer> stack = new Stack<>();
                    boolean loop = true;
                    while (!stack.isEmpty() || loop) {
                        int level = stack.size();
                        if (loop) {
                            if (level == slices.length) {
                                int[] ss = IntArrays.copy(starts);
                                int[] es = IntArrays.copy(ends);
                                futures.add(executor.submit(() -> {
                                    BaseIntTensorStride s = (BaseIntTensorStride) this.narrowAll(false, ss, es);
                                    BaseIntTensorStride d = (BaseIntTensorStride) dst.narrowAll(false, ss, es);
                                    directCopyTo(s, d, askOrder);
                                    return null;
                                }));
                                loop = false;
                            } else {
                                stack.push(0);
                                starts[level] = 0;
                                ends[level] = Math.min(slices[level], layout.dim(level));
                            }
                        } else {
                            int last = stack.pop();
                            if (last != lens[level - 1] - 1) {
                                last++;
                                stack.push(last);
                                starts[level - 1] = last * slices[level - 1];
                                ends[level - 1] = Math.min((last + 1) * slices[level - 1], layout.dim(level - 1));
                                loop = true;
                            }
                        }
                    }
                    for (var future : futures) {
                        future.get();
                    }
                    executor.shutdown();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }

                return dst;
            }

            directCopyTo(this, dst, askOrder);
            return dst;
        }
        throw new IllegalArgumentException("Not implemented for this tensor type.");
    }

    private void directCopyTo(BaseIntTensorStride src, BaseIntTensorStride dst, Order askOrder) {
        var chd = StrideLoopDescriptor.of(src.layout, askOrder);
        var it2 = dst.ptrIterator(askOrder);
        for (int ptr : chd.offsets) {
            for (int i = ptr; i < ptr + chd.bound; i += chd.step) {
                dst.array[it2.nextInt()] = src.array[i];
            }
        }
    }

    @Override
    public int[] toArray() {
        if (shape().rank() != 1) {
            throw new IllegalArgumentException("Only one dimensional tensors can be transformed into array.");
        }
        int[] copy = new int[size()];
        int pos = 0;
        for (int offset : loop.offsets) {
            for (int i = offset; i < loop.bound + offset; i++) {
                copy[pos++] = array[i];
            }
        }
        return copy;
    }

    @Override
    public int[] asArray() {
        if (shape().rank() != 1) {
            throw new IllegalArgumentException("Only one dimensional tensors can be transformed into array.");
        }
        if (array.length == shape().dim(0) && layout.stride(0) == 1) {
            return array;
        }
        return toArray();
    }
}