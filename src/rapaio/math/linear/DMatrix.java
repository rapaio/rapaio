/*
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/
 *
 * Copyright 2013 - 2022 Aurelian Tutuianu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package rapaio.math.linear;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.DoubleStream;

import rapaio.core.distributions.Distribution;
import rapaio.core.distributions.Normal;
import rapaio.data.Frame;
import rapaio.data.Var;
import rapaio.math.linear.decomposition.DoubleCholeskyDecomposition;
import rapaio.math.linear.decomposition.DoubleEigenDecomposition;
import rapaio.math.linear.decomposition.DoubleLUDecomposition;
import rapaio.math.linear.decomposition.DoubleQRDecomposition;
import rapaio.math.linear.decomposition.DoubleSVDecomposition;
import rapaio.math.linear.dense.DMatrixDenseC;
import rapaio.math.linear.dense.DVectorDense;
import rapaio.printer.Printable;
import rapaio.util.collection.IntArrays;
import rapaio.util.function.Double2DoubleFunction;
import rapaio.util.function.IntInt2DoubleBiFunction;

/**
 * Dense matrix with double precision floating point values
 * <p>
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 2/3/16.
 */
public interface DMatrix extends Serializable, Printable {

    /**
     * Creates a matrix with given {@code rows} and {@code cols} filled with values {@code 0}.
     *
     * @param rows number of rows
     * @param cols number of columns
     * @return new empty matrix with default storage type
     */
    static DMatrixDenseC empty(int rows, int cols) {
        return DMatrixDenseC.empty(rows, cols);
    }

    /**
     * Builds an identity matrix with n rows and n columns.
     * An identity matrix is a matrix with 1 on the main diagonal
     * and 0 otherwise.
     *
     * @param n number of rows and also number of columns
     * @return a new instance of identity matrix of order n
     */
    static DMatrixDenseC eye(int n) {
        return DMatrixDenseC.eye(n);
    }

    /**
     * Builds a dense matrix filled with {@code 0}, except the main diagonal
     * where the elements are the values from the vector given as parameter.
     *
     * @param v vector with elements which will be saved on the main diagonal
     * @return dense diagonal matrix
     */
    static DMatrixDenseC diagonal(DVector v) {
        return DMatrixDenseC.diagonal(v);
    }

    /**
     * Builds a new matrix filled with a given value and with default storage type.
     *
     * @param rows number of rows
     * @param cols number of columns
     * @param fill fill value for all matrix cells
     * @return new matrix filled with value
     */
    static DMatrixDenseC fill(int rows, int cols, double fill) {
        return DMatrixDenseC.fill(rows, cols, fill);
    }

    /**
     * Builds a new matrix filled with values computed by a given function
     * which receives as parameter the row and column of each element and
     * is stored in default storage format.
     *
     * @param rows number of rows
     * @param cols number of columns
     * @param fun  lambda function which computes a value given row and column positions
     * @return new matrix filled with value
     */
    static DMatrixDenseC fill(int rows, int cols, IntInt2DoubleBiFunction fun) {
        return DMatrixDenseC.fill(rows, cols, fun);
    }

    /**
     * Build a matrix with values generated by a standard normal distribution and
     * is stored in the default storage format.
     *
     * @param rows number of rows
     * @param cols number of columns
     * @return matrix filled with random values
     */
    static DMatrixDenseC random(int rows, int cols) {
        return random(rows, cols, Normal.std());
    }

    /**
     * Build a matrix with values generated by a distribution given as parameter and
     * is stored in the specified storage format.
     *
     * @param rows number of rows
     * @param cols number of columns
     * @return matrix filled with random values
     */
    static DMatrixDenseC random(int rows, int cols, Distribution distribution) {
        return DMatrixDenseC.random(rows, cols, distribution);
    }

    /**
     * Copy values from an array of arrays into a matrix. Matrix storage type is the default type and values
     * are row oriented.
     *
     * @param values array of arrays of values
     * @return matrix which hold a range of data
     */
    static DMatrixDenseC copy(double[][] values) {
        return DMatrixDenseC.copy(values);
    }

    /**
     * Copy values from an array of arrays into a matrix. Matrix storage type is the default type and values
     * are row or column oriented depending on the value of {@code byRows}.
     *
     * @param byRows true means row first orientation, otherwise column first orientation
     * @param values array of arrays of values
     * @return matrix which hold a range of data
     */
    static DMatrixDenseC copy(boolean byRows, double[][] values) {
        if (byRows) {
            return DMatrixDenseC.copy(0, values.length, 0, values[0].length, true, values);
        } else {
            return DMatrixDenseC.copy(0, values[0].length, 0, values.length, false, values);
        }
    }

