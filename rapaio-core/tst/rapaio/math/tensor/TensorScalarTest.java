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

package rapaio.math.tensor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TensorScalarTest {

    private TensorManager.OfType<Byte> tmb;
    private TensorManager.OfType<Integer> tmi;
    private TensorManager.OfType<Double> tmd;

    @BeforeEach
    void beforeEach() {
        tmb = Tensors.ofByte();
        tmi = Tensors.ofInt();
        tmd = Tensors.ofDouble();
    }

    @Test
    void binaryScalarTest() {

        Tensor<?> v1 = tmd.seq(Shape.of(3)).add(10);
        for (int i = 0; i < 3; i++) {
            assertEquals(10 + i, v1.get(i).doubleValue());
        }

        Tensor<?> v2 = tmi.seq(Shape.of(3)).add(10.);
        for (int i = 0; i < 3; i++) {
            assertEquals(10 + i, v2.get(i).intValue());
        }

        Tensor<?> v3 = tmb.seq(Shape.of(3)).add(10.);
        for (int i = 0; i < 3; i++) {
            assertEquals(10 + i, v3.get(i).byteValue());
        }
    }
}
