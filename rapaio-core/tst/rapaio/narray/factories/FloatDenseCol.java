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

package rapaio.narray.factories;

import rapaio.narray.NArray;
import rapaio.narray.NArrayManager;
import rapaio.narray.Order;
import rapaio.narray.Shape;

public final class FloatDenseCol extends FloatDense {

    public FloatDenseCol(NArrayManager arrayManager) {
        super(arrayManager);
    }

    @Override
    public NArray<Float> seq(Shape shape) {
        return manager.seq(dt, shape, Order.F);
    }

    @Override
    public NArray<Float> zeros(Shape shape) {
        return manager.zeros(dt, shape, Order.F);
    }

    @Override
    public NArray<Float> random(Shape shape) {
        return manager.random(dt, shape, random, Order.F);
    }
}
