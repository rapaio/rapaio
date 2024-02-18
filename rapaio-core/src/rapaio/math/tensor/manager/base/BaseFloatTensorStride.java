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

package rapaio.math.tensor.manager.base;

import static rapaio.util.Hardware.CORES;
import static rapaio.util.Hardware.L2_CACHE_SIZE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

import rapaio.data.OperationNotAvailableException;
import rapaio.math.tensor.DType;
import rapaio.math.tensor.Order;
import rapaio.math.tensor.Shape;
import rapaio.math.tensor.Storage;
import rapaio.math.tensor.Tensor;
import rapaio.math.tensor.TensorManager;
import rapaio.math.tensor.iterators.StrideLoopDescriptor;
import rapaio.math.tensor.iterators.StridePointerIterator;
import rapaio.math.tensor.layout.StrideLayout;
import rapaio.math.tensor.layout.StrideWrapper;
import rapaio.math.tensor.manager.AbstractStrideTensor;
import rapaio.math.tensor.operator.TensorAssociativeOp;
import rapaio.math.tensor.operator.TensorBinaryOp;
import rapaio.math.tensor.operator.TensorUnaryOp;
import rapaio.util.collection.IntArrays;
import rapaio.util.function.IntIntBiFunction;

public class BaseFloatTensorStride extends AbstractStrideTensor<Float> {

    public BaseFloatTensorStride(TensorManager engine, StrideLayout layout, Storage<Float> storage) {
        super(engine, layout, storage);
    }

    @Override
    public DType<Float> dtype() {
        return DType.FLOAT;
    }

    @Override
    public Tensor<Float> reshape(Shape askShape, Order askOrder) {
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
        Tensor<Float> copy = engine.zeros(dtype(), askShape, askOrder);
        var copyIt = copy.ptrIterator(Order.C);
        while (it.hasNext()) {
            copy.ptrSetFloat(copyIt.nextInt(), storage.getFloat(it.nextInt()));
        }
        return copy;
    }

    @Override
    public Tensor<Float> flatten(Order askOrder) {
        askOrder = Order.autoFC(askOrder);
        var result = engine.ofFloat().zeros(Shape.of(layout.size()), askOrder);
        var out = result.storage();
        int ptr = 0;
        var it = loopIterator(askOrder);
        while (it.hasNext()) {
            int off = it.nextInt();
            for (int i = 0, p = off; i < it.size(); i++, p += it.step()) {
                out.setFloat(ptr++, storage.getFloat(p));
            }
        }
        return result;
    }

    @Override
    public Float get(int... indexes) {
        return storage.getFloat(layout.pointer(indexes));
    }

    @Override
    public void set(Float value, int... indexes) {
        storage.setFloat(layout.pointer(indexes), value);
    }

    @Override
    public void inc(Float value, int... indexes) {
        storage.incFloat(layout.pointer(indexes), value);
    }

    @Override
    public Float ptrGet(int ptr) {
        return storage.getFloat(ptr);
    }

    @Override
    public void ptrSet(int ptr, Float value) {
        storage.setFloat(ptr, value);
    }

    @Override
    public BaseFloatTensorStride apply_(Order askOrder, IntIntBiFunction<Float> apply) {
        var it = ptrIterator(askOrder);
        int i = 0;
        while (it.hasNext()) {
            int p = it.nextInt();
            storage.setFloat(p, apply.applyAsInt(i++, p));
        }
        return this;
    }

    @Override
    public Tensor<Float> apply_(Function<Float, Float> fun) {
        var ptrIter = ptrIterator(Order.S);
        while (ptrIter.hasNext()) {
            int ptr = ptrIter.nextInt();
            storage.setFloat(ptr, fun.apply(storage.getFloat(ptr)));
        }
        return this;
    }

    @Override
    public Tensor<Float> fill_(Float value) {
        for (int offset : loop.offsets) {
            if (loop.step == 1) {
                storage.fillFloat(value, offset, loop.size);
            } else {
                for (int i = 0, p = offset; i < loop.size; i++, p += loop.step) {
                    storage.setFloat(p, value);
                }
            }
        }
        return this;
    }

