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

package rapaio.experiment.ml.regression.tree.nbrtree;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntList;
import rapaio.core.stat.Variance;
import rapaio.core.stat.WeightedOnlineStat;
import rapaio.data.Frame;
import rapaio.data.Mapping;
import rapaio.data.Var;
import rapaio.data.VarDouble;
import rapaio.experiment.ml.regression.tree.NestedBoostingRTree;
import rapaio.ml.common.VarSelector;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 4/16/19.
 */
public class NBRTreeNode implements Serializable {

    private static final long serialVersionUID = -377340948451917779L;

    private final int id;
    private final NBRTreeNode parent;

    // learning artifacts
    private List<Double> factors = new ArrayList<>();
    private List<NBRFunction> functions = new ArrayList<>();
    private boolean isLeaf;
    private String splitVarName;
    private double splitValue;
    private NBRTreeNode leftNode;
    private NBRTreeNode rightNode;

    public NBRTreeNode(int id, NBRTreeNode parent) {
        this.id = id;
        this.parent = parent;
    }

    public List<Double> getFactors() {
        return factors;
    }

    public List<NBRFunction> getFunctions() {
        return functions;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public String getSplitVarName() {
        return splitVarName;
    }

    public double getSplitValue() {
        return splitValue;
    }

    public NBRTreeNode getLeftNode() {
        return leftNode;
    }

    public NBRTreeNode getRightNode() {
        return rightNode;
    }

    public void coreFit(NestedBoostingRTree tree, Frame df, Var weights) {

        String targetName = tree.firstTargetName();
        int maxDepth = tree.getMaxDepth();

        Var y = df.rvar(targetName);

//        NBRFunction mean = NBRFunction.CONSTANT;
//        VarDouble prediction = mean.fit(df, weights, y, null);
//        this.functions.add(mean);
//        this.factors.add(1.0);
//        Var residual = VarDouble.from(df.rowCount(), r -> y.getDouble(r) - prediction.getDouble(r)).withName(y.name());
//        this.coreNodeFit(df, weights, residual, tree, 1);
        this.coreNodeFit(df, weights, y, tree, 1);
    }

    private void coreNodeFit(Frame df, Var weights, Var y, NestedBoostingRTree tree, int depth) {

        Var originalY = y;
        VarSelector varSelector = tree.getVarSelector().newInstance();
        varSelector.withVarNames(tree.inputNames());

        // find best fit function
        String[] testVarNames = varSelector.nextVarNames();
        VarDouble prediction = VarDouble.fill(df.rowCount(), 0.0);

        double residualErrorScore = tree.getLoss().computeResidualErrorScore(y);

        for (int k = 0; k < tree.getBasisCount(); k++) {
            Candidate bestCandidate = null;
            for (String testVarName : testVarNames) {
                Candidate candidate = computeCandidate(df, weights, y, testVarName, tree);
                if (Double.isNaN(candidate.score) || residualErrorScore < candidate.score) {
                    continue;
                }
                if (bestCandidate == null) {
                    bestCandidate = candidate;
                    continue;
                }
                if (bestCandidate.score > candidate.score) {
                    bestCandidate = candidate;
                }
            }
            if (bestCandidate == null) {
                isLeaf = true;
                return;
            }
            double factor = computeFactor(y, bestCandidate.prediction);
//            double factor = 1;
            factors.add(factor);
            functions.add(bestCandidate.function);
            Candidate bc = bestCandidate;
            Var oldY = y;
            Var oldPrediction = prediction;
            y = VarDouble.from(y.rowCount(), r -> oldY.getDouble(r) -
                    factor * tree.getLearningRate() * bc.prediction.getDouble(r));
            prediction = VarDouble.from(y.rowCount(), r -> oldPrediction.getDouble(r) +
                    factor * tree.getLearningRate() * bc.prediction.getDouble(r));
            residualErrorScore = tree.getLoss().computeResidualErrorScore(y);
        }

        // check if we found something

        // check depth
        if (depth == tree.getMaxDepth()) {
            // this is a final leaf because of depth
            isLeaf = true;
            return;
        }

        // pre compute errors
        int[] rows = new int[y.rowCount()];
        for (int i = 0; i < y.rowCount(); i++) {
            rows[i] = i;
        }

        // find best split point for each test variable
        String bestSplitVarName = null;
        double bestSplitError = Double.NaN;
        double bestSplitValue = Double.NaN;

        for (String testVarName : testVarNames) {
            int testNameIndex = df.varIndex(testVarName);
            IntArrays.quickSort(rows, 0, y.rowCount(),
                    (o1, o2) -> Double.compare(df.getDouble(o1, testNameIndex), df.getDouble(o2, testNameIndex)));
            WeightedOnlineStat left_os = new WeightedOnlineStat();
            WeightedOnlineStat right_os = new WeightedOnlineStat();

            double[] leftVar = new double[y.rowCount()];
            double[] rightVar = new double[y.rowCount()];
            double[] leftWeight = new double[y.rowCount()];
            double[] rightWeight = new double[y.rowCount()];

            WeightedOnlineStat so = WeightedOnlineStat.empty();

            for (int i = 1; i < y.rowCount(); i++) {
                so.update(y.getDouble(rows[i]), weights.getDouble(rows[i]));
                leftWeight[i] = so.weightSum();
                leftVar[i] = so.variance();
            }
            so = WeightedOnlineStat.empty();
            for (int i = y.rowCount() - 1; i >= 0; i--) {
                so.update(y.getDouble(rows[i]), weights.getDouble(rows[i]));
                rightWeight[i] = so.weightSum();
                rightVar[i] = so.variance();
            }

            for (int i = tree.getMinCount(); i < y.rowCount() - tree.getMinCount() - 1; i++) {
                if (df.getDouble(rows[i], testNameIndex) == df.getDouble(rows[i + 1], testNameIndex)) {
                    continue;
                }

                double err = leftVar[i] / leftWeight[i] + rightVar[i] / rightWeight[i];
                if (Double.isNaN(bestSplitError) || bestSplitError > err) {
                    bestSplitError = err;
                    bestSplitVarName = testVarName;
                    bestSplitValue = (df.getDouble(rows[i + 1], testNameIndex) + df.getDouble(rows[i], testNameIndex)) / 2.0;
                }
            }
        }

        // find children if we have a split
        // if we do not have a split we remain as leaf

        if (Double.isNaN(bestSplitError)) {
            isLeaf = true;
            return;
        }

        // we update the model
        isLeaf = false;
        splitValue = bestSplitValue;
        splitVarName = bestSplitVarName;

        // and perform the split and call learning further on


        IntList leftRows = new IntArrayList();
        IntList rightRows = new IntArrayList();

        int testNameIndex = df.varIndex(splitVarName);
        double sd = Variance.of(df.rvar(testNameIndex)).sdValue();
        double step = sd * tree.getDiffusion();
        for (int i = 0; i < df.rowCount(); i++) {
            if (df.getDouble(i, testNameIndex) < splitValue + step) {
                leftRows.add(i);
            }
            if (df.getDouble(i, testNameIndex) > splitValue - step) {
                rightRows.add(i);
            }
        }

        leftNode = new NBRTreeNode(-1, this);
        rightNode = new NBRTreeNode(-1, this);

        Mapping leftMap = Mapping.wrap(leftRows);
        Mapping rightMap = Mapping.wrap(rightRows);

        Var pred = prediction;
        VarDouble residuals = VarDouble.from(y.rowCount(), row -> originalY.getDouble(row) - pred.getDouble(row)).withName(y.name());

        leftNode.coreNodeFit(
                df.mapRows(leftMap).copy(),
                weights.mapRows(leftMap).copy(),
                residuals.mapRows(leftMap).copy(), tree, depth + 1);
        rightNode.coreNodeFit(
                df.mapRows(rightMap).copy(),
                weights.mapRows(rightMap).copy(),
                residuals.mapRows(rightMap).copy(), tree, depth + 1);
    }

    private double computeFactor(Var resiual, VarDouble fx) {
        double up = 0.0;
        double down = 0.0;
        for (int i = 0; i < resiual.rowCount(); i++) {
            up += resiual.getDouble(i) * fx.getDouble(i);
            down += fx.getDouble(i) * fx.getDouble(i);
        }
        if (!Double.isFinite(up / down)) {
            return 0.0;
        }
//        if (up / down < 0.9) {
//            System.out.println(up / down);
//        }
//        return 1;
        return up / down;
    }

    private Candidate computeCandidate(Frame df, Var weights, Var y, String testVarName, NestedBoostingRTree tree) {
        NBRFunction function = tree.getNbrFunction().newInstance();
        VarDouble pred = function.fit(df, weights, y, testVarName);
        if (pred == null) {
            return new Candidate(Double.NaN, testVarName, function, null);
        }
        double score = tree.getLoss().computeErrorScore(y, pred);
        return new Candidate(score, testVarName, function, pred);
    }
}

class Candidate {
    public final double score;
    public final String testVarName;
    public final NBRFunction function;
    public final VarDouble prediction;


    Candidate(double score, String testVarName, NBRFunction function, VarDouble prediction) {
        this.score = score;
        this.testVarName = testVarName;
        this.function = function;
        this.prediction = prediction;
    }
}