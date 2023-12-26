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

package rapaio.math.tensor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import rapaio.math.tensor.iterators.LoopIterator;
import rapaio.math.tensor.iterators.PointerIterator;
import rapaio.printer.Printable;
import rapaio.util.function.IntIntBiFunction;

/**
 * Generic tensor interface. A tensor is a multidimensional array.
 *
 * @param <N> Generic data type
 * @param <T> Generic tensor type
 */
public interface Tensor<N extends Number, T extends Tensor<N, T>> extends Printable, Iterable<N> {

    /**
     * @return tensor engine
     */
    TensorEngine engine();

    /**
     * @return tensor data type
     */
    DType<N, T> dtype();

    /**
     * @return tensor layout
     */
    Layout layout();

    /**
     * @return tensor shape
     */
    default Shape shape() {
        return layout().shape();
    }

    /**
     * @return number of dimensions or rank
     */
    default int rank() {
        return layout().rank();
    }

    /**
     * @return number of elements
     */
    default int size() {
        return layout().size();
    }

    default boolean isScalar() {
        return shape().dims().length == 0;
    }

    /**
     * Creates a new tensor with a different shape. If possible, the data will not be copied.
     * If data is copied, the result will be a dense tensor of default order.
     * <p>
     * In order to reshape a tensor, the source shape and destination shape must have the same size.
     *
     * @param shape destination shape
     * @return new tensor instance, wrapping, if possible, the data from the old tensor.
     */
    default T reshape(Shape shape) {
        return reshape(shape, Order.defaultOrder());
    }

    /**
     * Creates a new tensor with a different shape. If possible, the data will not be copied.
     * <p>
     * In order to reshape a tensor, the source shape and destination shape must have the same size.
     *
     * @param shape    destination shape
     * @param askOrder destination order, if the data will be copied, otherwise the parameter is ignored.
     * @return new tensor instance, wrapping, if possible, the data from the old tensor.
     */
    T reshape(Shape shape, Order askOrder);

    default T transposeNew() {
        return transposeNew(Order.defaultOrder());
    }

    default T transposeNew(Order askOrder) {
        return copy(askOrder).transpose();
    }

    /**
     * Transpose of a tensor. A transposed tensor is a tensor which reverts axis, the first axis becomes the last,
     * the second axis becomes the second to last and so on. Data storage remain the same, no new storage copy is created.
     * As such, any modification on a transposed tensor will affect the original tensor.
     *
     * @return a transposed view of the tensor
     */
    T transpose();

    /**
     * Collapses the tensor into one dimension in the order given as parameter. It creates a new tensor copy
     * only if needed (no stride could be created).
     *
     * @param askOrder order of the elements
     * @return a tensor with elements in given order (new copy if needed)
     */
    T ravel(Order askOrder);

    /**
     * Creates a copy of the array, flattened into one dimension. The order of the elements is given as parameter.
     *
     * @param askOrder order of the elements
     * @return a copy of the tensor with elements in asked order.
     */
    T flatten(Order askOrder);

    /**
     * Collapses all dimensions equal with one. This operation does not create a new copy of the data.
     * If no dimensions have size one, the same tensor is returned.
     *
     * @return view of the same tensor with all dimensions equal with one collapsed
     */
    T squeeze();

    /**
     * Collapses the given axis if equals with one. This operation does not create a new copy of the data.
     * If dimension doesn't have size one, the same tensor is returned.
     *
     * @return view of the same tensor with the given dimension equal with one collapsed
     */
    T squeeze(int axis);

    /**
     * Creates a new tensor view with an additional dimension at the position specified by {@param axis}.
     * Specified axis value should be between 0 (inclusive) and the number of dimensions (inclusive).
     *
     * @param axis index of the axis to be added
     * @return new view tensor with added axis
     */
    T unsqueeze(int axis);

    /**
     * Creates a tensor view with dimensions permuted in the order specified in parameter. The
     * parameter is an integer array containing all values from closed interval {@code [0,(rank-1)]}.
     * The order in which those values are passed defined the dimension permutation.
     *
     * @param dims dimension permutation
     * @return new tensor view with permuted dimensions
     */
    T permute(int[] dims);

    /**
     * Creates a new tensor view with source axis moved into the given destination position.
     *
     * @param src source axis
     * @param dst destination axis position
     * @return new view tensor with moved axis
     */
    T moveAxis(int src, int dst);

