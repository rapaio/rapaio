package rapaio.printer;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rapaio.core.RandomSource;
import rapaio.core.distributions.Normal;
import rapaio.data.Frame;
import rapaio.data.Var;
import rapaio.data.VarDouble;
import rapaio.data.VarNominal;
import rapaio.datasets.Datasets;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 11/25/18.
 */
public class TextTableTest {

    @BeforeEach
    void beforeEach() {
        RandomSource.setSeed(123);
    }

    @Test
    void testSimple() {
        Frame iris = Datasets.loadIrisDataset().mapRows(0, 1, 2, 50, 51, 52, 100, 101, 102);
        TextTable tt = TextTable.empty(iris.rowCount() + 1, iris.varCount() + 1);
        for (int i = 0; i < iris.varCount(); i++) {
            tt.set(0, i + 1, iris.rvar(i).name(), 1);
        }
        for (int i = 0; i < iris.rowCount(); i++) {
            tt.set(i + 1, 0, i + ".", 1);
        }
        for (int i = 0; i < iris.rowCount(); i++) {
            for (int j = 0; j < iris.varCount(); j++) {
                tt.set(i + 1, j + 1, iris.getLabel(i, j), iris.rvar(j).type().isNumeric() ? 1 : 1);
            }
        }
        String rawText = tt.getRawText();
        assertEquals("   sepal-length sepal-width petal-length petal-width      class \n" +
                "0.          5.1         3.5          1.4         0.2     setosa \n" +
                "1.          4.9         3.0          1.4         0.2     setosa \n" +
                "2.          4.7         3.2          1.3         0.2     setosa \n" +
                "3.          7.0         3.2          4.7         1.4 versicolor \n" +
                "4.          6.4         3.2          4.5         1.5 versicolor \n" +
                "5.          6.9         3.1          4.9         1.5 versicolor \n" +
                "6.          6.3         3.3          6.0         2.5  virginica \n" +
                "7.          5.8         2.7          5.1         1.9  virginica \n" +
                "8.          7.1         3.0          5.9         2.1  virginica \n", rawText);
    }

    @Test
    void testDotCentering() {
        TextTable tt = TextTable.empty(5, 1);
        tt.set(0, 0, "23343.345", 0, '.');
        tt.set(1, 0, "2342342323343.", 0, '.');
        tt.set(2, 0, "343.345", 0, '.');
        tt.set(3, 0, "343.3453424", 0, '.');
        tt.set(4, 0, "2.3454434", 0, '.');

        assertEquals("        23343.345     \n" +
                "2342342323343.        \n" +
                "          343.345     \n" +
                "          343.3453424 \n" +
                "            2.3454434 \n", tt.getRawText());
    }

    @Test
    void testDotMixt() {
        TextTable tt = TextTable.empty(5, 1);
        tt.set(0, 0, "23343.345", -1, '.');
        tt.set(1, 0, "2342342323343.", -1, '.');
        tt.set(2, 0, "343.345", 0, '.');
        tt.set(3, 0, "343.3453424", -1, '.');
        tt.set(4, 0, "2.3454434", 1, '.');

        assertEquals("        23343.345     \n" +
                "2342342323343.        \n" +
                "          343.345     \n" +
                "          343.3453424 \n" +
                "            2.3454434 \n", tt.getRawText());
    }

