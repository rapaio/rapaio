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

package rapaio.ml.model.linear;

import java.io.Serial;

import rapaio.darray.DArray;
import rapaio.data.Frame;
import rapaio.data.Var;
import rapaio.data.transform.AddIntercept;
import rapaio.ml.model.linear.impl.BaseLinearRegressionModel;

/**
 * @author <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 8/21/20.
 */
public class WeightedLinearRegression extends BaseLinearRegressionModel<WeightedLinearRegression> {

    /**
     * Builds a linear regression model with intercept.
     *
     * @return new instance of linear regression model
     */
    public static WeightedLinearRegression newModel() {
        return new WeightedLinearRegression();
    }

    @Serial
    private static final long serialVersionUID = 8595413796946622895L;

    @Override
    public WeightedLinearRegression newInstance() {
        return new WeightedLinearRegression().copyParameterValues(this);
    }

    @Override
    public String name() {
        return "WeightedLinearRegression";
    }

    @Override
    protected FitSetup prepareFit(Frame df, Var weights, String... targetVarNames) {
        // add intercept variable
        Frame transformed = intercept.get() ? AddIntercept.transform().fitApply(df) : df;

        // collect standard information
        return super.prepareFit(transformed, weights, targetVarNames);
    }

    @Override
    protected boolean coreFit(Frame df, Var weights) {
        var w = weights.darray().apply_(Math::sqrt);
        DArray<Double> X = df.mapVars(inputNames()).darray().mul(w.stretch(1));
        DArray<Double> Y = df.mapVars(targetNames()).darray().mul(w.stretch(1));
        beta = X.qr().solve(Y);
        return true;
    }
}
