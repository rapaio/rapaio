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

package rapaio.nn.tensors;

import rapaio.darray.Compare;
import rapaio.darray.DArray;
import rapaio.nn.Tensor;

public final class CompareTrue extends Tensor {

    private final DArray<?> mask;

    public CompareTrue(Tensor x, Compare cmp, double threshold) {
        super(x.tm(), CompareTrue.class.getSimpleName());

        this.mask = x.value().copy().compareMask_(cmp, threshold);
        this.setValue(mask.mul(x.value()));
        backEdge(x, () -> this.grad().mul(mask));
    }

    public DArray<?> mask() {
        return mask;
    }
}