    @Test
    void testDynamic() {
        VarNominal headerCol = VarNominal.empty().name("header col");
        VarDouble x1 = VarDouble.empty().name("x1");
        VarDouble x2 = VarDouble.empty().name("x2");
        VarDouble x3 = VarDouble.empty().name("x1");

        Normal normal = Normal.of(0, 20);
        for (int i = 0; i < 30; i++) {
            headerCol.addLabel(randomString());
            x1.addDouble(normal.sampleNext());
            x2.addDouble(normal.sampleNext());
            x3.addDouble(normal.sampleNext());
        }

        TextTable tt = TextTable.empty(headerCol.rowCount() + 1, 4, 1, 1);
        tt.set(0, 0, headerCol.name(), 0);
        tt.set(0, 1, x1.name(), 0);
        tt.set(0, 2, x2.name(), 0);
        tt.set(0, 3, x3.name(), 0);

        for (int i = 0; i < headerCol.rowCount(); i++) {
            tt.set(i + 1, 0, headerCol.getLabel(i), 0);
            tt.set(i + 1, 1, Format.floatFlex(x1.getDouble(i)), 0, '.');
            tt.set(i + 1, 2, Format.floatFlex(x2.getDouble(i)), 0, '.');
            tt.set(i + 1, 3, Format.floatFlex(x3.getDouble(i)), 0, '.');
        }

        assertEquals("header col     x1      \n" +
                "  999999   -10.3975897 \n" +
                "  989997    11.4228522 \n" +
                "  979799     5.1848768 \n" +
                "  989898   -26.7227345 \n" +
                "  989799    -0.0068314 \n" +
                "  989897    -3.0649885 \n" +
                "  979798   -24.3172998 \n" +
                "  979999    10.1338974 \n" +
                "  979897    15.1330668 \n" +
                "  999897    12.8722178 \n" +
                "  979898     6.2702582 \n" +
                "  989997    36.3624695 \n" +
                "  979797   -10.9652756 \n" +
                "  979997     3.3390545 \n" +
                "  989898     8.453027  \n" +
                "  989799   -10.3517055 \n" +
                "  999898    -0.7302866 \n" +
                "  979798   -29.5404927 \n" +
                "  979999    41.3615705 \n" +
                "  979797     3.1042482 \n" +
                "  979797   -18.1822648 \n" +
                "  979997    18.9428089 \n" +
                "  989899    -8.5881503 \n" +
                "  999797    16.2451777 \n" +
                "  989998     7.7186976 \n" +
                "  989798   -41.1261094 \n" +
                "  999898    41.7470811 \n" +
                "  999797   -22.1976789 \n" +
                "  999999    34.9367874 \n" +
                "  999897   -24.7709022 \n" +
                "\n" +
                "header col     x2      \n" +
                "  999999     3.7379485 \n" +
                "  989997   -29.2332262 \n" +
                "  979799   -15.672808  \n" +
                "  989898    -3.8095887 \n" +
                "  989799    41.4251087 \n" +
                "  989897   -16.0310845 \n" +
                "  979798    20.6345505 \n" +
                "  979999     9.1530262 \n" +
                "  979897    -4.2091832 \n" +
                "  999897    31.3450903 \n" +
                "  979898    -7.5640252 \n" +
                "  989997    11.9810382 \n" +
                "  979797   -16.4312454 \n" +
                "  979997     0.7376601 \n" +
                "  989898    18.9186874 \n" +
                "  989799    -2.6925228 \n" +
                "  999898   -23.1653109 \n" +
                "  979798    -0.646863  \n" +
                "  979999    12.3816197 \n" +
                "  979797    21.04082   \n" +
                "  979797    -8.4855522 \n" +
                "  979997    12.2610176 \n" +
                "  989899   -14.9328976 \n" +
                "  999797    -9.8161122 \n" +
                "  989998     5.8388153 \n" +
                "  989798   -14.839929  \n" +
                "  999898   -23.9164675 \n" +
                "  999797    -3.5516805 \n" +
                "  999999     8.7091984 \n" +
                "  999897    -5.4354875 \n" +
                "\n" +
                "header col     x1      \n" +
                "  999999   -12.935949  \n" +
                "  989997    16.5667972 \n" +
                "  979799    -2.0997226 \n" +
                "  989898    22.947924  \n" +
                "  989799    16.9617833 \n" +
                "  989897     5.8062882 \n" +
                "  979798   -20.1644745 \n" +
                "  979999     6.5726676 \n" +
                "  979897   -17.8997236 \n" +
                "  999897    18.7648089 \n" +
                "  979898   -19.1781901 \n" +
                "  989997    -8.5143005 \n" +
                "  979797    -2.1162765 \n" +
                "  979997   -20.338834  \n" +
                "  989898   -20.4969131 \n" +
                "  989799    12.7701972 \n" +
                "  999898    -6.1167291 \n" +
                "  979798   -18.7265816 \n" +
                "  979999    15.5546356 \n" +
                "  979797   -25.4420671 \n" +
                "  979797   -21.6472563 \n" +
                "  979997    15.2371083 \n" +
                "  989899    26.8589865 \n" +
                "  999797   -39.6465554 \n" +
                "  989998   -21.0344345 \n" +
                "  989798   -20.9578747 \n" +
                "  999898   -31.8052    \n" +
                "  999797    14.5710563 \n" +
                "  999999    16.6328341 \n" +
                "  999897    30.2430254 \n" +
                "\n", tt.getText(1));

        assertEquals("header col     x1          x2      \n" +
                "  999999   -10.3975897   3.7379485 \n" +
                "  989997    11.4228522 -29.2332262 \n" +
                "  979799     5.1848768 -15.672808  \n" +
                "  989898   -26.7227345  -3.8095887 \n" +
                "  989799    -0.0068314  41.4251087 \n" +
                "  989897    -3.0649885 -16.0310845 \n" +
                "  979798   -24.3172998  20.6345505 \n" +
                "  979999    10.1338974   9.1530262 \n" +
                "  979897    15.1330668  -4.2091832 \n" +
                "  999897    12.8722178  31.3450903 \n" +
                "  979898     6.2702582  -7.5640252 \n" +
                "  989997    36.3624695  11.9810382 \n" +
                "  979797   -10.9652756 -16.4312454 \n" +
                "  979997     3.3390545   0.7376601 \n" +
                "  989898     8.453027   18.9186874 \n" +
                "  989799   -10.3517055  -2.6925228 \n" +
                "  999898    -0.7302866 -23.1653109 \n" +
                "  979798   -29.5404927  -0.646863  \n" +
                "  979999    41.3615705  12.3816197 \n" +
                "  979797     3.1042482  21.04082   \n" +
                "  979797   -18.1822648  -8.4855522 \n" +
                "  979997    18.9428089  12.2610176 \n" +
                "  989899    -8.5881503 -14.9328976 \n" +
                "  999797    16.2451777  -9.8161122 \n" +
                "  989998     7.7186976   5.8388153 \n" +
                "  989798   -41.1261094 -14.839929  \n" +
                "  999898    41.7470811 -23.9164675 \n" +
                "  999797   -22.1976789  -3.5516805 \n" +
                "  999999    34.9367874   8.7091984 \n" +
                "  999897   -24.7709022  -5.4354875 \n" +
                "\n" +
                "header col     x1      \n" +
                "  999999   -12.935949  \n" +
                "  989997    16.5667972 \n" +
                "  979799    -2.0997226 \n" +
                "  989898    22.947924  \n" +
                "  989799    16.9617833 \n" +
                "  989897     5.8062882 \n" +
                "  979798   -20.1644745 \n" +
                "  979999     6.5726676 \n" +
                "  979897   -17.8997236 \n" +
                "  999897    18.7648089 \n" +
                "  979898   -19.1781901 \n" +
                "  989997    -8.5143005 \n" +
                "  979797    -2.1162765 \n" +
                "  979997   -20.338834  \n" +
                "  989898   -20.4969131 \n" +
                "  989799    12.7701972 \n" +
                "  999898    -6.1167291 \n" +
                "  979798   -18.7265816 \n" +
                "  979999    15.5546356 \n" +
                "  979797   -25.4420671 \n" +
                "  979797   -21.6472563 \n" +
                "  979997    15.2371083 \n" +
                "  989899    26.8589865 \n" +
                "  999797   -39.6465554 \n" +
                "  989998   -21.0344345 \n" +
                "  989798   -20.9578747 \n" +
                "  999898   -31.8052    \n" +
                "  999797    14.5710563 \n" +
                "  999999    16.6328341 \n" +
                "  999897    30.2430254 \n" +
                "\n", tt.getText(40));


        assertEquals("header col     x1          x2          x1      \n" +
                "  999999   -10.3975897   3.7379485 -12.935949  \n" +
                "  989997    11.4228522 -29.2332262  16.5667972 \n" +
                "  979799     5.1848768 -15.672808   -2.0997226 \n" +
                "  989898   -26.7227345  -3.8095887  22.947924  \n" +
                "  989799    -0.0068314  41.4251087  16.9617833 \n" +
                "  989897    -3.0649885 -16.0310845   5.8062882 \n" +
                "  979798   -24.3172998  20.6345505 -20.1644745 \n" +
                "  979999    10.1338974   9.1530262   6.5726676 \n" +
                "  979897    15.1330668  -4.2091832 -17.8997236 \n" +
                "  999897    12.8722178  31.3450903  18.7648089 \n" +
                "  979898     6.2702582  -7.5640252 -19.1781901 \n" +
                "  989997    36.3624695  11.9810382  -8.5143005 \n" +
                "  979797   -10.9652756 -16.4312454  -2.1162765 \n" +
                "  979997     3.3390545   0.7376601 -20.338834  \n" +
                "  989898     8.453027   18.9186874 -20.4969131 \n" +
                "  989799   -10.3517055  -2.6925228  12.7701972 \n" +
                "  999898    -0.7302866 -23.1653109  -6.1167291 \n" +
                "  979798   -29.5404927  -0.646863  -18.7265816 \n" +
                "  979999    41.3615705  12.3816197  15.5546356 \n" +
                "  979797     3.1042482  21.04082   -25.4420671 \n" +
                "  979797   -18.1822648  -8.4855522 -21.6472563 \n" +
                "  979997    18.9428089  12.2610176  15.2371083 \n" +
                "  989899    -8.5881503 -14.9328976  26.8589865 \n" +
                "  999797    16.2451777  -9.8161122 -39.6465554 \n" +
                "  989998     7.7186976   5.8388153 -21.0344345 \n" +
                "  989798   -41.1261094 -14.839929  -20.9578747 \n" +
                "  999898    41.7470811 -23.9164675 -31.8052    \n" +
                "  999797   -22.1976789  -3.5516805  14.5710563 \n" +
                "  999999    34.9367874   8.7091984  16.6328341 \n" +
                "  999897   -24.7709022  -5.4354875  30.2430254 \n", tt.getText(50));

        assertEquals("header col     x1          x2          x1      header col     x1          x2          x1      \n" +
                "  999999   -10.3975897   3.7379485 -12.935949    989799   -10.3517055  -2.6925228  12.7701972 \n" +
                "  989997    11.4228522 -29.2332262  16.5667972   999898    -0.7302866 -23.1653109  -6.1167291 \n" +
                "  979799     5.1848768 -15.672808   -2.0997226   979798   -29.5404927  -0.646863  -18.7265816 \n" +
                "  989898   -26.7227345  -3.8095887  22.947924    979999    41.3615705  12.3816197  15.5546356 \n" +
                "  989799    -0.0068314  41.4251087  16.9617833   979797     3.1042482  21.04082   -25.4420671 \n" +
                "  989897    -3.0649885 -16.0310845   5.8062882   979797   -18.1822648  -8.4855522 -21.6472563 \n" +
                "  979798   -24.3172998  20.6345505 -20.1644745   979997    18.9428089  12.2610176  15.2371083 \n" +
                "  979999    10.1338974   9.1530262   6.5726676   989899    -8.5881503 -14.9328976  26.8589865 \n" +
                "  979897    15.1330668  -4.2091832 -17.8997236   999797    16.2451777  -9.8161122 -39.6465554 \n" +
                "  999897    12.8722178  31.3450903  18.7648089   989998     7.7186976   5.8388153 -21.0344345 \n" +
                "  979898     6.2702582  -7.5640252 -19.1781901   989798   -41.1261094 -14.839929  -20.9578747 \n" +
                "  989997    36.3624695  11.9810382  -8.5143005   999898    41.7470811 -23.9164675 -31.8052    \n" +
                "  979797   -10.9652756 -16.4312454  -2.1162765   999797   -22.1976789  -3.5516805  14.5710563 \n" +
                "  979997     3.3390545   0.7376601 -20.338834    999999    34.9367874   8.7091984  16.6328341 \n" +
                "  989898     8.453027   18.9186874 -20.4969131   999897   -24.7709022  -5.4354875  30.2430254 \n", tt.getText(100));

        assertEquals("header col     x1          x2          x1      header col     x1          x2          x1      header col     x1          x2          x1      header col     x1          x2          x1      \n" +
                "  999999   -10.3975897   3.7379485 -12.935949    979897    15.1330668  -4.2091832 -17.8997236   999898    -0.7302866 -23.1653109  -6.1167291   989998     7.7186976   5.8388153 -21.0344345 \n" +
                "  989997    11.4228522 -29.2332262  16.5667972   999897    12.8722178  31.3450903  18.7648089   979798   -29.5404927  -0.646863  -18.7265816   989798   -41.1261094 -14.839929  -20.9578747 \n" +
                "  979799     5.1848768 -15.672808   -2.0997226   979898     6.2702582  -7.5640252 -19.1781901   979999    41.3615705  12.3816197  15.5546356   999898    41.7470811 -23.9164675 -31.8052    \n" +
                "  989898   -26.7227345  -3.8095887  22.947924    989997    36.3624695  11.9810382  -8.5143005   979797     3.1042482  21.04082   -25.4420671   999797   -22.1976789  -3.5516805  14.5710563 \n" +
                "  989799    -0.0068314  41.4251087  16.9617833   979797   -10.9652756 -16.4312454  -2.1162765   979797   -18.1822648  -8.4855522 -21.6472563   999999    34.9367874   8.7091984  16.6328341 \n" +
                "  989897    -3.0649885 -16.0310845   5.8062882   979997     3.3390545   0.7376601 -20.338834    979997    18.9428089  12.2610176  15.2371083   999897   -24.7709022  -5.4354875  30.2430254 \n" +
                "  979798   -24.3172998  20.6345505 -20.1644745   989898     8.453027   18.9186874 -20.4969131   989899    -8.5881503 -14.9328976  26.8589865 \n" +
                "  979999    10.1338974   9.1530262   6.5726676   989799   -10.3517055  -2.6925228  12.7701972   999797    16.2451777  -9.8161122 -39.6465554 \n", tt.getText(200));

        assertEquals("header col     x1          x2          x1      header col     x1          x2          x1      header col     x1          x2          x1      header col     x1          x2          x1      header col     x1          x2          x1      \n" +
                "  999999   -10.3975897   3.7379485 -12.935949    979798   -24.3172998  20.6345505 -20.1644745   979797   -10.9652756 -16.4312454  -2.1162765   979999    41.3615705  12.3816197  15.5546356   989998     7.7186976   5.8388153 -21.0344345 \n" +
                "  989997    11.4228522 -29.2332262  16.5667972   979999    10.1338974   9.1530262   6.5726676   979997     3.3390545   0.7376601 -20.338834    979797     3.1042482  21.04082   -25.4420671   989798   -41.1261094 -14.839929  -20.9578747 \n" +
                "  979799     5.1848768 -15.672808   -2.0997226   979897    15.1330668  -4.2091832 -17.8997236   989898     8.453027   18.9186874 -20.4969131   979797   -18.1822648  -8.4855522 -21.6472563   999898    41.7470811 -23.9164675 -31.8052    \n" +
                "  989898   -26.7227345  -3.8095887  22.947924    999897    12.8722178  31.3450903  18.7648089   989799   -10.3517055  -2.6925228  12.7701972   979997    18.9428089  12.2610176  15.2371083   999797   -22.1976789  -3.5516805  14.5710563 \n" +
                "  989799    -0.0068314  41.4251087  16.9617833   979898     6.2702582  -7.5640252 -19.1781901   999898    -0.7302866 -23.1653109  -6.1167291   989899    -8.5881503 -14.9328976  26.8589865   999999    34.9367874   8.7091984  16.6328341 \n" +
                "  989897    -3.0649885 -16.0310845   5.8062882   989997    36.3624695  11.9810382  -8.5143005   979798   -29.5404927  -0.646863  -18.7265816   999797    16.2451777  -9.8161122 -39.6465554   999897   -24.7709022  -5.4354875  30.2430254 \n", tt.getText(10000000));
    }

