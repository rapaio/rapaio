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

package unlinkedlist;

import static rapaio.graphics.opt.GOptions.color;
import static rapaio.graphics.opt.GOptions.labels;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;
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

import commons.Utils;
import rapaio.data.Frame;
import rapaio.data.preprocessing.RefSort;
import rapaio.data.preprocessing.VarApply;
import rapaio.graphics.Plotter;
import rapaio.graphics.plot.Plot;
import rapaio.graphics.plot.artist.Legend;
import rapaio.io.Csv;
import rapaio.math.tensor.Storage;
import rapaio.math.tensor.storage.array.ArrayStorageFactory;
import rapaio.sys.WS;
import rapaio.util.collection.DoubleArrays;

@BenchmarkMode( {Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class MemoryLayoutBenchmark {


    @State(Scope.Benchmark)
    public static class BenchmarkState {
//        @Param( {"100", "1000", "10000"})
        @Param( {"100", "1000"})
//        @Param( {"100"})
        private int n;

        double[] array;
        MemorySegment msArray;
        MemorySegment msExternal;
        Storage<Double> storage;

        @Setup(Level.Invocation)
        public void setup() {
            Random random = new Random(42);
            array = DoubleArrays.newFrom(0, n, _ -> random.nextDouble());
            msArray = MemorySegment.ofArray(array);
            storage = new ArrayStorageFactory().ofDouble().from(array);
            msExternal = Arena.ofAuto().allocate(n * 8, 8);
        }
    }

    @Benchmark
    public void testArrayAdd(BenchmarkState bs, Blackhole bh) {
        for (int i = 0; i < bs.n; i++) {
            bs.array[i] = Math.log1p(bs.array[i]);
        }
        bh.consume(bs.array);
    }

    @Benchmark
    public void testMsAdd(BenchmarkState bs, Blackhole bh) {
        VarHandle vh = ValueLayout.JAVA_DOUBLE.arrayElementVarHandle(bs.n);
        for (int i = 0; i < bs.n; i++) {
            double value = (double) vh.get(bs.msArray, 0, i);
            vh.set(bs.msArray, 0, i, Math.log1p(value));
        }
        bh.consume(bs.msArray);
    }

    @Benchmark
    public void testMsExtAdd(BenchmarkState bs, Blackhole bh) {
        VarHandle vh = ValueLayout.JAVA_DOUBLE.arrayElementVarHandle(bs.n);
        for (int i = 0; i < bs.n; i++) {
            double value = (double) vh.get(bs.msExternal, 0, i);
            vh.set(bs.msExternal, 0, i, Math.log1p(value));
        }
        bh.consume(bs.msExternal);
    }

    @Benchmark
    public void testStorageAdd(BenchmarkState bs, Blackhole bh) {
        for (int i = 0; i < bs.n; i++) {
            double value = bs.storage.get(i);
            bs.storage.setDouble(i, Math.log1p(value));
        }
        bh.consume(bs.storage);
    }

    public static void main(String[] args) throws RunnerException, IOException {
        Options opt = new OptionsBuilder()
                .include(MemoryLayoutBenchmark.class.getSimpleName())
                .warmupTime(TimeValue.seconds(2))
                .warmupIterations(2)
                .measurementTime(TimeValue.seconds(2))
                .measurementIterations(3)
                .forks(1)
                .resultFormat(ResultFormatType.CSV)
                .result(Utils.resultPath(MemoryLayoutBenchmark.class))
                .build();
        new Runner(opt).run();
        Utils.resultPromote(MemoryLayoutBenchmark.class);
        printResults();
    }

    public static void printResults() {
        Frame df = Csv.instance().quotes.set(true).read(Utils.resultPath(MemoryLayoutBenchmark.class));
        Plot plot = Plotter.plot();
        int i = 1;
        for (String benchmark : df.rvar("Benchmark").levels().stream().skip(1).toList()) {
            Frame sel = df.stream().filter(s -> s.getLabel("Benchmark").equals(benchmark)).toMappedFrame();
            sel = sel.fapply(RefSort.by(sel.rvar("Score").refComparator()));
            plot.lines(sel.rvar("Param: n"), sel.rvar("Score").fapply(VarApply.onDouble(Math::log1p)), color(i));
            i++;
        }
        plot.legend(Legend.UP_LEFT, labels(df.rvar("Benchmark").levels().stream().skip(1).toArray(String[]::new)));
        WS.draw(plot);
    }
}
