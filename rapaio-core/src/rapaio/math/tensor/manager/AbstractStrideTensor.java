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

package rapaio.math.tensor.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import rapaio.data.VarDouble;
import rapaio.math.tensor.Order;
import rapaio.math.tensor.Shape;
import rapaio.math.tensor.Storage;
import rapaio.math.tensor.Tensor;
import rapaio.math.tensor.TensorManager;
import rapaio.math.tensor.iterators.DensePointerIterator;
import rapaio.math.tensor.iterators.LoopIterator;
import rapaio.math.tensor.iterators.PointerIterator;
import rapaio.math.tensor.iterators.ScalarLoopIterator;
import rapaio.math.tensor.iterators.StrideLoopDescriptor;
import rapaio.math.tensor.iterators.StrideLoopIterator;
import rapaio.math.tensor.iterators.StridePointerIterator;
import rapaio.math.tensor.layout.StrideLayout;
import rapaio.math.tensor.layout.StrideWrapper;
import rapaio.math.tensor.manager.base.BaseDoubleTensorStride;
import rapaio.math.tensor.manager.vector.VectorDoubleTensorStride;
import rapaio.math.tensor.operator.TensorAssociativeOp;
import rapaio.math.tensor.operator.TensorBinaryOp;
import rapaio.math.tensor.operator.TensorUnaryOp;
import rapaio.printer.Printer;
import rapaio.printer.TextTable;
import rapaio.printer.opt.POpt;

public abstract class AbstractStrideTensor<N extends Number> implements Tensor<N> {

    protected final Storage<N> storage;
    protected final StrideLayout layout;
    protected final TensorManager engine;
    protected final StrideLoopDescriptor loop;

    public AbstractStrideTensor(TensorManager engine, StrideLayout layout, Storage<N> storage) {
        this.storage = storage;
        this.layout = layout;
        this.engine = engine;
        this.loop = StrideLoopDescriptor.of(layout, layout.storageFastOrder());
    }

    @Override
    public final Storage<N> storage() {
        return storage;
    }

    @Override
    public final TensorManager manager() {
        return engine;
    }

    @Override
    public final StrideLayout layout() {
        return layout;
    }


    @Override
    public final Tensor<N> t_() {
        return manager().stride(dtype(), layout.revert(), storage);
    }

    @Override
    public final Tensor<N> ravel(Order askOrder) {
        var compact = layout.computeFortranLayout(askOrder, true);
        if (compact.shape().rank() == 1) {
            return engine.stride(dtype(), compact, storage);
        }
        return flatten(askOrder);
    }

    @Override
    public final Tensor<N> squeeze(int axis) {
        return layout.shape().dim(axis) != 1 ? this : engine.stride(dtype(), layout.squeeze(axis), storage);
    }

    @Override
    public final Tensor<N> unsqueeze(int axis) {
        return engine.stride(dtype(), layout.unsqueeze(axis), storage);
    }

    @Override
    public final Tensor<N> permute(int... dims) {
        return engine.stride(dtype(), layout().permute(dims), storage);
    }

    @Override
    public final Tensor<N> moveAxis(int src, int dst) {
        return engine.stride(dtype(), layout.moveAxis(src, dst), storage);
    }

    @Override
    public final Tensor<N> swapAxis(int src, int dst) {
        return engine.stride(dtype(), layout.swapAxis(src, dst), storage);
    }

    @Override
    public final Tensor<N> narrow(int axis, boolean keepdim, int start, int end) {
        return engine.stride(dtype(), layout.narrow(axis, keepdim, start, end), storage);
    }

    @Override
    public final Tensor<N> narrowAll(boolean keepdim, int[] starts, int[] ends) {
        return engine.stride(dtype(), layout.narrowAll(keepdim, starts, ends), storage);
    }

    @Override
    public final List<Tensor<N>> split(int axis, boolean keepdim, int... indexes) {
        List<Tensor<N>> result = new ArrayList<>(indexes.length);
        for (int i = 0; i < indexes.length; i++) {
            result.add(narrow(axis, keepdim, indexes[i], i < indexes.length - 1 ? indexes[i + 1] : shape().dim(axis)));
        }
        return result;
    }

