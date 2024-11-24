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

package rapaio.narray.storage;

import rapaio.narray.DType;
import rapaio.narray.Storage;

public abstract class IntStorage extends Storage {

    @Override
    public final DType<Integer> dtype() {
        return DType.INTEGER;
    }

    @Override
    public final byte getByte(int ptr) {
        return (byte) getInt(ptr);
    }

    @Override
    public final void setByte(int ptr, byte value) {
        setInt(ptr, value);
    }

    @Override
    public final void incByte(int ptr, byte value) {
        incInt(ptr, value);
    }

    @Override
    public final void fill(byte value, int start, int len) {
        fill((int)value, start, len);
    }


    public abstract int getInt(int ptr);

    public abstract void setInt(int ptr, int v);

    public abstract void incInt(int ptr, int value);

    public abstract void fill(int value, int start, int len);


    @Override
    public final float getFloat(int ptr) {
        return getInt(ptr);
    }

    @Override
    public final void setFloat(int ptr, float value) {
        setInt(ptr, (int) value);
    }

    @Override
    public final void incFloat(int ptr, float value) {
        incInt(ptr, (int) value);
    }

    @Override
    public final void fill(float value, int start, int len) {
        fill((int) value, start, len);
    }


    @Override
    public final double getDouble(int ptr) {
        return getInt(ptr);
    }

    @Override
    public final void setDouble(int ptr, double value) {
        setInt(ptr, (int) value);
    }

    @Override
    public final void incDouble(int ptr, double value) {
        incInt(ptr, (int) value);
    }

    @Override
    public final void fill(double value, int start, int len) {
        fill((int) value, start, len);
    }
}