    /**
     * Swap two axis. This does not affect the storage.
     *
     * @param src source axis
     * @param dst destination axis
     * @return new view tensor with swapped axis
     */
    T swapAxis(int src, int dst);

    /**
     * Creates a new tensor view with truncated axis, all other axes remain the same.
     *
     * @param axis    axis to be truncated
     * @param keepDim keep dimension or not
     * @param start   start index inclusive
     * @param end     end index exclusive
     * @return new view tensor with truncated axis
     */
    T narrow(int axis, boolean keepDim, int start, int end);

    T narrowAll(boolean keepDim, int[] starts, int[] ends);

    /**
     * Splits the tensor into multiple view tensors along a given axis.
     * The resulting tensors are narrowed versions of the original tensor,
     * with the start index being the current index, and the end
     * being the next index or the end of the dimension.
     *
     * @param axis    axis to split along
     * @param indexes indexes to split along, being start indexes for truncation
     * @return list of new tensors with truncated data.
     */
    List<T> split(int axis, boolean keepDim, int... indexes);

    List<T> splitAll(boolean keepDim, int[][] indexes);

    /**
     * Slices the tensor along a given axis.
     * The resulting tensors are truncated versions of the original one with size given by step.
     * The last tensor in list might have lesser dimension size if step does not divide dimension size
     *
     * @param axis axis to slice along
     * @param step step size
     * @return list of new tensors with truncated data.
     */
    default List<T> chunk(int axis, boolean keepDim, int step) {
        int dim = layout().shape().dim(axis);
        int[] indexes = new int[Math.ceilDiv(dim, step)];
        indexes[0] = 0;
        for (int i = 1; i < indexes.length; i++) {
            indexes[i] = Math.min(indexes[i - 1] + step, dim);
        }
        return split(axis, keepDim, indexes);
    }

    default List<T> chunkAll(boolean keepDim, int[] steps) {
        if (layout().rank() != steps.length) {
            throw new IllegalArgumentException("Array of steps must have the length equals with rank.");
        }
        int[][] indexes = new int[steps.length][];
        for (int i = 0; i < steps.length; i++) {
            indexes[i] = new int[Math.ceilDiv(layout().shape().dim(i), steps[i])];
            indexes[i][0] = 0;
            for (int j = 1; j < indexes[i].length; j++) {
                indexes[i][j] = Math.min(indexes[i][j - 1] + steps[i], layout().shape().dim(i));
            }
        }
        return splitAll(keepDim, indexes);
    }

    /**
     * Removes a tensor dimension. Returns a list of all chunks along a given dimension, already without it.
     * All tensors from view will be tensor views.
     *
     * @param axis axis to be removed
     * @return list of tensor views
     */
    default List<T> unbind(int axis) {
        return chunk(axis, false, 1);
    }

    @SuppressWarnings("unchecked")
    default T stack(int axis, Collection<? extends T> tensors) {
        List<T> list = new ArrayList<>();
        list.add((T) this);
        list.addAll(tensors);
        return engine().stack(axis, list);
    }

    @SuppressWarnings("unchecked")
    default T concat(int axis, Collection<? extends T> tensors) {
        List<T> list = new ArrayList<>();
        list.add((T) this);
        list.addAll(tensors);
        return engine().concat(axis, list);
    }

    T repeat(int axis, int repeat, boolean stack);

    T tile(int[] repeats);

    T take(Order order, int... indexes);

    /**
     * Get value at indexed position. An indexed position is a tuple of rank
     * dimension, with an integer value on each dimension.
     *
     * @param indexes indexed position
     * @return value at indexed position
     */
    N get(int... indexes);

    /**
     * Sets value at indexed position.
     *
     * @param value   value to be set
     * @param indexes indexed position
     */
    void set(N value, int... indexes);

    /**
     * Get value at pointer. A pointer is an index value at the memory layout.
     *
     * @param ptr data pointer
     * @return element at data pointer
     */
    N ptrGet(int ptr);

    /**
     * Sets value at given pointer.
     *
     * @param ptr   data pointer
     * @param value element value to be set at data pointer
     */
    void ptrSet(int ptr, N value);

    /**
     * Produces an iterator over the values from this tensor in the
     * storage order.
     *
     * @return value iterator
     */
    default Iterator<N> iterator() {
        return iterator(Order.S);
    }