    @Override
    public final List<Tensor<N>> splitAll(boolean keepdim, int[][] indexes) {
        if (indexes.length != rank()) {
            throw new IllegalArgumentException(
                    "Indexes length of %d is not the same as shape rank %d.".formatted(indexes.length, rank()));
        }
        List<Tensor<N>> results = new ArrayList<>();
        int[] starts = new int[indexes.length];
        int[] ends = new int[indexes.length];
        splitAllRecursive(results, indexes, keepdim, starts, ends, 0);
        return results;
    }

    private void splitAllRecursive(List<Tensor<N>> results, int[][] indexes, boolean keepdim, int[] starts, int[] ends, int level) {
        if (level == indexes.length) {
            return;
        }
        for (int i = 0; i < indexes[level].length; i++) {
            starts[level] = indexes[level][i];
            ends[level] = i < indexes[level].length - 1 ? indexes[level][i + 1] : shape().dim(level);
            if (level == indexes.length - 1) {
                results.add(narrowAll(keepdim, starts, ends));
            } else {
                splitAllRecursive(results, indexes, keepdim, starts, ends, level + 1);
            }
        }
    }

    @Override
    public final Tensor<N> repeat(Order order, int axis, int repeat, boolean stack) {
        List<Tensor<N>> copies = new ArrayList<>(repeat);
        for (int i = 0; i < repeat; i++) {
            copies.add(this);
        }
        if (stack) {
            return engine.stack(order, axis, copies);
        } else {
            return engine.concat(order, axis, copies);
        }
    }

    @Override
    public final Tensor<N> expand(int axis, int dim) {
        if (layout.dim(axis) != 1) {
            throw new IllegalArgumentException(STR."Dimension \{axis} must have size 1, but have size \{layout.dim(axis)}.");
        }
        if (dim < 1) {
            throw new IllegalArgumentException(STR."Dimension of the new axis \{dim} must be positive.");
        }
        int[] newDims = Arrays.copyOf(layout.dims(), layout.dims().length);
        int[] newStrides = Arrays.copyOf(layout.strides(), layout.strides().length);

        newDims[axis] = dim;
        newStrides[axis] = 0;
        return engine.stride(dtype(), StrideLayout.of(Shape.of(newDims), layout.offset(), newStrides), storage);
    }

    @Override
    public final Tensor<N> take(Order order, int axis, int... indices) {

        if (axis < 0 || axis >= layout.rank()) {
            throw new IllegalArgumentException(STR."Axis value \{axis} is out of bounds.");
        }
        if (indices == null || indices.length == 0) {
            throw new IllegalArgumentException("Indices cannot be empty.");
        }
        for (int index : indices) {
            if (index < 0 || index >= layout.dim(axis)) {
                throw new IllegalArgumentException(STR."Index values are invalid, must be in range [0,\{layout.dim(axis) - 1}].");
            }
        }

        // check if we can handle only through stride layout

        // a single element
        if (indices.length == 1) {
            int[] newDims = Arrays.copyOf(layout.dims(), layout.dims().length);
            int[] newStrides = Arrays.copyOf(layout.strides(), layout.strides().length);
            newDims[axis] = 1;
            newStrides[axis] = 1;
            int newOffset = layout().offset() + indices[0] * layout.stride(axis);
            return engine.stride(dtype(), StrideLayout.of(Shape.of(newDims), newOffset, newStrides), storage);
        }

        // a geometric sequence of indices, even if the step is 0 (repeated elements)
        if (indices[1] - indices[0] >= 0) {
            int step = indices[1] - indices[0];
            boolean validSequence = true;
            for (int i = 2; i < indices.length; i++) {
                if (indices[i] - indices[i - 1] != step) {
                    validSequence = false;
                    break;
                }
            }
            if (validSequence) {
                int[] newDims = Arrays.copyOf(layout.dims(), layout.dims().length);
                int[] newStrides = Arrays.copyOf(layout.strides(), layout.strides().length);
                newDims[axis] = indices.length;
                newStrides[axis] = layout.stride(axis) * step;
                int newOffset = layout.offset() + indices[0] * layout.stride(axis);
                return engine.stride(dtype(), StrideLayout.of(Shape.of(newDims), newOffset, newStrides), storage);
            }
        }

        // if we failed, we copy data into a new tensor
        List<Tensor<N>> slices = new ArrayList<>();
        for (int index : indices) {
            slices.add(narrow(axis, true, index, index + 1));
        }
        return engine.concat(order, axis, slices);
    }

