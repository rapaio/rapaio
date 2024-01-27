/*
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/
 *
 *    Copyright 2013 Aurelian Tutuianu
 *    Copyright 2014 Aurelian Tutuianu
 *    Copyright 2015 Aurelian Tutuianu
 *    Copyright 2016 Aurelian Tutuianu
 *    Copyright 2017 Aurelian Tutuianu
 *    Copyright 2018 Aurelian Tutuianu
 *    Copyright 2019 Aurelian Tutuianu
 *    Copyright 2020 Aurelian Tutuianu
 *    Copyright 2021 Aurelian Tutuianu
 *    Copyright 2022 Aurelian Tutuianu
 *    Copyright 2023 Aurelian Tutuianu
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import rapaio.core.distributions.StudentT;
import rapaio.core.stat.Quantiles;
import rapaio.data.Frame;
import rapaio.data.Var;
import rapaio.data.VarDouble;
import rapaio.data.preprocessing.AddIntercept;
import rapaio.math.MathTools;
import rapaio.math.tensor.Shape;
import rapaio.math.tensor.Tensor;
import rapaio.math.tensor.TensorManager;
import rapaio.ml.model.RegressionResult;
import rapaio.ml.model.linear.impl.BaseLinearRegressionModel;
import rapaio.printer.Format;
import rapaio.printer.Printer;
import rapaio.printer.TextTable;
import rapaio.printer.opt.POpt;

/**
 * @author <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 2/1/18.
 */
public class LinearRegressionResult extends RegressionResult {

    private static final TensorManager.OfType<Double> tmd = TensorManager.base().ofDouble();
    protected final BaseLinearRegressionModel<?> lm;
    protected Tensor<Double> beta_hat;
    protected Tensor<Double> beta_std_error;
    protected Tensor<Double> beta_t_value;
    protected Tensor<Double> beta_p_value;
    protected String[][] beta_significance;

    public LinearRegressionResult(BaseLinearRegressionModel<?> model, Frame df, boolean withResiduals, double[] quantiles) {
        super(model, df, withResiduals, quantiles);
        this.lm = model;
    }

    public Tensor<Double> getBetaHat() {
        return beta_hat;
    }

    public Tensor<Double> getBetaStdError() {
        return beta_std_error;
    }

    public Tensor<Double> getBetaTValue() {
        return beta_t_value;
    }

    public Tensor<Double> getBetaPValue() {
        return beta_p_value;
    }

    public String[][] getBetaSignificance() {
        return beta_significance;
    }

    @Override
    public void buildComplete() {
        super.buildComplete();

        // compute artifacts

        String[] inputs = lm.inputNames();
        String[] targets = lm.targetNames();

        beta_hat = lm.getAllCoefficients().copy();
        beta_std_error = tmd.zeros(Shape.of(inputs.length, targets.length));
        beta_t_value = tmd.zeros(Shape.of(inputs.length, targets.length));
        beta_p_value = tmd.zeros(Shape.of(inputs.length, targets.length));
        beta_significance = new String[inputs.length][targets.length];

        if (withResiduals) {

            for (int i = 0; i < lm.targetNames().length; i++) {

                String targetName = lm.targetName(i);
                VarDouble res = residuals.get(targetName);

                int degrees = res.size() - model.inputNames().length;
                double var = rss.get(targetName) / degrees;
                var coeff = beta_hat.takesq(1, i);

                Frame features = df;
                Set<String> availableFeatures = new HashSet<>(Arrays.asList(df.varNames()));
                for (String inputName : model.inputNames()) {
                    if (AddIntercept.INTERCEPT.equals(inputName) && !availableFeatures.contains(inputName)) {
                        features = df.bindVars(VarDouble.fill(df.rowCount(), 1).name(AddIntercept.INTERCEPT)).copy();
                    }
                }
                Tensor<Double> X = features.mapVars(model.inputNames()).dtNew();
                Tensor<Double> m_beta_hat = X.t().mm(X).qr().inv();

                for (int j = 0; j < model.inputNames().length; j++) {
                    beta_std_error.setDouble(Math.sqrt(m_beta_hat.get(j, j) * var), j, i);
                    beta_t_value.setDouble(coeff.get(j) / beta_std_error.get(j, i), j, i);
                    double pValue = degrees < 1 ? Double.NaN : StudentT.of(degrees).cdf(-Math.abs(beta_t_value.get(j, i))) * 2;
                    beta_p_value.setDouble(pValue, j, i);
                    String signif = " ";
                    if (pValue <= 0.1)
                        signif = ".";
                    if (pValue <= 0.05)
                        signif = "*";
                    if (pValue <= 0.01)
                        signif = "**";
                    if (pValue <= 0.001)
                        signif = "***";
                    beta_significance[j][i] = signif;
                }
            }
        }
    }

