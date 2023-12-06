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

package rapaio.codegen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CodeGenApp {

    private static final List<CodeGenTemplate> templates;

    static {
        templates = new ArrayList<>();

        Replace[] floatReplaces = new Replace[] {
                Replace.of("double", "float"),
                Replace.of("Double", "Float"),
                Replace.of("DTensor", "FTensor"),
                Replace.of("DStorage", "FStorage"),
                Replace.of("DOUBLE", "FLOAT")
        };

        templates.add(new CodeGenTemplate()
                .src.set("rapaio/math/tensor/DTensor.java")
                .dst.set("rapaio/math/tensor/FTensor.java")
                .replaces.set(floatReplaces)
        );

        templates.add(new CodeGenTemplate()
                .src.set("rapaio/math/tensor/mill/varray/DTensorStride.java")
                .dst.set("rapaio/math/tensor/mill/varray/FTensorStride.java")
                .replaces.set(floatReplaces)
        );
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Pass root as parameter.");
        }
        String root = args[0];
        templates.forEach(template -> {
            try {
                template.run(root);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
