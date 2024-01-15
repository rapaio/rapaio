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

import rapaio.math.tensor.DType;
import rapaio.math.tensor.Order;
import rapaio.math.tensor.Shape;
import rapaio.math.tensor.Statistics;
import rapaio.math.tensor.Storage;
import rapaio.math.tensor.Tensor;
import rapaio.math.tensor.TensorEngine;
import rapaio.math.tensor.engine.AbstractTensor;
import rapaio.math.tensor.engine.varray.VectorizedDoubleTensorStride;
import rapaio.math.tensor.iterators.DensePointerIterator;
import rapaio.math.tensor.iterators.LoopIterator;
import rapaio.math.tensor.iterators.PointerIterator;
import rapaio.math.tensor.iterators.ScalarLoopIterator;
import rapaio.math.tensor.iterators.StrideLoopDescriptor;
import rapaio.math.tensor.iterators.StrideLoopIterator;
import rapaio.math.tensor.iterators.StridePointerIterator;
import rapaio.math.tensor.layout.StrideLayout;
import rapaio.math.tensor.layout.StrideWrapper;
import rapaio.math.tensor.operator.TensorAssociativeOp;
import rapaio.math.tensor.operator.TensorBinaryOp;
import rapaio.math.tensor.operator.TensorUnaryOp;
import rapaio.util.NotImplementedException;
import rapaio.util.collection.IntArrays;
import rapaio.util.function.IntIntBiFunction;

public sealed class BaseDoubleTensorStride extends AbstractTensor<Double> permits VectorizedDoubleTensorStride {

    protected final StrideLayout layout;
    protected final TensorEngine engine;
    protected final StrideLoopDescriptor loop;

    public BaseDoubleTensorStride(TensorEngine engine, StrideLayout layout, Storage<Double> storage) {
        super(storage);
        this.layout = layout;
        this.engine = engine;
        this.loop = StrideLoopDescriptor.of(layout, layout.storageFastOrder());
    }

