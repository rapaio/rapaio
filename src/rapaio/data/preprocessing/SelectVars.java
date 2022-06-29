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

package rapaio.data.preprocessing;

import java.io.Serial;

import rapaio.data.Frame;
import rapaio.data.VarRange;

/**
 * Filter to map vars from a data frame.
 * <p>
 * Created by padreati on 1/15/16.
 */
public class SelectVars extends AbstractTransform {

    public static SelectVars map(VarRange varRange) {
        return new SelectVars(varRange);
    }

    @Serial
    private static final long serialVersionUID = 5540246008233767364L;

    private SelectVars(VarRange varRange) {
        super(varRange);
    }

    @Override
    public SelectVars newInstance() {
        return new SelectVars(varRange);
    }

    @Override
    protected void coreFit(Frame df) {
    }

    @Override
    public Frame coreApply(Frame df) {
        return df.mapVars(varNames);
    }
}