    /**
     * Copy values from an array of arrays into a matrix. Matrix storage type and row/column
     * orientation are given as parameter.
     * <p>
     * This is the most customizable way to transfer values from an array of arrays into a matrix.
     * It allows creating of a matrix from a rectangular range of values.
     *
     * @param byRows   if true values are row oriented, if false values are column oriented
     * @param rowStart starting row inclusive
     * @param rowEnd   end row exclusive
     * @param colStart column row inclusive
     * @param colEnd   column end exclusive
     * @param values   array of arrays of values
     * @return matrix which hold a range of data
     */
    static DMatrixDenseC copy(int rowStart, int rowEnd, int colStart, int colEnd, boolean byRows, double[][] values) {
        return DMatrixDenseC.copy(rowStart, rowEnd, colStart, colEnd, byRows, values);
    }

    /**
     * Copies values from an array into a matrix with default row orientation and default storage type.
     * <p>
     * The layout of data is described by {@code inputRows} and {@code columnRows} and this is the same size
     * for the resulted matrix.
     *
     * @param inputRows number of rows for data layout
     * @param inputCols number of columns for data layout
     * @param values    array of values
     * @return matrix with a range of values copied from original array
     */
    static DMatrixDenseC copy(int inputRows, int inputCols, double... values) {
        return DMatrixDenseC.copy(inputRows, inputCols, 0, inputRows, 0, inputCols, true, values);
    }

    static DMatrixDenseC copy(int inputRows, int inputCols, boolean byRows, double... values) {
        return DMatrixDenseC.copy(inputRows, inputCols, 0, inputRows, 0, inputCols, byRows, values);
    }

    /**
     * Copies values from an array into a matrix.
     * <p>
     * This is the most customizable way to copy values from a contiguous arrays into a matrix.
     * <p>
     * The layout of data is described by {@code inputRows} and {@code columnRows}.
     * The row or column orientation is determined by {@code byRows} parameter. If {@code byRows} is true,
     * the values from the array are interpreted as containing rows one after another. If {@code byRows} is
     * false then the interpretation is that the array contains columns one after another.
     * <p>
     * The method allows creation of an array using a contiguous range of rows and columns described by
     * parameters.
     *
     * @param byRows    value orientation: true if row oriented, false if column oriented
     * @param inputRows number of rows for data layout
     * @param inputCols number of columns for data layout
     * @param rowStart  row start inclusive
     * @param rowEnd    row end exclusive
     * @param colStart  column start inclusive
     * @param colEnd    column end exclusive
     * @param values    array of values
     * @return matrix with a range of values copied from original array
     */
    static DMatrixDenseC copy(int inputRows, int inputCols, int rowStart, int rowEnd, int colStart, int colEnd, boolean byRows,
            double... values) {
        return DMatrixDenseC.copy(inputRows, inputCols, rowStart, rowEnd, colStart, colEnd, byRows, values);
    }

    /**
     * Copies data from a data frame using the default data storage frame.
     * Data is collected from frame using {@link Frame#getDouble(int, int)} calls.
     *
     * @param df data frame
     * @return matrix with collected values
     */
    static DMatrixDenseC copy(Frame df) {
        return DMatrixDenseC.copy(df);
    }

    /**
     * Copies data from a list of variables using the default data storage frame.
     * Data is collected from frame using {@link Frame#getDouble(int, int)} calls.
     *
     * @param vars array of variables
     * @return matrix with collected values
     */
    static DMatrixDenseC copy(Var... vars) {
        return DMatrixDenseC.copy(vars);
    }

    static DMatrixDenseC copy(boolean byRows, DVector... vectors) {
        return DMatrixDenseC.copy(byRows, vectors);
    }

    /**
     * @return number of rows of the matrix
     */
    int rows();

    /**
     * @return number of columns of the matrix
     */
    int cols();

    /**
     * Getter for value found at given row and column index.
     *
     * @param row row index
     * @param col column index
     * @return value at given row index and column index
     */
    double get(final int row, final int col);

    /**
     * Sets value at the given row and column indexes
     *
     * @param row   row index
     * @param col   column index
     * @param value value to be set
     */
    void set(final int row, int col, final double value);

