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

package rapaio.math.nn.operations;

import rapaio.math.nn.Node;

@Deprecated
public final class OpAxisStd extends BaseOpNode {

    private final int axis;
    private final int ddof;
    private final Node x;
    private final Node mean;

    public OpAxisStd(Node x, int axis, int ddof, Node mean) {
        super(x.dtype(), "axisMean");
        this.axis = axis;
        this.ddof = ddof;
        this.x = x;
        this.mean = mean;
        forward();
    }

    private void forward() {
        this.setValue(x.value().stdc(axis, ddof));
        var mu = mean != null ? mean : x.axisMean(axis);
        backEdge(x, () -> this.grad().mul(x.value().sub(mu.value()).div(this.value()).div(x.value().dim(axis) - ddof)));
//        backEdge(mu, () -> this.grad().mul(x.value().sub(mu.value()).div(x.value().dim(axis) - ddof)));
    }
}
