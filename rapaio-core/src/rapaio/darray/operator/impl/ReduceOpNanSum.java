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

package rapaio.darray.operator.impl;

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;
import rapaio.darray.Simd;
import rapaio.darray.Storage;
import rapaio.darray.iterators.StrideLoopDescriptor;
import rapaio.darray.operator.DArrayReduceOp;

public final class ReduceOpNanSum extends DArrayReduceOp {

    @Override
    public boolean floatingPointOnly() {
        return false;
    }

    private static final byte initByte = 0;
    private static final int initInt = 0;
    private static final float initFloat = 0f;
    private static final double initDouble = 0d;

    @Override
    protected byte reduceByteVectorUnit(StrideLoopDescriptor<Byte> loop, Storage storage) {
        byte result = initByte;
        for (int p : loop.offsets) {
            ByteVector a = Simd.broadcast(initByte);
            int i = 0;
            for (; i < loop.simdBound; i += loop.simdLen) {
                ByteVector v = storage.getByteVector(p);
                a = a.add(v);
                p += loop.simdLen;
            }
            result += a.reduceLanes(VectorOperators.ADD);
            for (; i < loop.bound; i++) {
                result += storage.getByte(p);
                p++;
            }
        }
        return result;
    }

    @Override
    protected byte reduceByteVectorStep(StrideLoopDescriptor<Byte> loop, Storage storage) {
        byte result = initByte;
        for (int p : loop.offsets) {
            ByteVector a = Simd.broadcast(initByte);
            int i = 0;
            for (; i < loop.simdBound; i += loop.simdLen) {
                ByteVector v = storage.getByteVector(p, loop.simdOffsets(), 0);
                a = a.add(v);
                p += loop.simdLen * loop.step;
            }
            result += a.reduceLanes(VectorOperators.ADD);
            for (; i < loop.bound; i++) {
                result += storage.getByte(p);
                p += loop.step;
            }
        }
        return result;
    }

    @Override
    protected byte reduceByteDefault(StrideLoopDescriptor<Byte> loop, Storage storage) {
        byte result = initByte;
        for (int p : loop.offsets) {
            for (int i = 0; i < loop.bound; i++) {
                result += storage.getByte(p);
                p += loop.step;
            }
        }
        return result;
    }

    @Override
    protected int reduceIntVectorUnit(StrideLoopDescriptor<Integer> loop, Storage storage) {
        int result = initInt;
        for (int p : loop.offsets) {
            IntVector a = Simd.broadcast(initInt);
            int i = 0;
            for (; i < loop.simdBound; i += loop.simdLen) {
                IntVector v = storage.getIntVector(p);
                a = a.add(v);
                p += loop.simdLen;
            }
            result += a.reduceLanes(VectorOperators.ADD);
            for (; i < loop.bound; i++) {
                result += storage.getInt(p);
                p++;
            }
        }
        return result;
    }

    @Override
    protected int reduceIntVectorStep(StrideLoopDescriptor<Integer> loop, Storage storage) {
        int result = initInt;
        for (int p : loop.offsets) {
            IntVector a = Simd.broadcast(initInt);
            int i = 0;
            for (; i < loop.simdBound; i += loop.simdLen) {
                IntVector v = storage.getIntVector(p, loop.simdOffsets(), 0);
                a = a.add(v);
                p += loop.simdLen * loop.step;
            }
            result += a.reduceLanes(VectorOperators.ADD);
            for (; i < loop.bound; i++) {
                result += storage.getInt(p);
                p += loop.step;
            }
        }
        return result;
    }

    @Override
    protected int reduceIntDefault(StrideLoopDescriptor<Integer> loop, Storage storage) {
        int result = initInt;
        for (int p : loop.offsets) {
            for (int i = 0; i < loop.bound; i++) {
                result += storage.getInt(p);
                p += loop.step;
            }
        }
        return result;
    }

