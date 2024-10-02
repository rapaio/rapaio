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

package rapaio.experiment.math.nn.layer.container;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rapaio.experiment.math.nn.DiffTensor;
import rapaio.experiment.math.nn.Module;

public class Sequential extends Module {

    public static Sequential of(String name, List<Module> modules) {
        return new Sequential(name, modules);
    }

    private final List<Module> submodules = new ArrayList<>();

    private Sequential(String name, List<Module> modules) {
        super(name);
        validateInputs(modules);
        registerModules(modules);
    }

    private void validateInputs(List<Module> modules) {
        // assert unique instances
        Set<Module> set = new HashSet<>();
        set.add(this);
        for (var module : modules) {
            if (set.contains(module)) {
                throw new IllegalArgumentException(
                        "Invalid module, since module instances are not unique. Duplicated module name: " + name);
            }
            set.add(module);
        }
    }

    private void registerModules(List<Module> modules) {
        if (modules == null || modules.isEmpty()) {
            throw new IllegalArgumentException("Empty list of modules.");
        }
        submodules.addAll(modules);
    }

    @Override
    public void bindBefore(List<Module> modules) {

    }

    @Override
    public void bindAfter(List<Module> modules) {

    }

    @Override
    public List<DiffTensor> forward(List<? extends DiffTensor> inputValues) {
        return null;
    }
}
