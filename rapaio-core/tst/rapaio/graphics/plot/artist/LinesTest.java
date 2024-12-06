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

package rapaio.graphics.plot.artist;

import static rapaio.graphics.Plotter.gridLayer;
import static rapaio.graphics.Plotter.lines;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import rapaio.data.Frame;
import rapaio.data.Mapping;
import rapaio.data.Var;
import rapaio.data.transform.VarApply;
import rapaio.datasets.Datasets;
import rapaio.printer.Figure;
import rapaio.printer.ImageTools;

public class LinesTest extends AbstractArtistTest {

    private Frame df;

    @BeforeEach
    void setUp() throws Exception {
        df = Datasets.loadLifeScience().mapRows(Mapping.range(2000));
        ImageTools.setBestRenderingHints();
    }

    @Test
    void testLines() throws IOException {

        Var x = df.rvar(0).fapply(VarApply.onDouble(Math::log1p)).name("x").stream().complete().toMappedVar();
        Figure fig = gridLayer(1, 2)
                .add(lines(x))
                .add(lines(x).yLim(-2, -1));
        assertTest(fig, "lines-test");
    }
}
