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
import rapaio.math.tensor.Layout;
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
import rapaio.math.tensor.operator.Broadcast;
import rapaio.math.tensor.operator.TensorBinaryOp;
import rapaio.math.tensor.operator.TensorOp;
import rapaio.math.tensor.operator.TensorReduceOp;
import rapaio.math.tensor.operator.TensorUnaryOp;
import rapaio.printer.Format;
import rapaio.util.collection.IntArrays;
import rapaio.util.function.IntIntBiFunction;

public final class BaseFloatTensorStride extends AbstractStrideTensor<Float> {

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
        if (Order.A == askOrder) {
            if (layout.isCOrdered()) {
                askOrder = Order.C;
            } else if (layout.isFOrdered()) {
                askOrder = Order.F;
            } else {
                askOrder = Order.defaultOrder();
            }
        }
        if (Order.S == askOrder) {
            throw new IllegalArgumentException("Illegal order specification.");
        }
        StrideLayout newLayout = layout.attemptReshape(askShape, askOrder);
        if (newLayout != null) {
            return manager.ofFloat().stride(newLayout, storage);
        }
        var it = new StridePointerIterator(layout, askOrder);
        Tensor<Float> copy = manager.ofFloat().zeros(askShape, askOrder);
        var copyIt = copy.ptrIterator(askOrder);
        while (it.hasNext()) {
            copy.ptrSetFloat(copyIt.nextInt(), storage.getFloat(it.nextInt()));
        }
        return copy;
    }

    @Override
    public Tensor<Float> flatten(Order askOrder) {
        askOrder = Order.autoFC(askOrder);
        var result = manager.ofFloat().zeros(Shape.of(layout.size()), askOrder);
        var out = result.storage();
        int ptr = 0;
        var loop = StrideLoopDescriptor.of(layout, askOrder, dtype().vectorSpecies());
        for (int p : loop.offsets) {
            for (int i = 0; i < loop.size; i++) {
                out.setFloat(ptr++, storage.getFloat(p));
                p += loop.step;
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
    public Tensor<Float> unaryOp_(TensorUnaryOp op) {
        if (op.floatingPointOnly() && !dtype().floatingPoint()) {
            throw new IllegalArgumentException("This operation is available only for floating point tensors.");
        }
        op.applyFloat(loop, storage);
        return this;
    }

    @Override
    public Tensor<Float> binaryOp_(TensorBinaryOp op, Tensor<?> other) {
        if (other.isScalar()) {
            return binaryOp_(op, other.getFloat());
        }
        Broadcast.ElementWise broadcast = Broadcast.elementWise(List.of(this, other));
        if (!broadcast.valid()) {
            throw new IllegalArgumentException(
                    String.format("Operation could not be applied on tensors with shape: %s, %s", shape(), other.shape()));
        }
        if (!broadcast.hasShape(this)) {
            throw new IllegalArgumentException("Broadcast cannot be applied for inplace operations.");
        }
        other = broadcast.transform(other);
        var order = layout.storageFastOrder();
        order = order == Order.S ? Order.defaultOrder() : order;

        var it = ptrIterator(order);
        var refIt = other.ptrIterator(order);
        while (it.hasNext()) {
            int next = it.nextInt();
            storage.setFloat(next, op.applyFloat(storage.getFloat(next), other.ptrGetFloat(refIt.nextInt())));
        }
        return this;
    }

    @Override
    public <M extends Number> Tensor<Float> binaryOp_(TensorBinaryOp op, M value) {
        float v = value.floatValue();
        for (int p : loop.offsets) {
            for (int i = 0; i < loop.size; i++) {
                storage.setFloat(p, op.applyFloat(storage.getFloat(p), v));
                p += loop.step;
            }
        }
        return this;
    }

    @Override
    public Tensor<Float> fma_(Float a, Tensor<?> t) {
        if (t.isScalar()) {
            float tVal = t.getFloat();
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
            storage.setFloat(next, (float) Math.fma(t.ptrGetFloat(refIt.nextInt()), aVal, storage.getFloat(next)));
        }
        return this;
    }

    // LINEAR ALGEBRA OPERATIONS

    @Override
    public Float inner(Tensor<?> other) {
        return inner(other, 0, shape().dim(0));
    }

    @Override
    public Float inner(Tensor<?> other, int start, int end) {
        if (shape().rank() != 1 || other.shape().rank() != 1 || shape().dim(0) != other.shape().dim(0)) {
            throw new IllegalArgumentException(
                    "Operands are not valid for vector dot product (v = %s, v = %s)."
                            .formatted(shape().toString(), other.shape().toString()));
        }
        if (start >= end || start < 0 || end > other.shape().dim(0)) {
            throw new IllegalArgumentException("Start and end indexes are invalid (start: %d, end: %s).".formatted(start, end));
        }
        BaseFloatTensorStride dts = (BaseFloatTensorStride) other;

        int offset1 = layout.offset();
        int offset2 = dts.layout.offset();
        int step1 = layout.stride(0);
        int step2 = dts.layout.stride(0);

        float sum = 0;
        for (int i = start; i < end; i++) {
            sum += (float) (storage.getFloat(offset1 + i * step1) * dts.storage.getFloat(offset2 + i * step2));
        }
        return sum;
    }

    @Override
    public Tensor<Float> mv(Tensor<?> other, Order askOrder) {
        if (shape().rank() != 2 || other.shape().rank() != 1 || shape().dim(1) != other.shape().dim(0)) {
            throw new IllegalArgumentException(
                    String.format("Operands are not valid for matrix-vector multiplication (m = %s, v = %s).",
                            shape(), other.shape()));
        }
        var result = manager.ofFloat().zeros(Shape.of(shape().dim(0)), askOrder);
        for (int i = 0; i < shape().dim(0); i++) {
            result.ptrSetFloat(i, takesq(0, i).inner(other));
        }
        return result;
    }

    @Override
    public Tensor<Float> bmv(Tensor<?> other, Order askOrder) {
        BaseFloatTensorStride a = this;
        Tensor<?> b = other;
        if (a.isScalar()) {
            a = (BaseFloatTensorStride) a.strexp(0, 1).strexp(1, 1);
        }
        if (other.isScalar()) {
            b = b.strexp(0, 1);
        }
        if (a.rank() == 2 && b.rank() == 1 && a.dim(1) == b.dim(0)) {
            // simple case, create a batch of 1 for each element
            return ((BaseFloatTensorStride) a.stretch(0)).bmvInternal(b.stretch(0), askOrder);
        }
        if (a.rank() == 3 && b.rank() == 1 && a.dim(2) == b.dim(0)) {
            // batch on matrix, add batch to vector
            return a.bmvInternal(b.strexp(0, a.dim(0)), askOrder);
        }
        if (a.rank() == 2 && b.rank() == 2 && a.dim(1) == b.dim(1)) {
            // batch on vector, add batch to matrix
            return ((BaseFloatTensorStride) a.strexp(0, b.dim(0))).bmvInternal(b, askOrder);
        }
        if (a.rank() == 3 && b.rank() == 2 && a.dim(2) == b.dim(1) && a.dim(0) == b.dim(0)) {
            // no need of batching
            return a.bmvInternal(b, askOrder);
        }
        throw new IllegalArgumentException(String.format(
                "Tensors are not valid for batch matrix vector multiplication (bm : %s, bv = %s)", shape(), other.shape()));
    }

    private Tensor<Float> bmvInternal(Tensor<?> other, Order askOrder) {
        Tensor<Float> res = manager.ofFloat().zeros(Shape.of(dim(0), dim(1)), askOrder);
        for (int b = 0; b < dim(0); b++) {
            takesq(0, b).mv(other.takesq(0, b)).copyTo(res.takesq(0, b));
        }
        return res;
    }

    @Override
    public Tensor<Float> vtm(Tensor<?> other, Order askOrder) {
        if (shape().rank() != 1 || other.rank() != 2 || shape().dim(0) != other.dim(0)) {
            throw new IllegalArgumentException(
                    String.format("Operands are not valid for vector transpose matrix multiplication (v = %s, m = %s).",
                            shape(), other.shape())
            );
        }
        var result = manager.ofFloat().zeros(Shape.of(other.dim(1)), askOrder);
        for (int i = 0; i < other.dim(1); i++) {
            result.ptrSetFloat(i, this.inner(other.takesq(1, i)));
        }
        return result;
    }

    @Override
    public Tensor<?> bvtm(Tensor<?> other, Order askOrder) {
        BaseFloatTensorStride a = this;
        Tensor<?> b = other;
        if (a.isScalar()) {
            a = (BaseFloatTensorStride) a.stretch(0);
        }
        if (other.isScalar()) {
            b = b.stretch(0, 1);
        }
        if (a.rank() == 1 && b.rank() == 2 && a.dim(0) == b.dim(0)) {
            // simple case, create a batch of 1 for each element
            return ((BaseFloatTensorStride) a.stretch(0)).bvtmInternal(b.stretch(0), askOrder);
        }
        if (a.rank() == 2 && b.rank() == 2 && a.dim(1) == b.dim(0)) {
            // batch on vector, add batch to matrix
            return a.bvtmInternal(b.strexp(0, a.dim(0)), askOrder);
        }
        if (a.rank() == 1 && b.rank() == 3 && a.dim(0) == b.dim(1)) {
            // batch on matrix, add batch to vector
            return ((BaseFloatTensorStride) a.strexp(0, b.dim(0))).bvtmInternal(b, askOrder);
        }
        if (a.rank() == 2 && b.rank() == 3 && a.dim(1) == b.dim(1) && a.dim(0) == b.dim(0)) {
            // no need of batching
            return a.bvtmInternal(b, askOrder);
        }
        throw new IllegalArgumentException(String.format(
                "Tensors are not valid for batch vector transpose matrix multiplication (bv : %s, bm = %s)", shape(), other.shape()));
    }

    private Tensor<Float> bvtmInternal(Tensor<?> other, Order askOrder) {
        Tensor<Float> res = manager.ofFloat().zeros(Shape.of(dim(0), other.dim(2)), askOrder);
        for (int b = 0; b < dim(0); b++) {
            takesq(0, b).vtm(other.takesq(0, b)).copyTo(res.takesq(0, b));
        }
        return res;
    }

    @Override
    public Tensor<Float> mm(Tensor<?> other, Order askOrder) {
        if (shape().rank() != 2 || other.shape().rank() != 2 || shape().dim(1) != other.shape().dim(0)) {
            throw new IllegalArgumentException(
                    String.format("Operands are not valid for matrix-matrix multiplication (m = %s, v = %s).", shape(), other.shape()));
        }
        if (askOrder == Order.S) {
            throw new IllegalArgumentException("Illegal askOrder value, must be Order.C or Order.F");
        }
        var ret = manager.ofFloat().zeros(Shape.of(shape().dim(0), other.shape().dim(1)), askOrder);
        return mmInternal(other, ret);
    }

    private Tensor<Float> mmInternal(Tensor<?> other, Tensor<Float> to) {
        int m = shape().dim(0);
        int n = shape().dim(1);
        int p = other.shape().dim(1);

        List<Tensor<Float>> rows = chunk(0, false, 1);
        List<Tensor<Float>> cols = other.cast(dtype()).chunk(1, false, 1);

        int chunk = (int) Math.floor(Math.sqrt(L2_CACHE_SIZE / 2. / CORES / dtype().byteCount()));
        chunk = chunk >= 8 ? chunk - chunk % 8 : chunk;

        int vectorChunk = chunk > 64 ? chunk * 4 : chunk;
        int innerChunk = chunk > 64 ? (int) Math.ceil(Math.sqrt(chunk / 4.)) : (int) Math.ceil(Math.sqrt(chunk));

        int off = ((StrideLayout) to.layout()).offset();
        int iStride = ((StrideLayout) to.layout()).stride(0);
        int jStride = ((StrideLayout) to.layout()).stride(1);

        List<Future<?>> futures = new ArrayList<>();
        try (ExecutorService service = Executors.newFixedThreadPool(manager.cpuThreads())) {
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
                                    float value = to.ptrGetFloat(off + i * iStride + j * jStride);
                                    to.ptrSetFloat(off + i * iStride + j * jStride, (float) (value + krow.inner(cols.get(j), k, end)));
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
        return to;
    }

    @Override
    public Tensor<Float> bmm(Tensor<?> other, Order askOrder) {
        if (rank() == 2 && other.rank() == 2 && dim(1) == other.dim(0)) {
            return ((BaseFloatTensorStride) stretch(0)).bmmInternal(other.stretch(0), askOrder);
        }
        if (rank() == 3 && other.rank() == 2 && dim(2) == other.dim(0)) {
            return bmmInternal(other.strexp(0, dim(0)), askOrder);
        }
        if (rank() == 2 && other.rank() == 3 && dim(1) == other.dim(1)) {
            return ((BaseFloatTensorStride) strexp(0, other.dim(0))).bmmInternal(other, askOrder);
        }
        if (rank() == 3 && other.rank() == 3 && dim(0) == other.dim(0) && dim(2) == other.dim(1)) {
            return bmmInternal(other, askOrder);
        }
        throw new IllegalArgumentException(String.format(
                "Tensors are not valid for batch matrix-matrix multiplication (bm1: %s, bm2: %s)", shape(), other.shape()));
    }

    private Tensor<Float> bmmInternal(Tensor<?> other, Order askOrder) {
        Tensor<Float> res = manager.ofFloat().zeros(Shape.of(dim(0), dim(1), other.dim(2)), askOrder);
        for (int b = 0; b < dim(0); b++) {
            ((BaseFloatTensorStride) takesq(0, b)).mmInternal(other.takesq(0, b), res.takesq(0, b));
        }
        return res;
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
    public Tensor<Float> diag(int diagonal) {
        if (isScalar() && diagonal == 0) {
            return this;
        }
        if (isVector()) {
            int n = dim(0) + Math.abs(diagonal);
            Tensor<Float> m = manager.ofFloat().zeros(Shape.of(n, n));
            for (int i = 0; i < dim(0); i++) {
                m.setFloat(getFloat(i), i + Math.abs(Math.min(diagonal, 0)), i + Math.max(diagonal, 0));
            }
            return m;
        }
        if (isMatrix()) {
            int d = diagonal >= 0 ? dim(1) : dim(0);
            int len = diagonal >= 0 ? d - diagonal : d + diagonal;
            if (len <= 0) {
                throw new IllegalArgumentException("Diagonal " + diagonal + " does not exists for shape " + shape() + ".");
            }
            float[] diag = new float[len];
            for (int i = 0; i < len; i++) {
                diag[i] = getFloat(i + Math.abs(Math.min(diagonal, 0)), i + Math.max(diagonal, 0));
            }
            return manager().ofFloat().stride(Shape.of(len), diag);
        }
        throw new OperationNotAvailableException("This operation is available for tensors with shape " + shape() + ".");
    }

    @Override
    public Float norm(Float pow) {
        if (!dtype().floatingPoint()) {
            throw new OperationNotAvailableException("This operation is only available on floating point data types.");
        }
        if (pow < 0) {
            throw new IllegalArgumentException(String.format("Norm power p=%s must be greater or equal with 0.", Format.floatFlex(pow)));
        }
        if (dtype().castValue(0).equals(pow)) {
            return (float) shape().size();
        }
        if (dtype().castValue(1).equals(pow)) {
            return abs().sum();
        }
        if (dtype().castValue(2).equals(pow)) {
            return (float) Math.sqrt(sqr().sum());
        }
        float sum = (float) 0;
        var loop = StrideLoopDescriptor.of(layout, Order.S, dtype().vectorSpecies());
        for (int p : loop.offsets) {
            for (int i = 0; i < loop.size; i++) {
                float value = (float) Math.abs(storage.getFloat(p));
                sum += (float) Math.pow(value, pow);
                p += loop.step;
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

        Tensor<Float> res = manager.ofFloat().zeros(Shape.of(newDims), Order.autoFC(order));
        var resIt = res.ptrIterator(Order.C);
        var it = new StridePointerIterator(StrideLayout.of(newDims, layout().offset(), newStrides), Order.C);
        while (it.hasNext()) {
            int ptr = it.nextInt();
            var stride = manager.ofFloat().stride(StrideLayout.of(Shape.of(selDim), ptr, new int[] {selStride}), storage);
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
        for (int p : loop.offsets) {
            for (int i = 0; i < loop.size; i++) {
                sum += (float) (storage.getFloat(p) - mean);
                p += loop.step;
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
        for (int p : loop.offsets) {
            for (int i = 0; i < loop.size; i++) {
                float v = storage.getFloat(p);
                p += loop.step;
                if (dtype().isNaN(v)) {
                    continue;
                }
                sum += (float) (v - mean);
            }
        }
        return (float) (mean + sum / size);
    }

    @Override
    public Float varc(int ddof) {
        if (!dtype().floatingPoint()) {
            throw new IllegalArgumentException("Operation available only for float tensors.");
        }
        int size = size();
        float mean = (float) mean();

        float sum2 = 0;
        float sum3 = 0;
        for (int p : loop.offsets) {
            for (int i = 0; i < loop.size; i++) {
                float centered = (float) (storage.getFloat(p) - mean);
                sum2 += (float) (centered * centered);
                sum3 += centered;
                p += loop.step;
            }
        }
        return (float) ((sum2 - (sum3 * sum3) / (size - ddof)) / (size - ddof));
    }

    @Override
    public int argmax(Order order) {
        int argmax = -1;
        float argvalue = TensorOp.reduceMax().initFloat();
        var i = 0;
        var loop = StrideLoopDescriptor.of(layout, order, dtype().vectorSpecies());
        for (int p : loop.offsets) {
            for (int j = 0; j < loop.size; j++) {
                float value = storage.getFloat(p);
                p += loop.step;
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
        float argvalue = TensorOp.reduceMin().initFloat();
        var i = 0;
        var loop = StrideLoopDescriptor.of(layout, order, dtype().vectorSpecies());
        for (int p : loop.offsets) {
            for (int j = 0; j < loop.size; j++) {
                float value = storage.getFloat(p);
                p += loop.step;
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
        for (int p : loop.offsets) {
            for (int i = 0; i < loop.size; i++) {
                if (dtype().isNaN(storage.getFloat(p))) {
                    count++;
                }
                p += loop.step;
            }
        }
        return count;
    }

    @Override
    public int zeroCount() {
        int count = 0;
        for (int p : loop.offsets) {
            for (int i = 0; i < loop.size; i++) {
                if (storage.getFloat(p) == 0) {
                    count++;
                }
                p += loop.step;
            }
        }
        return count;
    }

    @Override
    public Float reduceOp(TensorReduceOp op) {
        float agg = op.initFloat();
        for (int p : loop.offsets) {
            for (int i = 0; i < loop.size; i++) {
                agg = op.applyFloat(agg, storage.getFloat(p));
                p += loop.step;
            }
        }
        return agg;
    }

    @Override
    public Float nanAssociativeOp(TensorReduceOp op) {
        float aggregate = op.initFloat();
        for (int p : loop.offsets) {
            for (int i = 0; i < loop.size; i++) {
                if (!dtype().isNaN(storage.getFloat(p))) {
                    aggregate = op.applyFloat(aggregate, storage.getFloat(p));
                }
                p += loop.step;
            }
        }
        return aggregate;
    }

    @Override
    public Tensor<Float> associativeOpNarrow(TensorReduceOp op, Order order, int axis) {
        int[] newDims = layout.shape().narrowDims(axis);
        int[] newStrides = layout.narrowStrides(axis);
        int selDim = layout.dim(axis);
        int selStride = layout.stride(axis);

        Tensor<Float> res = manager.ofFloat().zeros(Shape.of(newDims), Order.autoFC(order));
        var it = new StridePointerIterator(StrideLayout.of(newDims, layout().offset(), newStrides), Order.C);
        var resIt = res.ptrIterator(Order.C);
        while (it.hasNext()) {
            int ptr = it.nextInt();
            float value = StrideWrapper.of(ptr, selStride, selDim, this).aggregate(op.initFloat(), op::applyFloat);
            res.ptrSet(resIt.next(), value);
        }
        return res;
    }

    @Override
    public Tensor<Float> nanAssociativeOpNarrow(TensorReduceOp op, Order order, int axis) {
        int[] newDims = layout.shape().narrowDims(axis);
        int[] newStrides = layout.narrowStrides(axis);
        int selDim = layout.dim(axis);
        int selStride = layout.stride(axis);

        Tensor<Float> res = manager.ofFloat().zeros(Shape.of(newDims), Order.autoFC(order));
        var it = new StridePointerIterator(StrideLayout.of(newDims, layout().offset(), newStrides), Order.C);
        var resIt = res.ptrIterator(Order.C);
        while (it.hasNext()) {
            int ptr = it.nextInt();
            float value = StrideWrapper.of(ptr, selStride, selDim, this).nanAggregate(DType.FLOAT, op.initFloat(), op::applyFloat);
            res.ptrSet(resIt.next(), value);
        }
        return res;
    }

    @Override
    public Tensor<Float> copy(Order askOrder) {
        askOrder = Order.autoFC(askOrder);

        var copy = manager.ofFloat().storage().zeros(size());
        var dst = manager.ofFloat().stride(StrideLayout.ofDense(shape(), 0, askOrder), copy);

        if (layout.storageFastOrder() == askOrder) {
            sameLayoutCopy(copy, askOrder);
        } else {
            copyTo(dst);
        }
        return dst;
    }

    private void sameLayoutCopy(Storage<Float> copy, Order askOrder) {
        var loop = StrideLoopDescriptor.of(layout, askOrder, dtype().vectorSpecies());
        var last = 0;
        for (int p : loop.offsets) {
            for (int i = 0; i < loop.size; i++) {
                copy.setFloat(last++, storage.getFloat(p));
                p += loop.step;
            }
        }
    }

    @Override
    public Tensor<Float> copyTo(Tensor<Float> to) {

        Order askOrder = Layout.storageFastTandemOrder(layout, to.layout());

        if (to instanceof BaseFloatTensorStride dst) {

            int limit = Math.floorDiv(L2_CACHE_SIZE, dtype().byteCount() * 2 * manager.cpuThreads() * 8);

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

                try (ExecutorService executor = Executors.newFixedThreadPool(manager.cpuThreads())) {
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
        var loop = StrideLoopDescriptor.of(src.layout, askOrder, dtype().vectorSpecies());
        var it2 = dst.ptrIterator(askOrder);
        for (int p : loop.offsets) {
            for (int i = 0; i < loop.size; i++) {
                dst.storage.setFloat(it2.nextInt(), src.storage.getFloat(p));
                p += loop.step;
            }
        }
    }

    @Override
    public String toString() {
        return String.format("BaseStride{%s,%s,%s,%s}\n%s", dtype().id(), Arrays.toString(layout.dims()), layout.offset(),
                Arrays.toString(layout.strides()), toContent());
    }
}