    /**
     * Increment the value at given position.
     *
     * @param row   row index
     * @param col   column index
     * @param value value to be added
     */
    void inc(final int row, final int col, final double value);

    /**
     * Returns a vector build from values of the row with given index in the matrix.
     * <p>
     * Depending on implementation, the vector can be a view over the original data.
     *
     * @param row index of the selected row
     * @return result vector reference
     */
    DVector mapRow(final int row);

    /**
     * Returns a vector build from values of the row with given index in the matrix
     * and stores the values into the given {@param to} vector.
     *
     * @param row index of the selected row
     * @return destination vector
     */
    DVector mapRowTo(DVector to, final int row);

    /**
     * Returns a vector build from values of the row with given index in the matrix
     * into a new vector.
     *
     * @param row index of the selected row
     * @return new row vector
     */
    default DVector mapRowNew(final int row) {
        return mapRowTo(new DVectorDense(cols()), row);
    }

    /**
     * Returns a vector build from values of the column with given index in the matrix.
     *
     * @param col index of the selected column
     * @return result vector reference
     */
    DVector mapCol(final int col);

    /**
     * Returns a vector build from values of the column with given index in the matrix
     * and stores the values into the given {@param to} vector.
     *
     * @param col index of the selected column
     * @return destination vector
     */
    DVector mapColTo(DVector to, final int col);

    /**
     * Returns a vector build from values of the column with given index in the matrix
     * into a new vector.
     *
     * @param col index of the selected column
     * @return new row vector
     */
    default DVector mapColNew(final int col) {
        return mapColTo(new DVectorDense(rows()), col);
    }

    /**
     * Creates a new matrix which contains only the rows
     * specified by given indexes.
     *
     * @param indexes row indexes
     * @return result matrix reference
     */
    DMatrix mapRows(int... indexes);

    /**
     * Stores in {@param to} matrix only the rows specified by given indexes.
     *
     * @param indexes row indexes
     * @return result matrix reference
     */
    DMatrix mapRowsTo(DMatrix to, int... indexes);

    /**
     * Creates a new matrix which contains only the rows
     * specified by given indexes.
     *
     * @param indexes row indexes
     * @return result matrix reference
     */
    default DMatrix mapRowsNew(int... indexes) {
        DMatrixDenseC to = new DMatrixDenseC(indexes.length, cols());
        return mapRowsTo(to, indexes);
    }

    /**
     * Creates a view matrix which contains only the columns
     * specified by given indexes.
     *
     * @param indexes row indexes
     * @return result matrix reference
     */
    DMatrix mapCols(int... indexes);

    /**
     * Stores into matrix {@param to} only the columns specified by given indexes.
     *
     * @param indexes row indexes
     * @return result matrix reference
     */
    DMatrix mapColsTo(DMatrix to, int... indexes);

    /**
     * Creates a new matrix which contains only the columns
     * specified by given indexes.
     *
     * @param indexes row indexes
     * @return result matrix reference
     */
    default DMatrix mapColsNew(int... indexes) {
        DMatrixDenseC to = new DMatrixDenseC(rows(), indexes.length);
        return mapColsTo(to, indexes);
    }

    /**
     * Creates a new vector with the index value from each row (axis=0) or
     * column (axis=1). The length of the index array should match the number
     * of rows (axis=0) o columns (axis=1).
     *
     * @param indexes index for each element
     * @param axis    0 for rows, 1 for columns
     * @return vector with indexed values
     */
    DVector mapValues(int axis, int... indexes);

    /**
     * Creates a new view matrix which contains only rows with
     * indices in the given range starting from {@param start} inclusive
     * and ending at {@param end} exclusive.
     *
     * @param start start row index (inclusive)
     * @param end   end row index (exclusive)
     * @return result matrix reference
     */
    DMatrix rangeRows(int start, int end);

    /**
     * Filters rows with indices in the given range starting from {@code start} inclusive
     * and ending at {@code end} exclusive and copies the values into {@code to} matrix.
     *
     * @param start start row index (inclusive)
     * @param end   end row index (exclusive)
     * @return result matrix
     */
    DMatrix rangeRowsTo(DMatrix to, int start, int end);

