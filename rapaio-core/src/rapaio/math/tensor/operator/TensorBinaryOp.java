/*
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/
 *
 *    Copyright 2013 Aurelian Tutuianu
 *    Copyright 2014 Aurelian Tutuianu
 *    Copyright 2015 Aurelian Tutuianu
 *    Copyright 2016 Aurelian Tutuianu
 *    Copyright 2017 Aurelian Tutuianu
 *    Copyright 2018 Aurelian Tutuianu
 *    Copyright 2019 Aurelian Tutuianu
 *    Copyright 2020 Aurelian Tutuianu
 *    Copyright 2021 Aurelian Tutuianu
 *    Copyright 2022 Aurelian Tutuianu
 *    Copyright 2023 Aurelian Tutuianu
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

package rapaio.math.tensor.operator;

import jdk.incubator.vector.VectorOperators;

public interface TensorBinaryOp {

    TensorBinaryOp ADD = new BinaryOpAdd();
    TensorBinaryOp SUB = new BinaryOpSub();
    TensorBinaryOp MUL = new BinaryOpMul();
    TensorBinaryOp DIV = new BinaryOpDiv();

    VectorOperators.Binary vop();

    double applyDouble(double a, double b);

    float applyFloat(float a, float b);

    int applyInt(int a, int b);

    byte applyByte(int a, int b);
}

final class BinaryOpAdd implements TensorBinaryOp {

    @Override
    public VectorOperators.Binary vop() {
        return VectorOperators.ADD;
    }

    @Override
    public double applyDouble(double v, double a) {
        return v + a;
    }

    @Override
    public float applyFloat(float v, float a) {
        return v + a;
    }

    @Override
    public int applyInt(int a, int b) {
        return a + b;
    }

    @Override
    public byte applyByte(int a, int b) {
        return (byte) (a + b);
    }
}

final class BinaryOpSub implements TensorBinaryOp {

    @Override
    public VectorOperators.Binary vop() {
        return VectorOperators.SUB;
    }

    @Override
    public double applyDouble(double v, double a) {
        return v - a;
    }

    @Override
    public float applyFloat(float v, float a) {
        return v - a;
    }

    @Override
    public int applyInt(int a, int b) {
        return a - b;
    }

    @Override
    public byte applyByte(int a, int b) {
        return (byte) (a - b);
    }
}

final class BinaryOpMul implements TensorBinaryOp {

    @Override
    public VectorOperators.Binary vop() {
        return VectorOperators.MUL;
    }

    @Override
    public double applyDouble(double v, double a) {
        return v * a;
    }

    @Override
    public float applyFloat(float v, float a) {
        return v * a;
    }

    @Override
    public int applyInt(int a, int b) {
        return a * b;
    }

    @Override
    public byte applyByte(int a, int b) {
        return (byte) (a * b);
    }
}

final class BinaryOpDiv implements TensorBinaryOp {

    @Override
    public VectorOperators.Binary vop() {
        return VectorOperators.DIV;
    }

    @Override
    public double applyDouble(double v, double a) {
        return v / a;
    }

    @Override
    public float applyFloat(float v, float a) {
        return v / a;
    }

    @Override
    public int applyInt(int a, int b) {
        return a / b;
    }

    @Override
    public byte applyByte(int a, int b) {
        return (byte) (a / b);
    }
}
