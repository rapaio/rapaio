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

package rapaio.experiment.math.nn.operations;

import java.util.List;

import rapaio.experiment.math.nn.Context;
import rapaio.experiment.math.nn.Node;
import rapaio.math.tensor.DType;

public abstract class OpNode extends Node {

    private final Context c;

    protected OpNode(Context c, DType<?> dtype, String name) {
        super(dtype, name);
        this.c = c;
    }

    @Override
    public List<Node> children() {
        return List.of();
    }

    @Override
    public List<Runnable> forward() {
        return List.of();
    }
}