    /**
     * Creates a new copy matrix which contains only rows with
     * indices in the given range starting from {@param start} inclusive
     * and ending at {@param end} exclusive.
     *
     * @param start start row index (inclusive)
     * @param end   end row index (exclusive)
     * @return result matrix reference
     */
    default DMatrix rangeRowsNew(int start, int end) {
        DMatrixDenseC to = new DMatrixDenseC(end - start, cols());
        return rangeRowsTo(to, start, end);
    }

    /**
     * Creates a new view matrix which contains only columns with
     * indices in the given range starting from {@param start} inclusive
     * and ending at {@param end} exclusive.
     *
     * @param start start col index (inclusive)
     * @param end   end col index (exclusive)
     * @return result matrix reference
     */
    DMatrix rangeCols(int start, int end);

    /**
     * Creates a new matrix which contains only columns with
     * indices in the given range starting from {@param start} inclusive
     * and ending at {@param end} exclusive and stores the result
     * into {@code to} matrix.
     *
     * @param start start col index (inclusive)
     * @param end   end col index (exclusive)
     * @return result matrix reference
     */
    DMatrix rangeColsTo(DMatrix to, int start, int end);

    /**
     * Creates a new matrix which contains only columns with
     * indices in the given range starting from {@param start} inclusive
     * and ending at {@param end} exclusive and stores the result
     * into a new matrix.
     *
     * @param start start col index (inclusive)
     * @param end   end col index (exclusive)
     * @return result matrix reference
     */
    default DMatrix rangeColsNew(int start, int end) {
        DMatrixDenseC to = new DMatrixDenseC(rows(), end - start);
        return rangeColsTo(to, start, end);
    }

    /**
     * Builds a view matrix having all rows not specified by given indexes.
     *
     * @param indexes rows to be removed
     * @return new mapped matrix containing all rows not specified by indexes
     */
    default DMatrix removeRows(int... indexes) {
        int[] rows = IntArrays.removeIndexesFromDenseSequence(0, rows(), indexes);
        return mapRows(rows);
    }

    /**
     * Builds view matrix having all rows not specified by given indexes
     * and stores the values into the {@param to} matrix.
     *
     * @param indexes rows to be removed
     * @return new mapped matrix containing all rows not specified by indexes
     */
    default DMatrix removeRowsTo(DMatrix to, int... indexes) {
        int[] rows = IntArrays.removeIndexesFromDenseSequence(0, rows(), indexes);
        return mapRowsTo(to, rows);
    }

    /**
     * Builds a new copy matrix having all rows not specified by given indexes.
     *
     * @param indexes rows to be removed
     * @return new mapped matrix containing all rows not specified by indexes
     */
    default DMatrix removeRowsNew(int... indexes) {
        int[] rows = IntArrays.removeIndexesFromDenseSequence(0, rows(), indexes);
        return mapRowsNew(rows);
    }

    /**
     * Builds a view matrix having all columns not specified by given indexes.
     *
     * @param indexes columns to be removed
     * @return new mapped matrix containing all columns not specified by indexes
     */
    default DMatrix removeCols(int... indexes) {
        int[] cols = IntArrays.removeIndexesFromDenseSequence(0, cols(), indexes);
        return mapCols(cols);
    }

    /**
     * Builds a new matrix having columns not specified by given indexes and stores the
     * values into {@param to}.
     *
     * @param indexes columns to be removed
     * @return new mapped matrix containing all columns not specified by indexes
     */
    default DMatrix removeColsTo(DMatrix to, int... indexes) {
        int[] cols = IntArrays.removeIndexesFromDenseSequence(0, cols(), indexes);
        return mapColsTo(to, cols);
    }

    /**
     * Builds a new matrix having columns not specified by given indexes and stores the
     * values into a new matrix.
     *
     * @param indexes columns to be removed
     * @return new mapped matrix containing all columns not specified by indexes
     */
    default DMatrix removeColsNew(int... indexes) {
        int[] cols = IntArrays.removeIndexesFromDenseSequence(0, cols(), indexes);
        return mapColsNew(cols);
    }

    /**
     * Adds a scalar value to all elements of a matrix in place.
     *
     * @param x value to be added
     * @return instance of the result matrix
     */
    DMatrix add(double x);

    /**
     * Computes the sum between matrix and a given scalar and store the result
     * into the given matrix.
     *
     * @param x  value to be added
     * @param to destination matrix
     * @return instance of the result matrix
     */
    DMatrix addTo(DMatrix to, double x);

