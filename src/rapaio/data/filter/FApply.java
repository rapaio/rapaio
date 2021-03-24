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

package rapaio.data.filter;

import rapaio.data.Frame;
import rapaio.data.VRange;
import rapaio.data.stream.FSpot;
import rapaio.util.function.Double2DoubleFunction;
import rapaio.util.function.Int2IntFunction;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Update a double variable by changing it's value using a function.
 * <p>
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> at 12/15/14.
 */
public class FApply extends AbstractFFilter {

    public static FApply onSpot(Consumer<FSpot> consumer, VRange vRange) {
        return new FApply(Type.SPOT, consumer, F_DOUBLE, F_INT, F_STRING, vRange);
    }

    public static FApply onDouble(Double2DoubleFunction fun, VRange vRange) {
        return new FApply(Type.DOUBLE, F_SPOT, fun, F_INT, F_STRING, vRange);
    }

    public static FApply onInt(Int2IntFunction fun, VRange vRange) {
        return new FApply(Type.INT, F_SPOT, F_DOUBLE, fun, F_STRING, vRange);
    }

    public static FApply onLabel(Function<String, String> fun, VRange vRange) {
        return new FApply(Type.LABEL, F_SPOT, F_DOUBLE, F_INT, fun, vRange);
    }

    private static final long serialVersionUID = 3982915877968295381L;
    private static final Consumer<FSpot> F_SPOT = vSpot -> {
    };
    private static final Double2DoubleFunction F_DOUBLE = key -> key;
    private static final Int2IntFunction F_INT = (int key) -> key;
    private static final Function<String, String> F_STRING = key -> key;

    private final Type type;
    private final Consumer<FSpot> spotConsumer;
    private final Double2DoubleFunction doubleFunction;
    private final Int2IntFunction intFunction;
    private final Function<String, String> stringFunction;


    private FApply(Type type,
                   Consumer<FSpot> spotCOnsumer,
                   Double2DoubleFunction doubleFunction,
                   Int2IntFunction intFunction,
                   Function<String, String> stringFunction,
                   VRange vRange) {
        super(vRange);
        this.type = type;
        this.spotConsumer = spotCOnsumer;
        this.doubleFunction = doubleFunction;
        this.intFunction = intFunction;
        this.stringFunction = stringFunction;
    }

    @Override
    public FFilter newInstance() {
        return new FApply(type, spotConsumer, doubleFunction, intFunction, stringFunction, vRange);
    }

    @Override
    protected void coreFit(Frame df) {
    }

    @Override
    public Frame apply(Frame df) {
        if (type == Type.SPOT) {
            df.stream().forEach(spotConsumer);
            return df;
        }
        for (String name : varNames) {
            int varIndex = df.varIndex(name);

            switch (type) {
                case DOUBLE:
                    for (int i = 0; i < df.rowCount(); i++) {
                        df.setDouble(i, varIndex, doubleFunction.apply(df.getDouble(i, varIndex)));
                    }
                    break;
                case INT:
                    for (int i = 0; i < df.rowCount(); i++) {
                        df.setInt(i, varIndex, intFunction.applyAsInt(df.getInt(i, varIndex)));
                    }
                    break;
                case LABEL:
                    for (int i = 0; i < df.rowCount(); i++) {
                        df.setLabel(i, varIndex, stringFunction.apply(df.getLabel(i, varIndex)));
                    }
                    break;
            }

        }
        return df;
    }

    private enum Type {
        SPOT,
        DOUBLE,
        INT,
        LABEL
    }
}