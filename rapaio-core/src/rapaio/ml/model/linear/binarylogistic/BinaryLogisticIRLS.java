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

package rapaio.ml.model.linear.binarylogistic;

import static java.lang.StrictMath.abs;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import rapaio.core.param.ParamSet;
import rapaio.core.param.ValueParam;
import rapaio.math.MathTools;
import rapaio.math.narray.NArray;
import rapaio.math.narray.Shape;
import rapaio.math.narray.NArrays;
import rapaio.math.narray.matrix.CholeskyDecomposition;

/**
 * @author <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 3/21/20.
 */
public class BinaryLogisticIRLS extends ParamSet<BinaryLogisticIRLS> {

    @Serial
    private static final long serialVersionUID = -1351523770434554322L;

    /**
     * Threshold value used to assess convergence of a solution
     */
    public final ValueParam<Double, BinaryLogisticIRLS> eps = new ValueParam<>(this, 1e-20, "eps");

    /**
     * Maximum number of iterations
     */
    public final ValueParam<Integer, BinaryLogisticIRLS> maxIter = new ValueParam<>(this, 10, "maxIter");

    /**
     * L2 regularization penalty
     */
    public final ValueParam<Double, BinaryLogisticIRLS> lambdap = new ValueParam<>(this, 0.0, "lambda");

    public final ValueParam<NArray<Double>, BinaryLogisticIRLS> xp = new ValueParam<>(this, null, "x");

    public final ValueParam<NArray<Double>, BinaryLogisticIRLS> yp = new ValueParam<>(this, null, "y");

    /**
     * Initial weights
     */
    public final ValueParam<NArray<Double>, BinaryLogisticIRLS> w0 = new ValueParam<>(this, null, "w0");

    public record Result(List<Double> nlls, List<NArray<Double>> ws, boolean converged) {

        public NArray<Double> w() {
            if (!ws.isEmpty()) {
                return ws.getLast();
            }
            return NArrays.scalar(Double.NaN);
        }

        public double nll() {
            if (nlls.size() > 1) {
                return nlls.getLast();
            }
            return Double.NaN;
        }
    }

    public BinaryLogisticIRLS.Result fit() {

        NArray<Double> x = xp.get();
        NArray<Double> y = yp.get();
        NArray<Double> ny = NArrays.full(Shape.of(y.size()), 1.).sub_(y);
        NArray<Double> w = w0.get();
        double lambda = lambdap.get();
        NArray<Double> p = x.mv(w).apply_(MathTools::logistic);
        NArray<Double> np = p.apply(v -> 1 - v);

        int it = 0;
        // current solution
        ArrayList<NArray<Double>> ws = new ArrayList<>();
        ws.add(w);
        List<Double> nlls = new ArrayList<>();
        nlls.add(negativeLogLikelihood(y, ny, w, lambda, p, np));

        while (it++ < maxIter.get()) {

            NArray<Double> wnew = iterate(w, x, y, lambda, p, np);

            p = x.mv(wnew).apply(MathTools::logistic);
            np = p.apply(v -> 1 - v);
            double nll = negativeLogLikelihood(y, ny, wnew, lambda, p, np);

            double nll_delta = nll - nlls.getLast();
            if (it > 1 && (abs(nll_delta / nll) <= eps.get() /*|| nll_delta > 0*/)) {
                return new BinaryLogisticIRLS.Result(nlls, ws, true);
            }
            ws.add(wnew);
            nlls.add(nll);
            w = wnew;
        }
        return new BinaryLogisticIRLS.Result(nlls, ws, false);
    }

    private double negativeLogLikelihood(
            NArray<Double> y, NArray<Double> ny, NArray<Double> w, double lambda, NArray<Double> p, NArray<Double> np) {
        NArray<Double> logp = p.clamp(1e-6, Double.NaN).log();
        NArray<Double> lognp = np.clamp(1e-6, Double.NaN).log();

        return -logp.inner(y) - lognp.inner(ny) + lambda * w.norm(2.) / 2;
    }

    private NArray<Double> iterate(
            NArray<Double> vw, NArray<Double> mx, NArray<Double> vy, double lambda, NArray<Double> vp, NArray<Double> vnp) {

        // p(1-p) diag from p diag
        NArray<Double> pvar = vp.mul(vnp).clamp(1e-6, Double.NaN);

        // H = X^t * I{p(1-p)} * X + I_lambda
        NArray<Double> xta = mx.t().mul(pvar.stretch(0).expand(0, mx.t().dim(0)));
        NArray<Double> h = xta.mm(mx);
        if (lambda > 0) {
            for (int i = 0; i < h.dim(0); i++) {
                h.incDouble(lambda, i, i);
            }
        }

        // z = Xw + I{p(1-p)}^{-1} (y-p)
        NArray<Double> z = mx.mv(vw).add(vy.sub(vp).div_(pvar));
        NArray<Double> right = xta.mv(z);

        // solve IRLS
        CholeskyDecomposition<Double> chol = h.cholesky();
        if (chol.isSPD()) {
            return chol.solve(right);
        } else {
            return h.qr().solve(right);
        }
    }
}
