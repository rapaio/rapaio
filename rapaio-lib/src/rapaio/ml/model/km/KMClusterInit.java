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

package rapaio.ml.model.km;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import rapaio.core.SamplingTools;
import rapaio.darray.DArray;
import rapaio.ml.common.distance.Distance;
import rapaio.util.collection.Doubles;
import rapaio.util.collection.Ints;

/**
 * Function which produces initial centroids for KMeans algorithm
 * <p>
 *
 * @author <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 9/23/15.
 */
public enum KMClusterInit implements Serializable {

    Forgy {
        public DArray<Double> init(Random random, Distance distance, DArray<Double> m, int k) {
            return m.sel(0, SamplingTools.sampleWOR(random, m.dim(0), k));
        }
    },
    PlusPlus {
        @Override
        public DArray<Double> init(final Random random, Distance distance, DArray<Double> m, int k) {

            int[] centroids = Ints.fill(k, -1);

            centroids[0] = random.nextInt(m.dim(0));
            Set<Integer> ids = new HashSet<>();
            ids.add(centroids[0]);

            double[] p = new double[m.dim(0)];
            for (int i = 1; i < k; i++) {
                // fill weights with 0
                Arrays.fill(p, 0);
                // assign weights to the minimum distance to center
                for (int j = 0; j < m.dim(0); j++) {
                    if (ids.contains(j)) {
                        continue;
                    }
                    p[j] = distance.compute(m.selsq(0, centroids[0]), m.selsq(0, j));
                    for (int l = 1; l < i; l++) {
                        p[j] = Math.min(p[j], distance.compute(m.selsq(0, centroids[l]), m.selsq(0, j)));
                    }
                }
                // normalize the weights
                double sum = Doubles.sum(p, 0, p.length);
                Doubles.div(p, 0, sum, p.length);

                int next = SamplingTools.sampleWeightedWR(random, 1, p)[0];
                centroids[i] = next;
                ids.add(next);
            }

            return m.sel(0, centroids);
        }
    };

    public abstract DArray<Double> init(Random random, Distance distance, DArray<Double> m, int k);
}
