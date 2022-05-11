package rapaio.graphics.plot.artist;

import static rapaio.graphics.Plotter.*;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import rapaio.core.correlation.CorrSpearman;
import rapaio.core.tools.DistanceMatrix;
import rapaio.data.Frame;
import rapaio.datasets.Datasets;
import rapaio.graphics.plot.artist.AbstractArtistTest;

public class CorrGramTest extends AbstractArtistTest {

    @Test
    void testCorrGram() throws IOException {
        Frame sel = Datasets.loadHousing();
        DistanceMatrix d = CorrSpearman.of(sel).matrix();
        assertTest(corrGram(d), "corrgram-test");
    }

}