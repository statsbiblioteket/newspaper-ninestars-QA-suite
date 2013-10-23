package dk.statsbiblioteket.medieplatform.newspaper.ninestars;

import dk.statsbiblioteket.medieplatform.autonomous.Batch;
import dk.statsbiblioteket.medieplatform.autonomous.ResultCollector;
import dk.statsbiblioteket.medieplatform.autonomous.RunnableComponent;
import dk.statsbiblioteket.newspaper.md5checker.MD5CheckerComponent;
import dk.statsbiblioteket.util.xml.DOM;
import dk.statsbiblioteket.util.xml.XPathSelector;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

public class NinestarsBatchQATest {

    /**
     * Test that the batch id can be parsed from command line parameters
     */
    @Test
    public void testParseArgs(){
        String[] args = new String[]{"B400022028241-RT2"};
        Batch batch = NinestarsBatchQA.getBatch(args);
        Assert.assertEquals(batch.getBatchID(),"400022028241");
        Assert.assertEquals(batch.getRoundTripNumber().intValue(),2);

    }

    /**
     * Test the conversion from a resultCollector to a ninestars QA report
     */
    @Test
    public void testConvert() {

        ResultCollector resultCollector = new ResultCollector("batch", "0.1");
        String reference = "reference";
        String type = "type";
        resultCollector.addFailure(reference, type, "component", "description", "details1\n", "details2\n");

        String converted = NinestarsUtils.convertResult(resultCollector);
        //System.out.println(converted);

        URL schemaFile = Thread.currentThread().getContextClassLoader().getResource("xsd/qaresult.xsd");
        try {
            checkSchema(converted, schemaFile);
        } catch (SAXException | IOException e) {
            Assert.fail();
        }
        Document convertedAsDom = DOM.stringToDOM(converted, true);
        XPathSelector xpath = DOM.createXPathSelector("qa", "http://schemas.statsbiblioteket.dk/qaresult/");
        String typeSelected = xpath.selectString(convertedAsDom,
                                                 "/qa:qaresult/qa:qafailures/qa:qafailure[qa:filereference = '"
                                                 + reference + "']/qa:type");
        Assert.assertEquals(typeSelected, type);

    }


    /**
     * Utility method to check the xml against a schema
     * @param xml the xml to validate
     * @param schemaFile the xml schema to use
     * @throws SAXException if the validation failed
     * @throws IOException if the schema could not be read
     */
    private void checkSchema(String xml,
                             URL schemaFile)
            throws
            SAXException,
            IOException {
        Source xmlFile = new StreamSource(new StringReader(xml));
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory.newSchema(schemaFile);
        Validator validator = schema.newValidator();
        validator.validate(xmlFile);
    }


    /**
     * Test the invocation of the mock component
     * @throws WorkException if the mock component failed
     * @throws IOException
     */
    @Test
    public void testRunComponentMock()
            throws
            WorkException,
            IOException {
        MockComponent component = new MockComponent(getProperties());
        ResultCollector result1 = new ResultCollector(component.getComponentName(), component.getComponentVersion());
        ResultCollector result = NinestarsBatchQA.doWork(null, component, result1);
        Assert.assertTrue(result.isSuccess());

    }

    /**
     * Test the invocation of the MD5CheckerComponent
     * @throws WorkException if the work failed
     * @throws IOException if the propertiesfile could not be read
     */
    @Test(groups = "integrationTest", enabled = true)
    public void testRunComponentMD5()
            throws
            WorkException,
            IOException {
        RunnableComponent component = new MD5CheckerComponent(getProperties());
        ResultCollector result1 = new ResultCollector(component.getComponentName(), component.getComponentVersion());
        Batch batch = new Batch("400022028241");
        ResultCollector result = NinestarsBatchQA.doWork(batch, component, result1);
        Assert.assertTrue(result.isSuccess());

    }

    /**
        * Test the invocation of the MD5CheckerComponent
        * @throws WorkException if the work failed
        * @throws IOException if the propertiesfile could not be read
        */
       @Test(groups = "integrationTest", enabled = true)
       public void testMain()
               throws
               Exception {
           String batchFolder = System.getProperty("integration.test.newspaper.testdata") + "/small-test-batch/";
           Properties properties = new Properties(System.getProperties());
           properties.load(new FileInputStream(System.getProperty("integration.test.newspaper.properties")));
           String jdbcURL = properties.getProperty("mfpak.postgres.url");
           jdbcURL = jdbcURL+"?user="+properties.getProperty("mfpak.postgres.user");
           jdbcURL = jdbcURL+"&password="+properties.getProperty("mfpak.postgres.password");
           Batch batch = new Batch("400022028241");
           System.getProperties().setProperty("atNinestars",Boolean.FALSE.toString());
           Assert.assertEquals(NinestarsBatchQA.doMain(batchFolder + batch.getFullID(), jdbcURL),0);
       }


    private Properties getProperties()
            throws
            IOException {
        Properties props = new Properties(System.getProperties());
        File configFile = getFile("config.properties");
        props.load(new FileInputStream(configFile));
        System.out.println(props.getProperty("scratch"));
        return props;
    }

    private File getFile(String name)
            throws
            FileNotFoundException {
        try {
            return new File(Thread.currentThread().getContextClassLoader().getResource(name).toURI());
        } catch (URISyntaxException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

}
