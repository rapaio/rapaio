/*
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/
 *
 *    Copyright 2013 Aurelian Tutuianu
 *    Copyright 2014 Aurelian Tutuianu
 *    Copyright 2015 Aurelian Tutuianu
 *    Copyright 2016 Aurelian Tutuianu
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

package rapaio.data;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;


/**
 * Created by <a href="mailto:padreati@yahoo.com>Aurelian Tutuianu</a>.
 */
public class VRangeTest {

    @Test
    public void testSmoke() {
        Frame df = SolidFrame.byVars(
                NumVar.empty().withName("a"),
                NumVar.empty().withName("b"),
                NumVar.empty().withName("c"),
                NumVar.empty().withName("d"),
                NomVar.empty(0, "A", "B").withName("x"),
                NomVar.empty(0, "C", "D").withName("y")
        );

        test(VRange.of(0, 2), df,
                new int[]{0, 2},
                new String[]{"a", "c"},
                new String[]{"b", "d", "x", "y"});

        test(VRange.of("a", "c"), df,
                new int[]{0, 2},
                new String[]{"a", "c"},
                new String[]{"b", "d", "x", "y"});

        test(VRange.of(Arrays.asList("a", "c")), df,
                new int[]{0, 2},
                new String[]{"a", "c"},
                new String[]{"b", "d", "x", "y"});

        test(VRange.byName(name -> name.compareTo("x") >= 0), df,
                new int[]{4, 5},
                new String[]{"x", "y"},
                new String[]{"a", "b", "c", "d"});

        test(VRange.byFilter(rvar -> rvar.name().compareTo("x") >= 0), df,
                new int[]{4, 5},
                new String[]{"x", "y"},
                new String[]{"a", "b", "c", "d"});

        test(VRange.onlyTypes(VarType.NUMERIC), df,
                new int[]{4, 5},
                new String[]{"x", "y"},
                new String[]{"a", "b", "c", "d"});

        test(VRange.all(), df,
                new int[]{0, 1, 2, 3, 4, 5},
                new String[]{"a", "b", "c", "d", "x", "y"},
                new String[]{});

        test(VRange.of("all"), df,
                new int[]{0, 1, 2, 3, 4, 5},
                new String[]{"a", "b", "c", "d", "x", "y"},
                new String[]{});

        test(VRange.of("a~d"), df,
                new int[]{0, 1, 2, 3},
                new String[]{"a", "b", "c", "d"},
                new String[]{"x", "y"});

        test(VRange.of("0~3"), df,
                new int[]{0, 1, 2, 3},
                new String[]{"a", "b", "c", "d"},
                new String[]{"x", "y"});
    }

    @Test(expected = NumberFormatException.class)
    public void testWrongNumber() {
        Frame df = SolidFrame.byVars(
                NumVar.empty().withName("a"),
                NumVar.empty().withName("b"),
                NumVar.empty().withName("c"),
                NumVar.empty().withName("d"),
                NomVar.empty(0, "A", "B").withName("x"),
                NomVar.empty(0, "C", "D").withName("y")
        );

        test(VRange.of("0~af"), df,
                new int[]{0, 1, 2, 3},
                new String[]{"a", "b", "c", "d"},
                new String[]{"x", "y"});
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoIndexes() {
        VRange.of(new int[]{});
    }

    private void test(VRange range, Frame df, int[] indexes, String[] names, String[] reverse) {
        int[] indexesReal = range.parseVarIndexes(df).stream().mapToInt(x -> x).toArray();
        String[] namesReal = range.parseVarNames(df).toArray(new String[0]);
        String[] reverseReal = range.parseInverseVarNames(df).toArray(new String[0]);
    }
}