    @Override
    public DType<Double> dtype() {
        return DType.DOUBLE;
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
    public Tensor<Double> reshape(Shape askShape, Order askOrder) {
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
        Tensor<Double> copy = engine.ofDouble().zeros(askShape, askOrder);
        var copyIt = copy.ptrIterator(Order.C);
        while (it.hasNext()) {
            copy.ptrSetDouble(copyIt.nextInt(), storage.getDouble(it.nextInt()));
        }
        return copy;
    }

    @Override
    public Tensor<Double> t_() {
        return engine.ofDouble().stride(layout.revert(), storage);
    }

    @Override
    public Tensor<Double> ravel(Order askOrder) {
        var compact = layout.computeFortranLayout(askOrder, true);
        if (compact.shape().rank() == 1) {
            return engine.ofDouble().stride(compact, storage);
        }
        return flatten(askOrder);
    }

    @Override
    public Tensor<Double> flatten(Order askOrder) {
        askOrder = Order.autoFC(askOrder);
        var out = engine.ofDouble().storage().zeros(layout.size());
        int p = 0;
        var it = loopIterator(askOrder);
        while (it.hasNext()) {
            int pointer = it.nextInt();
            for (int i = pointer; i < pointer + it.bound(); i += it.step()) {
                out.setDouble(p++, storage.getDouble(i));
            }
        }
        return engine.ofDouble().stride(StrideLayout.of(Shape.of(layout.size()), 0, new int[] {1}), out);
    }

    @Override
    public Tensor<Double> squeeze() {
        return layout.shape().unitDimCount() == 0 ? this : engine.ofDouble().stride(layout.squeeze(), storage);
    }

    @Override
    public Tensor<Double> squeeze(int axis) {
        return layout.shape().dim(axis) != 1 ? this : engine.ofDouble().stride(layout.squeeze(axis), storage);
    }

    @Override
    public Tensor<Double> unsqueeze(int axis) {
        return engine.ofDouble().stride(layout().unsqueeze(axis), storage);
    }

    @Override
    public Tensor<Double> moveAxis(int src, int dst) {
        return engine.ofDouble().stride(layout.moveAxis(src, dst), storage);
    }

    @Override
    public Tensor<Double> swapAxis(int src, int dst) {
        return engine.ofDouble().stride(layout.swapAxis(src, dst), storage);
    }

    @Override
    public Tensor<Double> narrow(int axis, boolean keepdim, int start, int end) {
        return engine.ofDouble().stride(layout.narrow(axis, keepdim, start, end), storage);
    }

    @Override
    public Tensor<Double> narrowAll(boolean keepdim, int[] starts, int[] ends) {
        return engine.ofDouble().stride(layout.narrowAll(keepdim, starts, ends), storage);
    }

    @Override
    public List<Tensor<Double>> split(int axis, boolean keepdim, int... indexes) {
        List<Tensor<Double>> result = new ArrayList<>(indexes.length);
        for (int i = 0; i < indexes.length; i++) {
            result.add(narrow(axis, keepdim, indexes[i], i < indexes.length - 1 ? indexes[i + 1] : shape().dim(axis)));
        }
        return result;
    }

    @Override
    public List<Tensor<Double>> splitAll(boolean keepdim, int[][] indexes) {
        if (indexes.length != rank()) {
            throw new IllegalArgumentException(
                    "Indexes length of %d is not the same as shape rank %d.".formatted(indexes.length, rank()));
        }
        List<Tensor<Double>> results = new ArrayList<>();
        int[] starts = new int[indexes.length];
        int[] ends = new int[indexes.length];
        splitAllRec(results, indexes, keepdim, starts, ends, 0);
        return results;
    }

    private void splitAllRec(List<Tensor<Double>> results, int[][] indexes, boolean keepdim, int[] starts, int[] ends, int level) {
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
    public Tensor<Double> repeat(int axis, int repeat, boolean stack) {
        List<Tensor<Double>> copies = new ArrayList<>(repeat);
        for (int i = 0; i < repeat; i++) {
            copies.add(this);
        }
        if (stack) {
            return engine.stack(axis, copies);
        } else {
            return engine.concat(axis, copies);
        }
    }

    @Override
    public Tensor<Double> tile(int[] repeats) {
        throw new NotImplementedException();
    }

    @Override
    public Tensor<Double> expand(int axis, int dim) {
        if (layout.dim(axis) != 1) {
            throw new IllegalArgumentException(STR."Dimension \{axis} does not have size 1.");
        }
        if (dim < 1) {
            throw new IllegalArgumentException(STR."Dimension of the new axis \{dim} must be positive.");
        }
        int[] newDims = Arrays.copyOf(layout.dims(), layout.dims().length);
        int[] newStrides = Arrays.copyOf(layout.strides(), layout.strides().length);

        newDims[axis] = dim;
        newStrides[axis] = 0;
        return engine.ofDouble().stride(StrideLayout.of(Shape.of(newDims), layout.offset(), newStrides), storage);
    }

    @Override
    public Tensor<Double> permute(int[] dims) {
        return engine.ofDouble().stride(layout().permute(dims), storage);
    }

    @Override
    public Tensor<Double> sort_(int axis, boolean asc) {
        int[] newDims = layout.shape().narrowDims(axis);
        int[] newStrides = layout.narrowStrides(axis);
        int selDim = layout.dim(axis);
        int selStride = layout.stride(axis);

        var it = new StridePointerIterator(StrideLayout.of(Shape.of(newDims), layout().offset(), newStrides), Order.C, false);
        while (it.hasNext()) {
            StrideWrapper.of(it.nextInt(), selStride, selDim, this).sort(asc);
        }
        return this;
    }

    @Override
    public Tensor<Double> sort(Order order, int axis, boolean asc) {
        return copy(order).sort_(axis, asc);
    }

    @Override
    public void indirectSort(int[] indices, boolean asc) {
        if (layout.rank() != 1) {
            throw new IllegalArgumentException("Tensor must be flat (have a single dimension).");
        }
        for (int index : indices) {
            if (index < 0 || index >= layout.size()) {
                throw new IllegalArgumentException("Indices must be semi-positive and less than the size of the tensor.");
            }
        }
        StrideWrapper.of(layout.offset(), layout.stride(0), layout.dim(0), this).sortIndirect(indices, asc);
    }

    @Override
    public Double get(int... indexes) {
        return storage.getDouble(layout.pointer(indexes));
    }

    @Override
    public void set(Double value, int... indexes) {
        storage.setDouble(layout.pointer(indexes), value);
    }


    @Override
    public Double ptrGet(int ptr) {
        return storage.getDouble(ptr);
    }

    @Override
    public void ptrSet(int ptr, Double value) {
        storage.setDouble(ptr, value);
    }

    @Override
    public Iterator<Double> iterator(Order askOrder) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(ptrIterator(askOrder), Spliterator.ORDERED), false)
                .map(storage::getDouble).iterator();
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
    public BaseDoubleTensorStride apply_(Order askOrder, IntIntBiFunction<Double> apply) {
        var it = ptrIterator(askOrder);
        int i = 0;
        while (it.hasNext()) {
            int p = it.nextInt();
            storage.set(p, apply.applyAsInt(i++, p));
        }
        return this;
    }

    @Override
    public Tensor<Double> apply_(Function<Double, Double> fun) {
        var ptrIter = ptrIterator(Order.S);
        while (ptrIter.hasNext()) {
            int ptr = ptrIter.nextInt();
            storage.set(ptr, fun.apply(storage.get(ptr)));
        }
        return this;
    }

    @Override
    public Tensor<Double> fill_(Double value) {
        for (int offset : loop.offsets) {
            for (int i = 0; i < loop.size; i++) {
                int p = offset + i * loop.step;
                storage.set(p, value);
            }
        }
        return this;
    }

    @Override
    public Tensor<Double> fillNan_(Double value) {
        for (int offset : loop.offsets) {
            for (int i = 0; i < loop.size; i++) {
                int p = offset + i * loop.step;
                if (dtype().isNaN(storage.getDouble(p))) {
                    storage.setDouble(p, value);
                }
            }
        }
        return this;
    }

    @Override
    public Tensor<Double> clamp_(Double min, Double max) {
        for (int offset : loop.offsets) {
            for (int i = 0; i < loop.size; i++) {
                int p = offset + i * loop.step;
                if (!dtype().isNaN(min) && storage.getDouble(p) < min) {
                    storage.setDouble(p, min);
                }
                if (!dtype().isNaN(max) && storage.getDouble(p) > max) {
                    storage.setDouble(p, max);
                }
            }
        }
        return this;
    }

    private void unaryOpStep(TensorUnaryOp op) {
        for (int off : loop.offsets) {
            for (int i = 0; i < loop.size; i++) {
                int p = off + i * loop.step;
                storage.setDouble(p, op.applyDouble(storage.getDouble(p)));
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
    public Tensor<Double> abs_() {
        unaryOp(TensorUnaryOp.ABS);
        return this;
    }

    @Override
    public Tensor<Double> negate_() {
        unaryOp(TensorUnaryOp.NEG);
        return this;
    }

    @Override
    public Tensor<Double> log_() {
        unaryOp(TensorUnaryOp.LOG);
        return this;
    }

    @Override
    public Tensor<Double> log1p_() {
        unaryOp(TensorUnaryOp.LOG1P);
        return this;
    }

    @Override
    public Tensor<Double> exp_() {
        unaryOp(TensorUnaryOp.EXP);
        return this;
    }

    @Override
    public Tensor<Double> expm1_() {
        unaryOp(TensorUnaryOp.EXPM1);
        return this;
    }

    @Override
    public Tensor<Double> sin_() {
        unaryOp(TensorUnaryOp.SIN);
        return this;
    }

    @Override
    public Tensor<Double> asin_() {
        unaryOp(TensorUnaryOp.ASIN);
        return this;
    }

    @Override
    public Tensor<Double> sinh_() {
        unaryOp(TensorUnaryOp.SINH);
        return this;
    }

    @Override
    public Tensor<Double> cos_() {
        unaryOp(TensorUnaryOp.COS);
        return this;
    }

    @Override
    public Tensor<Double> acos_() {
        unaryOp(TensorUnaryOp.ACOS);
        return this;
    }

    @Override
    public Tensor<Double> cosh_() {
        unaryOp(TensorUnaryOp.COSH);
        return this;
    }

    @Override
    public Tensor<Double> tan_() {
        unaryOp(TensorUnaryOp.TAN);
        return this;
    }

    @Override
    public Tensor<Double> atan_() {
        unaryOp(TensorUnaryOp.ATAN);
        return this;
    }

    @Override
    public Tensor<Double> tanh_() {
        unaryOp(TensorUnaryOp.TANH);
        return this;
    }

    protected void binaryVectorOp(TensorBinaryOp op, Tensor<Double> b) {
        if (b.isScalar()) {
            binaryScalarOp(op, b.getDouble());
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
            storage.setDouble(next, op.applyDouble(storage.getDouble(next), b.ptrGet(refIt.nextInt())));
        }
    }

    @Override
    public Tensor<Double> add_(Tensor<Double> tensor) {
        binaryVectorOp(TensorBinaryOp.ADD, tensor);
        return this;
    }

    @Override
    public Tensor<Double> sub_(Tensor<Double> tensor) {
        binaryVectorOp(TensorBinaryOp.SUB, tensor);
        return this;
    }

    @Override
    public Tensor<Double> mul_(Tensor<Double> tensor) {
        binaryVectorOp(TensorBinaryOp.MUL, tensor);
        return this;
    }

    @Override
    public Tensor<Double> div_(Tensor<Double> tensor) {
        binaryVectorOp(TensorBinaryOp.DIV, tensor);
        return this;
    }

    void binaryScalarOpStep(TensorBinaryOp op, double value) {
        for (int offset : loop.offsets) {
            for (int i = 0; i < loop.size; i++) {
                int p = offset + i * loop.step;
                storage.setDouble(p, op.applyDouble(storage.getDouble(p), value));
            }
        }
    }

    protected void binaryScalarOp(TensorBinaryOp op, double value) {
        binaryScalarOpStep(op, value);
    }

    @Override
    public BaseDoubleTensorStride add_(Double value) {
        binaryScalarOp(TensorBinaryOp.ADD, value);
        return this;
    }

    @Override
    public BaseDoubleTensorStride sub_(Double value) {
        binaryScalarOp(TensorBinaryOp.SUB, value);
        return this;
    }

    @Override
    public BaseDoubleTensorStride mul_(Double value) {
        binaryScalarOp(TensorBinaryOp.MUL, value);
        return this;
    }

    @Override
    public BaseDoubleTensorStride div_(Double value) {
        binaryScalarOp(TensorBinaryOp.DIV, value);
        return this;
    }

    @Override
    public Tensor<Double> fma_(Double a, Tensor<Double> t) {
        if (t.isScalar()) {
            double tVal = t.getDouble(0);
            return add_((double) (a * tVal));
        }
        if (!shape().equals(t.shape())) {
            throw new IllegalArgumentException("Tensors does not have the same shape.");
        }
        double aVal = a;
        var order = layout.storageFastOrder();
        order = order == Order.S ? Order.defaultOrder() : order;

        var it = ptrIterator(order);
        var refIt = t.ptrIterator(order);
        while (it.hasNext()) {
            int next = it.nextInt();
            storage.setDouble(next, (double) Math.fma(t.ptrGet(refIt.nextInt()), aVal, storage.getDouble(next)));
        }
        return this;
    }

    @Override
    public Double vdot(Tensor<Double> tensor) {
        return vdot(tensor, 0, shape().dim(0));
    }

    @Override
    public Double vdot(Tensor<Double> tensor, int start, int end) {
        if (shape().rank() != 1 || tensor.shape().rank() != 1 || shape().dim(0) != tensor.shape().dim(0)) {
            throw new IllegalArgumentException(
                    "Operands are not valid for vector dot product (v = %s, v = %s)."
                            .formatted(shape().toString(), tensor.shape().toString()));
        }
        if (start >= end || start < 0 || end > tensor.shape().dim(0)) {
            throw new IllegalArgumentException("Start and end indexes are invalid (start: %d, end: %s).".formatted(start, end));
        }
        BaseDoubleTensorStride dts = (BaseDoubleTensorStride) tensor;
        int step1 = layout.stride(0);
        int step2 = dts.layout.stride(0);

        int start1 = layout.offset() + start * step1;
        int end1 = layout.offset() + end * step1;
        int start2 = dts.layout.offset() + start * step2;

        double sum = 0;
        for (int i = start1; i < end1; i += step1) {
            sum += (double) (storage.getDouble(i) * dts.storage.getDouble(start2));
            start2 += step2;
        }
        return sum;
    }

    @Override
    public Tensor<Double> mv(Tensor<Double> tensor) {
        if (shape().rank() != 2 || tensor.shape().rank() != 1 || shape().dim(1) != tensor.shape().dim(0)) {
            throw new IllegalArgumentException(
                    STR."Operands are not valid for matrix-vector multiplication \{"(m = %s, v = %s).".formatted(shape(),
                            tensor.shape())}");
        }
        var result = engine.ofDouble().storage().zeros(shape().dim(0));
        var it = ptrIterator(Order.C);
        for (int i = 0; i < shape().dim(0); i++) {
            var innerIt = tensor.ptrIterator(Order.C);
            double sum = 0;
            for (int j = 0; j < shape().dim(1); j++) {
                sum += (double) (ptrGetDouble(it.nextInt()) * tensor.ptrGetDouble(innerIt.nextInt()));
            }
            result.setDouble(i, sum);
        }
        StrideLayout layout = StrideLayout.ofDense(Shape.of(shape().dim(0)), 0, Order.C);
        return engine.ofDouble().stride(layout, result);
    }

    @Override
    public Tensor<Double> mm(Tensor<Double> t, Order askOrder) {
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

        var result = engine.ofDouble().storage().zeros(m * p);
        var ret = engine.ofDouble().stride(StrideLayout.ofDense(Shape.of(m, p), 0, askOrder), result);

        List<Tensor<Double>> rows = chunk(0, false, 1);
        List<Tensor<Double>> cols = t.chunk(1, false, 1);

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
                                var krow = (BaseDoubleTensorStride) rows.get(i);
                                for (int j = c; j < ce; j++) {
                                    result.incDouble(i * iStride + j * jStride, krow.vdot(cols.get(j), k, end));
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
    public Statistics<Double> stats() {
        if (!dtype().isFloat()) {
            throw new IllegalArgumentException("Operation available only for float tensors.");
        }

        int size = size();
        int nanSize = 0;
        double mean;
        double nanMean;
        double variance;
        double nanVariance;

        // first pass compute raw mean
        double sum = 0;
        double nanSum = 0;
        for (int offset : loop.offsets) {
            for (int i = 0; i < loop.size; i++) {
                int p = offset + i * loop.step;
                sum += storage.getDouble(p);
                if (!dtype().isNaN(storage.getDouble(p))) {
                    nanSum += storage.getDouble(p);
                    nanSize++;
                }
            }
        }
        mean = (double) (sum / size);
        nanMean = (double) (nanSum / nanSize);

        // second pass adjustments for mean
        sum = 0;
        nanSum = 0;
        for (int offset : loop.offsets) {
            for (int i = 0; i < loop.size; i++) {
                int p = offset + i * loop.step;
                sum += (double) (storage.getDouble(p) - mean);
                if (!dtype().isNaN(storage.getDouble(p))) {
                    nanSum += (double) (storage.getDouble(p) - nanMean);
                }
            }
        }
        mean += (double) (sum / size);
        nanMean += (double) (nanSum / nanSize);

        // third pass compute variance
        double sum2 = 0;
        double sum3 = 0;
        double nanSum2 = 0;
        double nanSum3 = 0;

        for (int offset : loop.offsets) {
            for (int i = 0; i < loop.size; i++) {
                int p = offset + i * loop.step;
                sum2 += (double) ((storage.getDouble(p) - mean) * (storage.getDouble(p) - mean));
                sum3 += (double) (storage.getDouble(p) - mean);
                if (!dtype().isNaN(storage.getDouble(p))) {
                    nanSum2 += (double) ((storage.getDouble(p) - nanMean) * (storage.getDouble(p) - nanMean));
                    nanSum3 += (double) (storage.getDouble(p) - nanMean);
                }
            }
        }
        variance = (double) ((sum2 - (sum3 * sum3) / size) / size);
        nanVariance = (double) ((nanSum2 - (nanSum3 * nanSum3) / nanSize) / nanSize);

        return new Statistics<>(dtype(), size, nanSize, mean, nanMean, variance, nanVariance);
    }

    @Override
    public Double sum() {
        return associativeOp(TensorAssociativeOp.ADD);
    }

    @Override
    public Tensor<Double> sum(Order order, int axis) {
        return associativeOpNarrow(TensorAssociativeOp.ADD, order, axis);
    }

    @Override
    public Double nanSum() {
        return nanAssociativeOp(TensorAssociativeOp.ADD);
    }

    @Override
    public Tensor<Double> nanSum(Order order, int axis) {
        return nanAssociativeOpNarrow(TensorAssociativeOp.ADD, order, axis);
    }

    @Override
    public Double prod() {
        return associativeOp(TensorAssociativeOp.MUL);
    }

    @Override
    public Tensor<Double> prod(Order order, int axis) {
        return associativeOpNarrow(TensorAssociativeOp.MUL, order, axis);
    }

    @Override
    public Double nanProd() {
        return nanAssociativeOp(TensorAssociativeOp.MUL);
    }

    @Override
    public Tensor<Double> nanProd(Order order, int axis) {
        return nanAssociativeOpNarrow(TensorAssociativeOp.MUL, order, axis);
    }

    @Override
    public Double max() {
        return associativeOp(TensorAssociativeOp.MAX);
    }

    @Override
    public Tensor<Double> max(Order order, int axis) {
        return associativeOpNarrow(TensorAssociativeOp.MAX, order, axis);
    }

    @Override
    public Double nanMax() {
        return nanAssociativeOp(TensorAssociativeOp.MAX);
    }

    @Override
    public Tensor<Double> nanMax(Order order, int axis) {
        return nanAssociativeOpNarrow(TensorAssociativeOp.MAX, order, axis);
    }

    @Override
    public Double min() {
        return associativeOp(TensorAssociativeOp.MIN);
    }

    @Override
    public Tensor<Double> min(Order order, int axis) {
        return associativeOpNarrow(TensorAssociativeOp.MIN, order, axis);
    }

    @Override
    public Double nanMin() {
        return nanAssociativeOp(TensorAssociativeOp.MIN);
    }

    @Override
    public Tensor<Double> nanMin(Order order, int axis) {
        return nanAssociativeOpNarrow(TensorAssociativeOp.MIN, order, axis);
    }

    @Override
    public int nanCount() {
        int count = 0;
        for (int offset : loop.offsets) {
            for (int i = 0; i < loop.size; i++) {
                int p = offset + i * loop.step;
                if (dtype().isNaN(storage.getDouble(p))) {
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
            for (int i = 0; i < loop.size; i++) {
                int p = offset + i * loop.step;
                if (storage.getDouble(p) == 0) {
                    count++;
                }
            }
        }
        return count;
    }

    protected double associativeOp(TensorAssociativeOp op) {
        double agg = op.initialDouble();
        for (int offset : loop.offsets) {
            for (int i = 0; i < loop.size; i++) {
                int p = offset + i * loop.step;
                agg = op.applyDouble(agg, storage.getDouble(p));
            }
        }
        return agg;
    }

    protected double nanAssociativeOp(TensorAssociativeOp op) {
        double aggregate = op.initialDouble();
        for (int offset : loop.offsets) {
            for (int i = 0; i < loop.size; i++) {
                int p = offset + i * loop.step;
                if (!dtype().isNaN(storage.getDouble(p))) {
                    aggregate = op.applyDouble(aggregate, storage.getDouble(p));
                }
            }
        }
        return aggregate;
    }

    protected Tensor<Double> associativeOpNarrow(TensorAssociativeOp op, Order order, int axis) {
        int[] newDims = layout.shape().narrowDims(axis);
        int[] newStrides = layout.narrowStrides(axis);
        int selDim = layout.dim(axis);
        int selStride = layout.stride(axis);

        Tensor<Double> res = engine.ofDouble().zeros(Shape.of(newDims), Order.autoFC(order));
        var it = new StridePointerIterator(StrideLayout.of(newDims, layout().offset(), newStrides), Order.C);
        var resIt = res.ptrIterator(Order.C);
        while (it.hasNext()) {
            int ptr = it.nextInt();
            double value = StrideWrapper.of(ptr, selStride, selDim, this).aggregate(op.initialDouble(), op::applyDouble);
            res.ptrSet(resIt.next(), value);
        }
        return res;
    }

    protected Tensor<Double> nanAssociativeOpNarrow(TensorAssociativeOp op, Order order, int axis) {
        int[] newDims = layout.shape().narrowDims(axis);
        int[] newStrides = layout.narrowStrides(axis);
        int selDim = layout.dim(axis);
        int selStride = layout.stride(axis);

        Tensor<Double> res = engine.ofDouble().zeros(Shape.of(newDims), Order.autoFC(order));
        var it = new StridePointerIterator(StrideLayout.of(newDims, layout().offset(), newStrides), Order.C);
        var resIt = res.ptrIterator(Order.C);
        while (it.hasNext()) {
            int ptr = it.nextInt();
            double value = StrideWrapper.of(ptr, selStride, selDim, this).nanAggregate(DType.DOUBLE, op.initialDouble(), op::applyDouble);
            res.ptrSet(resIt.next(), value);
        }
        return res;
    }

    @Override
    public Tensor<Double> copy(Order askOrder) {
        askOrder = Order.autoFC(askOrder);

        var copy = engine.ofDouble().storage().zeros(size());
        var dst = engine.ofDouble().stride(StrideLayout.ofDense(shape(), 0, askOrder), copy);

        if (layout.storageFastOrder() == askOrder) {
            sameLayoutCopy(copy, askOrder);
        } else {
            copyTo(dst, askOrder);
        }
        return dst;
    }

    private void sameLayoutCopy(Storage<Double> copy, Order askOrder) {
        var loop = StrideLoopDescriptor.of(layout, askOrder);
        var last = 0;
        for (int offset : loop.offsets) {
            for (int i = 0; i < loop.size; i++) {
                int p = offset + i * loop.step;
                copy.setDouble(last++, storage.getDouble(p));
            }
        }
    }

    @Override
    public Tensor<Double> copyTo(Tensor<Double> to, Order askOrder) {

        if (to instanceof BaseDoubleTensorStride dst) {

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
                                    BaseDoubleTensorStride s = (BaseDoubleTensorStride) this.narrowAll(false, ss, es);
                                    BaseDoubleTensorStride d = (BaseDoubleTensorStride) dst.narrowAll(false, ss, es);
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

    private void directCopyTo(BaseDoubleTensorStride src, BaseDoubleTensorStride dst, Order askOrder) {
        var loop = StrideLoopDescriptor.of(src.layout, askOrder);
        var it2 = dst.ptrIterator(askOrder);
        for (int offset : loop.offsets) {
            for (int i = 0; i < loop.size; i++) {
                int p = offset + i * loop.step;
                dst.storage.setDouble(it2.nextInt(), src.storage.getDouble(p));
            }
        }
    }

    public double[] toArray() {
        if (shape().rank() != 1) {
            throw new IllegalArgumentException("Only one dimensional tensors can be transformed into array.");
        }
        double[] copy = new double[size()];
        int pos = 0;
        for (int offset : loop.offsets) {
            for (int i = 0; i < loop.size; i++) {
                int p = offset + i * loop.step;
                copy[pos++] = storage.getDouble(p);
            }
        }
        return copy;
    }

    public double[] asArray() {
        if (shape().rank() != 1) {
            throw new IllegalArgumentException("Only one dimensional tensors can be transformed into array.");
        }
        // TODO FIX
//        if (storage.size() == shape().dim(0) && layout.stride(0) == 1) {
//            return storage.;
//        }
        return toArray();
    }
}