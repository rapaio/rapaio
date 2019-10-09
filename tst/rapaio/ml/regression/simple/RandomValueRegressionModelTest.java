package rapaio.ml.regression.simple;

import org.junit.Before;
import org.junit.Test;
import rapaio.core.RandomSource;
import rapaio.core.distributions.Normal;
import rapaio.core.distributions.Uniform;
import rapaio.core.tests.KSTestOneSample;
import rapaio.data.Frame;
import rapaio.datasets.Datasets;
import rapaio.ml.regression.RegressionResult;

import static org.junit.Assert.*;

/**
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 7/9/19.
 */
public class RandomValueRegressionModelTest {

    private String father = "Father";
    private String son = "Son";
    private Frame df;

    @Before
    public void setUp() throws Exception {
        RandomSource.setSeed(123);
        df = Datasets.loadPearsonHeightDataset();
    }

    @Test
    public void testRandomValueRegression() {
        RegressionResult fit1 = RandomValueRegressionModel.newRVR().fit(df, father).predict(df);
        RegressionResult fit2 = RandomValueRegressionModel.from(Normal.of(10, 0.1)).fit(df, father).predict(df);

        // unsignificant if test on true distribution
        assertTrue(KSTestOneSample.from(fit1.firstPrediction(), Uniform.of(0, 1)).pValue() > 0.01);
        assertTrue(KSTestOneSample.from(fit2.firstPrediction(), Normal.of(10, 0.1)).pValue() > 0.01);

        // significant if test on a different distribution
        assertTrue(KSTestOneSample.from(fit1.firstPrediction(), Normal.of(10, 0.1)).pValue() < 0.01);
        assertTrue(KSTestOneSample.from(fit2.firstPrediction(), Uniform.of(0, 1)).pValue() < 0.01);
    }

    @Test
    public void testNaming() {
        RandomValueRegressionModel model = RandomValueRegressionModel.newRVR();
        assertEquals("RandomValueRegression", model.name());
        assertEquals("RandomValueRegression(distribution:Uniform(a=0,b=1))", model.newInstance().fullName());

        assertEquals("Normal(mu=10, sd=20)", RandomValueRegressionModel.from(Normal.of(10, 20)).newInstance().distribution().name());

        assertEquals("Regression predict summary\n" +
                "=======================\n" +
                "Model class: RandomValueRegression\n" +
                "Model instance: RandomValueRegression(distribution:Uniform(a=0,b=1))\n" +
                "> model not trained.\n", model.content());

        model = model.fit(df, "Son");
        assertEquals("RandomValueRegression(distribution:Uniform(a=0,b=1))", model.toString());
        assertEquals("Regression predict summary\n" +
                "=======================\n" +
                "Model class: RandomValueRegression\n" +
                "Model instance: RandomValueRegression(distribution:Uniform(a=0,b=1))\n" +
                "> model is trained.\n" +
                "> input variables: \n" +
                "1. Father double \n" +
                "> target variables: \n" +
                "1. Son double \n" +
                "Model is trained.\n", model.content());
        assertEquals("Regression predict summary\n" +
                "=======================\n" +
                "Model class: RandomValueRegression\n" +
                "Model instance: RandomValueRegression(distribution:Uniform(a=0,b=1))\n" +
                "> model is trained.\n" +
                "> input variables: \n" +
                "1. Father double \n" +
                "> target variables: \n" +
                "1. Son double \n" +
                "Model is trained.\n", model.fullContent());
        assertEquals("Regression predict summary\n" +
                "=======================\n" +
                "Model class: RandomValueRegression\n" +
                "Model instance: RandomValueRegression(distribution:Uniform(a=0,b=1))\n" +
                "> model is trained.\n" +
                "> input variables: \n" +
                "1. Father double \n" +
                "> target variables: \n" +
                "1. Son double \n" +
                "Model is trained.\n", model.summary());
    }
}
