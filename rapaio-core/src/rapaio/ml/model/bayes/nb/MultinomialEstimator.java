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

package rapaio.ml.model.bayes.nb;

import static java.lang.Math.exp;
import static java.lang.Math.log;

import static rapaio.math.MathTools.lnGamma;

import java.io.Serial;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import rapaio.core.tools.DensityVector;
import rapaio.data.Frame;
import rapaio.data.Var;
import rapaio.data.VarRange;
import rapaio.narray.NArray;
import rapaio.narray.NArrays;
import rapaio.narray.Shape;
import rapaio.printer.Format;

/**
 * Naive Bayes Multinomial estimator allows one to model two or more variables
 * being generated by a Multinomial distribution. The values from each of those
 * variables must contain semi-positive integer values, since this is the domain
 * of the multinomial distribution.
 * <p>
 * It is not possible to model this using a single variable. The particular case of
 * Binomial distribution requires a variable for the positive successes and an additional
 * variable for the total number of events. The same setup can be described by having a
 * variable for the positive number of outcomes and an additional variable for the
 * negative number of outcomes, which leads to the requirement of having at least two
 * variables to model the distribution. Since the later setup can be used immediately
 * in the extended case of Multinomial distribution, we prefer this setup to cover both
 * situations.
 * <p>
 *
 * @author <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 3/13/20.
 */
public class MultinomialEstimator extends AbstractEstimator {

    public static MultinomialEstimator fromNames(String... testNames) {
        return new MultinomialEstimator(DEFAULT_LAPLACE, Arrays.asList(testNames));
    }

    public static MultinomialEstimator fromNames(List<String> testNames) {
        return new MultinomialEstimator(DEFAULT_LAPLACE, testNames);
    }

    public static MultinomialEstimator fromRange(Frame df, VarRange varRange) {
        return new MultinomialEstimator(DEFAULT_LAPLACE, varRange.parseVarNames(df));
    }

    public static MultinomialEstimator fromNames(double laplaceSmoother, String... testNames) {
        return new MultinomialEstimator(laplaceSmoother, Arrays.asList(testNames));
    }

    public static MultinomialEstimator fromNames(double laplaceSmoother, List<String> testNames) {
        return new MultinomialEstimator(laplaceSmoother, testNames);
    }

    public static MultinomialEstimator fromRange(double laplaceSmoother, Frame df, VarRange varRange) {
        return new MultinomialEstimator(laplaceSmoother, varRange.parseVarNames(df));
    }

    @Serial
    private static final long serialVersionUID = -3469344351162271991L;
    public static final double eps = 1e-300; // minimum value to avoid NaN
    public static final double DEFAULT_LAPLACE = 1e-5; // small empirical value

    private final double laplaceSmoother;
    private List<String> targetLevels;
    private Map<String, NArray<Double>> densityMap;

    private MultinomialEstimator(double laplaceSmoother, List<String> testNames) {
        super(testNames);
        this.laplaceSmoother = laplaceSmoother;
        if (testNames == null || testNames.size() < 2) {
            throw new IllegalArgumentException("Multinomial estimator must apply to at least 2 variables.");
        }
    }

    @Override
    public Estimator newInstance() {
        return new MultinomialEstimator(getLaplaceSmoother(), getTestNames());
    }

    public double getLaplaceSmoother() {
        return laplaceSmoother;
    }

    @Override
    public String name() {
        return "Multinomial{laplaceSmoother=" + Format.floatFlexLong(laplaceSmoother) + ", tests=[" + String.join(",", getTestNames())
                + "]}";
    }

    @Override
    public String fittedName() {
        if (targetLevels == null) {
            return name();
        }
        return "Multinomial{laplaceSmoother=%s, tests=[%s],distributions=[%s]}".formatted(
                Format.floatFlexLong(laplaceSmoother),
                String.join(",", getTestNames()),
                targetLevels.stream()
                        .map(targetLevel -> targetLevel + ":[" + densityMap.get(targetLevel)
                                .stream().map(Format::floatFlexLong).collect(Collectors.joining(",")) + "]")
                        .collect(Collectors.joining(","))
        );
    }

    @Override
    public boolean fit(Frame df, Var weights, String targetName) {
        validateFit(df, weights, targetName);

        targetLevels = df.levels(targetName).stream().skip(1).collect(Collectors.toList());

        Map<String, DensityVector<String>> countDensities = new HashMap<>();
        for (int i = 0; i < df.rowCount(); i++) {
            // skip missing target
            if (df.isMissing(i, targetName)) {
                continue;
            }
            // update each test count conditioned on target level
            String targetLevel = df.getLabel(i, targetName);
            var density = countDensities.computeIfAbsent(targetLevel, level -> {
                DensityVector<String> d = DensityVector.emptyByLabels(true, getTestNames());
                for (String testName : d.index().getValues()) {
                    d.increment(testName, laplaceSmoother);
                }
                return d;
            });
            for (final String testName : getTestNames()) {
                density.increment(testName, df.getDouble(i, testName));
            }
        }

        densityMap = new HashMap<>();
        countDensities.forEach((level, density) -> {
            // add normalized densities to prediction map
            densityMap.put(level, NArrays.stride(density.normalize().streamValues().toArray()));
        });

        return false;
    }

    private void validateFit(Frame df, Var weights, String targetName) {
        Set<String> dfVarNames = new HashSet<>(Arrays.asList(df.varNames()));
        for (final var testName : getTestNames()) {
            if (testName.equals(targetName)) {
                throw new IllegalArgumentException("Target variable cannot be a test variable.");
            }
            if (!dfVarNames.contains(testName)) {
                throw new IllegalArgumentException("Frame does not contain some test variables, for example: " + testName + ".");
            }
            if (df.rvar(testName).stream().mapToDouble().anyMatch(value -> value != Math.rint(value) || value < 0)) {
                throw new IllegalArgumentException("Test variable name: " + testName + " must contain only semi-positive integer values.");
            }
        }
    }

    @Override
    public double predict(Frame df, int row, String targetLevel) {
        // if not fitted
        if (targetLevel == null || !densityMap.containsKey(targetLevel)) {
            return Double.NaN;
        }

        // build count vector
        List<String> testNames = getTestNames();
        NArray<Double> x = NArrays.zeros(Shape.of(testNames.size()));
        for (int i = 0; i < testNames.size(); i++) {
            x.setDouble(df.getDouble(row, testNames.get(i)), i);
        }

        // compute multinomial log pmf using gamma
        double n = x.sum();
        double result = lnGamma(n + 1);
        for (int i = 0; i < x.size(); i++) {
            result += x.getDouble(i) * log(densityMap.get(targetLevel).getDouble(i));
            result -= lnGamma(x.getDouble(i) + 1);
        }

        // and exponentiate it before return
        result = exp(result);
        if (!Double.isFinite(result)) {
            return eps;
        }
        return Math.max(result, eps);
    }
}