    /**
     * Produces an iterator over values from this tensor in the order
     * specified by parameter value.
     *
     * @param askOrder traversing order
     * @return value iterator
     */
    Iterator<N> iterator(Order askOrder);

    /**
     * Produces an iterator of data pointer values in the storage order.
     *
     * @return data pointer iterator
     */
    default PointerIterator ptrIterator() {
        return ptrIterator(Order.S);
    }

    /**
     * Produces an iterator of data pointer values in the order specified
     * by parameter value.
     *
     * @param askOrder traversing order
     * @return data pointer iterator
     */
    PointerIterator ptrIterator(Order askOrder);

    /**
     * Produces a loop iterator in the storage order. A loop iterator iterates
     * through a series of objects which contains information which can be used
     * in a for loop instruction. All loops have the same size and step.
     * <p>
     * This kind of iterators are useful for computational reasons. In general
     * a for loop is faster than an iterator because it avoids the artifacts
     * produced. On the other side it requires more code on the usage side,
     * which eventually can be optimized by the compiler.
     *
     * @return loop iterator in storage order
     */
    default LoopIterator loopIterator() {
        return loopIterator(Order.S);
    }

    /**
     * Produces a loop iterator in the given order. A loop iterator iterates
     * through a series of objects which contains information which can be used
     * in a for loop instruction. All loops have the same size and step.
     * <p>
     * This kind of iterators are useful for computational reasons. In general
     * a for loop is faster than an iterator because it avoids the artifacts
     * produced. On the other side it requires more code on the usage side,
     * which eventually can be optimized by the compiler.
     *
     * @return loop iterator in storage order
     */
    LoopIterator loopIterator(Order askOrder);

    default T apply(IntIntBiFunction<N> fun) {
        return apply(Order.defaultOrder(), fun);
    }

    default T apply(Order askOrder, IntIntBiFunction<N> fun) {
        return copy(askOrder).apply_(askOrder, fun);
    }

    /**
     * Applies the given function over the elements of the tensor. The function has
     * two parameters: position and data pointer. The position describes
     * the index element in the order specified by {@code askOrder}. The data pointer values do not
     * depend on order.
     *
     * @param askOrder order used to iterate over positions
     * @param fun      function which produces values
     * @return new tensor with applied values
     */
    T apply_(Order askOrder, IntIntBiFunction<N> fun);

    default T apply(Function<N, N> fun) {
        return apply(Order.defaultOrder(), fun);
    }

    default T apply(Order askOrder, Function<N, N> fun) {
        return copy(askOrder).apply_(fun);
    }

    T apply_(Function<N, N> fun);

    T fill_(N value);

    T fillNan_(N value);

    T clamp_(N min, N max);

    default T abs() {
        return abs(Order.defaultOrder());
    }

    default T abs(Order order) {
        return copy(order).abs_();
    }

    T abs_();

    default T negate() {
        return negate(Order.defaultOrder());
    }

    default T negate(Order order) {
        return copy(order).negate_();
    }

    T negate_();

    default T log() {
        return log(Order.defaultOrder());
    }

    default T log(Order order) {
        return copy(order).log_();
    }

    T log_();

    default T log1p() {
        return log1p(Order.defaultOrder());
    }

    default T log1p(Order order) {
        return copy(order).log1p_();
    }

    T log1p_();

    default T exp() {
        return exp(Order.defaultOrder());
    }

    default T exp(Order order) {
        return copy(order).exp_();
    }

    T exp_();

    default T expm1() {
        return expm1(Order.defaultOrder());
    }

    default T expm1(Order order) {
        return copy(order).expm1_();
    }

    T expm1_();

    default T sin() {
        return sin(Order.defaultOrder());
    }

    default T sin(Order order) {
        return copy(order).sin_();
    }

    T sin_();

    default T asin() {
        return asin(Order.defaultOrder());
    }

    default T asin(Order order) {
        return copy(order).asin_();
    }

    T asin_();

    default T sinh() {
        return sinh(Order.defaultOrder());
    }

    default T sinh(Order order) {
        return copy(order).sinh_();
    }

    T sinh_();

    default T cos() {
        return cos(Order.defaultOrder());
    }

    default T cos(Order order) {
        return copy(order).cos_();
    }

    T cos_();

    default T acos() {
        return acos(Order.defaultOrder());
    }

    default T acos(Order order) {
        return copy(order).acos_();
    }

    T acos_();

    default T cosh() {
        return cosh(Order.defaultOrder());
    }