    @Override
    public Tensor<Float> fillNan_(Float value) {
        for (int offset : loop.offsets) {
            for (int i = 0, p = offset; i < loop.size; i++, p += loop.step) {
                if (dtype().isNaN(storage.getFloat(p))) {
                    storage.setFloat(p, value);
                }
            }
        }
        return this;
    }

    @Override
    public Tensor<Float> clamp_(Float min, Float max) {
        for (int offset : loop.offsets) {
            for (int i = 0, p = offset; i < loop.size; i++, p += loop.step) {
                if (!dtype().isNaN(min) && storage.getFloat(p) < min) {
                    storage.setFloat(p, min);
                }
                if (!dtype().isNaN(max) && storage.getFloat(p) > max) {
                    storage.setFloat(p, max);
                }
            }
        }
        return this;
    }

    private void unaryOpUnit(TensorUnaryOp op) {
        for (int off : loop.offsets) {
            for (int i = off; i < loop.size + off; i++) {
                storage.setFloat(i, op.applyFloat(storage.getFloat(i)));
            }
        }
    }

    private void unaryOpStep(TensorUnaryOp op) {
        for (int off : loop.offsets) {
            for (int i = 0, p = off; i < loop.size; i++, p += loop.step) {
                storage.setFloat(p, op.applyFloat(storage.getFloat(p)));
            }
        }
    }

    @Override
    protected void unaryOp(TensorUnaryOp op) {
        if (op.floatingPointOnly() && !dtype().floatingPoint()) {
            throw new IllegalArgumentException("This operation is available only for floating point tensors.");
        }
        if (loop.step == 1) {
            unaryOpUnit(op);
        } else {
            unaryOpStep(op);
        }
    }

    @Override
    protected void binaryVectorOp(TensorBinaryOp op, Tensor<Float> b) {
        if (b.isScalar()) {
            binaryScalarOp(op, b.getFloat());
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
            storage.setFloat(next, op.applyFloat(storage.getFloat(next), b.ptrGet(refIt.nextInt())));
        }
    }

    void binaryScalarOpStep(TensorBinaryOp op, float value) {
        for (int offset : loop.offsets) {
            for (int i = 0, p = offset; i < loop.size; i++, p += loop.step) {
                storage.setFloat(p, op.applyFloat(storage.getFloat(p), value));
            }
        }
    }

    @Override
    protected void binaryScalarOp(TensorBinaryOp op, Float value) {
        binaryScalarOpStep(op, value);
    }

    @Override
    public Tensor<Float> fma_(Float a, Tensor<Float> t) {
        if (t.isScalar()) {
            float tVal = t.getFloat(0);
            return add_((float) (a * tVal));
        }
        if (!shape().equals(t.shape())) {
            throw new IllegalArgumentException("Tensors does not have the same shape.");
        }
        float aVal = a;
        var order = layout.storageFastOrder();
        order = order == Order.S ? Order.defaultOrder() : order;

        var it = ptrIterator(order);
        var refIt = t.ptrIterator(order);
        while (it.hasNext()) {
            int next = it.nextInt();
            storage.setFloat(next, (float) Math.fma(t.ptrGet(refIt.nextInt()), aVal, storage.getFloat(next)));
        }
        return this;
    }

    @Override
    public Float vdot(Tensor<Float> tensor) {
        return vdot(tensor, 0, shape().dim(0));
    }

    @Override
    public Float vdot(Tensor<Float> tensor, int start, int end) {
        if (shape().rank() != 1 || tensor.shape().rank() != 1 || shape().dim(0) != tensor.shape().dim(0)) {
            throw new IllegalArgumentException(
                    "Operands are not valid for vector dot product (v = %s, v = %s)."
                            .formatted(shape().toString(), tensor.shape().toString()));
        }
        if (start >= end || start < 0 || end > tensor.shape().dim(0)) {
            throw new IllegalArgumentException("Start and end indexes are invalid (start: %d, end: %s).".formatted(start, end));
        }
        BaseFloatTensorStride dts = (BaseFloatTensorStride) tensor;
        int step1 = layout.stride(0);
        int step2 = dts.layout.stride(0);

        int start1 = layout.offset() + start * step1;
        int end1 = layout.offset() + end * step1;
        int start2 = dts.layout.offset() + start * step2;

        float sum = 0;
        for (int i = start1; i < end1; i += step1) {
            sum += (float) (storage.getFloat(i) * dts.storage.getFloat(start2));
            start2 += step2;
        }
        return sum;
    }

