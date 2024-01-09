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
import rapaio.math.tensor.engine.varray.VectorizedByteTensorStride;
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

public sealed class BaseByteTensorStride extends AbstractTensor<Byte>
        permits VectorizedByteTensorStride {

    protected final StrideLayout layout;
    protected final TensorEngine engine;
    protected final StrideLoopDescriptor loop;

    public BaseByteTensorStride(TensorEngine engine, Shape shape, int offset, int[] strides, Storage<Byte> storage) {
        this(engine, StrideLayout.of(shape, offset, strides), storage);
    }

    public BaseByteTensorStride(TensorEngine engine, Shape shape, int offset, Order order, Storage<Byte> storage) {
        this(engine, StrideLayout.ofDense(shape, offset, order), storage);
    }

    public BaseByteTensorStride(TensorEngine engine, StrideLayout layout, Storage<Byte> storage) {
        super(storage);
        this.layout = layout;
        this.engine = engine;
        this.loop = StrideLoopDescriptor.of(layout, layout.storageFastOrder());
    }

    @Override
    public DType<Byte> dtype() {
        return DType.BYTE;
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
    public Tensor<Byte> reshape(Shape askShape, Order askOrder) {
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
        Tensor<Byte> copy = engine.ofByte().zeros(askShape, askOrder);
        var copyIt = copy.ptrIterator(Order.C);
        while (it.hasNext()) {
            copy.ptrSetByte(copyIt.nextInt(), storage.getByte(it.nextInt()));
        }
        return copy;
    }

    @Override
    public Tensor<Byte> transpose() {
        return engine.ofByte().stride(layout.revert(), storage);
    }

    @Override
    public Tensor<Byte> ravel(Order askOrder) {
        var compact = layout.computeFortranLayout(askOrder, true);
        if (compact.shape().rank() == 1) {
            return engine.ofByte().stride(compact, storage);
        }
        return flatten(askOrder);
    }

    @Override
    public Tensor<Byte> flatten(Order askOrder) {
        askOrder = Order.autoFC(askOrder);
        var out = engine.ofByte().storage().zeros(layout.size());
        int p = 0;
        var it = loopIterator(askOrder);
        while (it.hasNext()) {
            int pointer = it.nextInt();
            for (int i = pointer; i < pointer + it.bound(); i += it.step()) {
                out.setByte(p++, storage.getByte(i));
            }
        }
        return engine.ofByte().stride(Shape.of(layout.size()), 0, new int[] {1}, out);
    }

    @Override
    public Tensor<Byte> squeeze() {
        return layout.shape().unitDimCount() == 0 ? this : engine.ofByte().stride(layout.squeeze(), storage);
    }

    @Override
    public Tensor<Byte> squeeze(int axis) {
        return layout.shape().dim(axis) != 1 ? this : engine.ofByte().stride(layout.squeeze(axis), storage);
    }

    @Override
    public Tensor<Byte> unsqueeze(int axis) {
        return engine.ofByte().stride(layout().unsqueeze(axis), storage);
    }

    @Override
    public Tensor<Byte> moveAxis(int src, int dst) {
        return engine.ofByte().stride(layout.moveAxis(src, dst), storage);
    }

    @Override
    public Tensor<Byte> swapAxis(int src, int dst) {
        return engine.ofByte().stride(layout.swapAxis(src, dst), storage);
    }

    @Override
    public Tensor<Byte> narrow(int axis, boolean keepdim, int start, int end) {
        return engine.ofByte().stride(layout.narrow(axis, keepdim, start, end), storage);
    }

    @Override
    public Tensor<Byte> narrowAll(boolean keepdim, int[] starts, int[] ends) {
        return engine.ofByte().stride(layout.narrowAll(keepdim, starts, ends), storage);
    }

    @Override
    public List<Tensor<Byte>> split(int axis, boolean keepdim, int... indexes) {
        List<Tensor<Byte>> result = new ArrayList<>(indexes.length);
        for (int i = 0; i < indexes.length; i++) {
            result.add(narrow(axis, keepdim, indexes[i], i < indexes.length - 1 ? indexes[i + 1] : shape().dim(axis)));
        }
        return result;
    }

    @Override
    public List<Tensor<Byte>> splitAll(boolean keepdim, int[][] indexes) {
        if (indexes.length != rank()) {
            throw new IllegalArgumentException(
                    "Indexes length of %d is not the same as shape rank %d.".formatted(indexes.length, rank()));
        }
        List<Tensor<Byte>> results = new ArrayList<>();
        int[] starts = new int[indexes.length];
        int[] ends = new int[indexes.length];
        splitAllRec(results, indexes, keepdim, starts, ends, 0);
        return results;
    }

    private void splitAllRec(List<Tensor<Byte>> results, int[][] indexes, boolean keepdim, int[] starts, int[] ends, int level) {
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
    public Tensor<Byte> repeat(int axis, int repeat, boolean stack) {
        List<Tensor<Byte>> copies = new ArrayList<>(repeat);
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
    public Tensor<Byte> tile(int[] repeats) {
        throw new NotImplementedException();
    }

    @Override
    public Tensor<Byte> expand(int axis, int dim) {
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
        return engine.ofByte().stride(StrideLayout.of(Shape.of(newDims), layout.offset(), newStrides), storage);
    }

    @Override
    public Tensor<Byte> permute(int[] dims) {
        return engine.ofByte().stride(layout().permute(dims), storage);
    }

    @Override
    public Byte get(int... indexes) {
        return storage.getByte(layout.pointer(indexes));
    }

    @Override
    public void set(Byte value, int... indexes) {
        storage.setByte(layout.pointer(indexes), value);
    }


    @Override
    public Byte ptrGet(int ptr) {
        return storage.getByte(ptr);
    }

    @Override
    public void ptrSet(int ptr, Byte value) {
        storage.setByte(ptr, value);
    }

    @Override
    public Iterator<Byte> iterator(Order askOrder) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(ptrIterator(askOrder), Spliterator.ORDERED), false)
                .map(storage::getByte).iterator();
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
    public BaseByteTensorStride apply_(Order askOrder, IntIntBiFunction<Byte> apply) {
        var it = ptrIterator(askOrder);
        int i = 0;
        while (it.hasNext()) {
            int p = it.nextInt();
            storage.set(p, apply.applyAsInt(i++, p));
        }
        return this;
    }

    @Override
    public Tensor<Byte> apply_(Function<Byte, Byte> fun) {
        var ptrIter = ptrIterator(Order.S);
        while (ptrIter.hasNext()) {
            int ptr = ptrIter.nextInt();
            storage.set(ptr, fun.apply(storage.get(ptr)));
        }
        return this;
    }

    @Override
    public Tensor<Byte> fill_(Byte value) {
        for (int offset : loop.offsets) {
            for (int i = offset; i < loop.bound + offset; i += loop.step) {
                storage.set(i, value);
            }
        }
        return this;
    }

    @Override
    public Tensor<Byte> fillNan_(Byte value) {
        for (int offset : loop.offsets) {
            for (int i = offset; i < loop.bound + offset; i += loop.step) {
                if (dtype().isNaN(storage.getByte(i))) {
                    storage.setByte(i, value);
                }
            }
        }
        return this;
    }

    @Override
    public Tensor<Byte> clamp_(Byte min, Byte max) {
        for (int offset : loop.offsets) {
            for (int i = offset; i < loop.bound + offset; i += loop.step) {
                if (!dtype().isNaN(min) && storage.getByte(i) < min) {
                    storage.setByte(i, min);
                }
                if (!dtype().isNaN(max) && storage.getByte(i) > max) {
                    storage.setByte(i, max);
                }
            }
        }
        return this;
    }

    @Override
    public Tensor<Byte> take(Order order, int... indexes) {
        throw new NotImplementedException();
    }

    private void unaryOpStep(TensorUnaryOp op) {
        for (int off : loop.offsets) {
            for (int i = off; i < loop.bound + off; i += loop.step) {
                storage.setByte(i, op.applyByte(storage.getByte(i)));
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
    public Tensor<Byte> abs_() {
        unaryOp(TensorUnaryOp.ABS);
        return this;
    }

    @Override
    public Tensor<Byte> negate_() {
        unaryOp(TensorUnaryOp.NEG);
        return this;
    }

    @Override
    public Tensor<Byte> log_() {
        unaryOp(TensorUnaryOp.LOG);
        return this;
    }

    @Override
    public Tensor<Byte> log1p_() {
        unaryOp(TensorUnaryOp.LOG1P);
        return this;
    }

    @Override
    public Tensor<Byte> exp_() {
        unaryOp(TensorUnaryOp.EXP);
        return this;
    }

    @Override
    public Tensor<Byte> expm1_() {
        unaryOp(TensorUnaryOp.EXPM1);
        return this;
    }

    @Override
    public Tensor<Byte> sin_() {
        unaryOp(TensorUnaryOp.SIN);
        return this;
    }

    @Override
    public Tensor<Byte> asin_() {
        unaryOp(TensorUnaryOp.ASIN);
        return this;
    }

    @Override
    public Tensor<Byte> sinh_() {
        unaryOp(TensorUnaryOp.SINH);
        return this;
    }

    @Override
    public Tensor<Byte> cos_() {
        unaryOp(TensorUnaryOp.COS);
        return this;
    }

    @Override
    public Tensor<Byte> acos_() {
        unaryOp(TensorUnaryOp.ACOS);
        return this;
    }

    @Override
    public Tensor<Byte> cosh_() {
        unaryOp(TensorUnaryOp.COSH);
        return this;
    }

    @Override
    public Tensor<Byte> tan_() {
        unaryOp(TensorUnaryOp.TAN);
        return this;
    }

    @Override
    public Tensor<Byte> atan_() {
        unaryOp(TensorUnaryOp.ATAN);
        return this;
    }

    @Override
    public Tensor<Byte> tanh_() {
        unaryOp(TensorUnaryOp.TANH);
        return this;
    }

    protected void binaryVectorOp(TensorBinaryOp op, Tensor<Byte> b) {
        if (b.isScalar()) {
            binaryScalarOp(op, b.getByte());
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
            storage.setByte(next, op.applyByte(storage.getByte(next), b.ptrGet(refIt.nextInt())));
        }
    }

    @Override
    public Tensor<Byte> add_(Tensor<Byte> tensor) {
        binaryVectorOp(TensorBinaryOp.ADD, tensor);
        return this;
    }

    @Override
    public Tensor<Byte> sub_(Tensor<Byte> tensor) {
        binaryVectorOp(TensorBinaryOp.SUB, tensor);
        return this;
    }

    @Override
    public Tensor<Byte> mul_(Tensor<Byte> tensor) {
        binaryVectorOp(TensorBinaryOp.MUL, tensor);
        return this;
    }

    @Override
    public Tensor<Byte> div_(Tensor<Byte> tensor) {
        binaryVectorOp(TensorBinaryOp.DIV, tensor);
        return this;
    }

    void binaryScalarOpStep(TensorBinaryOp op, byte value) {
        for (int offset : loop.offsets) {
            for (int i = offset; i < loop.bound + offset; i += loop.step) {
                storage.setByte(i, op.applyByte(storage.getByte(i), value));
            }
        }
    }

    protected void binaryScalarOp(TensorBinaryOp op, byte value) {
        binaryScalarOpStep(op, value);
    }

    @Override
    public BaseByteTensorStride add_(Byte value) {
        binaryScalarOp(TensorBinaryOp.ADD, value);
        return this;
    }

    @Override
    public BaseByteTensorStride sub_(Byte value) {
        binaryScalarOp(TensorBinaryOp.SUB, value);
        return this;
    }

    @Override
    public BaseByteTensorStride mul_(Byte value) {
        binaryScalarOp(TensorBinaryOp.MUL, value);
        return this;
    }

    @Override
    public BaseByteTensorStride div_(Byte value) {
        binaryScalarOp(TensorBinaryOp.DIV, value);
        return this;
    }

    @Override
    public Tensor<Byte> fma_(Byte a, Tensor<Byte> t) {
        if (t.isScalar()) {
            byte tVal = t.getByte(0);
            return add_((byte) (a * tVal));
        }
        if (!shape().equals(t.shape())) {
            throw new IllegalArgumentException("Tensors does not have the same shape.");
        }
        byte aVal = a;
        var order = layout.storageFastOrder();
        order = order == Order.S ? Order.defaultOrder() : order;

        var it = ptrIterator(order);
        var refIt = t.ptrIterator(order);
        while (it.hasNext()) {
            int next = it.nextInt();
            storage.setByte(next, (byte) Math.fma(t.ptrGet(refIt.nextInt()), aVal, storage.getByte(next)));
        }
        return this;
    }

    @Override
    public Byte vdot(Tensor<Byte> tensor) {
        return vdot(tensor, 0, shape().dim(0));
    }

    @Override
    public Byte vdot(Tensor<Byte> tensor, int start, int end) {
        if (shape().rank() != 1 || tensor.shape().rank() != 1 || shape().dim(0) != tensor.shape().dim(0)) {
            throw new IllegalArgumentException(
                    "Operands are not valid for vector dot product (v = %s, v = %s)."
                            .formatted(shape().toString(), tensor.shape().toString()));
        }
        if (start >= end || start < 0 || end > tensor.shape().dim(0)) {
            throw new IllegalArgumentException("Start and end indexes are invalid (start: %d, end: %s).".formatted(start, end));
        }
        BaseByteTensorStride dts = (BaseByteTensorStride) tensor;
        int step1 = layout.stride(0);
        int step2 = dts.layout.stride(0);

        int start1 = layout.offset() + start * step1;
        int end1 = layout.offset() + end * step1;
        int start2 = dts.layout.offset() + start * step2;

        byte sum = 0;
        for (int i = start1; i < end1; i += step1) {
            sum += (byte) (storage.getByte(i) * dts.storage.getByte(start2));
            start2 += step2;
        }
        return sum;
    }

    @Override
    public Tensor<Byte> mv(Tensor<Byte> tensor) {
        if (shape().rank() != 2 || tensor.shape().rank() != 1 || shape().dim(1) != tensor.shape().dim(0)) {
            throw new IllegalArgumentException(
                    STR."Operands are not valid for matrix-vector multiplication \{"(m = %s, v = %s).".formatted(shape(),
                            tensor.shape())}");
        }
        var result = engine.ofByte().storage().zeros(shape().dim(0));
        var it = ptrIterator(Order.C);
        for (int i = 0; i < shape().dim(0); i++) {
            var innerIt = tensor.ptrIterator(Order.C);
            byte sum = 0;
            for (int j = 0; j < shape().dim(1); j++) {
                sum += (byte) (ptrGetByte(it.nextInt()) * tensor.ptrGetByte(innerIt.nextInt()));
            }
            result.setByte(i, sum);
        }
        StrideLayout layout = StrideLayout.ofDense(Shape.of(shape().dim(0)), 0, Order.C);
        return engine.ofByte().stride(layout, result);
    }

    @Override
    public Tensor<Byte> mm(Tensor<Byte> t, Order askOrder) {
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

        var result = engine.ofByte().storage().zeros(m * p);
        var ret = engine.ofByte().stride(StrideLayout.ofDense(Shape.of(m, p), 0, askOrder), result);

        List<Tensor<Byte>> rows = chunk(0, false, 1);
        List<Tensor<Byte>> cols = t.chunk(1, false, 1);

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
                                var krow = (BaseByteTensorStride) rows.get(i);
                                for (int j = c; j < ce; j++) {
                                    result.incByte(i * iStride + j * jStride, krow.vdot(cols.get(j), k, end));
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
    public Statistics<Byte> stats() {
        if (!dtype().isFloat()) {
            throw new IllegalArgumentException("Operation available only for float tensors.");
        }

        int size = size();
        int nanSize = 0;
        byte mean;
        byte nanMean;
        byte variance;
        byte nanVariance;

        // first pass compute raw mean
        byte sum = 0;
        byte nanSum = 0;
        for (int offset : loop.offsets) {
            int i = offset;
            for (; i < loop.bound + offset; i += loop.step) {
                sum += storage.getByte(i);
                if (!dtype().isNaN(storage.getByte(i))) {
                    nanSum += storage.getByte(i);
                    nanSize++;
                }
            }
        }
        mean = (byte) (sum / size);
        nanMean = (byte) (nanSum / nanSize);

        // second pass adjustments for mean
        sum = 0;
        nanSum = 0;
        for (int offset : loop.offsets) {
            int i = offset;
            for (; i < loop.bound + offset; i += loop.step) {
                sum += (byte) (storage.getByte(i) - mean);
                if (!dtype().isNaN(storage.getByte(i))) {
                    nanSum += (byte) (storage.getByte(i) - nanMean);
                }
            }
        }
        mean += (byte) (sum / size);
        nanMean += (byte) (nanSum / nanSize);

        // third pass compute variance
        byte sum2 = 0;
        byte sum3 = 0;
        byte nanSum2 = 0;
        byte nanSum3 = 0;

        for (int offset : loop.offsets) {
            int i = offset;
            for (; i < loop.bound + offset; i += loop.step) {
                sum2 += (byte) ((storage.getByte(i) - mean) * (storage.getByte(i) - mean));
                sum3 += (byte) (storage.getByte(i) - mean);
                if (!dtype().isNaN(storage.getByte(i))) {
                    nanSum2 += (byte) ((storage.getByte(i) - nanMean) * (storage.getByte(i) - nanMean));
                    nanSum3 += (byte) (storage.getByte(i) - nanMean);
                }
            }
        }
        variance = (byte) ((sum2 - (sum3 * sum3) / size) / size);
        nanVariance = (byte) ((nanSum2 - (nanSum3 * nanSum3) / nanSize) / nanSize);

        return new Statistics<>(dtype(), size, nanSize, mean, nanMean, variance, nanVariance);
    }

    @Override
    public Byte sum() {
        return associativeOp(TensorAssociativeOp.ADD);
    }

    @Override
    public Byte nanSum() {
        return nanAssociativeOp(TensorAssociativeOp.ADD);
    }

    @Override
    public Byte prod() {
        return associativeOp(TensorAssociativeOp.MUL);
    }

    @Override
    public Byte nanProd() {
        return nanAssociativeOp(TensorAssociativeOp.MUL);
    }

    @Override
    public Byte max() {
        return associativeOp(TensorAssociativeOp.MAX);
    }

    @Override
    public Byte nanMax() {
        return nanAssociativeOp(TensorAssociativeOp.MAX);
    }

    @Override
    public Byte min() {
        return associativeOp(TensorAssociativeOp.MIN);
    }

    @Override
    public Byte nanMin() {
        return nanAssociativeOp(TensorAssociativeOp.MIN);
    }

    @Override
    public int nanCount() {
        int count = 0;
        for (int offset : loop.offsets) {
            int i = offset;
            for (; i < loop.bound + offset; i += loop.step) {
                if (dtype().isNaN(storage.getByte(i))) {
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
                if (storage.getByte(i) == 0) {
                    count++;
                }
            }
        }
        return count;
    }

    protected byte associativeOp(TensorAssociativeOp op) {
        byte agg = op.initialByte();
        for (int offset : loop.offsets) {
            for (int i = offset; i < loop.bound + offset; i += loop.step) {
                agg = op.applyByte(agg, storage.getByte(i));
            }
        }
        return agg;
    }

    protected byte nanAssociativeOp(TensorAssociativeOp op) {
        byte aggregate = op.initialByte();
        for (int offset : loop.offsets) {
            for (int i = offset; i < loop.bound + offset; i += loop.step) {
                if (!dtype().isNaN(storage.getByte(i))) {
                    aggregate = op.applyByte(aggregate, storage.getByte(i));
                }
            }
        }
        return aggregate;
    }

    @Override
    public Tensor<Byte> copy(Order askOrder) {
        askOrder = Order.autoFC(askOrder);

        var copy = engine.ofByte().storage().zeros(size());
        var dst = engine.ofByte().stride(StrideLayout.ofDense(shape(), 0, askOrder), copy);

        if (layout.storageFastOrder() == askOrder) {
            sameLayoutCopy(copy, askOrder);
        } else {
            copyTo(dst, askOrder);
        }
        return dst;
    }

    private void sameLayoutCopy(Storage<Byte> copy, Order askOrder) {
        var chd = StrideLoopDescriptor.of(layout, askOrder);
        var last = 0;
        for (int ptr : chd.offsets) {
            for (int i = ptr; i < ptr + chd.bound; i += chd.step) {
                copy.setByte(last++, storage.getByte(i));
            }
        }
    }

    @Override
    public Tensor<Byte> copyTo(Tensor<Byte> to, Order askOrder) {

        if (to instanceof BaseByteTensorStride dst) {

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
                                    BaseByteTensorStride s = (BaseByteTensorStride) this.narrowAll(false, ss, es);
                                    BaseByteTensorStride d = (BaseByteTensorStride) dst.narrowAll(false, ss, es);
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

    private void directCopyTo(BaseByteTensorStride src, BaseByteTensorStride dst, Order askOrder) {
        var chd = StrideLoopDescriptor.of(src.layout, askOrder);
        var it2 = dst.ptrIterator(askOrder);
        for (int ptr : chd.offsets) {
            for (int i = ptr; i < ptr + chd.bound; i += chd.step) {
                dst.storage.setByte(it2.nextInt(), src.storage.getByte(i));
            }
        }
    }

    public byte[] toArray() {
        if (shape().rank() != 1) {
            throw new IllegalArgumentException("Only one dimensional tensors can be transformed into array.");
        }
        byte[] copy = new byte[size()];
        int pos = 0;
        for (int offset : loop.offsets) {
            for (int i = offset; i < loop.bound + offset; i++) {
                copy[pos++] = storage.getByte(i);
            }
        }
        return copy;
    }

    public byte[] asArray() {
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
