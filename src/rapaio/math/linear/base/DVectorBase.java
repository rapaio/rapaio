/*
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/
 *
 * Copyright 2013 - 2021 Aurelian Tutuianu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package rapaio.math.linear.base;

import java.util.Arrays;
import java.util.stream.DoubleStream;

import rapaio.data.VarDouble;
import rapaio.math.linear.DVector;
import rapaio.math.linear.dense.DVectorMap;
import rapaio.math.linear.option.AlgebraOption;
import rapaio.math.linear.option.AlgebraOptions;

/**
 * DVector implementation used only as a test bed for the abstract vector functionality.
 */
public class DVectorBase extends AbstractDVector {

    private final double[] values;

    public DVectorBase(double[] values) {
        this.values = values;
    }

    @Override
    public int size() {
        return values.length;
    }

    @Override
    public DVector map(int[] indexes, AlgebraOption<?>... opts) {
        if (AlgebraOptions.from(opts).isCopy()) {
            double[] copy = new double[indexes.length];
            for (int i = 0; i < indexes.length; i++) {
                copy[i] = get(indexes[i]);
            }
            return DVector.wrap(copy);
        }
        return new DVectorMap(0, indexes, values);
    }

    @Override
    public DVector copy() {
        return new DVectorBase(Arrays.copyOf(values, values.length));
    }

    @Override
    public double get(int i) {
        return values[i];
    }

    @Override
    public void set(int i, double value) {
        values[i] = value;
    }

    @Override
    public void inc(int i, double value) {
        values[i] += value;
    }

    @Override
    public DoubleStream valueStream() {
        return DoubleStream.of(values);
    }

    @Override
    public VarDouble dVar(AlgebraOption<?>... opts) {
        return VarDouble.wrap(values);
    }
}