    /**
     * Computes the sum between matrix and the given scalar and
     * store the result into a new matrix.
     *
     * @param x value to be added
     * @return instance of the result matrix
     */
    default DMatrix addNew(double x) {
        DMatrixDenseC copy = new DMatrixDenseC(rows(), cols());
        return addTo(copy, x);
    }

    /**
     * Add vector values each row/column of the matrix in place.
     *
     * @param x    vector to be added
     * @param axis 0 for rows, 1 for columns
     * @return same matrix with added values
     */
    DMatrix add(DVector x, int axis);

    /**
     * Computes the sum of vector values and each row/column of the matrix and
     * store the result to given {@param to} matrix.
     *
     * @param x    vector to be added
     * @param axis 0 for rows, 1 for columns
     * @param to   matrix to store the result
     * @return result matrix
     */
    DMatrix addTo(DMatrix to, DVector x, int axis);

    /**
     * Computes the sum of vector values to each row/column of the matrix
     * and stores the result into a new matrix.
     *
     * @param x    vector to be added
     * @param axis 0 for rows, 1 for columns
     * @return new result instance
     */
    default DMatrix addNew(DVector x, int axis) {
        DMatrixDenseC to = new DMatrixDenseC(rows(), cols());
        return addTo(to, x, axis);
    }

    /**
     * Adds element wise values from given matrix in place.
     *
     * @param b matrix with elements to be added
     * @return instance of the result matrix
     */
    DMatrix add(DMatrix b);

    /**
     * Adds element wise values from given matrix and store result into
     * given {@param to} matrix.
     *
     * @param b  matrix with elements to be added
     * @param to result matrix
     * @return instance of the result matrix
     */
    DMatrix addTo(DMatrix to, DMatrix b);

    /**
     * Adds element wise values from given matrix and store the result
     * into a new matrix.
     *
     * @param b matrix with elements to be added
     * @return instance of the result matrix
     */
    default DMatrix addNew(DMatrix b) {
        DMatrix to = new DMatrixDenseC(rows(), cols());
        return addTo(to, b);
    }

    /**
     * Subtract a scalar value to all elements of a matrix in place.
     *
     * @param x value to be subtracted
     * @return instance of the result matrix
     */
    DMatrix sub(double x);

    /**
     * Computes the difference between all elements of the matrix and the scalar value
     * and stores the result into {@param to} matrix.
     *
     * @param x  value to be subtracted
     * @param to result destination matrix
     * @return the result matrix
     */
    DMatrix subTo(DMatrix to, double x);

    /**
     * Computes the difference between all elements of the matrix and the scalar value
     * and stores the result into a new matrix.
     *
     * @param x value to be subtracted
     * @return instance of the result matrix
     */
    default DMatrix subNew(double x) {
        DMatrix to = new DMatrixDenseC(rows(), cols());
        return subTo(to, x);
    }

    /**
     * Subtract vector values from all rows (axis 0) or columns (axis 1).
     *
     * @param x    vector to be added
     * @param axis 0 for rows, 1 for columns
     * @return same matrix with added values
     */
    DMatrix sub(DVector x, int axis);

    /**
     * Computes the difference between vector values and each row (axis 0) or column (axis 1)
     * and stores the result into a {@code to} matrix.
     *
     * @param x    vector to be added
     * @param axis 0 for rows, 1 for columns
     * @param to   result matrix
     * @return result matrix
     */
    DMatrix subTo(DMatrix to, DVector x, int axis);

    /**
     * Computes the difference between vector values and each row (axis 0) or column (axis 1)
     * and stores the result into a new matrix.
     *
     * @param x    vector to be added
     * @param axis 0 for rows, 1 for columns
     * @return same matrix with added values
     */
    default DMatrix subNew(DVector x, int axis) {
        DMatrix to = new DMatrixDenseC(rows(), cols());
        return subTo(to, x, axis);
    }

    /**
     * Subtracts element wise values from given matrix in place.
     *
     * @param b matrix with elements to be substracted
     * @return instance of the result matrix
     */
    DMatrix sub(DMatrix b);

    /**
     * Computes the difference between this matrix and the given {@code b} matrix
     * and stores the result into {@code to} matrix.
     *
     * @param b  matrix with elements to be subtracted
     * @param to result matrix
     * @return instance of the result matrix
     */
    DMatrix subTo(DMatrix to, DMatrix b);