    @Override
    public final Tensor<N> sort_(int axis, boolean asc) {
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
    public final byte getByte(int... indexes) {
        return storage.getByte(layout().pointer(indexes));
    }

    @Override
    public final int getInt(int... indexes) {
        return storage.getInt(layout().pointer(indexes));
    }

    @Override
    public final float getFloat(int... indexes) {
        return storage.getFloat(layout().pointer(indexes));
    }

    @Override
    public final double getDouble(int... indexes) {
        return storage.getDouble(layout().pointer(indexes));
    }

    @Override
    public final void setByte(byte value, int... indexes) {
        storage.setByte(layout().pointer(indexes), value);
    }

    @Override
    public final void setInt(int value, int... indexes) {
        storage.setInt(layout().pointer(indexes), value);
    }

    @Override
    public final void setFloat(float value, int... indexes) {
        storage.setFloat(layout().pointer(indexes), value);
    }

    @Override
    public final void setDouble(double value, int... indexes) {
        storage.setDouble(layout().pointer(indexes), value);
    }

    @Override
    public final void incByte(byte value, int... indexes) {
        storage.incByte(layout().pointer(indexes), value);
    }

    @Override
    public final void incInt(int value, int... indexes) {
        storage.incInt(layout().pointer(indexes), value);
    }

    @Override
    public final void incFloat(float value, int... indexes) {
        storage.incFloat(layout().pointer(indexes), value);
    }

    @Override
    public final void incDouble(double value, int... indexes) {
        storage.incDouble(layout().pointer(indexes), value);
    }

    @Override
    public final byte ptrGetByte(int ptr) {
        return storage.getByte(ptr);
    }

    @Override
    public final int ptrGetInt(int ptr) {
        return storage.getInt(ptr);
    }

    @Override
    public final float ptrGetFloat(int ptr) {
        return storage.getFloat(ptr);
    }

    @Override
    public final double ptrGetDouble(int ptr) {
        return storage.getDouble(ptr);
    }

    @Override
    public final void ptrSetByte(int ptr, byte value) {
        storage.setByte(ptr, value);
    }

    @Override
    public final void ptrSetInt(int ptr, int value) {
        storage.setInt(ptr, value);
    }

    @Override
    public final void ptrSetFloat(int ptr, float value) {
        storage.setFloat(ptr, value);
    }

    @Override
    public final void ptrSetDouble(int ptr, double value) {
        storage.setDouble(ptr, value);
    }

    @Override
    public final Iterator<N> iterator(Order askOrder) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(ptrIterator(askOrder), Spliterator.ORDERED), false)
                .map(storage::get).iterator();
    }

    @Override
    public final Stream<N> stream(Order order) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator(order), Spliterator.ORDERED), false);
    }

    @Override
    public final PointerIterator ptrIterator(Order askOrder) {
        if (layout.isCOrdered() && askOrder != Order.F) {
            return new DensePointerIterator(layout.shape(), layout.offset(), layout.stride(-1));
        }
        if (layout.isFOrdered() && askOrder != Order.C) {
            return new DensePointerIterator(layout.shape(), layout.offset(), layout.stride(0));
        }
        return new StridePointerIterator(layout, askOrder);
    }

    @Override
    public final LoopIterator loopIterator(Order askOrder) {
        if (layout.rank() == 0) {
            return new ScalarLoopIterator(layout.offset());
        }
        return new StrideLoopIterator(layout, askOrder);
    }

    protected abstract void unaryOp(TensorUnaryOp op);

    protected abstract void binaryVectorOp(TensorBinaryOp op, Tensor<N> b);

    protected abstract void binaryScalarOp(TensorBinaryOp op, N value);

    protected abstract N associativeOp(TensorAssociativeOp op);

    protected abstract N nanAssociativeOp(TensorAssociativeOp op);

    protected abstract Tensor<N> associativeOpNarrow(TensorAssociativeOp op, Order order, int axis);

    protected abstract Tensor<N> nanAssociativeOpNarrow(TensorAssociativeOp op, Order order, int axis);

    @Override
    public final Tensor<N> rint_() {
        unaryOp(TensorUnaryOp.RINT);
        return this;
    }

    @Override
    public final Tensor<N> ceil_() {
        unaryOp(TensorUnaryOp.CEIL);
        return this;
    }

    @Override
    public final Tensor<N> floor_() {
        unaryOp(TensorUnaryOp.FLOOR);
        return this;
    }

    @Override
    public final Tensor<N> abs_() {
        unaryOp(TensorUnaryOp.ABS);
        return this;
    }

    @Override
    public final Tensor<N> negate_() {
        unaryOp(TensorUnaryOp.NEG);
        return this;
    }

    @Override
    public final Tensor<N> log_() {
        unaryOp(TensorUnaryOp.LOG);
        return this;
    }

    @Override
    public final Tensor<N> log1p_() {
        unaryOp(TensorUnaryOp.LOG1P);
        return this;
    }

    @Override
    public final Tensor<N> exp_() {
        unaryOp(TensorUnaryOp.EXP);
        return this;
    }

    @Override
    public final Tensor<N> expm1_() {
        unaryOp(TensorUnaryOp.EXPM1);
        return this;
    }

    @Override
    public final Tensor<N> sin_() {
        unaryOp(TensorUnaryOp.SIN);
        return this;
    }

    @Override
    public final Tensor<N> asin_() {
        unaryOp(TensorUnaryOp.ASIN);
        return this;
    }

    @Override
    public final Tensor<N> sinh_() {
        unaryOp(TensorUnaryOp.SINH);
        return this;
    }

    @Override
    public final Tensor<N> cos_() {
        unaryOp(TensorUnaryOp.COS);
        return this;
    }

    @Override
    public final Tensor<N> acos_() {
        unaryOp(TensorUnaryOp.ACOS);
        return this;
    }

    @Override
    public final Tensor<N> cosh_() {
        unaryOp(TensorUnaryOp.COSH);
        return this;
    }

    @Override
    public final Tensor<N> tan_() {
        unaryOp(TensorUnaryOp.TAN);
        return this;
    }

    @Override
    public final Tensor<N> atan_() {
        unaryOp(TensorUnaryOp.ATAN);
        return this;
    }

    @Override
    public final Tensor<N> tanh_() {
        unaryOp(TensorUnaryOp.TANH);
        return this;
    }

    @Override
    public final Tensor<N> sqr_() {
        unaryOp(TensorUnaryOp.SQR);
        return this;
    }

    @Override
    public final Tensor<N> sqrt_() {
        unaryOp(TensorUnaryOp.SQRT);
        return this;
    }

    @Override
    public final Tensor<N> add_(Tensor<N> tensor) {
        binaryVectorOp(TensorBinaryOp.ADD, tensor);
        return this;
    }

    @Override
    public final Tensor<N> sub_(Tensor<N> tensor) {
        binaryVectorOp(TensorBinaryOp.SUB, tensor);
        return this;
    }

    @Override
    public final Tensor<N> mul_(Tensor<N> tensor) {
        binaryVectorOp(TensorBinaryOp.MUL, tensor);
        return this;
    }

    @Override
    public final Tensor<N> div_(Tensor<N> tensor) {
        binaryVectorOp(TensorBinaryOp.DIV, tensor);
        return this;
    }

    @Override
    public final Tensor<N> min_(Tensor<N> tensor) {
        binaryVectorOp(TensorBinaryOp.MIN, tensor);
        return this;
    }

    @Override
    public final Tensor<N> max_(Tensor<N> tensor) {
        binaryVectorOp(TensorBinaryOp.MAX, tensor);
        return this;
    }

    @Override
    public final Tensor<N> add_(N value) {
        binaryScalarOp(TensorBinaryOp.ADD, value);
        return this;
    }

    @Override
    public final Tensor<N> sub_(N value) {
        binaryScalarOp(TensorBinaryOp.SUB, value);
        return this;
    }

    @Override
    public final Tensor<N> mul_(N value) {
        binaryScalarOp(TensorBinaryOp.MUL, value);
        return this;
    }

    @Override
    public final Tensor<N> div_(N value) {
        binaryScalarOp(TensorBinaryOp.DIV, value);
        return this;
    }

    @Override
    public final Tensor<N> min_(N value) {
        binaryScalarOp(TensorBinaryOp.MIN, value);
        return this;
    }

    @Override
    public final Tensor<N> max_(N value) {
        binaryScalarOp(TensorBinaryOp.MAX, value);
        return this;
    }

    @Override
    public final N sum() {
        return associativeOp(TensorAssociativeOp.ADD);
    }

    @Override
    public final Tensor<N> sum(Order order, int axis) {
        return associativeOpNarrow(TensorAssociativeOp.ADD, order, axis);
    }

    @Override
    public final N nanSum() {
        return nanAssociativeOp(TensorAssociativeOp.ADD);
    }

    @Override
    public final Tensor<N> nanSum(Order order, int axis) {
        return nanAssociativeOpNarrow(TensorAssociativeOp.ADD, order, axis);
    }

    @Override
    public final N prod() {
        return associativeOp(TensorAssociativeOp.MUL);
    }

    @Override
    public final Tensor<N> prod(Order order, int axis) {
        return associativeOpNarrow(TensorAssociativeOp.MUL, order, axis);
    }

    @Override
    public final N nanProd() {
        return nanAssociativeOp(TensorAssociativeOp.MUL);
    }

    @Override
    public final Tensor<N> nanProd(Order order, int axis) {
        return nanAssociativeOpNarrow(TensorAssociativeOp.MUL, order, axis);
    }

    @Override
    public final N max() {
        return associativeOp(TensorAssociativeOp.MAX);
    }

    @Override
    public final Tensor<N> max(Order order, int axis) {
        return associativeOpNarrow(TensorAssociativeOp.MAX, order, axis);
    }

    @Override
    public final N nanMax() {
        return nanAssociativeOp(TensorAssociativeOp.MAX);
    }

    @Override
    public final Tensor<N> nanMax(Order order, int axis) {
        return nanAssociativeOpNarrow(TensorAssociativeOp.MAX, order, axis);
    }

    @Override
    public final N min() {
        return associativeOp(TensorAssociativeOp.MIN);
    }

    @Override
    public final Tensor<N> min(Order order, int axis) {
        return associativeOpNarrow(TensorAssociativeOp.MIN, order, axis);
    }

    @Override
    public final N nanMin() {
        return nanAssociativeOp(TensorAssociativeOp.MIN);
    }

    @Override
    public final Tensor<N> nanMin(Order order, int axis) {
        return nanAssociativeOpNarrow(TensorAssociativeOp.MIN, order, axis);
    }

    protected abstract Tensor<N> alongAxisOperation(Order order, int axis, Function<Tensor<N>, N> op);

    @Override
    public final N std() {
        return dtype().castValue(Math.sqrt(var().doubleValue()));
    }

    @Override
    public final Tensor<N> mean(Order order, int axis) {
        return alongAxisOperation(order, axis, Tensor::mean);
    }

    @Override
    public final Tensor<N> std(Order order, int axis) {
        return alongAxisOperation(order, axis, Tensor::std);
    }

    @Override
    public final N stdc(int ddof) {
        return dtype().castValue(Math.sqrt(varc(ddof).doubleValue()));
    }

    @Override
    public final Tensor<N> stdc(Order order, int axis, int ddof) {
        return alongAxisOperation(order, axis, t -> stdc(ddof));
    }

    @Override
    public final Tensor<N> var(Order order, int axis) {
        return alongAxisOperation(order, axis, Tensor::var);
    }

    @Override
    public final Tensor<N> varc(Order order, int axis, int ddof) {
        return alongAxisOperation(order, axis, t -> t.varc(ddof));
    }

    @Override
    public final VarDouble dv() {
        if (layout().rank() != 1) {
            throw new IllegalArgumentException("Only one dimensional tensors can be converted to VarDouble.");
        }
        if (this instanceof BaseDoubleTensorStride bs) {
            if (bs.layout().offset() == 0 && bs.layout().stride(0) == 1) {
                return VarDouble.wrap(bs.asArray());
            }
        }
        if (this instanceof VectorDoubleTensorStride bs) {
            if (bs.layout().offset() == 0 && bs.layout().stride(0) == 1) {
                return VarDouble.wrap(bs.asArray());
            }
        }
        double[] copy = new double[layout().size()];
        var it = iterator(Order.C);
        for (int i = 0; i < copy.length; i++) {
            copy[i] = it.next().doubleValue();
        }
        return VarDouble.wrap(copy);
    }

    @Override
    public final String toContent(Printer printer, POpt<?>... options) {

        final int MAX_COL_VALUES = 21;
        boolean maxColHit = false;
        int cols = 2 + shape().dim(-1);
        if (shape().dim(-1) > MAX_COL_VALUES) {
            maxColHit = true;
            cols = 2 + MAX_COL_VALUES;
        }

        final int MAX_ROW_VALUES = 41;
        boolean maxRowHit = false;
        int rows = shape().size() / shape().dim(-1);
        if (shape().size() / shape().dim(-1) > MAX_ROW_VALUES) {
            maxRowHit = true;
            rows = MAX_ROW_VALUES;
        }

        TextTable tt = TextTable.empty(rows, cols, 0, 0);

        var p = printer.withOptions(options);
        int row = 0;
        if (maxRowHit) {
            for (; row < MAX_ROW_VALUES - 1; row++) {
                tt.textCenter(row, 0, rowStart(shape(), row));
                tt.textLeft(row, cols - 1, rowEnd(shape(), row));
                appendValues(p, tt, row, cols, maxColHit);
            }
            for (int i = 0; i < cols; i++) {
                tt.textCenter(row, i, "...");
            }
        } else {
            for (; row < rows; row++) {
                tt.textCenter(row, 0, rowStart(shape(), row));
                tt.textLeft(row, cols - 1, rowEnd(shape(), row));
                appendValues(p, tt, row, cols, maxColHit);
            }
        }

        return tt.getText(-1);
    }

    private String rowStart(Shape shape, int row) {
        int[] index = shape.index(Order.C, row * shape.dim(-1));
        StringBuilder sb = new StringBuilder();
        for (int c = shape.rank() - 1; c >= 0; c--) {
            if (index[c] == 0) {
                sb.append("[");
            } else {
                break;
            }
        }
        while (sb.length() < shape.rank()) {
            sb.insert(0, " ");
        }
        return sb.toString();
    }

    private String rowEnd(Shape shape, int row) {
        int[] index = shape.index(Order.C, (row + 1) * shape.dim(-1) - 1);
        StringBuilder sb = new StringBuilder();
        for (int c = shape.rank() - 1; c >= 0; c--) {
            if (index[c] == shape.dim(c) - 1) {
                sb.append("]");
            } else {
                break;
            }
        }
        return sb.toString();
    }

    private void appendValues(Printer printer, TextTable tt, int row, int cols, boolean maxColHit) {
        for (int i = 0; i < cols - 2; i++) {
            double value = getDouble(shape().index(Order.C, row * shape().dim(-1) + i));
            tt.floatString(row, i + 1, printer.getOptions().getFloatFormat().format(value));
        }
        if (maxColHit) {
            tt.textCenter(row, cols - 2, "...");
        }
    }

    @Override
    public final String toFullContent(Printer printer, POpt<?>... options) {
        int cols = 2 + shape().dim(-1);
        int rows = shape().size() / shape().dim(-1);

        TextTable tt = TextTable.empty(rows, cols, 0, 0);

        int row = 0;
        for (; row < rows; row++) {
            tt.textCenter(row, 0, rowStart(shape(), row));
            tt.textLeft(row, cols - 1, rowEnd(shape(), row));
            appendValues(printer, tt, row, cols, false);
        }

        return tt.getText(-1);
    }

    @Override
    public final String toSummary(Printer printer, POpt<?>... options) {
        return toString();
    }
}
