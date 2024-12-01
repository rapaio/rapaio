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

package rapaio.narray.operator.impl;

import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;
import rapaio.data.OperationNotAvailableException;
import rapaio.narray.Storage;
import rapaio.narray.iterators.StrideLoopDescriptor;
import rapaio.narray.operator.NArrayReduceOp;

public final class ReduceOpMean extends NArrayReduceOp {

    @Override
    public boolean floatingPointOnly() {
        return true;
    }

    private static final float initFloat = 0;
    private static final double initDouble = 0;

    @Override
    protected byte reduceByteVectorUnit(StrideLoopDescriptor<Byte> loop, Storage storage) {
        throw new OperationNotAvailableException();
    }

    @Override
    protected byte reduceByteVectorStep(StrideLoopDescriptor<Byte> loop, Storage storage) {
        throw new OperationNotAvailableException();
    }

    @Override
    protected byte reduceByteDefault(StrideLoopDescriptor<Byte> loop, Storage storage) {
        throw new OperationNotAvailableException();
    }

    @Override
    protected int reduceIntVectorUnit(StrideLoopDescriptor<Integer> loop, Storage storage) {
        throw new OperationNotAvailableException();
    }

    @Override
    protected int reduceIntVectorStep(StrideLoopDescriptor<Integer> loop, Storage storage) {
        throw new OperationNotAvailableException();
    }

    @Override
    protected int reduceIntDefault(StrideLoopDescriptor<Integer> loop, Storage storage) {
        throw new OperationNotAvailableException();
    }

    @Override
    protected float reduceFloatVectorUnit(StrideLoopDescriptor<Float> loop, Storage storage) {
        float sum = initFloat;
        for (int p : loop.offsets) {
            FloatVector a = FloatVector.broadcast(loop.vs, initFloat);
            int i = 0;
            for (; i < loop.simdBound; i += loop.simdLen) {
                FloatVector v = storage.getFloatVector(loop.vs, p);
                a = a.add(v);
                p += loop.simdLen;
            }
            sum += a.reduceLanes(VectorOperators.ADD);
            for (; i < loop.size; i++) {
                sum += storage.getFloat(p);
                p++;
            }
        }
        float count = loop.size * loop.offsets.length;
        float mean = sum / count;

        sum = 0;
        FloatVector vmean = FloatVector.broadcast(loop.vs, mean);
        for (int p : loop.offsets) {
            FloatVector a = FloatVector.broadcast(loop.vs, initFloat);
            int i = 0;
            for (; i < loop.simdBound; i += loop.simdLen) {
                FloatVector v = storage.getFloatVector(loop.vs, p);
                v = v.sub(vmean);
                a = a.add(v);
                p += loop.simdLen;
            }
            sum += a.reduceLanes(VectorOperators.ADD);
            for (; i < loop.size; i++) {
                sum += storage.getFloat(p) - mean;
                p++;
            }
        }

        return mean + sum / count;
    }

    @Override
    protected float reduceFloatVectorStep(StrideLoopDescriptor<Float> loop, Storage storage) {
        float sum = initFloat;
        for (int p : loop.offsets) {
            FloatVector a = FloatVector.broadcast(loop.vs, initFloat);
            int i = 0;
            for (; i < loop.simdBound; i += loop.simdLen) {
                FloatVector v = storage.getFloatVector(loop.vs, p, loop.simdOffsets(), 0);
                a = a.add(v);
                p += loop.simdLen * loop.step;
            }
            sum += a.reduceLanes(VectorOperators.ADD);
            for (; i < loop.size; i++) {
                sum += storage.getFloat(p);
                p += loop.step;
            }
        }
        float count = loop.size * loop.offsets.length;
        float mean = sum / count;

        sum = 0;
        for (int p : loop.offsets) {
            FloatVector a = FloatVector.broadcast(loop.vs, initFloat);
            int i = 0;
            for (; i < loop.simdBound; i += loop.simdLen) {
                FloatVector v = storage.getFloatVector(loop.vs, p, loop.simdOffsets(), 0);
                v = v.sub(mean);
                a = a.add(v);
                p += loop.simdLen * loop.step;
            }
            sum += a.reduceLanes(VectorOperators.ADD);
            for (; i < loop.size; i++) {
                sum += storage.getFloat(p) - mean;
                p += loop.step;
            }
        }

        return mean + sum / count;
    }