    default T cosh(Order order) {
        return copy(order).cosh_();
    }

    T cosh_();

    default T tan() {
        return tan(Order.defaultOrder());
    }

    default T tan(Order order) {
        return copy(order).tan_();
    }

    T tan_();

    default T atan() {
        return atan(Order.defaultOrder());
    }

    default T atan(Order order) {
        return copy(order).atan_();
    }

    T atan_();

    default T tanh() {
        return tanh(Order.defaultOrder());
    }

    default T tanh(Order order) {
        return copy(order).tanh_();
    }

    T tanh_();

    default T add(T tensor) {
        return add(tensor, Order.defaultOrder());
    }

    default T add(T tensor, Order order) {
        if(isScalar()) {
            return tensor.copy(order).add_(get());
        }
        return copy(order).add_(tensor);
    }

    T add_(T tensor);

    default T sub(T tensor) {
        return sub(tensor, Order.defaultOrder());
    }

    default T sub(T tensor, Order order) {
        if(isScalar()) {
            return tensor.copy(order).sub_(get());
        }
        return copy(order).sub_(tensor);
    }

    T sub_(T tensor);

    default T mul(T tensor) {
        return mul(tensor, Order.defaultOrder());
    }

    default T mul(T tensor, Order order) {
        if(isScalar()) {
            return tensor.copy(order).mul_(get());
        }
        return copy(order).mul_(tensor);
    }

    T mul_(T tensor);

    default T div(T tensor) {
        return div(tensor, Order.defaultOrder());
    }

    default T div(T tensor, Order order) {
        if(isScalar()) {
            return tensor.copy(order).div_(get());
        }
        return copy(order).div_(tensor);
    }

    T div_(T tensor);

    default T add(N value) {
        return add(value, Order.defaultOrder());
    }

    default T add(N value, Order order) {
        return copy(order).add_(value);
    }

    T add_(N value);

    default T sub(N value) {
        return sub(value, Order.defaultOrder());
    }

    default T sub(N value, Order order) {
        return copy(order).sub_(value);
    }

    T sub_(N value);

    default T mul(N value) {
        return mul(value, Order.defaultOrder());
    }

    default T mul(N value, Order order) {
        return copy(order).mul_(value);
    }

    T mul_(N value);

    default T div(N value) {
        return div(value, Order.defaultOrder());
    }

    default T div(N value, Order order) {
        return copy(order).div_(value);
    }

    T div_(N value);

    N vdot(T tensor);

    N vdot(T tensor, int start, int end);

    T mv(T tensor);

    default T mm(T tensor) {
        return mm(tensor, Order.defaultOrder());
    }

    T mm(T tensor, Order askOrder);

    Statistics<N, T> stats();

    N sum();

    N nanSum();

    N prod();

    N nanProd();

    N max();

    N nanMax();

    N min();

    N nanMin();

    int nanCount();

    int zeroCount();

    /**
     * Creates a copy of the original tensor with the given order. Only {@link Order#C} or {@link Order#F} are allowed.
     * <p>
     * The order does not determine how values are read, but how values will be stored.
     *
     * @return new copy of the tensor
     */
    default T copy() {
        return copy(Order.defaultOrder());
    }

    /**
     * Creates a copy of the original tensor with the given order. Only {@link Order#C} or {@link Order#F} are allowed.
     * <p>
     * The order does not determine how values are read, but how values will be stored.
     *
     * @param askOrder desired order of the copy tensor.
     * @return new copy of the tensor
     */
    T copy(Order askOrder);

    default T copyTo(T dst) {
        return copyTo(dst, Order.S);
    }

    T copyTo(T dst, Order askOrder);

    default boolean deepEquals(Object t) {
        return deepEquals(t, 1e-100);
    }

    default boolean deepEquals(Object t, double tol) {
        if (t instanceof Tensor<?, ?> dt) {
            if (!layout().shape().equals(dt.layout().shape())) {
                return false;
            }
            var it1 = iterator(Order.C);
            var it2 = dt.iterator(Order.C);

            while (it1.hasNext()) {
                Number v1 = it1.next();
                Number v2 = it2.next();

                if (v1 == null && v2 == null) {
                    continue;
                }
                if (v1 == null || v2 == null) {
                    return false;
                }
                if (Math.abs(v1.doubleValue() - v2.doubleValue()) > tol) {
                    return false;
                }
            }
        }
        return true;
    }
}
