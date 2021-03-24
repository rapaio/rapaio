/*
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/
 *
 *    Copyright 2013 - 2021 Aurelian Tutuianu
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

package rapaio.math.optimization;

import rapaio.data.VarDouble;
import rapaio.math.functions.RDerivative;
import rapaio.math.functions.RFunction;
import rapaio.math.linear.DVector;
import rapaio.math.optimization.linesearch.BacktrackLineSearch;
import rapaio.math.optimization.linesearch.LineSearch;
import rapaio.ml.common.ParamSet;
import rapaio.ml.common.ValueParam;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.abs;

/**
 * Implements the gradient descend optimization algorithm. Gradient descent is an optimization
 * algorithm which finds local minimum of a function using the gradient of the function.
 * Since this is a minimization algorithm, with each iteration it advances in the direction
 * of negative gradient to improve the function.
 * <p>
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 10/18/17.
 */
public class SteepestDescentMinimize extends ParamSet<SteepestDescentMinimize> implements Minimize {

    private static final long serialVersionUID = 6935528214774334177L;

    public final ValueParam<Double, SteepestDescentMinimize> tol = new ValueParam<>(this,
            1e-10, "tol", "Tolerance error admissible for accepting a convergent solution");
    public final ValueParam<Integer, SteepestDescentMinimize> maxIt = new ValueParam<>(this,
            10, "maxIt", "Maximum number of iterations");

    public final ValueParam<LineSearch, SteepestDescentMinimize> lineSearch = new ValueParam<>(this,
            BacktrackLineSearch.fromDefaults(), "lineSearch", "Line search algorithm");

    private final DVector x;
    private final RFunction f;
    private final RDerivative d1f;

    private DVector sol;

    private final List<DVector> solutions = new ArrayList<>();
    private VarDouble errors;
    private boolean converged = false;

    public SteepestDescentMinimize(DVector x, RFunction f, RDerivative d1f) {
        this.x = x;
        this.f = f;
        this.d1f = d1f;
    }

    public VarDouble getErrors() {
        return errors;
    }

    @Override
    public void compute() {
        converged = false;
        sol = x.copy();
        for (int i = 0; i < maxIt.get(); i++) {
            solutions.add(sol.copy());
            DVector delta_x = d1f.apply(sol).mult(-1);
            if (abs(delta_x.norm(2)) < tol.get()) {
                converged = true;
                break;
            }
            double t = lineSearch.get().search(f, d1f, x, delta_x);
            sol.add(delta_x.mult(t));
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("solution: ").append(sol.toString()).append("\n");
        return sb.toString();
    }

    public List<DVector> solutions() {
        return solutions;
    }

    public DVector solution() {
        return sol;
    }

    @Override
    public boolean hasConverged() {
        return converged;
    }
}