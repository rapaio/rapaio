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

package rapaio.math.nn.optimizer;

import java.util.Collection;
import java.util.HashMap;

import rapaio.core.param.Param;
import rapaio.core.param.ParamSet;
import rapaio.core.param.ValueParam;
import rapaio.math.nn.Node;
import rapaio.math.nn.Optimizer;
import rapaio.math.tensor.Tensor;

public class SGD extends ParamSet<SGD> implements Optimizer {

    public final Param<Double, SGD> lr = new ValueParam<>(this, 1e-4, "learning rate");
    public final Param<Double, SGD> weightDecay = new ValueParam<>(this, 0d, "weight decay");
    public final Param<Double, SGD> momentum = new ValueParam<>(this, 0d, "momentum");
    public final Param<Double, SGD> dampening = new ValueParam<>(this, 0d, "dampening");
    public final Param<Boolean, SGD> nesterov = new ValueParam<>(this, false, "nesterov");
    public final Param<Boolean, SGD> maximize = new ValueParam<>(this, false, "maximize");


    private final Collection<Node> params;

    private final HashMap<Node, Tensor<?>> mus = new HashMap<>();

    public SGD(Collection<Node> params) {
        this.params = params;
    }

    @Override
    public final void zeroGrad() {
        params.forEach(Node::zeroGrad);
    }

    @Override
    public void step() {
        for (var param : params) {
            stepParam(param);
        }
    }

    private void stepParam(Node node) {
        Tensor<?> gt = node.grad();
        if (weightDecay.get() != 0) {
            gt = gt.add(node.value().mul(weightDecay.get()));
        }
        if (momentum.get() != 0) {
            var mu = mus.get(node);
            mu = (mu == null) ? gt : mu.mul(momentum.get()).add(gt.mul(1 - dampening.get()));
            mus.put(node, mu);

            if (nesterov.get()) {
                gt = gt.add(mu.mul(momentum.get()));
            } else {
                gt = mu;
            }
        }
        if (maximize.get()) {
            node.value().add_(gt.mul(lr.get()));
        } else {
            node.value().sub_(gt.mul(lr.get()));
        }
    }
}
