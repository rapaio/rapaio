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

package rapaio.data.format;

import rapaio.data.Frame;
import rapaio.data.Var;
import rapaio.data.VarType;
import rapaio.printer.TextTable;

public class TextTableUtil {

    public static void textType(TextTable tt, int row, int col, Frame df, int pos, String varName) {
        if (df.type(varName) == VarType.DOUBLE) {
            tt.floatFlex(row, col, df.getDouble(pos, varName));
        } else {
            tt.textRight(row, col, df.getLabel(pos, varName));
        }
    }

    public static void textType(TextTable tt, int row, int col, Var var, int pos) {
        if (var.type() == VarType.DOUBLE) {
            tt.floatFlex(row, col, var.getDouble(pos));
        } else {
            tt.textRight(row, col, var.getLabel(pos));
        }
    }
}
