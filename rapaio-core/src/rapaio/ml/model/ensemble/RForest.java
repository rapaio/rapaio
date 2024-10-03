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

package rapaio.ml.model.ensemble;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import rapaio.data.Frame;
import rapaio.data.Var;
import rapaio.data.VarDouble;
import rapaio.data.VarType;
import rapaio.data.sample.RowSampler;
import rapaio.ml.common.Capabilities;
import rapaio.ml.common.VarSelector;
import rapaio.core.param.Param;
import rapaio.core.param.ValueParam;
import rapaio.ml.model.RegressionModel;
import rapaio.ml.model.RegressionResult;
import rapaio.ml.model.RunInfo;
import rapaio.ml.model.tree.RTree;
import rapaio.ml.model.tree.rtree.Splitter;
import rapaio.printer.Printer;
import rapaio.printer.opt.POpt;
import rapaio.util.parralel.ParallelStreamCollector;

/**
 * @author <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> at 1/15/15.
 */
public class RForest extends RegressionModel<RForest, RegressionResult, RunInfo<RForest>> {

    public static RForest newBagging() {
        return new RForest()
                .model.set(RTree
                        .newCART()
                        .varSelector.set(VarSelector.all())
                        .splitter.set(Splitter.Random)
                        .minCount.set(1)
                );
    }

    public static RForest newRF() {
        return new RForest()
                .model.set(RTree.newCART()
                        .varSelector.set(VarSelector.auto())
                        .splitter.set(Splitter.Random)
                        .minCount.set(1)
                );
    }

    public static RForest newRF(RegressionModel<?, ?, ?> model) {
        return new RForest().model.set(model);
    }

    @Serial
    private static final long serialVersionUID = -3926256335736143438L;

    /**
     * Weak learner model
     */
    public final Param<RegressionModel<?, ?, ?>, RForest> model = new ValueParam<>(this,
            RTree.newCART()
                    .varSelector.set(VarSelector.auto())
                    .splitter.set(Splitter.Random)
                    .minCount.set(1),
            "model", Objects::nonNull);

    private final List<RegressionModel<?, ?, ?>> regressions = new ArrayList<>();

    @Override
    public RForest newInstance() {
        return new RForest().copyParameterValues(this);
    }

    @Override
    public String name() {
        return "RForest";
    }

    @Override
    public Capabilities capabilities() {
        return new Capabilities()
                .inputs(1, 1_000_000, true, VarType.BINARY, VarType.INT, VarType.DOUBLE, VarType.NOMINAL)
                .targets(1, 1, false, VarType.DOUBLE);
    }

    @Override
    protected boolean coreFit(Frame df, Var weights) {
        regressions.clear();
        Random random = getRandom();
        long[] seeds = IntStream.range(0, runs.get()).mapToLong(i -> random.nextLong()).toArray();
        int threads = computeThreads();
        ExecutorService executor = Executors.newWorkStealingPool(threads);
        IntStream.range(0, runs.get()).boxed()
                .collect(ParallelStreamCollector.streamingOrdered(s -> buildWeakPredictor(df, weights, s, seeds[s]), executor, threads))
                .forEach(info -> {
                    regressions.add(info.model);
                    runningHook.get().accept(RunInfo.forRegression(this, info.run));
                });
        executor.shutdownNow();
        return true;
    }

    private record WeakPredictorInfo(RegressionModel<?, ?, ?> model, int run) {
    }

    private WeakPredictorInfo buildWeakPredictor(Frame df, Var weights, int run, long seed) {
        RowSampler.Sample sample = rowSampler.get().nextSample(new Random(seed), df, weights);
        RegressionModel<?, ?, ?> m = model.get().newInstance().seed.set(seed);
        return new WeakPredictorInfo(m.fit(sample.df(), sample.weights(), targetNames), run);
    }

    public List<RegressionModel<?, ?, ?>> getFittedModels() {
        return regressions;
    }

    @Override
    protected RegressionResult corePredict(Frame df, boolean withResiduals, final double[] quantiles) {
        RegressionResult fit = RegressionResult.build(this, df, withResiduals, quantiles);
        List<VarDouble> results = regressions
                .parallelStream()
                .map(r -> r.predict(df, false).firstPrediction()).toList();
        var pred = fit.firstPrediction().tensor_();
        pred.fill_(0.0);
        for (VarDouble result : results) {
            pred.add_(result.tensor_());
        }
        pred.div_((double) regressions.size());
        if (withResiduals) {
            fit.buildComplete();
        }
        return fit;
    }

    @Override
    public String toString() {
        return fullName() + ", is fitted: " + isFitted();
    }

    @Override
    public String toSummary(Printer printer, POpt<?>... options) {
        return "Model:\n" + fullName() + "\nfitted: " + isFitted() + "\n";
    }

    @Override
    public String toContent(POpt<?>... options) {
        return toSummary(options);
    }

    @Override
    public String toFullContent(POpt<?>... options) {
        return toSummary(options);
    }
}