    /**
     * Computes the difference between this matrix and the given {@code b} matrix
     * and stores the result into a new matrix.
     *
     * @param b matrix with elements to be subtracted
     * @return instance of the result matrix
     */
    default DMatrix subNew(DMatrix b) {
        DMatrix to = new DMatrixDenseC(rows(), cols());
        return subTo(to, b);
    }

    /**
     * Multiply a scalar value to all elements of a matrix in place.
     *
     * @param x value to be multiplied with
     * @return instance of the result matrix
     */
    DMatrix mul(double x);

    /**
     * Computes the product between the matrix and the given scalar and stores the
     * result into {@code to} matrix.
     *
     * @param x  value to be multiplied with
     * @param to result matrix
     * @return instance of the result matrix
     */
    DMatrix mulTo(DMatrix to, double x);

    /**
     * Computes the product between the matrix and the given scalar and stores the
     * result into a new matrix.
     *
     * @param x value to be multiplied with
     * @return instance of the result matrix
     */
    default DMatrix mulNew(double x) {
        DMatrix to = new DMatrixDenseC(rows(), cols());
        return mulTo(to, x);
    }

    /**
     * Multiply vector values to all rows (axis 0) or columns (axis 1) in place.
     *
     * @param x    vector to be added
     * @param axis 0 for rows, 1 for columns
     * @return same matrix with added values
     */
    DMatrix mul(DVector x, int axis);

    /**
     * Multiply vector values to all rows (axis 0) or columns (axis 1) and
     * stores the result into {@code to} matrix.
     *
     * @param x    vector to be added
     * @param axis 0 for rows, 1 for columns
     * @param to   result matrix
     * @return result
     */
    DMatrix mulTo(DMatrix to, DVector x, int axis);

    /**
     * Multiply vector values to all rows (axis 0) or columns (axis 1).
     *
     * @param x    vector to be added
     * @param axis 0 for rows, 1 for columns
     * @return same matrix with added values
     */
    default DMatrix mulNew(DVector x, int axis) {
        DMatrix to = new DMatrixDenseC(rows(), cols());
        return mulTo(to, x, axis);
    }

    /**
     * Multiplies element wise with given matrix in place.
     *
     * @param b matrix with elements to be multiplied with
     * @return instance of the result matrix
     */
    DMatrix mul(DMatrix b);

    /**
     * Multiplies element wise with given matrix and
     * stores the result into {@code to} matrix.
     *
     * @param b  matrix with elements to be multiplied with
     * @param to result matrix
     * @return result matrix
     */
    DMatrix mulTo(DMatrix to, DMatrix b);

    /**
     * Multiplies element wise with given matrix and
     * stores the result into a new matrix.
     *
     * @param b matrix with elements to be multiplied with
     * @return instance of the result matrix
     */
    default DMatrix mulNew(DMatrix b) {
        DMatrix to = new DMatrixDenseC(rows(), cols());
        return mulTo(to, b);
    }

    /**
     * Divide a scalar value from all elements of a matrix in place.
     *
     * @param x divisor value
     * @return instance of the result matrix
     */
    DMatrix div(double x);

    /**
     * Divide a scalar value from all elements of a matrix and stores the result
     * into {@code to} matrix.
     *
     * @param x  divisor value
     * @param to result matrix
     * @return result matrix
     */
    DMatrix divTo(DMatrix to, double x);

    /**
     * Divide a scalar value from all elements of a matrix and stores the result
     * into a new matrix.
     *
     * @param x divisor value
     * @return instance of the result matrix
     */
    default DMatrix divNew(double x) {
        DMatrix to = new DMatrixDenseC(rows(), cols());
        return divTo(to, x);
    }

    /**
     * Divide all rows (axis 0) or columns (axis 1) by elements of the given vector
     * in place.
     *
     * @param x    vector to be divided with
     * @param axis axis addition
     * @return same matrix with divided values
     */
    DMatrix div(DVector x, int axis);

    /**
     * Divide all rows (axis 0) or columns (axis 1) by elements of the given vector
     * and stores the result into {@code to} matrix.
     *
     * @param x    vector to be added
     * @param axis axis addition
     * @param to   result matrix
     * @return result matrix
     */
    DMatrix divTo(DMatrix to, DVector x, int axis);

    /**
     * Divide all rows (axis 0) or columns (axis 1) by elements of the given vector
     * and stores the result into a new matrix.
     *
     * @param x    vector to be added
     * @param axis axis addition
     * @return same matrix with added values
     */
    default DMatrix divNew(DVector x, int axis) {
        DMatrix to = new DMatrixDenseC(rows(), cols());
        return divTo(to, x, axis);
    }