    @Override
    public Tensor<Float> vpadCopy(int before, int after) {
        if (!isVector()) {
            throw new IllegalArgumentException("This operation is available only for vectors.");
        }
        Storage<Float> newStorage = engine.storage().ofFloat().zeros(before + dim(0) + after);
        var loop = loopIterator();
        while (loop.hasNext()) {
            int offset = loop.next();
            for (int i = 0; i < loop.size(); i++) {
                newStorage.setFloat(before + i, ptrGetFloat(offset + i * loop.step()));
            }
        }
        return engine.ofFloat().stride(Shape.of(before + dim(0) + after), Order.C, newStorage);
    }

    @Override
    public Tensor<Float> mv(Tensor<Float> tensor) {
        if (shape().rank() != 2 || tensor.shape().rank() != 1 || shape().dim(1) != tensor.shape().dim(0)) {
            throw new IllegalArgumentException(
                    STR."Operands are not valid for matrix-vector multiplication \{"(m = %s, v = %s).".formatted(shape(),
                            tensor.shape())}");
        }
        var result = engine.ofFloat().storage().zeros(shape().dim(0));
        var it = ptrIterator(Order.C);
        for (int i = 0; i < shape().dim(0); i++) {
            var innerIt = tensor.ptrIterator(Order.C);
            float sum = 0;
            for (int j = 0; j < shape().dim(1); j++) {
                sum += (float) (ptrGetFloat(it.nextInt()) * tensor.ptrGetFloat(innerIt.nextInt()));
            }
            result.setFloat(i, sum);
        }
        StrideLayout layout = StrideLayout.ofDense(Shape.of(shape().dim(0)), 0, Order.C);
        return engine.ofFloat().stride(layout, result);
    }

    @Override
    public Tensor<Float> mm(Tensor<Float> t, Order askOrder) {
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

        var result = engine.ofFloat().storage().zeros(m * p);
        var ret = engine.ofFloat().stride(StrideLayout.ofDense(Shape.of(m, p), 0, askOrder), result);

        List<Tensor<Float>> rows = chunk(0, false, 1);
        List<Tensor<Float>> cols = t.chunk(1, false, 1);

        int chunk = (int) Math.floor(Math.sqrt(L2_CACHE_SIZE / 2. / CORES / dtype().byteCount()));
        chunk = chunk >= 8 ? chunk - chunk % 8 : chunk;

        int vectorChunk = chunk > 64 ? chunk * 4 : chunk;
        int innerChunk = chunk > 64 ? (int) Math.ceil(Math.sqrt(chunk / 4.)) : (int) Math.ceil(Math.sqrt(chunk));

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
                                var krow = (BaseFloatTensorStride) rows.get(i);
                                for (int j = c; j < ce; j++) {
                                    result.incFloat(i * iStride + j * jStride, krow.vdot(cols.get(j), k, end));
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
    public Tensor<Float> scatter() {
        if (!isMatrix()) {
            throw new IllegalArgumentException("Scatter matrix can be computed only for matrices.");
        }
        Tensor<Float> scatter = engine.ofFloat().zeros(Shape.of(dim(1), dim(1)));
        Tensor<Float> mean = engine.ofFloat().zeros(Shape.of(dim(1)));
        for (int i = 0; i < dim(1); i++) {
            mean.setFloat((float) take(1, i).mean(), i);
        }
        for (int k = 0; k < dim(0); k++) {
            Tensor<Float> row = take(0, k).squeeze(0).sub(mean);
            for (int i = 0; i < row.size(); i++) {
                for (int j = 0; j < row.size(); j++) {
                    scatter.incFloat((float) (row.getFloat(i) * row.getFloat(j)), i, j);
                }
            }
        }
        return scatter;
    }

    @Override
    public Float trace() {
        if (!isMatrix()) {
            throw new OperationNotAvailableException("This operation is available only on tensor matrix.");
        }
        if (dim(0) != dim(1)) {
            throw new OperationNotAvailableException("This operation is available only on a square matrix.");
        }
        float trace = 0;
        for (int i = 0; i < dim(0); i++) {
            trace += getFloat(i, i);
        }
        return trace;
    }

    @Override
    public Float norm(Float pow) {
        if (pow < 0) {
            throw new IllegalArgumentException(STR."Norm power p=\{pow} must have a value greater than 0.");
        }
        if (dtype().castValue(1).equals(pow)) {
            return abs().sum();
        }
        if (dtype().castValue(2).equals(pow)) {
            return (float) Math.sqrt(sqr().sum());
        }

        float sum = (float) 0;
        var it = loopIterator();
        while (it.hasNext()) {
            int offset = it.next();
            for (int i = 0, p = offset; i < it.size(); i++, p += it.step()) {
                float value = (float) Math.abs(storage.getFloat(p));
                sum += (float) Math.pow(value, pow);
            }
        }
        return (float) Math.pow(sum, 1. / pow);
    }

    @Override
    public Tensor<Float> normalize_(Float pow) {
        return div_(norm(pow));
    }

    @Override
    protected Tensor<Float> alongAxisOperation(Order order, int axis, Function<Tensor<Float>, Float> op) {
        int[] newDims = layout.shape().narrowDims(axis);
        int[] newStrides = layout.narrowStrides(axis);
        int selDim = layout.dim(axis);
        int selStride = layout.stride(axis);

        Tensor<Float> res = engine.ofFloat().zeros(Shape.of(newDims), Order.autoFC(order));
        var resIt = res.ptrIterator(Order.C);
        var it = new StridePointerIterator(StrideLayout.of(newDims, layout().offset(), newStrides), Order.C);
        while (it.hasNext()) {
            int ptr = it.nextInt();
            var stride = engine.ofFloat().stride(StrideLayout.of(Shape.of(selDim), ptr, new int[] {selStride}), storage);
            res.ptrSet(resIt.next(), op.apply(stride));
        }
        return res;
    }

    @Override
    public Float mean() {
        if (!dtype().floatingPoint()) {
            throw new IllegalArgumentException("Operation available only for float tensors.");
        }
        int size = size();
        // first pass compute raw mean
        float sum = 0;
        for (int off : loop.offsets) {
            for (int i = 0; i < loop.size; i++) {
                sum += storage.getFloat(off + i * loop.step);
            }
        }
        float mean = (float) (sum / size);
        // second pass adjustments for mean
        sum = 0;
        for (int off : loop.offsets) {
            for (int i = 0; i < loop.size; i++) {
                sum += (float) (storage.getFloat(off + i * loop.step) - mean);
            }
        }
        return (float) (mean + sum / size);
    }

    @Override
    public Float nanMean() {
        if (!dtype().floatingPoint()) {
            throw new IllegalArgumentException("Operation available only for float tensors.");
        }
        int size = size() - nanCount();
        // first pass compute raw mean
        float sum = nanSum();

        float mean = (float) (sum / size);
        // second pass adjustments for mean
        sum = 0;
        for (int off : loop.offsets) {
            for (int i = 0; i < loop.size; i++) {
                float v = storage.getFloat(off + i * loop.step);
                if (dtype().isNaN(v)) {
                    continue;
                }
                sum += (float) (v - mean);
            }
        }
        return (float) (mean + sum / size);
    }

    @Override
    public Float var() {
        if (!dtype().floatingPoint()) {
            throw new IllegalArgumentException("Operation available only for float tensors.");
        }
        int size = size();
        // first pass compute raw mean
        float sum = 0;
        for (int offset : loop.offsets) {
            for (int i = 0; i < loop.size; i++) {
                sum += storage.getFloat(offset + i * loop.step);
            }
        }
        float mean = (float) (sum / size);
        // second pass adjustments for mean
        sum = 0;
        for (int offset : loop.offsets) {
            for (int i = 0; i < loop.size; i++) {
                sum += (float) (storage.getFloat(offset + i * loop.step) - mean);
            }
        }
        mean += (float) (sum / size);
        // third pass compute variance
        float sum2 = 0;
        float sum3 = 0;
        for (int offset : loop.offsets) {
            for (int i = 0, p = offset; i < loop.size; i++, p += loop.step) {
                sum2 += (float) ((storage.getFloat(p) - mean) * (storage.getFloat(p) - mean));
                sum3 += (float) (storage.getFloat(p) - mean);
            }
        }
        return (float) ((sum2 - (sum3 * sum3) / size) / size);
    }

    @Override
    public Float varc(int ddof) {
        if (!dtype().floatingPoint()) {
            throw new IllegalArgumentException("Operation available only for float tensors.");
        }
        int size = size();
        // first pass compute raw mean
        float sum = 0;
        for (int offset : loop.offsets) {
            for (int i = 0; i < loop.size; i++) {
                sum += storage.getFloat(offset + i * loop.step);
            }
        }
        float mean = (float) (sum / size);
        // second pass adjustments for mean
        sum = 0;
        for (int offset : loop.offsets) {
            for (int i = 0; i < loop.size; i++) {
                sum += (float) (storage.getFloat(offset + i * loop.step) - mean);
            }
        }
        mean += (float) (sum / size);
        // third pass compute variance
        float sum2 = 0;
        float sum3 = 0;
        for (int offset : loop.offsets) {
            for (int i = 0, p = offset; i < loop.size; i++, p += loop.step) {
                sum2 += (float) ((storage.getFloat(p) - mean) * (storage.getFloat(p) - mean));
                sum3 += (float) (storage.getFloat(p) - mean);
            }
        }
        return (float) ((sum2 - (sum3 * sum3) / (size - ddof)) / (size - ddof));
    }

    @Override
    public int argmax(Order order) {
        int argmax = -1;
        float argvalue = TensorAssociativeOp.MAX.initFloat();
        var i = 0;
        var it = loopIterator(order);
        while (it.hasNext()) {
            int offset = it.next();
            for (int j = 0; j < loop.size; j++) {
                float value = storage.getFloat(offset + j * loop.step);
                if (value > argvalue) {
                    argvalue = value;
                    argmax = i;
                }
                i++;
            }
        }
        return argmax;
    }

    @Override
    public int argmin(Order order) {
        int argmin = -1;
        float argvalue = TensorAssociativeOp.MIN.initFloat();
        var i = 0;
        var it = loopIterator(order);
        while (it.hasNext()) {
            int offset = it.next();
            for (int j = 0; j < loop.size; j++) {
                float value = storage.getFloat(offset + j * loop.step);
                if (value < argvalue) {
                    argvalue = value;
                    argmin = i;
                }
                i++;
            }
        }
        return argmin;
    }

    @Override
    public int nanCount() {
        int count = 0;
        for (int offset : loop.offsets) {
            for (int i = 0, p = offset; i < loop.size; i++, p += loop.step) {
                if (dtype().isNaN(storage.getFloat(p))) {
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
            for (int i = 0, p = offset; i < loop.size; i++, p += loop.step) {
                if (storage.getFloat(p) == 0) {
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    protected Float associativeOp(TensorAssociativeOp op) {
        float agg = op.initFloat();
        for (int offset : loop.offsets) {
            for (int i = 0, p = offset; i < loop.size; i++, p += loop.step) {
                agg = op.aggFloat(agg, storage.getFloat(p));
            }
        }
        return agg;
    }

    @Override
    protected Float nanAssociativeOp(TensorAssociativeOp op) {
        float aggregate = op.initFloat();
        for (int offset : loop.offsets) {
            for (int i = 0, p = offset; i < loop.size; i++, p += loop.step) {
                if (!dtype().isNaN(storage.getFloat(p))) {
                    aggregate = op.aggFloat(aggregate, storage.getFloat(p));
                }
            }
        }
        return aggregate;
    }

    @Override
    protected Tensor<Float> associativeOpNarrow(TensorAssociativeOp op, Order order, int axis) {
        int[] newDims = layout.shape().narrowDims(axis);
        int[] newStrides = layout.narrowStrides(axis);
        int selDim = layout.dim(axis);
        int selStride = layout.stride(axis);

        Tensor<Float> res = engine.ofFloat().zeros(Shape.of(newDims), Order.autoFC(order));
        var it = new StridePointerIterator(StrideLayout.of(newDims, layout().offset(), newStrides), Order.C);
        var resIt = res.ptrIterator(Order.C);
        while (it.hasNext()) {
            int ptr = it.nextInt();
            float value = StrideWrapper.of(ptr, selStride, selDim, this).aggregate(op.initFloat(), op::aggFloat);
            res.ptrSet(resIt.next(), value);
        }
        return res;
    }

    @Override
    protected Tensor<Float> nanAssociativeOpNarrow(TensorAssociativeOp op, Order order, int axis) {
        int[] newDims = layout.shape().narrowDims(axis);
        int[] newStrides = layout.narrowStrides(axis);
        int selDim = layout.dim(axis);
        int selStride = layout.stride(axis);

        Tensor<Float> res = engine.ofFloat().zeros(Shape.of(newDims), Order.autoFC(order));
        var it = new StridePointerIterator(StrideLayout.of(newDims, layout().offset(), newStrides), Order.C);
        var resIt = res.ptrIterator(Order.C);
        while (it.hasNext()) {
            int ptr = it.nextInt();
            float value = StrideWrapper.of(ptr, selStride, selDim, this).nanAggregate(DType.FLOAT, op.initFloat(), op::aggFloat);
            res.ptrSet(resIt.next(), value);
        }
        return res;
    }

    @Override
    public Tensor<Float> copy(Order askOrder) {
        askOrder = Order.autoFC(askOrder);

        var copy = engine.ofFloat().storage().zeros(size());
        var dst = engine.ofFloat().stride(StrideLayout.ofDense(shape(), 0, askOrder), copy);

        if (layout.storageFastOrder() == askOrder) {
            sameLayoutCopy(copy, askOrder);
        } else {
            copyTo(dst, askOrder);
        }
        return dst;
    }

    private void sameLayoutCopy(Storage<Float> copy, Order askOrder) {
        var loop = StrideLoopDescriptor.of(layout, askOrder);
        var last = 0;
        for (int offset : loop.offsets) {
            for (int i = 0, p = offset; i < loop.size; i++, p += loop.step) {
                copy.setFloat(last++, storage.getFloat(p));
            }
        }
    }

    @Override
    public Tensor<Float> copyTo(Tensor<Float> to, Order askOrder) {

        if (to instanceof BaseFloatTensorStride dst) {

            int limit = Math.floorDiv(L2_CACHE_SIZE, dtype().byteCount() * 2 * engine.cpuThreads() * 8);

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
                                    BaseFloatTensorStride s = (BaseFloatTensorStride) this.narrowAll(false, ss, es);
                                    BaseFloatTensorStride d = (BaseFloatTensorStride) dst.narrowAll(false, ss, es);
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

    private void directCopyTo(BaseFloatTensorStride src, BaseFloatTensorStride dst, Order askOrder) {
        var loop = StrideLoopDescriptor.of(src.layout, askOrder);
        var it2 = dst.ptrIterator(askOrder);
        for (int offset : loop.offsets) {
            for (int i = 0, p = offset; i < loop.size; i++, p += loop.step) {
                dst.storage.setFloat(it2.nextInt(), src.storage.getFloat(p));
            }
        }
    }

    @Override
    public String toString() {
        String strDIms = Arrays.toString(layout.dims());
        String strStrides = Arrays.toString(layout.strides());
        return STR."BaseStride{\{dtype().id()},\{strDIms},\{layout.offset()},\{strStrides}}\n\{toContent()}";
    }
}