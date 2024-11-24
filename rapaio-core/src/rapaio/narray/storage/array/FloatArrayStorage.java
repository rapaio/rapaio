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

package rapaio.narray.storage.array;

import java.util.Arrays;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorSpecies;
import rapaio.narray.storage.FloatStorage;

public final class FloatArrayStorage extends FloatStorage {

    private final float[] array;

    public FloatArrayStorage(byte[] array) {
        this.array = new float[array.length];
        for (int i = 0; i < array.length; i++) {
            this.array[i] = array[i];
        }
    }

    public FloatArrayStorage(int[] array) {
        this.array = new float[array.length];
        for (int i = 0; i < array.length; i++) {
            this.array[i] = array[i];
        }
    }

    public FloatArrayStorage(float[] array) {
        this.array = array;
    }

    public FloatArrayStorage(double[] array) {
        this.array = new float[array.length];
        for (int i = 0; i < array.length; i++) {
            this.array[i] = (float) array[i];
        }
    }

    @Override
    public int size() {
        return array.length;
    }

    @Override
    public boolean supportVectorization() {
        return true;
    }

    public float getFloat(int ptr) {
        return array[ptr];
    }

    public void setFloat(int ptr, float v) {
        array[ptr] = v;
    }

    @Override
    public void incFloat(int ptr, float value) {
        array[ptr] += value;
    }

    @Override
    public void fill(float value, int start, int len) {
        Arrays.fill(array, start, start + len, value);
    }

    @Override
    public FloatVector getFloatVector(VectorSpecies<Float> vs, int offset) {
        return FloatVector.fromArray(vs, array, offset);
    }

    @Override
    public FloatVector getFloatVector(VectorSpecies<Float> vs, int offset, int[] idx, int idxOffset) {
        return FloatVector.fromArray(vs, array, offset, idx, idxOffset);
    }

    @Override
    public void setFloatVector(FloatVector value, int offset) {
        value.intoArray(array, offset);
    }

    @Override
    public void setFloatVector(FloatVector value, int offset, int[] idx, int idxOffset) {
        value.intoArray(array, offset, idx, idxOffset);
    }

    public float[] array() {
        return array;
    }
}
