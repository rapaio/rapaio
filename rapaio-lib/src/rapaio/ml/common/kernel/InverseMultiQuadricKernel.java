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

package rapaio.ml.common.kernel;

import java.io.Serial;

import rapaio.darray.DArray;
import rapaio.printer.Format;

/**
 * Inverse Multiquadric Kernel
 * <p>
 * The Inverse Multi Quadric kernel. As with the GaussianPdf kernel,
 * it results in a kernel matrix with full rank (Micchelli, 1986)
 * and thus forms a infinite dimension feature space.
 * <p>
 * k(x, y) = \frac{1}{\sqrt{\lVert x-y \rVert^2 + \theta^2}}
 * <p>
 *
 * @author <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> at 1/19/15.
 */
public class InverseMultiQuadricKernel extends AbstractKernel {

    @Serial
    private static final long serialVersionUID = -2377890141986212381L;

    private final double c;
    private final double c_square;

    public InverseMultiQuadricKernel(double c) {
        this.c = c;
        this.c_square = c * c;
    }

    @Override
    public double compute(DArray<Double> v, DArray<Double> u) {
        double dot = deltaSumSquares(u, v);
        return 1.0 / Math.sqrt(dot * dot + c_square);
    }

    @Override
    public Kernel newInstance() {
        return new InverseMultiQuadricKernel(c);
    }

    @Override
    public String name() {
        return "InverseMultiQuadric(c=" + Format.floatFlex(c) + ")";
    }
}
