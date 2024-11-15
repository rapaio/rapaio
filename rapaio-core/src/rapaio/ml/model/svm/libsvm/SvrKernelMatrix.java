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

package rapaio.ml.model.svm.libsvm;

import rapaio.math.narrays.NArray;
import rapaio.math.narrays.Shape;
import rapaio.math.narrays.NArrays;
import rapaio.ml.common.kernel.Kernel;
import rapaio.util.Reference;
import rapaio.util.collection.TArrays;

class SvrKernelMatrix extends AbstractKernelMatrix {

    private final int l;
    private final byte[] sign;
    private final int[] index;
    private int nextBuffer;
    private final NArray<Double>[] buffer;

    SvrKernelMatrix(int len, NArray<Double>[] xs, Kernel kernel, long cacheSize) {
        super(xs, kernel, new Cache(len, cacheSize * (1 << 20)), new double[2 * len]);
        this.l = len;
        buffer = new NArray[] {
                NArrays.zeros(Shape.of(2 * len)),
                NArrays.zeros(Shape.of(2 * len))
        };
        sign = new byte[2 * len];
        index = new int[2 * len];
        for (int k = 0; k < len; k++) {
            sign[k] = 1;
            sign[k + len] = -1;
            index[k] = k;
            index[k + len] = k;
            qd[k] = kernel.compute(xs[k], xs[k]);
            qd[k + len] = qd[k];
        }
        nextBuffer = 0;
    }

    void swapIndex(int i, int j) {
        TArrays.swap(sign, i, j);
        TArrays.swap(index, i, j);
        TArrays.swap(qd, i, j);
    }

    NArray<Double> getQ(int i, int len) {
        Reference<NArray<Double>> data = new Reference<>();
        if (cache.getData(index[i], data, l) < l) {
            for (int j = 0; j < l; j++) {
                data.get().set(kernel.compute(xs[index[i]], xs[j]), j);
            }
        }

        // reorder and copy
        NArray<Double> buf = buffer[nextBuffer];
        nextBuffer = 1 - nextBuffer;
        byte si = sign[i];
        for (int j = 0; j < len; j++) {
            buf.set(si * sign[j] * data.get().get(index[j]), j);
        }
        return buf;
    }

    double[] getQD() {
        return qd;
    }
}
