package com.mopub.mobileads.util;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class XmlUtilsTest {

    // From Microsoft's sample xml documents page: https://msdn.microsoft.com/en-us/library/bb387026.aspx
    private String testXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<aw:PurchaseOrders xmlns:aw=\"https://www.adventure-works.com\">\n" +
            "  <aw:PurchaseOrder aw:PurchaseOrderNumber=\"99503\" aw:OrderDate=\"1999-10-20\">\n" +
            "    <aw:Address aw:Type=\"Shipping\">\n" +
            "      <aw:Name>Ellen Adams</aw:Name>\n" +
            "      <aw:Street>123 Maple Street</aw:Street>\n" +
            "      <aw:City>Mill Valley</aw:City>\n" +
            "      <aw:State>CA</aw:State>\n" +
            "      <aw:Zip>10999</aw:Zip>\n" +
            "      <aw:Country>USA</aw:Country>\n" +
            "    </aw:Address>\n" +
            "    <aw:Address aw:Type=\"Billing\">\n" +
            "      <aw:Name>Tai Yee</aw:Name>\n" +
            "      <aw:Street>8 Oak Avenue</aw:Street>\n" +
            "      <aw:City>Old Town</aw:City>\n" +
            "      <aw:State>PA</aw:State>\n" +
            "      <aw:Zip>95819</aw:Zip>\n" +
            "      <aw:Country>USA</aw:Country>\n" +
            "    </aw:Address>\n" +
            "    <aw:DeliveryNotes>Please leave packages in shed by driveway.</aw:DeliveryNotes>\n" +
            "    <aw:Items>\n" +
            "      <aw:Item aw:PartNumber=\"898-AZ\">\n" +
            "        <aw:ProductName>Lawnmower</aw:ProductName>\n" +
            "        <aw:Quantity>1</aw:Quantity>\n" +
            "        <aw:USPrice>148.95</aw:USPrice>\n" +
            "        <aw:Comment>Confirm this is electric</aw:Comment>\n" +
            "      </aw:Item>\n" +
            "      <aw:Item aw:PartNumber=\"926-AA\">\n" +
            "        <aw:ProductName>Baby Monitor</aw:ProductName>\n" +
            "        <aw:Quantity>2</aw:Quantity>\n" +
            "        <aw:USPrice>39.98</aw:USPrice>\n" +
            "        <aw:ShipDate>1999-05-21</aw:ShipDate>\n" +
            "      </aw:Item>\n" +
            "    </aw:Items>\n" +
            "  </aw:PurchaseOrder>\n" +
            "  <aw:PurchaseOrder aw:PurchaseOrderNumber=\"99505\" aw:OrderDate=\"1999-10-22\">\n" +
            "    <aw:Address aw:Type=\"Shipping\">\n" +
            "      <aw:Name>Cristian Osorio</aw:Name>\n" +
            "      <aw:Street>456 Main Street</aw:Street>\n" +
            "      <aw:City>Buffalo</aw:City>\n" +
            "      <aw:State>NY</aw:State>\n" +
            "      <aw:Zip>98112</aw:Zip>\n" +
            "      <aw:Country>USA</aw:Country>\n" +
            "    </aw:Address>\n" +
            "    <aw:Address aw:Type=\"Billing\">\n" +
            "      <aw:Name>Cristian Osorio</aw:Name>\n" +
            "      <aw:Street>456 Main Street</aw:Street>\n" +
            "      <aw:City>Buffalo</aw:City>\n" +
            "      <aw:State>NY</aw:State>\n" +
            "      <aw:Zip>98112</aw:Zip>\n" +
            "      <aw:Country>USA</aw:Country>\n" +
            "    </aw:Address>\n" +
            "    <aw:DeliveryNotes>Please notify me before shipping.</aw:DeliveryNotes>\n" +
            "    <aw:Items>\n" +
            "      <aw:Item aw:PartNumber=\"456-NM\">\n" +
            "        <aw:ProductName>Power Supply</aw:ProductName>\n" +
            "        <aw:Quantity>1</aw:Quantity>\n" +
            "        <aw:USPrice>45.99</aw:USPrice>\n" +
            "      </aw:Item>\n" +
            "    </aw:Items>\n" +
            "  </aw:PurchaseOrder>\n" +
            "  <aw:PurchaseOrder aw:PurchaseOrderNumber=\"99504\" aw:OrderDate=\"1999-10-22\">\n" +
            "    <aw:Address aw:Type=\"Shipping\">\n" +
            "      <aw:Name>Jessica Arnold</aw:Name>\n" +
            "      <aw:Street>4055 Madison Ave</aw:Street>\n" +
            "      <aw:City>Seattle</aw:City>\n" +
            "      <aw:State>WA</aw:State>\n" +
            "      <aw:Zip>98112</aw:Zip>\n" +
            "      <aw:Country>USA</aw:Country>\n" +
            "    </aw:Address>\n" +
            "    <aw:Address aw:Type=\"Billing\">\n" +
            "      <aw:Name>Jessica Arnold</aw:Name>\n" +
            "      <aw:Street>4055 Madison Ave</aw:Street>\n" +
            "      <aw:City>Buffalo</aw:City>\n" +
            "      <aw:State>NY</aw:State>\n" +
            "      <aw:Zip>98112</aw:Zip>\n" +
            "      <aw:Country>USA</aw:Country>\n" +
            "    </aw:Address>\n" +
            "    <aw:Items>\n" +
            "      <aw:Item aw:PartNumber=\"898-AZ\">\n" +
            "        <aw:ProductName>Computer Keyboard</aw:ProductName>\n" +
            "        <aw:Quantity>1</aw:Quantity>\n" +
            "        <aw:USPrice>29.99</aw:USPrice>\n" +
            "        <aw:Comment>this thing breaks all the time</aw:Comment>\n" +
            "      </aw:Item>\n" +
            "      <aw:Item aw:PartNumber=\"898-AM\">\n" +
            "        <aw:ProductName>Wireless Mouse</aw:ProductName>\n" +
            "        <aw:Quantity>1</aw:Quantity>\n" +
            "        <aw:USPrice>14.99</aw:USPrice>\n" +
            "      </aw:Item>\n" +
            "    </aw:Items>\n" +
            "  </aw:PurchaseOrder>\n" +
            "</aw:PurchaseOrders>";
    private Document testDoc;
    private Node purchaseOrderNode;


    @Before
    public void setUp() throws Exception {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setCoalescing(true);
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        testDoc = documentBuilder.parse(new InputSource(new StringReader(testXml)));
        purchaseOrderNode = testDoc.getFirstChild();
    }

    @Test
    public void getFirstMatchingNode_shouldReturnValue() {
        Node resultNode = XmlUtils.getFirstMatchingChildNode(purchaseOrderNode, "aw:PurchaseOrder");
        assertThat(resultNode).isNotNull();
    }

    @Test
    public void getFirstMatchingChildNode_shouldFindNode() throws Exception {
        ArrayList<String> testList = new ArrayList<String>();
        testList.add("1999-10-22");
        Node resultNode = XmlUtils.getFirstMatchingChildNode(purchaseOrderNode, "aw:PurchaseOrder", "aw:OrderDate", testList);
        assertThat(resultNode).isNotNull();
        assertThat(resultNode.getNodeName()).isEqualTo("aw:PurchaseOrder");
        assertThat(resultNode.getAttributes().getNamedItem("aw:OrderDate").getNodeValue()).isEqualTo("1999-10-22");
    }

    @Test
    public void getFirstMatchingChildNode_withUnmatchedAttribute_shouldNotFindNode() throws Exception {
        ArrayList<String> testList = new ArrayList<String>();
        testList.add("1999-10-");
        Node resultNode = XmlUtils.getFirstMatchingChildNode(purchaseOrderNode, "aw:PurchaseOrder", "aw:OrderDate", testList);
        assertThat(resultNode).isNull();
    }

    @Test
    public void getMatchingChildNodes_withNullAttributeValues_shouldReturnMultiple() throws Exception {
        List<Node> results = XmlUtils.getMatchingChildNodes(purchaseOrderNode, "aw:PurchaseOrder", "aw:OrderDate", null);
        assertThat(results.size()).isEqualTo(3);
    }

    @Test
    public void getMatchingChildNodes_withEmptyAttributeValues_shouldReturnNone() {
        List<Node> results = XmlUtils.getMatchingChildNodes(purchaseOrderNode, "aw:PurchaseOrder", "aw:OrderDate", new ArrayList<String>());
        assertThat(results).isEmpty();
    }

    @Test
    public void getAttributeValue_shouldReturnCorrectValue() throws Exception {
        Node child = XmlUtils.getFirstMatchingChildNode(purchaseOrderNode, "aw:PurchaseOrder");

        String purchaseOrderNumber = XmlUtils.getAttributeValue(child, "aw:PurchaseOrderNumber");
        String orderDate = XmlUtils.getAttributeValue(child, "aw:OrderDate");

        assertThat(purchaseOrderNumber).isEqualTo("99503");
        assertThat(orderDate).isEqualTo("1999-10-20");
    }

    @Test
    public void getListFromDocument_shouldReturnCorrectValue() throws Exception {
        // Get all the "aw:PurchaseOrder" nodes. If any of them have an "aw:OrderDate" attribute, extract a Date.
        List<Date> orderDates = XmlUtils.getListFromDocument(testDoc, "aw:PurchaseOrder", "aw:OrderDate", null, new XmlUtils.NodeProcessor<Date>() {
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

            @Override
            public Date process(final Node node) {
                try {
                    return formatter.parse(node.getAttributes().getNamedItem("aw:OrderDate").getNodeValue());
                } catch (ParseException e) {
                    return null;
                }
            }
        });

        assertThat(orderDates.size()).isEqualTo(3);
        assertThat(orderDates.get(0).getYear()).isEqualTo(99);
        assertThat(orderDates.get(0).getMonth()).isEqualTo(9);
        assertThat(orderDates.get(0).getDate()).isEqualTo(20);

        assertThat(orderDates.get(1).getYear()).isEqualTo(99);
        assertThat(orderDates.get(1).getMonth()).isEqualTo(9);
        assertThat(orderDates.get(1).getDate()).isEqualTo(22);

        assertThat(orderDates.get(2).getYear()).isEqualTo(99);
        assertThat(orderDates.get(2).getMonth()).isEqualTo(9);
        assertThat(orderDates.get(2).getDate()).isEqualTo(22);
    }

    @Test
    public void getFirstMatchFromDocument_shouldReturnCorrectValue() throws Exception {
        // Get the first "aw:PurchaseOrder" nodes. If it has an "aw:OrderDate" attribute, extract a Date.
        Date orderDate = XmlUtils.getFirstMatchFromDocument(testDoc, "aw:PurchaseOrder", "aw:OrderDate", null, new XmlUtils.NodeProcessor<Date>() {
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

            @Override
            public Date process(final Node node) {
                try {
                    return formatter.parse(node.getAttributes().getNamedItem("aw:OrderDate").getNodeValue());
                } catch (ParseException e) {
                    return null;
                }
            }
        });

        assertThat(orderDate).isNotNull();
        assertThat(orderDate.getYear()).isEqualTo(99);
        assertThat(orderDate.getMonth()).isEqualTo(9);
        assertThat(orderDate.getDate()).isEqualTo(20);
    }

    @Test
    public void getStringDataAsList_shouldFindDeepNested() throws Exception {
        final List<String> strings = XmlUtils.getStringDataAsList(testDoc, "aw:Comment", null, null);
        assertThat(strings.size()).isEqualTo(2);
        assertThat(strings.get(0)).isEqualTo("Confirm this is electric");
        assertThat(strings.get(1)).isEqualTo("this thing breaks all the time");
    }

    @Test
    public void getFirstMatchingStringData_shouldFindFirstMatch() throws Exception {
        final String firstMatch = XmlUtils.getFirstMatchingStringData(testDoc, "aw:Comment", null, null);
        assertThat(firstMatch).isNotNull();
        assertThat(firstMatch).isEqualTo("Confirm this is electric");
    }
}