    @Test
    void testFloat() {
        Normal normal = Normal.of(0, 10);
        Var x = VarDouble.from(5, normal::sampleNext);
        x.addDouble(1);
        x.addDouble(-122682378);

        TextTable tt = TextTable.empty(x.rowCount(), 1);
        for (int i = 0; i < x.rowCount(); i++) {
            tt.set(i, 0, Format.floatFlex(x.getDouble(i)), 1, '.');
        }
        // integers with no dots are misaligned, this is why we need custom methods
        assertEquals("     5.9229719 \n" +
                "    23.6147893 \n" +
                "    -6.6416266 \n" +
                "     2.7619375 \n" +
                "     8.6277539 \n" +
                "     1         \n" +
                "-122,682,378   \n", tt.getRawText());

        tt.set(4, 0, "left", ".right");
        tt.set(5, 0, "", ".34442");
        tt.set(6, 0, "-12", "");

        assertEquals("   5.9229719 \n" +
                "  23.6147893 \n" +
                "  -6.6416266 \n" +
                "   2.7619375 \n" +
                "left.right   \n" +
                "    .34442   \n" +
                " -12         \n", tt.getRawText());
    }

    private String randomString() {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            s.append('a' + RandomSource.nextInt('d' - 'a'));
        }
        return s.toString();
    }
}
