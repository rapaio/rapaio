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

package rapaio.ml.common.kernel;

import java.io.Serial;

import rapaio.math.tensor.Tensor;
import rapaio.ml.common.kernel.cache.KernelCache;
import rapaio.ml.common.kernel.cache.MapKernelCache;
import rapaio.ml.common.kernel.cache.SolidKernelCache;

/**
 * @author <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> at 1/16/15.
 */
public abstract class AbstractKernel implements Kernel {

    @Serial
    private static final long serialVersionUID = -2216556261751685749L;

    private KernelCache cache;

    @Override
    public void buildKernelCache(Tensor<Double> df) {
        if (df.dim(0) <= 10_000) {
            cache = new SolidKernelCache(df);
        } else {
            cache = new MapKernelCache();
        }
    }

    @Override
    public boolean isLinear() {
        return false;
    }

    protected double deltaSumSquares(Tensor<Double> u, Tensor<Double> v) {
        return u.sub(v).sqr().sum();
    }

    @Override
    public double compute(int row1, int row2, Tensor<Double> r1, Tensor<Double> r2) {
        Double value = cache.retrieve(row1, row2);
        if (value == null) {
            value = compute(r1, r2);
            cache.store(row1, row2, value);
        }
        return value;
    }

    @Override
    public void clean() {
        cache.clear();
    }
}

