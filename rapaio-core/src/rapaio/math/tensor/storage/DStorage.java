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

package rapaio.math.tensor.storage;

import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorMask;

public interface DStorage extends Storage<Double, DStorage> {

    @Override
    default Double getValue(int offset) {
        return get(offset);
    }

    double get(int offset);

    @Override
    default void setValue(int offset, Double v) {
        set(offset, v);
    }

    void set(int offset, double v);

    DoubleVector load(int offset);

    DoubleVector load(int offset, VectorMask<Double> mask);

    DoubleVector load(int offset, int[] indexMap, int mapOffset);

    DoubleVector load(int offset, int[] indexMap, int mapOffset, VectorMask<Double> mask);

    void save(DoubleVector v, int offset);

    void save(DoubleVector v, int offset, VectorMask<Double> mask);

    void save(DoubleVector v, int offset, int[] indexMap, int mapOffset);

    void save(DoubleVector v, int offset, int[] indexMap, int mapOffset, VectorMask<Double> mask);

    @Override
    default void fillValue(int start, int len, Double v) {
        fill(start, len, v);
    }

    void fill(int start, int len, double v);

    @Override
    DStorage copy();
}
