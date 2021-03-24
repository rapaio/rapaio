/*
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/
 *
 *    Copyright 2013 - 2021 Aurelian Tutuianu
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

package rapaio.ml.eval.split;

import lombok.AllArgsConstructor;
import lombok.Getter;
import rapaio.core.RandomSource;
import rapaio.data.Frame;
import rapaio.data.Mapping;
import rapaio.data.Var;
import rapaio.util.collection.IntArrays;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 3/3/20.
 */
@AllArgsConstructor
@Getter
public class KFold implements SplitStrategy {

    private final int rounds;
    private final int folds;

    public KFold(int folds) {
        this(1, folds);
    }

    @Override
    public List<Split> generateSplits(Frame df, Var weights) {
        if (folds > df.rowCount()) {
            throw new IllegalArgumentException("Cannot generate more folds than actual number of observations.");
        }

        List<Split> splits = new ArrayList<>();
        for (int round = 0; round < rounds; round++) {

            // allocate mappings, one for each fold
            Mapping[] mappings = new Mapping[folds];
            for (int i = 0; i < folds; i++) {
                mappings[i] = Mapping.empty();
            }

            // distribute rows in folds
            int[] rows = IntArrays.newSeq(0, df.rowCount());
            IntArrays.shuffle(rows, RandomSource.getRandom());
            int pos = 0;
            for (int row : rows) {
                mappings[pos++].add(row);
                if (pos == folds) {
                    pos = 0;
                }
            }

            // generate splits
            for (int i = 0; i < mappings.length; i++) {
                Mapping mapping = mappings[i];
                splits.add(Split.builder()
                        .round(round)
                        .fold(i)
                        .trainDf(df.removeRows(mapping))
                        .trainWeights(weights == null ? null : weights.removeRows(mapping))
                        .testDf(df.mapRows(mapping))
                        .testWeights(weights == null ? null : weights.mapRows(mapping))
                        .build());
            }

        }
        return splits;
    }
}