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

package rapaio.math.tensor;

import rapaio.math.tensor.operators.*;

public final class TensorOps {

    public static final Abs ABS = new Abs();
    public static final Neg NEG = new Neg();
    public static final Log LOG = new Log();
    public static final Log1p LOG1P = new Log1p();
    public static final Exp EXP = new Exp();
    public static final Expm1 EXPM1 = new Expm1();
    public static final Sin SIN = new Sin();
    public static final ASin ASIN = new ASin();
    public static final Sinh SINH = new Sinh();
    public static final Cos COS = new Cos();
    public static final ACos ACOS = new ACos();
    public static final Cosh COSH = new Cosh();
    public static final Tan TAN = new Tan();
    public static final ATan ATAN = new ATan();
    public static final Tanh TANH = new Tanh();

    public static final Add ADD = new Add();
    public static final Sub SUB = new Sub();
    public static final Mul MUL = new Mul();
    public static final Div DIV = new Div();
}