    /**
     * Divides element wise values with values from given matrix in place.
     *
     * @param b matrix with division elements
     * @return instance of the result matrix
     */
    DMatrix div(DMatrix b);

    /**
     * Divides element wise values with values from given matrix and
     * stores the result into {@code to} matrix.
     *
     * @param b  matrix with division elements
     * @param to result matrix
     * @return result matrix
     */
    DMatrix divTo(DMatrix to, DMatrix b);

    /**
     * Divides element wise values with values from given matrix and
     * stores the result into a new matrix.
     *
     * @param b matrix with division elements
     * @return instance of the result matrix
     */
    default DMatrix divNew(DMatrix b) {
        DMatrix to = new DMatrixDenseC(rows(), cols());
        return divTo(to, b);
    }

    /**
     * Apply the given function to all elements of the matrix.
     *
     * @param fun function to be applied
     * @return same instance matrix
     */
    DMatrix apply(Double2DoubleFunction fun);

    /**
     * Apply the given function to all elements of the matrix and store
     * the result into {@code to} matrix.
     *
     * @param fun function to be applied
     * @param to  result matrix
     * @return result matrix
     */
    DMatrix applyTo(DMatrix to, Double2DoubleFunction fun);

    /**
     * Apply the given function to all elements of the matrix and store
     * the result into a new matrix.
     *
     * @param fun function to be applied
     * @return new instance matrix
     */
    default DMatrix applyNew(Double2DoubleFunction fun) {
        DMatrix to = new DMatrixDenseC(rows(), cols());
        return applyTo(to, fun);
    }

    /**
     * Computes matrix vector multiplication.
     *
     * @param b vector to be multiplied with
     * @return result vector
     */
    DVector dot(DVector b);

    /**
     * Computes matrix - matrix multiplication.
     *
     * @param b matrix to be multiplied with
     * @return matrix result
     */
    DMatrix dot(DMatrix b);

    /**
     * Trace of the matrix, if the matrix is square. The trace of a squared
     * matrix is the sum of the elements from the main diagonal.
     * Otherwise returns an exception.
     *
     * @return value of the matrix trace
     */
    double trace();

    /**
     * Matrix rank obtained using singular value decomposition.
     *
     * @return effective numerical rank, obtained from SVD.
     */
    int rank();

    /**
     * Creates an instance of a transposed matrix which is a view over original data, if possible.
     *
     * @return new transposed matrix
     */
    DMatrix t();

    /**
     * Creates an instance of a transposed matrix and store it into {@code to} matrix.
     *
     * @return new transposed matrix
     */
    DMatrix tTo(DMatrix to);

    /**
     * Creates an instance of a transposed matrix and stores it into a new matrix.
     *
     * @return new transposed matrix
     */
    default DMatrix tNew() {
        DMatrix to = new DMatrixDenseC(rows(), cols());
        return tTo(to);
    }

    /**
     * Vector with values from main diagonal
     */
    DVector diag();

    /**
     * Computes scatter matrix.
     *
     * @return scatter matrix instance
     */
    DMatrix scatter();

    /**
     * Builds a vector with maximum values from rows/cols.
     * If axis = 0 and matrix has m rows and n columns, the resulted vector
     * will have size m and will contain in each position the maximum
     * value from the row with that position.
     *
     * @param axis axis for which to compute maximal values
     * @return vector with result values
     */
    DVector max(int axis);

    /**
     * Builds a vector with indexes of the maximum values from rows/columns.
     * Thus if a matrix has m rows and n columns, the resulted vector
     * will have size m and will contain in each position the maximum
     * value from the row with that position.
     *
     * @return vector with indexes of max value values
     */
    int[] argmax(int axis);

    /**
     * Builds a vector with minimum values from rows/cols.
     * If axis = 0 and matrix has m rows and n columns, the resulted vector
     * will have size m and will contain in each position the minimum
     * value from the row with that position.
     *
     * @param axis axis for which to compute maximal values
     * @return vector with result values
     */
    DVector min(int axis);

    /**
     * Builds a vector with indexes of the minimum value index from rows/columns.
     * If a matrix has m rows and n columns, the resulted vector
     * will have size m and will contain in each position the minimum
     * value index from the row with that position.
     *
     * @return vector with indexes of max values
     */
    int[] argmin(int axis);

