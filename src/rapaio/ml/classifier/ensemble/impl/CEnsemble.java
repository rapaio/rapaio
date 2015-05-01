/*
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/
 *
 *    Copyright 2013 Aurelian Tutuianu
 *    Copyright 2014 Aurelian Tutuianu
 *    Copyright 2015 Aurelian Tutuianu
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
 */

package rapaio.ml.classifier.ensemble.impl;

import rapaio.core.sample.Sample;
import rapaio.data.Frame;
import rapaio.data.Var;
import rapaio.ml.classifier.AbstractClassifier;
import rapaio.ml.classifier.Classifier;
import rapaio.ml.classifier.ClassifierFit;
import rapaio.ml.classifier.RunningClassifier;
import rapaio.ml.classifier.tree.CTree;
import rapaio.ml.eval.ConfusionMatrix;
import rapaio.util.func.SFunction;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;

/**
 * @author <a href="mailto:padreati@yahoo.com>Aurelian Tutuianu</a>
 */
public abstract class CEnsemble extends AbstractClassifier implements RunningClassifier {

    private static final long serialVersionUID = -145958939373105497L;

    protected int runs = 0;
    protected int topRuns = Integer.MAX_VALUE;
    protected boolean oobComp = false;
    protected Classifier c = CTree.newC45();
    protected SFunction<Classifier, Double> topSelector;
    protected BaggingMode baggingMode = BaggingMode.VOTING;
    //
    protected double totalOobInstances = 0;
    protected double totalOobError = 0;
    protected double oobError = Double.NaN;
    protected List<Classifier> predictors = new ArrayList<>();
    protected List<Double> predictorScores = new ArrayList<>();

    public CEnsemble withOobComp(boolean oobCompute) {
        this.oobComp = oobCompute;
        return this;
    }

    public double getOobError() {
        return oobError;
    }

    public CEnsemble withRuns(int runs) {
        this.runs = runs;
        return this;
    }

    public CEnsemble withBaggingMode(BaggingMode baggingMode) {
        this.baggingMode = baggingMode;
        return this;
    }

    public CEnsemble withTopSelector(int topRuns, SFunction<Classifier, Double> topSelector) {
        this.topRuns = topRuns;
        this.topSelector = topSelector;
        return this;
    }

    @Override
    public void learn(Frame df, Var weights, String... targetVarNames) {

        prepareLearning(df, weights, targetVarNames);
        if (targetNames().length != 1) {
            throw new IllegalArgumentException("Forest classifiers can learn only one target variable");
        }

        predictors.clear();

        totalOobInstances = 0;
        totalOobError = 0;

        IntStream.range(0, runs).parallel().forEach(s -> buildWeakPredictor(df, weights));

        if (oobComp) {
            oobError = totalOobError / totalOobInstances;
        }
    }

    @Override
    public void learnFurther(Frame df, Var weights, String targetVars, int additionalRuns) {

        if (this.targetNames() != null && dictionaries() != null) {
            this.runs += additionalRuns;
        } else {
            this.runs = additionalRuns;
            learn(df, targetVars);
            return;
        }
        ForkJoinPool pool = new ForkJoinPool(Math.max(1, Runtime.getRuntime().availableProcessors() - 1));
        pool.execute(() -> {
            IntStream.range(0, runs).parallel().forEach(s -> buildWeakPredictor(df, weights));
        });
    }

    private void buildWeakPredictor(Frame df, Var weights) {
        Classifier weak = c.newInstance();

        Sample sample = sampler.newSample(df, weights);

        Frame trainFrame = sample.df;
        Var trainWeights = sample.weights;
        Frame oobFrame = df.removeRows(sample.mapping);

        weak.learn(trainFrame, trainWeights, firstTargetName());
        if (oobComp) {
            System.out.println("This must be corrected, right now is wrong!@@@@@@");
            ClassifierFit cp = weak.predict(oobFrame);
            double oobError = new ConfusionMatrix(oobFrame.var(firstTargetName()), cp.firstClasses()).errorCases();
            synchronized (this) {
                totalOobInstances += oobFrame.rowCount();
                totalOobError += oobError;
            }
        }
        if (topRuns >= runs) {
            synchronized (this) {
                predictors.add(weak);
            }
        } else {
            synchronized (this) {
                double score = topSelector.apply(weak);
                if (predictors.size() < topRuns) {
                    predictors.add(weak);
                    predictorScores.add(score);
                } else {
                    int minIndex = -1;
                    for (int i = 0; i < predictors.size(); i++) {
                        if (predictorScores.get(i) < score) {
                            if (minIndex == -1 || predictorScores.get(i) < predictorScores.get(minIndex)) {
                                minIndex = i;
                            }
                        }
                    }
                    if (minIndex != -1) {
                        predictors.set(minIndex, weak);
                        predictorScores.set(minIndex, score);
                    }
                }
            }
        }
    }

    @Override
    public ClassifierFit predict(Frame df, boolean withClasses, boolean withDensities) {
        ClassifierFit cp = ClassifierFit.newEmpty(this, df, true, true);
        cp.addTarget(firstTargetName(), firstDict());

        List<Frame> treeDensities = new ArrayList<>();
        Iterator<Classifier> it = predictors.iterator();
        while (it.hasNext()) {
            ClassifierFit cpTree = it.next().predict(df, true, true);
            treeDensities.add(cpTree.firstDensity());
        }

        baggingMode.computeDensity(firstDict(), new ArrayList<>(treeDensities), cp.firstClasses(), cp.firstDensity());
        return cp;
    }

    @Override
    public void buildSummary(StringBuilder sb) {
        throw new NotImplementedException();
    }
}
