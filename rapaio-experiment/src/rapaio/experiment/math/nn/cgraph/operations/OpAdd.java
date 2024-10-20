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

package rapaio.experiment.math.nn.cgraph.operations;

import java.util.List;

import rapaio.experiment.math.nn.cgraph.Context;

public class OpAdd extends Node {

    private final Node left;
    private final Node right;

    public OpAdd(Context c, Node left, Node right) {
        super(c, "add");
        this.left = left;
        this.right = right;
    }

    @Override
    public List<Node> children() {
        return List.of(left, right);
    }

    @Override
    public List<Runnable> compute() {
        value.assign(left.value.tensor().add(right.value.tensor()));
        return List.of(
                () -> left.adjoint.add_(this.adjoint.tensor()),
                () -> right.adjoint.add_(this.adjoint.tensor())
        );
    }
}