    @Override
    public String toSummary(Printer printer, POpt<?>... options) {
        StringBuilder sb = new StringBuilder();
        sb.append(lm.headerSummary());
        sb.append("\n");

        for (int i = 0; i < lm.targetNames().length; i++) {
            String targetName = lm.targetName(i);
            sb.append("Target <<< ").append(targetName).append(" >>>\n\n");

            if (!withResiduals) {
                sb.append("> Coefficients: \n");
                var coeff = lm.getCoefficients(i);

                TextTable tt = TextTable.empty(coeff.size() + 1, 2, 1, 0);
                tt.textCenter(0, 0, "Name");
                tt.textCenter(0, 1, "Estimate");
                for (int j = 0; j < coeff.size(); j++) {
                    tt.textLeft(j + 1, 0, lm.inputName(j));
                    tt.floatFlex(j + 1, 1, coeff.get(j));
                }
                sb.append(tt.getRawText());
            } else {
                VarDouble res = residuals.get(targetName);

                int degrees = res.size() - model.inputNames().length;
                double var = rss.get(targetName) / degrees;
                double rs = rsquare.get(targetName);
                var coeff = lm.getCoefficients(i);
                double rsa = (rs * (res.size() - 1) - coeff.size() + 1) / degrees;

                int fdegree1 = model.inputNames().length - 1;
                double fvalue = (ess.get(targetName) * degrees) / (rss.get(targetName) * (fdegree1));
                double fpvalue = MathTools.fdist(fvalue, fdegree1, degrees);

                sb.append("> Residuals: \n");
                sb.append(getHorizontalSummary5(res, printer, options));
                sb.append("\n");

                sb.append("> Coefficients: \n");

                TextTable tt = TextTable.empty(coeff.size() + 1, 6, 1, 1);

                tt.textRight(0, 0, "Name");
                tt.textRight(0, 1, "Estimate");
                tt.textRight(0, 2, "Std. error");
                tt.textRight(0, 3, "t value");
                tt.textRight(0, 4, "P(>|t|)");
                tt.textRight(0, 5, "");
                for (int j = 0; j < coeff.size(); j++) {
                    tt.textLeft(j + 1, 0, model.inputName(j));
                    tt.floatMedium(j + 1, 1, coeff.get(j));
                    tt.floatMedium(j + 1, 2, beta_std_error.get(j, i));
                    tt.floatMedium(j + 1, 3, beta_t_value.get(j, i));
                    tt.pValue(j + 1, 4, beta_p_value.get(j, i));
                    tt.textLeft(j + 1, 5, beta_significance[j][i]);
                }
                sb.append(tt.getRawText());
                sb.append("--------\n");
                sb.append("Signif. codes:  0 '***' 0.001 '**' 0.01 '*' 0.05 '.' 0.1 ' ' 1\n\n");


                sb.append(String.format("Residual standard error: %s on %d degrees of freedom\n",
                        Format.floatFlex(Math.sqrt(var)), degrees));
                sb.append(String.format("Multiple R-squared:  %s, Adjusted R-squared:  %s\n",
                        Format.floatFlex(rs), Format.floatFlex(rsa)));
                sb.append(String.format("F-statistic: %s on %d and %d DF,  p-value: %s\n",
                        Format.floatFlexShort(fvalue), fdegree1, degrees, Format.pValue(fpvalue)));
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private String getHorizontalSummary5(Var var, Printer printer, POpt<?>... options) {
        TextTable tt1 = TextTable.empty(2, 5, 1, 0);

        String[] headers1 = new String[]{"Min", "1Q", "Median", "3Q", "Max"};
        double[] values1 = Quantiles.of(var, 0, 0.25, 0.5, 0.75, 1).values();
        for (int i = 0; i < 5; i++) {
            tt1.textRight(0, i, headers1[i]);
            tt1.floatFlex(1, i, values1[i]);
        }
        return tt1.getDynamicText(printer, options);
    }
}
