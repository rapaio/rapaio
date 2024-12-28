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

package rapaio.darray.operator.unary;

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.IntVector;
import rapaio.darray.Storage;
import rapaio.darray.iterators.StrideLoopDescriptor;
import rapaio.darray.operator.DArrayUnaryOp;


// This code is generated automatically

public class UnaryOpAbs extends DArrayUnaryOp {

    public UnaryOpAbs() {
        super(false);
    }

    @Override
    protected void applyUnitByte(StrideLoopDescriptor<Byte> loop, Storage s) {
        for (int p : loop.offsets) {
            int i = 0;
            for (; i < loop.simdBound; i += loop.simdLen) {
                ByteVector a = s.getByteVector(p);
                a = a.abs();
                s.setByteVector(a, p);
                p += loop.simdLen;
            }
            for (; i < loop.bound; i++) {
                byte a = s.getByte(p);
                a = (byte) Math.abs(a);
                s.setByte(p, a);
                p++;
            }
        }
    }

    @Override
    protected void applyStepByte(StrideLoopDescriptor<Byte> loop, Storage s) {
        for (int p : loop.offsets) {
            int i = 0;
            for (; i < loop.simdBound; i += loop.simdLen) {
                var a = s.getByteVector(p, loop.simdOffsets(), 0);
                a = a.abs();
                s.setByteVector(a, p, loop.simdOffsets(), 0);
                p += loop.step * loop.simdLen;
            }
            for (; i < loop.bound; i++) {
                byte a = s.getByte(p);
                a = (byte) Math.abs(a);
                s.setByte(p, a);
                p += loop.step;
            }
        }
    }

    @Override
    protected void applyGenericByte(StrideLoopDescriptor<Byte> loop, Storage s) {
        for (int p : loop.offsets) {
            for (int i = 0; i < loop.bound; i++) {
                byte a = s.getByte(p);
                a = (byte) Math.abs(a);
                s.setByte(p, a);
                p += loop.step;
            }
        }
    }

    @Override
    protected void applyUnitInt(StrideLoopDescriptor<Integer> loop, Storage s) {
        for (int p : loop.offsets) {
            int i = 0;
            for (; i < loop.simdBound; i += loop.simdLen) {
                IntVector a = s.getIntVector(p);
                a = a.abs();
                s.setIntVector(a, p);
                p += loop.simdLen;
            }
            for (; i < loop.bound; i++) {
                int a = s.getInt(p);
                a = Math.abs(a);
                s.setInt(p, a);
                p++;
            }
        }
    }

    @Override
    protected void applyStepInt(StrideLoopDescriptor<Integer> loop, Storage s) {
        for (int p : loop.offsets) {
            int i = 0;
            for (; i < loop.simdBound; i += loop.simdLen) {
                IntVector a = s.getIntVector(p, loop.simdOffsets(), 0);
                a = a.abs();
                s.setIntVector(a, p, loop.simdOffsets(), 0);
                p += loop.step * loop.simdLen;
            }
            for (; i < loop.bound; i++) {
                int a = s.getInt(p);
                a = Math.abs(a);
                s.setInt(p, a);
                p += loop.step;
            }
        }
    }

    @Override
    protected void applyGenericInt(StrideLoopDescriptor<Integer> loop, Storage s) {
        for (int p : loop.offsets) {
            for (int i = 0; i < loop.bound; i++) {
                int a = s.getInt(p);
                a = Math.abs(a);
                s.setInt(p, a);
                p++;
            }
        }
    }

    @Override
    protected void applyUnitFloat(StrideLoopDescriptor<Float> loop, Storage s) {
        for (int p : loop.offsets) {
            int i = 0;
            for (; i < loop.simdBound; i += loop.simdLen) {
                FloatVector a = s.getFloatVector(p);
                a = a.abs();
                s.setFloatVector(a, p);
                p += loop.simdLen;
            }
            for (; i < loop.bound; i++) {
                float a = s.getFloat(p);
                a = Math.abs(a);
                s.setFloat(p, a);
                p++;
            }
        }
    }

    @Override
    protected void applyStepFloat(StrideLoopDescriptor<Float> loop, Storage s) {
        for (int p : loop.offsets) {
            int i = 0;
            for (; i < loop.simdBound; i += loop.simdLen) {
                FloatVector a = s.getFloatVector(p, loop.simdOffsets(), 0);
                a = a.abs();
                s.setFloatVector(a, p, loop.simdOffsets(), 0);
                p += loop.step * loop.simdLen;
            }
            for (; i < loop.bound; i++) {
                float a = s.getFloat(p);
                a = Math.abs(a);
                s.setFloat(p, a);
                p += loop.step;
            }
        }
    }

    @Override
    protected void applyGenericFloat(StrideLoopDescriptor<Float> loop, Storage s) {
        for (int p : loop.offsets) {
            for (int i = 0; i < loop.bound; i++) {
                float a = s.getFloat(p);
                a = Math.abs(a);
                s.setFloat(p, a);
                p += loop.step;
            }
        }
    }

    @Override
    protected void applyUnitDouble(StrideLoopDescriptor<Double> loop, Storage s) {
        for (int p : loop.offsets) {
            int i = 0;
            for (; i < loop.simdBound; i += loop.simdLen) {
                DoubleVector a = s.getDoubleVector(p);
                a = a.abs();
                s.setDoubleVector(a, p);
                p += loop.simdLen;
            }
            for (; i < loop.bound; i++) {
                double a = s.getDouble(p);
                a = Math.abs(a);
                s.setDouble(p, a);
                p++;
            }
        }
    }

    @Override
    protected void applyStepDouble(StrideLoopDescriptor<Double> loop, Storage s) {
        for (int p : loop.offsets) {
            int i = 0;
            for (; i < loop.simdBound; i += loop.simdLen) {
                DoubleVector a = s.getDoubleVector(p, loop.simdOffsets(), 0);
                a = a.abs();
                s.setDoubleVector(a, p, loop.simdOffsets(), 0);
                p += loop.step * loop.simdLen;
            }
            for (; i < loop.bound; i++) {
                double a = s.getDouble(p);
                a = Math.abs(a);
                s.setDouble(p, a);
                p += loop.step;
            }
        }
    }

    @Override
    protected void applyGenericDouble(StrideLoopDescriptor<Double> loop, Storage s) {
        for (int p : loop.offsets) {
            for (int i = 0; i < loop.bound; i++) {
                double a = s.getDouble(p);
                a = Math.abs(a);
                s.setDouble(p, a);
                p += loop.step;
            }
        }
    }
}