    boolean isSymmetric();

    /**
     * Computes the sum of all elements from the matrix.
     *
     * @return scalar value with sum
     */
    double sum();

    /**
     * Computes the sum of all elements on the given axis. If axis
     * is 0 it will compute sum of rows, the resulting vector having size
     * as the number of columns. If the axis is 1 it will compute sums of columns.
     *
     * @param axis specifies the dimension used for summing
     * @return vector of sums on the given axis
     */
    DVector sum(int axis);

    /**
     * Computes the mean of all elements of the matrix.
     *
     * @return mean of all matrix elements
     */
    default double mean() {
        return sum() / (rows() * cols());
    }

    /**
     * Computes vector of means along the specified axis.
     *
     * @param axis 0 for rows,  for columns
     * @return vector of means along axis
     */
    default DVector mean(int axis) {
        return sum(axis).div(axis == 0 ? rows() : cols());
    }

    /**
     * Compute the variance of all elements of the matrix.
     *
     * @return variance of all elements of the matrix
     */
    double variance();

    /**
     * Computes vector of variances along the given axis of the matrix.
     *
     * @param axis 0 for rows, 1 for columns
     * @return vector of variances computed along given axis
     */
    DVector variance(int axis);

    /**
     * Compute the standard deviation of all elements of the matrix.
     *
     * @return standard deviation of all elements of the matrix
     */
    default double sd() {
        return Math.sqrt(variance());
    }

    /**
     * Computes vector of standard deviations along the given axis of the matrix.
     *
     * @param axis 0 for rows, 1 for columns
     * @return vector of standard deviations computed along given axis
     */
    default DVector sd(int axis) {
        return variance(axis).apply(Math::sqrt);
    }

    /**
     * Stream of double values, the element order is not guaranteed,
     * it depends on the implementation.
     *
     * @return double value stream
     */
    DoubleStream valueStream();

    /**
     * Creates a copy of a matrix.
     *
     * @return copy matrix reference
     */
    DMatrix copy();

    default DoubleCholeskyDecomposition cholesky() {
        return cholesky(false);
    }

    default DoubleCholeskyDecomposition cholesky(boolean rightFlag) {
        return new DoubleCholeskyDecomposition(this, rightFlag);
    }

    default DoubleLUDecomposition lu() {
        return lu(DoubleLUDecomposition.Method.GAUSSIAN_ELIMINATION);
    }

    default DoubleLUDecomposition lu(DoubleLUDecomposition.Method method) {
        return new DoubleLUDecomposition(this, method);
    }

    default DoubleQRDecomposition qr() {
        return new DoubleQRDecomposition(this);
    }

    default DoubleSVDecomposition svd() {
        return svd(true, true);
    }

    default DoubleSVDecomposition svd(boolean wantu, boolean wantv) {
        return new DoubleSVDecomposition(this, wantu, wantv);
    }

    default DoubleEigenDecomposition evd() {
        return new DoubleEigenDecomposition(this);
    }

    /**
     * Compares matrices using a tolerance of 1e-12 for values.
     * If the absolute difference between two values is less
     * than the specified tolerance, than the values are
     * considered equal.
     *
     * @param m matrix to compare with
     * @return true if dimensions and elements are equal
     */
    default boolean deepEquals(DMatrix m) {
        return deepEquals(m, 1e-12);
    }

    /**
     * Compares matrices using a tolerance for values.
     * If the absolute difference between two values is less
     * than the specified tolerance, than the values are
     * considered equal.
     *
     * @param m   matrix to compare with
     * @param eps tolerance
     * @return true if dimensions and elements are equal
     */
    boolean deepEquals(DMatrix m, double eps);

    /**
     * Creates a new matrix with a different shape which contains original values
     * in the same position and eventual extended space filled with given {@code fill} value.
     *
     * @param rows new number of rows
     * @param cols new number of columns
     * @param fill fill value for extended cells
     * @return new matrix instance
     */
    DMatrix resizeCopy(int rows, int cols, double fill);

    default DMatrix roundValues(int scale) {
        return roundValues(scale, RoundingMode.HALF_UP);
    }

    default DMatrix roundValues(int scale, RoundingMode mode) {
        apply(x -> BigDecimal.valueOf(x).setScale(scale, mode).doubleValue());
        return this;
    }
}
