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

package rapaio.math.tensor.storage;

import rapaio.math.tensor.DType;
import rapaio.math.tensor.Storage;

public abstract class FloatStorage implements Storage<Float> {

    @Override
    public final DType<Float> dType() {
        return DType.FLOAT;
    }


    @Override
    public final Float get(int ptr) {
        return getFloat(ptr);
    }

    @Override
    public final void set(int ptr, Float v) {
        setFloat(ptr, v);
    }

    @Override
    public final void inc(int ptr, Float value) {
        incFloat(ptr, value);
    }

    @Override
    public final void fill(Float value) {
        fillFloat(value);
    }


    @Override
    public final byte getByte(int ptr) {
        return (byte) getFloat(ptr);
    }

    @Override
    public final void setByte(int ptr, byte value) {
        setFloat(ptr, value);
    }

    @Override
    public final void incByte(int ptr, byte value) {
        incFloat(ptr, value);
    }

    @Override
    public final void fillByte(byte value) {
        fillFloat(value);
    }


    @Override
    public final int getInt(int ptr) {
        return (int) getFloat(ptr);
    }

    @Override
    public final void setInt(int ptr, int value) {
        setFloat(ptr, value);
    }

    @Override
    public final void incInt(int ptr, int value) {
        incFloat(ptr, value);
    }

    @Override
    public final void fillInt(int value) {
        fillFloat(value);
    }


    public abstract float getFloat(int ptr);

    public abstract void setFloat(int ptr, float v);

    public abstract void incFloat(int ptr, float value);

    public abstract void fillFloat(float value);


    @Override
    public final double getDouble(int ptr) {
        return getFloat(ptr);
    }

    @Override
    public final void setDouble(int ptr, double value) {
        setFloat(ptr, (float) value);
    }

    @Override
    public final void incDouble(int ptr, double value) {
        incFloat(ptr, (float) value);
    }

    @Override
    public final void fillDouble(double value) {
        fillFloat((float) value);
    }
}