    @Override
    protected float reduceFloatDefault(StrideLoopDescriptor<Float> loop, Storage storage) {
        float sum = initFloat;
        for (int p : loop.offsets) {
            for (int i = 0; i < loop.size; i++) {
                sum += storage.getFloat(p);
                p += loop.step;
            }
        }

        float count = loop.size * loop.offsets.length;
        float mean = sum / count;

        sum = 0;
        for (int p : loop.offsets) {
            for (int i = 0; i < loop.size; i++) {
                sum += storage.getFloat(p) - mean;
                p += loop.step;
            }
        }

        return mean + sum / count;
    }

    @Override
    protected double reduceDoubleVectorUnit(StrideLoopDescriptor<Double> loop, Storage storage) {
        double sum = initDouble;
        for (int p : loop.offsets) {
            DoubleVector a = DoubleVector.broadcast(loop.vs, initDouble);
            int i = 0;
            for (; i < loop.simdBound; i += loop.simdLen) {
                DoubleVector v = storage.getDoubleVector(loop.vs, p);
                a = a.add(v);
                p += loop.simdLen;
            }
            sum += a.reduceLanes(VectorOperators.ADD);
            for (; i < loop.size; i++) {
                sum += storage.getDouble(p);
                p++;
            }
        }

        double count = loop.size * loop.offsets.length;
        double mean = sum / count;

        sum = 0;
        for (int p : loop.offsets) {
            DoubleVector a = DoubleVector.broadcast(loop.vs, initDouble);
            int i = 0;
            for (; i < loop.simdBound; i += loop.simdLen) {
                DoubleVector v = storage.getDoubleVector(loop.vs, p);
                v = v.sub(mean);
                a = a.add(v);
                p += loop.simdLen;
            }
            sum += a.reduceLanes(VectorOperators.ADD);
            for (; i < loop.size; i++) {
                sum += storage.getDouble(p) - mean;
                p++;
            }
        }
        return mean + sum / count;
    }

    @Override
    protected double reduceDoubleVectorStep(StrideLoopDescriptor<Double> loop, Storage storage) {
        double sum = initDouble;
        for (int p : loop.offsets) {
            DoubleVector a = DoubleVector.broadcast(loop.vs, initDouble);
            int i = 0;
            for (; i < loop.simdBound; i += loop.simdLen) {
                DoubleVector v = storage.getDoubleVector(loop.vs, p, loop.simdOffsets(), 0);
                a = a.add(v);
                p += loop.simdLen * loop.step;
            }
            sum += a.reduceLanes(VectorOperators.ADD);
            for (; i < loop.size; i++) {
                sum += storage.getDouble(p);
                p += loop.step;
            }
        }

        double count = loop.size * loop.offsets.length;
        double mean = sum / count;
        sum = 0;
        for (int p : loop.offsets) {
            DoubleVector a = DoubleVector.broadcast(loop.vs, initDouble);
            int i = 0;
            for (; i < loop.simdBound; i += loop.simdLen) {
                DoubleVector v = storage.getDoubleVector(loop.vs, p, loop.simdOffsets(), 0);
                v = v.sub(mean);
                a = a.add(v);
                p += loop.simdLen * loop.step;
            }
            sum += a.reduceLanes(VectorOperators.ADD);
            for (; i < loop.size; i++) {
                sum += storage.getDouble(p) - mean;
                p += loop.step;
            }
        }

        return mean + sum / count;
    }

    @Override
    protected double reduceDoubleDefault(StrideLoopDescriptor<Double> loop, Storage storage) {
        double sum = initDouble;
        for (int p : loop.offsets) {
            for (int i = 0; i < loop.size; i++) {
                sum += storage.getDouble(p);
                p += loop.step;
            }
        }

        double count = loop.size * loop.offsets.length;
        double mean = sum / count;

        sum = 0;
        for (int p : loop.offsets) {
            for (int i = 0; i < loop.size; i++) {
                sum += storage.getDouble(p) - mean;
                p += loop.step;
            }
        }

        return mean + sum / count;
    }
}
