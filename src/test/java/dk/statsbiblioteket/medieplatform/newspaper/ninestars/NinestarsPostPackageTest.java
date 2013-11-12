package dk.statsbiblioteket.medieplatform.newspaper.ninestars;

import dk.statsbiblioteket.util.console.ProcessRunner;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.util.Properties;

public class NinestarsPostPackageTest {

    @Test(groups = "integrationTest", enabled = true)
    public void testMain() throws Exception {
        System.out.println("Running postPackageTest");

        String batchFolder =
                System.getProperty("integration.test.newspaper.testdata") + "/small-test-batch/" + "B400022028241-RT1";
        Properties properties = new Properties(System.getProperties());
        properties.load(new FileInputStream(System.getProperty("integration.test.newspaper.properties")));
        String jdbcURL = properties.getProperty("mfpak.postgres.url");
        jdbcURL = jdbcURL + "?user=" + properties.getProperty("mfpak.postgres.user");
        jdbcURL = jdbcURL + "&password=" + properties.getProperty("mfpak.postgres.password");
        ProcessRunner runner = new ProcessRunner("bash",
                                                 Thread.currentThread().getContextClassLoader().getResource(
                                                         "integrationTest.sh").getFile(),
                                                 batchFolder,
                                                 jdbcURL);
        runner.run();
        System.out.println(runner.getProcessOutputAsString());
        if (runner.getReturnCode() != 0){
            System.err.println(runner.getProcessErrorAsString());
        }
        Assert.assertEquals(runner.getReturnCode(),0);

    }
}