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

public final class MaxAssocOperator extends TensorAssociativeOp {

    @Override
    public byte initByte() {
        return Byte.MIN_VALUE;
    }

    @Override
    public byte applyByte(byte a, byte b) {
        return a >= b ? a : b;
    }

    @Override
    public ByteVector initByte(VectorSpecies<Byte> species) {
        return ByteVector.broadcast(species, Byte.MIN_VALUE);
    }

    @Override
    public ByteVector applyByte(ByteVector a, ByteVector b) {
        return a.max(b);
    }


    @Override
    public int initInt() {
        return Integer.MIN_VALUE;
    }

    @Override
    public int applyInt(int a, int b) {
        return Math.max(a, b);
    }

    @Override
    public IntVector initInt(VectorSpecies<Integer> species) {
        return IntVector.broadcast(species, Integer.MIN_VALUE);
    }

    @Override
    public IntVector applyInt(IntVector a, IntVector b) {
        return a.max(b);
    }


    @Override
    public float initFloat() {
        return Float.NEGATIVE_INFINITY;
    }

    @Override
    public float applyFloat(float a, float b) {
        return Math.max(a, b);
    }

    @Override
    public FloatVector initFloat(VectorSpecies<Float> species) {
        return FloatVector.broadcast(species, Float.NEGATIVE_INFINITY);
    }

    @Override
    public FloatVector applyFloat(FloatVector a, FloatVector b) {
        return a.max(b);
    }


    @Override
    public double initDouble() {
        return Double.NEGATIVE_INFINITY;
    }

    @Override
    public double applyDouble(double a, double b) {
        return Math.max(a, b);
    }

    @Override
    public DoubleVector initDouble(VectorSpecies<Double> species) {
        return DoubleVector.broadcast(species, Double.NEGATIVE_INFINITY);
    }

    @Override
    public DoubleVector applyDouble(DoubleVector a, DoubleVector b) {
        return a.max(b);
    }
}
