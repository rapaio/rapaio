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

package rapaio.nn.tensors;

import rapaio.darray.DArray;
import rapaio.nn.Tensor;

public class Standardize1d extends Tensor {

    private final DArray<?> mean;
    private final DArray<?> std;

    public Standardize1d(Tensor x, int axis, int ddof, double epsilon) {
        super(x.tm(), Standardize1d.class.getSimpleName());

        DArray<?> vx = x.value();
        mean = vx.mean1d(axis);
        std = x.value().var1d(axis, ddof, mean).add_(epsilon).sqrt_();

        DArray<?> vs = vx.sub(mean.stretch(axis)).div_(std.stretch(axis));

        this.setValue(vs);

        backEdge(x, () -> {
            DArray<?> ds = this.grad;
            var dsSum = ds.sum1d(axis).stretch(axis);
            var dssSum = ds.mul(vs).sum1d(axis).stretch(axis);

            var t1 = ds.div(std.stretch(axis));
            var t2 = dsSum.add(vs.mul(dssSum)).div(x.dim(axis)).div(std.stretch(axis));

            return t1.sub_(t2);
        });
    }

    public Tensor outputMean() {
        return tm.var(mean);
    }

    public Tensor outputStd() {
        return tm.var(std);
    }
}
