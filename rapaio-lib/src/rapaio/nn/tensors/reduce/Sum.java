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

package rapaio.nn.tensors.reduce;

import rapaio.darray.DArray;
import rapaio.nn.Tensor;

public class Sum extends Tensor {


    public Sum(Tensor x) {
        super(x.tm(), Sum.class.getSimpleName());

        this.setValue(tm.scalarArray(x.value().sum().doubleValue()));
        backEdge(x, () -> {
            DArray<?> grad = this.grad();
            // gradient is a scalar, we expand by child shape
            if (!x.value().isScalar()) {
                for (int i = 0; i < x.value().rank(); i++) {
                    grad = grad.strexp(i, x.value().dim(i));
                }
            }
            return grad;
        });
    }
}
