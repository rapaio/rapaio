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

package rapaio.experiment.math.nn;

import rapaio.narray.Shape;
import rapaio.nn.Autograd;
import rapaio.nn.Net;
import rapaio.nn.Tensor;
import rapaio.nn.TensorManager;
import rapaio.nn.layer.ELU;

public class SanboxExpr {

    public static void main() {

        try (TensorManager tm = TensorManager.ofFloat()) {
            Tensor x = tm.var(tm.seqArray(Shape.of(167)).mul_(0.12).sub_(10)).requiresGrad(true).name("x");
            Net elu = new ELU(tm);
            Tensor el = elu.forward11(x);
            Tensor exp = el.exp();
            Tensor sum = exp.sum();
            sum.setGrad(tm.scalarArray(1));
            Autograd.backward(sum).printTensors();
        }
    }
}