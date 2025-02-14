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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import rapaio.core.distributions.Normal;
import rapaio.darray.Order;
import rapaio.darray.Shape;
import rapaio.nn.Autograd;
import rapaio.nn.TensorManager;
import rapaio.nn.tensors.AbstractTensorTest;

public class MaxTest extends AbstractTensorTest {


    @ParameterizedTest
    @MethodSource("managers")
    void testMax(TensorManager tm) {
        var x = tm.randomTensor(Shape.of(3, 2, 4), Normal.std()).requiresGrad(true).name("x");
        var max = x.max(0).name("max");
        var sum = max.sum();
        sum.setGrad(tm.scalarArray(1));

        Autograd.backward(sum);

        assertNotNull(x.grad());

        var valueIt = x.value().ptrIterator(Order.C);
        var gradIt = x.grad().ptrIterator(Order.C);
        while (gradIt.hasNext()) {
            if (x.value().ptrGet(valueIt.next()).doubleValue() > 0) {
                assertEquals(1, x.grad().ptrGet(gradIt.next()).doubleValue());
            } else {
                assertEquals(0, x.grad().ptrGet(gradIt.next()).doubleValue());
            }
        }
    }

}
