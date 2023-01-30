/*
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/
 *
 * Copyright 2013 - 2022 Aurelian Tutuianu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package algebra.rapaio;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import algebra.Utils;
import rapaio.core.distributions.Normal;
import rapaio.data.Frame;
import rapaio.data.VarDouble;
import rapaio.data.preprocessing.RefSort;
import rapaio.graphics.Plotter;
import rapaio.graphics.plot.Plot;
import rapaio.graphics.plot.artist.Legend;
import rapaio.io.Csv;
import rapaio.math.linear.DVector;
import rapaio.math.linear.base.DVectorBase;
import rapaio.math.linear.dense.DVectorDense;
import rapaio.math.linear.dense.DVectorMap;
import rapaio.math.linear.dense.DVectorStride;
import rapaio.sys.WS;
import rapaio.sys.With;

@BenchmarkMode( {Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class DVectorPNormBenchmark {

    @State(Scope.Benchmark)
    public static class VectorState {
        @Param( {"100", "1000", "10000", "100000", "1000000"})
        private int n;

        private DVectorBase vb;
        private DVectorDense vd;
        private DVectorStride vs;
        private DVectorMap vm;
        private DVector varvd;
        private DVector varmap;

        private DVectorDense x1;

        @Setup(Level.Invocation)
        public void setup() {
            Random random = new Random(42);
            double[] dense = new double[n + 3];
            double[] base = new double[n];
            double[] stride = new double[3 + n * 2];
            int[] indexes = new int[n];
            for (int i = 0; i < n; i++) {
                double value = random.nextDouble();
                base[i] = value;
                dense[3 + i] = value;
                stride[3 + i * 2] = value;
                indexes[i] = i * 2;
            }
            vb = new DVectorBase(base);
            vd = new DVectorDense(3, n, dense);
            vs = new DVectorStride(3, 2, n, stride);
            vm = new DVectorMap(3, indexes, stride);
            varvd = VarDouble.wrap(base).dv();
            varmap = VarDouble.wrap(stride).dv().map(indexes);

            x1 = DVectorDense.random(n, Normal.std());
        }
    }

    @Benchmark
    public void testBase(VectorState s, Blackhole bh) {
        bh.consume(s.vb.norm(3));
    }

    @Benchmark
    public void testDense(VectorState s, Blackhole bh) {
        bh.consume(s.vd.norm(3));
    }

    @Benchmark
    public void testStride(VectorState s, Blackhole bh) {
        bh.consume(s.vs.norm(3));
    }

    @Benchmark
    public void testMap(VectorState s, Blackhole bh) {
        bh.consume(s.vm.norm(3));
    }

    @Benchmark
    public void testVarDense(VectorState s, Blackhole bh) {
        bh.consume(s.varvd.norm(3));
    }

    @Benchmark
    public void testVarMap(VectorState s, Blackhole bh) {
        bh.consume(s.varmap.norm(3));
    }

    public static void main(String[] args) throws RunnerException {
        runTest();
        printResults();
    }

    public static void runTest() throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(DVectorPNormBenchmark.class.getSimpleName())
                .warmupTime(TimeValue.seconds(1))
                .warmupIterations(3)
                .measurementTime(TimeValue.seconds(1))
                .measurementIterations(5)
                .forks(1)
                .resultFormat(ResultFormatType.CSV)
                .result(Utils.resultPath(DVectorPNormBenchmark.class))
                .build();

        new Runner(opt).run();
    }

    public static void printResults() {
        Frame df = Csv.instance().quotes.set(true).read(Utils.resultPath(DVectorPNormBenchmark.class));

        Plot plot = Plotter.plot();
        int i = 1;
        for (String benchmark : df.rvar("Benchmark").levels().stream().skip(1).toList()) {
            Frame sel = df.stream().filter(s -> s.getLabel("Benchmark").equals(benchmark))
                    .toMappedFrame();
            sel = sel.fapply(RefSort.by(sel.rvar("Score").refComparator()));
            plot.lines(sel.rvar("Param: n"), sel.rvar("Score"), With.color(i));
            i++;
        }
        plot.legend(Legend.UP_LEFT, With.labels(df.rvar("Benchmark").levels().stream().skip(1).toArray(String[]::new)));

        WS.draw(plot);
    }
}
