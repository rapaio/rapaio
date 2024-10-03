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

package rapaio.math.tensor.operator.impl;

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorSpecies;
import rapaio.math.tensor.operator.TensorAssociativeOp;

public final class AddAssocOperator extends TensorAssociativeOp {

    @Override
    public byte initByte() {
        return 0;
    }

    @Override
    public byte applyByte(byte a, byte b) {
        return (byte) (a + b);
    }

    @Override
    public ByteVector initByte(VectorSpecies<Byte> species) {
        return ByteVector.zero(species);
    }

    @Override
    public ByteVector applyByte(ByteVector a, ByteVector b) {
        return a.add(b);
    }


    @Override
    public int initInt() {
        return 0;
    }

    @Override
    public int applyInt(int a, int b) {
        return a + b;
    }

    @Override
    public IntVector initInt(VectorSpecies<Integer> species) {
        return IntVector.zero(species);
    }

    @Override
    public IntVector applyInt(IntVector a, IntVector b) {
        return a.add(b);
    }


    @Override
    public float initFloat() {
        return 0f;
    }

    @Override
    public float applyFloat(float v, float a) {
        return v + a;
    }

    @Override
    public FloatVector initFloat(VectorSpecies<Float> species) {
        return FloatVector.zero(species);
    }

    @Override
    public FloatVector applyFloat(FloatVector a, FloatVector b) {
        return a.add(b);
    }

    @Override
    public double initDouble() {
        return 0;
    }

    @Override
    public double applyDouble(double v, double a) {
        return v + a;
    }

    @Override
    public DoubleVector initDouble(VectorSpecies<Double> species) {
        return DoubleVector.zero(species);
    }

    @Override
    public DoubleVector applyDouble(DoubleVector a, DoubleVector b) {
        return a.add(b);
    }
}
