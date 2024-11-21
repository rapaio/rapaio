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

import rapaio.narray.storage.DoubleStorage;

public final class DoubleArrayStorage extends DoubleStorage {

    private final double[] array;

    public DoubleArrayStorage(byte[] array) {
        this.array = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            this.array[i] = array[i];
        }
    }

    public DoubleArrayStorage(int[] array) {
        this.array = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            this.array[i] = array[i];
        }
    }

    public DoubleArrayStorage(float[] array) {
        this.array = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            this.array[i] = array[i];
        }
    }

    public DoubleArrayStorage(double[] array) {
        this.array = array;
    }

    @Override
    public int size() {
        return array.length;
    }

    public double getDouble(int ptr) {
        return array[ptr];
    }

    public void setDouble(int ptr, double v) {
        array[ptr] = v;
    }

    @Override
    public void incDouble(int ptr, double value) {
        array[ptr] += value;
    }

    @Override
    public void fillDouble(double value, int start, int len) {
        Arrays.fill(array, start, start + len, value);
    }

    public double[] array() {
        return array;
    }
}
