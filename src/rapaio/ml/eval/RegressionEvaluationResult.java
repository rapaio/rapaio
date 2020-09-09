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

package rapaio.ml.eval;

import lombok.Getter;
import rapaio.core.stat.Mean;
import rapaio.core.stat.Variance;
import rapaio.data.Frame;
import rapaio.data.Group;
import rapaio.data.SolidFrame;
import rapaio.data.Var;
import rapaio.data.VarDouble;
import rapaio.data.VarInt;
import rapaio.data.VarNominal;
import rapaio.data.filter.FRefSort;
import rapaio.data.group.GroupFun;
import rapaio.ml.eval.metric.RegressionMetric;
import rapaio.ml.eval.split.Split;
import rapaio.ml.regression.RegressionResult;
import rapaio.printer.Printable;
import rapaio.printer.Printer;
import rapaio.printer.opt.POption;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Container for the results of a cross validation evaluation on regression
 * models.
 * <p>
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 8/13/19.
 */
public class RegressionEvaluationResult implements Printable {

    private static final String FIELD_ROUND = "round";
    private static final String FIELD_FOLD = "fold";

    @Getter
    private final RegressionEvaluation eval;
    @Getter
    private Frame trainScores;
    @Getter
    private Frame testScores;

    private final ReentrantLock scoresLock = new ReentrantLock();

    public RegressionEvaluationResult(RegressionEvaluation eval) {
        this.eval = eval;

        List<Var> vars = new ArrayList<>();
        vars.add(VarInt.empty().withName(FIELD_ROUND));
        vars.add(VarInt.empty().withName(FIELD_FOLD));
        for (RegressionMetric metric : eval.getMetrics()) {
            vars.add(VarDouble.empty().withName(metric.getName()));
        }
        trainScores = SolidFrame.byVars(vars).copy();
        testScores = trainScores.copy();
    }

    public double getMeanTrainScore(String metric) {
        return Mean.of(trainScores.rvar(metric)).value();
    }

    public double getMeanTestScore(String metric) {
        return Mean.of(testScores.rvar(metric)).value();
    }

    void appendRun(Split split, RegressionResult trainResult, RegressionResult testResult) {

        scoresLock.lock();
        try {
            int lastRow = trainScores.rowCount();

            trainScores.addRows(1);
            trainScores.setInt(lastRow, FIELD_ROUND, split.getRound());
            trainScores.setInt(lastRow, FIELD_FOLD, split.getFold());
            for (RegressionMetric metric : eval.getMetrics()) {
                trainScores.setDouble(lastRow, metric.getName(),
                        metric.compute(split.getTrainDf().rvar(eval.getTargetName()), trainResult).getValue());
            }
            trainScores = trainScores.fapply(FRefSort.by(
                    trainScores.rvar(FIELD_ROUND).refComparator(),
                    trainScores.rvar(FIELD_FOLD).refComparator()
            )).copy();

            testScores.addRows(1);
            testScores.setInt(lastRow, FIELD_ROUND, split.getRound());
            testScores.setInt(lastRow, FIELD_FOLD, split.getFold());
            for (RegressionMetric metric : eval.getMetrics()) {
                testScores.setDouble(lastRow, metric.getName(),
                        metric.compute(split.getTrainDf().rvar(eval.getTargetName()), trainResult).getValue());
            }

            testScores = testScores.fapply(FRefSort.by(
                    testScores.rvar(FIELD_ROUND).refComparator(),
                    testScores.rvar(FIELD_FOLD).refComparator()
            )).copy();
        } finally {
            scoresLock.unlock();
        }
    }

    private String toContentName(Printer printer, POption<?>... options) {
        StringBuilder sb = new StringBuilder();
        sb.append("Model:\n");
        sb.append(eval.getModel().fullName()).append("\n");
        return sb.toString();
    }

    private String toContentCVScore(Printer printer, POption<?>... options) {
        StringBuilder sb = new StringBuilder();
        sb.append("CV score\n");
        sb.append("=============\n");
        Var metricVar = VarNominal.empty().withName("metric");
        Var meanVar = VarDouble.empty().withName("mean");
        Var stdVar = VarDouble.empty().withName("std");
        Frame global = SolidFrame.byVars(metricVar, meanVar, stdVar);

        for (RegressionMetric metric : eval.getMetrics()) {
            global.addRows(1);
            global.setLabel(global.rowCount() - 1, "metric", metric.getName());
            global.setDouble(global.rowCount() - 1, "mean", Mean.of(trainScores.rvar(metric.getName())).value());
            global.setDouble(global.rowCount() - 1, "std", Variance.of(trainScores.rvar(metric.getName())).sdValue());
        }
        sb.append(global.toFullContent(printer, options));
        sb.append("\n");
        return sb.toString();
    }

    @Override
    public String toContent(Printer printer, POption<?>... options) {
        StringBuilder sb = new StringBuilder();

        sb.append(toContentName(printer, options));
        sb.append(toContentCVScore(printer, options));

        return sb.toString();
    }

    @Override
    public String toFullContent(Printer printer, POption<?>... options) {
        StringBuilder sb = new StringBuilder();
        sb.append("Model:\n");
        sb.append(eval.getModel().fullName()).append("\n");

        sb.append("Raw scores:\n");
        sb.append("===========\n");
        sb.append(trainScores.toFullContent(printer, options));
        sb.append("\n");

        sb.append("Round scores:\n");
        sb.append("=============\n");
        List<GroupFun> groupFuns = new ArrayList<>();
        for (RegressionMetric metric : eval.getMetrics()) {
            groupFuns.add(Group.mean(metric.getName()));
            groupFuns.add(Group.std(metric.getName()));
        }
        sb.append(Group.from(trainScores, "round").aggregate(groupFuns.toArray(GroupFun[]::new))
                .toFrame()
                .toFullContent(printer, options));
        sb.append("\n");

        sb.append(toContentCVScore(printer, options));

        return sb.toString();
    }
}

