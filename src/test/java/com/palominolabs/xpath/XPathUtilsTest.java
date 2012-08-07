package com.palominolabs.xpath;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.io.StringWriter;

import static com.palominolabs.xpath.XPathUtils.getXPathString;
import static com.palominolabs.xpath.XPathUtils.hasCssClass;
import static org.junit.Assert.assertEquals;

public class XPathUtilsTest {

    @Test
    public void testHasCssClassSimple() {
        assertEquals("contains(concat(' ', normalize-space(@class), ' '), ' foo ')", hasCssClass("foo"));
    }

    @Test
    public void testHasCssClassWithSpaces() {
        assertEquals("contains(concat(' ', normalize-space(@class), ' '), ' foo ')", hasCssClass(" foo      "));
    }

    @Test
    public void testHasCssClassWithQuotes() {
        // in practice this would never be a css class but at least we didn't break the xpath syntax!
        assertEquals("contains(concat(' ', normalize-space(@class), ' '), concat(' foo  \" ', \"'\", ' '))",
            hasCssClass(" foo  \" '  "));
    }

    @Test
    public void testXpathSearchForFixedStrings() throws ParserConfigurationException, XPathExpressionException,
        TransformerException {

        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        XPath xPath = XPathFactory.newInstance().newXPath();

        String[] fixedTestStrings = {
            "foo",                  // no quotes
            "\"foo",                // double quotes only
            "'foo",                 // single quotes only
            "'foo\"bar",            // both; double quotes in mid-string
            "'foo\"bar\"baz",       // multiple double quotes in mid-string
            "'foo\"",               // string ends with double quotes
            "'foo\"\"",             // string ends with run of double quotes
            "\"'foo",               // string begins with double quotes
            "\"\"'foo",             // string begins with run of double quotes
            "'foo\"\"bar",          // run of double quotes in mid-string
            "\"\"foo\"'\"bar\"\"",  // runs of double quotes at beginning, middle and end
            "''foo'\"'bar''",       // like above but with quotes swapped
            "'''''",
            "''\"'''",
            "\"\"'\"\"\""
        };
        for (String s : fixedTestStrings) {
            doXPathSearch(documentBuilder, transformer, xPath, s);
        }
    }

    @Test
    public void testXpathSearchForRandomStrings() throws TransformerException, XPathExpressionException,
        ParserConfigurationException {
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        XPath xPath = XPathFactory.newInstance().newXPath();

        for (int i = 0; i < 1000; i++) {
            String s = RandomStringUtils.random(16, "asdf'\"");
            doXPathSearch(documentBuilder, transformer, xPath, s);
        }
    }

    @Test
    public void testXPathStringPickSingleQuote() {
        assertEquals("concat('\"\"foo\"', \"'\", '\"bar\"\"')", getXPathString("\"\"foo\"'\"bar\"\""));
    }

    @Test
    public void testXPathStringPickDoubleQuote() {
        assertEquals("concat(\"''foo'\", '\"', \"'bar''\")", getXPathString("''foo'\"'bar''"));
    }

    private void doXPathSearch(DocumentBuilder documentBuilder, Transformer transformer, XPath xPath, String s) throws
        TransformerException, XPathExpressionException {
        Document document = documentBuilder.newDocument();
        Element root = document.createElement("root");
        root.setAttribute("attr", s);

        document.appendChild(root);

        StreamResult result = new StreamResult(new StringWriter());
        DOMSource source = new DOMSource(document);
        transformer.transform(source, result);
        String xmlString = result.getWriter().toString();

        String xpathStr = "//root[@attr = " + getXPathString(s) + "]";
        XPathExpression xPathExpression = xPath.compile(xpathStr);
        NodeList out = (NodeList) xPathExpression
            .evaluate(new InputSource(new StringReader(xmlString)), XPathConstants.NODESET);
        assertEquals("Didn't find " + s + " in " + xmlString + " with xpath " + xpathStr, 1, out.getLength());

        Node item = out.item(0);
        assertEquals(s, item.getAttributes().getNamedItem("attr").getNodeValue());
    }
}
