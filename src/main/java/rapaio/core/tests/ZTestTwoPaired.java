/*
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/
 *
 *    Copyright 2013 Aurelian Tutuianu
 *    Copyright 2014 Aurelian Tutuianu
 *    Copyright 2015 Aurelian Tutuianu
 *    Copyright 2016 Aurelian Tutuianu
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

package rapaio.core.tests;

import rapaio.core.HTTools;
import rapaio.core.distributions.Normal;
import rapaio.data.Numeric;
import rapaio.data.Var;
import rapaio.printer.Printable;

import static rapaio.core.CoreTools.mean;
import static rapaio.sys.WS.formatFlex;

/**
 * Two paired sample z test for testing mean of differences
 *
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 5/27/16.
 */
public class ZTestTwoPaired implements Printable {
    // parameters

    public final Var x;
    public final Var y;
    public final double mu;
    public final double sd;
    public final double sl;
    public final HTTools.Alternative alt;

    // computed

    public final Var xComplete;
    public final Var yComplete;

    public final double sampleMean;
    public final double zScore;
    public final double pValue;
    public final double ciLow;
    public final double ciHigh;

    public ZTestTwoPaired(Var x, Var y, double mu, double sd, double sl, HTTools.Alternative alt) {
        this.x = x;
        this.y = y;
        this.mu = mu;
        this.sd = sd;
        this.sl = sl;
        this.alt = alt;

        xComplete = x.stream().complete().toMappedVar();
        yComplete = y.stream().complete().toMappedVar();

        if (xComplete.rowCount() < 1 || yComplete.rowCount() < 1) {
            // nothing to do
            sampleMean = Double.NaN;

            zScore = Double.NaN;
            pValue = Double.NaN;
            ciLow = Double.NaN;
            ciHigh = Double.NaN;

            return;
        }

        Var complete = Numeric.empty();
        for (int i = 0; i < Math.min(x.rowCount(), y.rowCount()); i++) {
            if (!(x.missing(i) || y.missing(i)))
                complete.addValue(x.value(i) - y.value(i));
        }
        sampleMean = mean(complete).value();

        double sv = sd / Math.sqrt(complete.rowCount());

        zScore = (sampleMean - mu) / sv;

        Normal normal = new Normal(0, 1);
        switch (alt) {
            case GREATER_THAN:
                pValue = 1 - normal.cdf(zScore);
                break;
            case LESS_THAN:
                pValue = normal.cdf(zScore);
                break;
            default:
                pValue = normal.cdf(-Math.abs(zScore)) * 2;
        }

        ciLow = new Normal(sampleMean, sv).quantile(sl / 2);
        ciHigh = new Normal(sampleMean, sv).quantile(1 - sl / 2);
    }

    @Override
    public String summary() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("> HTTools.zTestTwoPaired\n");
        sb.append("\n");
        sb.append(" Two Paired z-test\n");
        sb.append("\n");
        sb.append("x complete rows: ").append(xComplete.rowCount()).append("/").append(x.rowCount()).append("\n");
        sb.append("y complete rows: ").append(yComplete.rowCount()).append("/").append(y.rowCount()).append("\n");
        sb.append("mean: ").append(formatFlex(mu)).append("\n");
        sb.append("x sd: ").append(formatFlex(sd)).append("\n");
        sb.append("significance level: ").append(formatFlex(sl)).append("\n");
        sb.append("alternative hypothesis: ").append(alt == HTTools.Alternative.TWO_TAILS ? "two tails " : "one tail ").append(alt.pCondition()).append("\n");
        sb.append("\n");
        sb.append("sample mean: ").append(formatFlex(sampleMean)).append("\n");
        sb.append("z score: ").append(formatFlex(zScore)).append("\n");
        sb.append("p-value: ").append(pValue).append("\n");

        sb.append("conf int: [").append(formatFlex(ciLow)).append(",").append(formatFlex(ciHigh)).append("]\n");

        return sb.toString();
    }

}