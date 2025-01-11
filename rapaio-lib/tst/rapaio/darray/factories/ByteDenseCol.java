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

package rapaio.darray.factories;

import rapaio.darray.DArray;
import rapaio.darray.DArrayManager;
import rapaio.darray.Order;
import rapaio.darray.Shape;

public final class ByteDenseCol extends ByteDense {

    public ByteDenseCol(DArrayManager arrayManager) {
        super(arrayManager);
    }

    @Override
    public DArray<Byte> seq(Shape shape) {
        return manager.seq(dt, shape, Order.F);
    }

    @Override
    public DArray<Byte> zeros(Shape shape) {
        return manager.zeros(dt, shape, Order.F);
    }

    @Override
    public DArray<Byte> random(Shape shape) {
        return manager.random(dt, shape, random, Order.F);
    }
}