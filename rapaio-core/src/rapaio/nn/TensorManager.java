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

package rapaio.nn;

import java.io.Closeable;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import rapaio.core.distributions.Distribution;
import rapaio.narray.DType;
import rapaio.narray.NArray;
import rapaio.narray.NArrayManager;
import rapaio.narray.Order;
import rapaio.narray.Shape;

public final class TensorManager implements Closeable {

    public static TensorManager ofFloat() {
        return new TensorManager(DType.FLOAT,
                Math.ceilDiv(Runtime.getRuntime().availableProcessors(), 2),
                Math.floorDiv(Runtime.getRuntime().availableProcessors(), 2));
    }

    public static TensorManager ofDouble() {
        return new TensorManager(DType.DOUBLE,
                Math.ceilDiv(Runtime.getRuntime().availableProcessors(), 2),
                Math.floorDiv(Runtime.getRuntime().availableProcessors(), 2));
    }

    private final DType<?> dt;
    private final Random random;

    private final NArrayManager arrayManager;
    private final int outerThreads;
    private final int innerThreads;

    private final ExecutorService outerExecutor;

    private TensorManager(DType<?> dt, int outerThreads, int innerThreads) {
        this.dt = dt;
        this.random = new Random();
        this.arrayManager = NArrayManager.base();
        this.outerThreads = outerThreads;
        this.innerThreads = innerThreads;

        this.outerExecutor = Executors.newFixedThreadPool(outerThreads);
    }

    public void seed(long seed) {
        random.setSeed(seed);
    }

    public Random random() {
        return random;
    }

    public DType<?> dtype() {
        return dt;
    }

    public ExecutorService outerExecutor() {
        return outerExecutor;
    }

    public int outerThreads() {
        return outerThreads;
    }

    public int innerThreads() {
        return innerThreads;
    }

    @Override
    public void close() {
//        outerExecutor.close();
        outerExecutor.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!outerExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                outerExecutor.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!outerExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("Pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            outerExecutor.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    // tensor and array creation

    public Variable var(NArray<?> value) {
        return new Variable(this, value);
    }

    public Variable var() {
        return new Variable(this);
    }

    public Variable scalarTensor(double value) {
        return new Variable(this, arrayManager.scalar(dt, value));
    }

    public Variable zerosTensor(Shape shape) {
        return new Variable(this, arrayManager.zeros(dt, shape));
    }

    public Variable fullTensor(Shape shape, double fill) {
        return new Variable(this, arrayManager.full(dt, shape, fill));
    }

    public Variable randomTensor(Shape shape, Random random) {
        return new Variable(this, arrayManager.random(dt, shape, random));
    }

    public Variable randomTensor(Shape shape, Distribution distribution, Random random) {
        return new Variable(this, arrayManager.random(dt, shape, distribution, random, Order.defaultOrder()));
    }

    public Variable randomTensor(Shape shape, Distribution distribution, Random random, Order askOrder) {
        return new Variable(this, arrayManager.random(dt, shape, distribution, random, askOrder));
    }

    public Variable seqTensor(Shape shape) {
        return new Variable(this, arrayManager.seq(dt, shape));
    }

    public Variable strideTensor(Shape shape, byte... values) {
        return new Variable(this, arrayManager.stride(dt, shape, Order.defaultOrder(), values));
    }

    public Variable strideTensor(Shape shape, int... values) {
        return new Variable(this, arrayManager.stride(dt, shape, Order.defaultOrder(), values));
    }

    public Variable strideTensor(Shape shape, double... values) {
        return new Variable(this, arrayManager.stride(dt, shape, Order.defaultOrder(), values));
    }

    public Variable strideTensor(Shape shape, float... values) {
        return new Variable(this, arrayManager.stride(dt, shape, Order.defaultOrder(), values));
    }


    public NArray<?> scalarArray(double value) {
        return arrayManager.scalar(dt, value);
    }

    public NArray<?> zerosArray(Shape shape) {
        return arrayManager.zeros(dt, shape);
    }

    public NArray<?> zerosArray(DType<?> dt, Shape shape) {
        return arrayManager.zeros(dt, shape);
    }

    public NArray<?> fullArray(Shape shape, double fill) {
        return arrayManager.full(dt, shape, fill);
    }

    public NArray<?> randomArray(Shape shape, Random random) {
        return arrayManager.random(dt, shape, random);
    }

    public NArray<?> randomArray(Shape shape, Distribution distribution, Random random) {
        return arrayManager.random(dt, shape, distribution, random, Order.defaultOrder());
    }

    public NArray<?> randomArray(Shape shape, Distribution distribution, Random random, Order askOrder) {
        return arrayManager.random(dt, shape, distribution, random, askOrder);
    }

    public NArray<?> seqArray(Shape shape) {
        return arrayManager.seq(dt, shape);
    }

    public NArray<?> strideArray(Shape shape, byte... values) {
        return arrayManager.stride(dt, shape, Order.defaultOrder(), values);
    }

    public NArray<?> strideArray(Shape shape, int... values) {
        return arrayManager.stride(dt, shape, Order.defaultOrder(), values);
    }

    public NArray<?> strideArray(Shape shape, double... values) {
        return arrayManager.stride(dt, shape, Order.defaultOrder(), values);
    }

    public NArray<?> strideArray(Shape shape, float... values) {
        return arrayManager.stride(dt, shape, Order.defaultOrder(), values);
    }
}
