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

package rapaio.util.vectorization;

import jdk.incubator.vector.VectorOperators;

public enum DoubleBinaryOp {

    ADD() {
        @Override
        public double apply(double a, double b) {
            return a + b;
        }

        @Override
        public VectorOperators.Associative operator() {
            return VectorOperators.ADD;
        }
    };

    public abstract double apply(double a, double b);

    public abstract VectorOperators.Associative operator();
}