    @Override
    protected float reduceFloatVectorUnit(StrideLoopDescriptor<Float> loop, Storage storage) {
        float result = initFloat;
        for (int p : loop.offsets) {
            FloatVector a = Simd.broadcast(initFloat);
            int i = 0;
            for (; i < loop.simdBound; i += loop.simdLen) {
                FloatVector v = storage.getFloatVector(p);
                VectorMask<Float> m = v.test(VectorOperators.IS_NAN);
                a = a.add(v, m.not());
                p += loop.simdLen;
            }
            VectorMask<Float> m = a.test(VectorOperators.IS_NAN);
            result += a.reduceLanes(VectorOperators.ADD, m.not());
            for (; i < loop.bound; i++) {
                float value = storage.getFloat(p);
                if(!Float.isNaN(value)) {
                    result += value;
                }
                p++;
            }
        }
        return result;
    }

    @Override
    protected float reduceFloatVectorStep(StrideLoopDescriptor<Float> loop, Storage storage) {
        float result = initFloat;
        for (int p : loop.offsets) {
            FloatVector a = Simd.broadcast(initFloat);
            int i = 0;
            for (; i < loop.simdBound; i += loop.simdLen) {
                FloatVector v = storage.getFloatVector(p, loop.simdOffsets(), 0);
                VectorMask<Float> m = v.test(VectorOperators.IS_NAN);
                a = a.add(v, m.not());
                p += loop.simdLen * loop.step;
            }
            VectorMask<Float> m = a.test(VectorOperators.IS_NAN);
            result += a.reduceLanes(VectorOperators.ADD, m.not());
            for (; i < loop.bound; i++) {
                float value = storage.getFloat(p);
                if(!Float.isNaN(value)) {
                    result += value;
                }
                p += loop.step;
            }
        }
        return result;
    }

    @Override
    protected float reduceFloatDefault(StrideLoopDescriptor<Float> loop, Storage storage) {
        float result = initFloat;
        for (int p : loop.offsets) {
            for (int i = 0; i < loop.bound; i++) {
                float value = storage.getFloat(p);
                if(!Float.isNaN(value)) {
                    result += value;
                }
                p += loop.step;
            }
        }
        return result;
    }

    @Override
    protected double reduceDoubleVectorUnit(StrideLoopDescriptor<Double> loop, Storage storage) {
        double result = initDouble;
        for (int p : loop.offsets) {
            DoubleVector a = Simd.broadcast(initDouble);
            int i = 0;
            for (; i < loop.simdBound; i += loop.simdLen) {
                DoubleVector v = storage.getDoubleVector(p);
                VectorMask<Double> m = v.test(VectorOperators.IS_NAN);
                a = a.add(v, m.not());
                p += loop.simdLen;
            }
            VectorMask<Double> m = a.test(VectorOperators.IS_NAN);
            result += a.reduceLanes(VectorOperators.ADD, m.not());
            for (; i < loop.bound; i++) {
                double value = storage.getDouble(p);
                if(!Double.isNaN(value)) {
                    result += value;
                }
                p++;
            }
        }
        return result;
    }

    @Override
    protected double reduceDoubleVectorStep(StrideLoopDescriptor<Double> loop, Storage storage) {
        double result = initDouble;
        for (int p : loop.offsets) {
            DoubleVector a = Simd.broadcast(initDouble);
            int i = 0;
            for (; i < loop.simdBound; i += loop.simdLen) {
                DoubleVector v = storage.getDoubleVector(p, loop.simdOffsets(), 0);
                VectorMask<Double> m = v.test(VectorOperators.IS_NAN);
                a = a.add(v, m.not());
                p += loop.simdLen * loop.step;
            }
            VectorMask<Double> m = a.test(VectorOperators.IS_NAN);
            result += a.reduceLanes(VectorOperators.ADD, m.not());
            for (; i < loop.bound; i++) {
                double value = storage.getDouble(p);
                if(!Double.isNaN(value)) {
                    result += value;
                }
                p += loop.step;
            }
        }
        return result;
    }

    @Override
    protected double reduceDoubleDefault(StrideLoopDescriptor<Double> loop, Storage storage) {
        double result = initDouble;
        for (int p : loop.offsets) {
            for (int i = 0; i < loop.bound; i++) {
                double value = storage.getDouble(p);
                if(!Double.isNaN(value)) {
                    result += value;
                }
                p += loop.step;
            }
        }
        return result;
    }
}
