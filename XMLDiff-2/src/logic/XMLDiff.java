package logic;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.swtjar.SWTLoader;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import userinterface.Windows;

public class XMLDiff {
    private static final short TAG_TYPE = 6, TAG_NAME_DIF = 0, ATTR_COUNT = 1,
	    ATTR_DIF = 2, CHILD_COUNT = 3, TEXT_DIF = 5, CHILD_DIF = 4,
	    ATTR_COUNT_ZERO = 7, COMMENT_DIF = 8;

    public static Boolean ignoreWhitespace = true, isDifferent = false;
    public static Document doc1, doc2;
    public static String xmlFileName1 = "XML1", xmlFileName2 = "XML2";
    public static String xmlFilePath1 = "XML1", xmlFilePath2 = "XML2";
    public static File file1, file2;
    public static Node dif1, dif2;
    public static Stack<Node> differences = new Stack<Node>();
    public static Stack<Integer> differences_type = new Stack<Integer>();
    public static int differenceCount = 0;
    public static ArrayList<ArrayList<String>> attributeDifferences = null;
    public static ArrayList<ArrayList<Integer>> attributeDifferencesIndices = new ArrayList<ArrayList<Integer>>();
    public static String fontName = "Consolas";

    public static boolean ignoreComment = true;
    public static String errorMessage = "";

    public static void setXMLFiles(String x, int xmlnum) {
	try {
	    errorMessage = "";
	    DocumentBuilderFactory factory = DocumentBuilderFactory
		    .newInstance();
	    DocumentBuilder builder = factory.newDocumentBuilder();
	    builder.setErrorHandler(new SimpleErrorHandler());

	    if (xmlnum == 1) {
		xmlFilePath1 = x;
		file1 = new File(xmlFilePath1);
		xmlFileName1 = file1.getName();
		doc1 = builder.parse(file1);

	    } else {
		xmlFilePath2 = x;
		file2 = new File(xmlFilePath2);
		xmlFileName2 = file2.getName();
		doc2 = builder.parse(file2);
	    }
	} catch (ParserConfigurationException e) {
	    // e.printStackTrace();
	} catch (SAXException e) {
	    // e.printStackTrace();
	} catch (IOException e) {
	    // e.printStackTrace();
	}
    }

    public static void main(String[] args) throws Exception {

	ClassPathHack.addURL("org.eclipse.jface_3.10.0.v20130904-1154.jar");
	ClassPathHack
		.addURL("org.eclipse.core.commands_3.6.100.v20130515-1857.jar");
	ClassPathHack
		.addURL("org.eclipse.equinox.common_3.6.200.v20130402-1505.jar");
	ClassPathHack.addURL("org.eclipse.osgi_3.10.0.v20130912-1517.jar");
	ClassPathHack
		.addURL("org.eclipse.ui.workbench_3.105.100.v20130916-1807.jar");
	ClassPathHack.addURL("swing2swt.jar");
	fontName = getFontNameBasedOnOS();

	Display display = new Display();

	Shell shell = new Shell(display);
	shell.setLayout(new FillLayout());
	shell.setText("XML Difference");
	try {
	    shell.setImage(new Image(display, XMLDiff.class.getClassLoader()
		    .getResourceAsStream("images/Internet-xml-icon.png")));
	} catch (Exception e) {
	    shell.setImage(new Image(display, XMLDiff.class.getClassLoader()
		    .getResourceAsStream("Internet-xml-icon.png")));
	}
	shell.setLocation(0, 0);

	Windows window = new Windows(shell, SWT.FILL);
	window.setLocation(0, 0);

	shell.pack();
	shell.open();
	while (!shell.isDisposed()) {
	    if (!display.readAndDispatch())
		display.sleep();
	}
	display.dispose();
    }

    private static String getFontNameBasedOnOS() {
	String osName = System.getProperty("os.name").toLowerCase();
	String fontName = osName.contains("win") ? "Consolas" : osName
		.contains("mac") ? "Monaco" : osName.contains("linux")
		|| osName.contains("nix") ? "Monospace" : "";
	int fontSize = osName.contains("mac") ? 11 : 10;
	Windows.fontSize = fontSize;
	return fontName;
    }

    private static boolean isEqual(Node n1, Node n2) {
	boolean ret = true;

	if (n1.getNodeType() != n2.getNodeType()) {
	    pushDifference(n1, n2, TAG_TYPE);
	    return false;
	}

	if (n1.getNodeType() == Document.TEXT_NODE) {
	    ret = n1.getNodeValue().equals(n2.getNodeValue());
	    if (!ret) {
		pushDifference(n1, n2, TEXT_DIF);
	    }
	    return ret;
	}

	if (n1.getNodeType() == Document.COMMENT_NODE) {
	    ret = n1.getNodeValue().equals(n2.getNodeValue());
	    if (!ret) {
		pushDifference(n1, n2, COMMENT_DIF);
	    }
	    return ret;
	}

	if (n1.getNodeName() != n2.getNodeName()) {
	    pushDifference(n1, n2, TAG_NAME_DIF);
	    return false;
	}

	if (n1.hasAttributes() ^ n2.hasAttributes()) {
	    pushDifference(n1, n2, ATTR_COUNT_ZERO);
	    return false;
	} else if (n1.hasAttributes() && n2.hasAttributes()) {
	    if (n1.getAttributes().getLength() != n2.getAttributes()
		    .getLength()) {
		pushDifference(n1, n2, ATTR_COUNT);
		return false;
	    }
	    attributeDifferences = checkAttributes(n1, n2);
	    ret = ret && (attributeDifferences.get(0).size() == 0);
	    if (!ret) {
		pushDifference(n1, n2, ATTR_DIF);
		return ret;
	    }
	}

	if (ignoreWhitespace) {
	    removeWhitespaces(n1);
	    removeWhitespaces(n2);
	}
	if (ignoreComment) {
	    removeComment(n1);
	    removeComment(n2);
	}

	if (n1.getChildNodes().getLength() != n2.getChildNodes().getLength()) {
	    pushDifference(n1, n2, CHILD_COUNT);
	    return false;
	} else {
	    for (int i = 0; i < n1.getChildNodes().getLength(); i++) {
		ret = ret
			&& isEqual(n1.getChildNodes().item(i), n2
				.getChildNodes().item(i));

		if (!ret) {
		    pushDifference(n1, n2, CHILD_DIF, i);
		    break;
		}
	    }
	}

	return ret;
    }

    private static void removeComment(Node n1) {
	for (int i = 0; i < n1.getChildNodes().getLength(); i++) {
	    Node n = n1.getChildNodes().item(i);
	    if (n.getNodeType() == Document.COMMENT_NODE) {
		n1.removeChild(n);
	    }
	    if (n.hasChildNodes()) {
		removeComment(n);
	    }
	}
    }

    private static void removeWhitespaces(Node n1) {
	for (int i = 0; i < n1.getChildNodes().getLength(); i++) {
	    Node n = n1.getChildNodes().item(i);
	    if (n.getNodeType() == Document.TEXT_NODE
		    && n.getNodeValue().matches("\\s+")) {
		n1.removeChild(n);
	    }
	    if (n.hasChildNodes()) {
		removeWhitespaces(n);
	    }
	}
    }

    private static ArrayList<ArrayList<String>> checkAttributes(Node n1, Node n2) {
	ArrayList<ArrayList<String>> a = new ArrayList<ArrayList<String>>();
	ArrayList<String> a1 = new ArrayList<String>();
	ArrayList<String> a2 = new ArrayList<String>();

	for (int i = 0; i < n1.getAttributes().getLength(); i++) {
	    a1.add(n1.getAttributes().item(i).toString());
	    a2.add(n2.getAttributes().item(i).toString());
	}

	ArrayList<String> x1 = new ArrayList<String>(a1);
	ArrayList<String> x2 = new ArrayList<String>(a2);

	x1.removeAll(a2);
	x2.removeAll(a1);
	a.add(x1);
	a.add(x2);

	ArrayList<Integer> i1 = new ArrayList<Integer>();
	ArrayList<Integer> i2 = new ArrayList<Integer>();

	for (String s1 : x1) {
	    i1.add(a1.indexOf(s1));
	}
	for (String s2 : x2) {
	    i2.add(a2.indexOf(s2));
	}

	if (i1.size() > 0) {
	    attributeDifferencesIndices.add(i1);
	    attributeDifferencesIndices.add(i2);
	}

	return a;
    }

    private static void pushDifference(Node n1, Node n2,
	    int... typeOfDifferences) {
	if (typeOfDifferences[0] == CHILD_DIF)
	    differences_type.push(typeOfDifferences[1]);
	differences_type.push(typeOfDifferences[0]);
	differences.push(n2);
	differences.push(n1);
	if (typeOfDifferences[0] == ATTR_DIF
		|| typeOfDifferences[0] == ATTR_COUNT
		|| typeOfDifferences[0] == ATTR_COUNT_ZERO
		|| typeOfDifferences[0] == CHILD_COUNT
		|| typeOfDifferences[0] == TAG_NAME_DIF) {
	    differences_type.push(-1);
	    differences_type.push(new Integer(CHILD_DIF));
	    differences.push(n2);
	    differences.push(n1);
	}
    }

    private static Document buildXMLDifference()
	    throws ParserConfigurationException {
	Document doc = DocumentBuilderFactory.newInstance()
		.newDocumentBuilder().newDocument();

	Element difference = doc.createElement("differences");
	doc.appendChild(difference);
	Attr attrDif = doc.createAttribute("value");
	attrDif.setTextContent(isDifferent.toString());
	difference.setAttributeNode(attrDif);
	Node bottom = difference;

	if (isDifferent) {
	    Element leafDiff = doc.createElement("difference");
	    Attr diffCount = doc.createAttribute("no");
	    diffCount.setTextContent("" + differenceCount++);
	    leafDiff.setAttributeNode(diffCount);
	    bottom.appendChild(leafDiff);
	    bottom = leafDiff;
	}
	
	// if the two XMLs are different, enter the loop
	while (isDifferent) {
	    // differences is the stack containing the nodes where
	    // difference occurs
	    Node n1 = differences.pop();
	    Node n2 = differences.pop();
	    int typeOfDifference = differences_type.pop();

	    // while the stack is not empty
	    if (differences.size() != 0) {
		// create new <tag>
		Element newN = doc.createElement("tag");
		
		// create a series of <tag> inside <tag>
		// denoting the path to the difference position
		if (typeOfDifference == CHILD_DIF) {
		    int index_diff = differences_type.pop();
		    if (index_diff != -1) {
			Attr attr = doc.createAttribute("next_child_index");
			attr.setTextContent("" + index_diff);
			newN.setAttributeNode(attr);
		    }

		    Attr attrTagName = doc.createAttribute("_name");
		    attrTagName.setTextContent(n1.getNodeName());
		    newN.setAttributeNode(attrTagName);

		    // this is for handling difference in tag name
		    // i.e. there are two names in the final <tag>
		    if (!n1.getNodeName().equals(n2.getNodeName())) {
			newN.setAttribute("_name2", n2.getNodeName());
		    }
		}
		bottom.appendChild(newN);
		bottom = newN;

	    } else if (differences.size() == 0) { 
		// when the stack is empty, we create the leaf
		Element[] diffLeaves = createDifferenceLeaves(typeOfDifference,
			doc, n1, n2);
		bottom.appendChild(diffLeaves[0]);
		bottom.appendChild(diffLeaves[1]);
		break;
	    }
	}

	return doc;
    }

    private static Element[] createDifferenceLeaves(int diff, Document doc,
	    Node n1, Node n2) {
	Element e1 = doc.createElement("inDocument1");
	Element e2 = doc.createElement("inDocument2");
	e1.setAttribute("_name", xmlFileName1);
	e2.setAttribute("_name", xmlFileName2);
	Node t1 = null, t2 = null;
	Attr attr = doc.createAttribute("difference_type");

	switch (diff) {
	case TAG_TYPE:
	    t1 = doc.createTextNode(getNodeType(n1.getNodeType()));
	    t2 = doc.createTextNode(getNodeType(n2.getNodeType()));
	    attr.setTextContent("node_type");
	    break;
	case TEXT_DIF:
	    t1 = doc.createTextNode(n1.getNodeValue());
	    t2 = doc.createTextNode(n2.getNodeValue());
	    attr.setTextContent("text_value");
	    break;
	case COMMENT_DIF:
	    t1 = doc.createTextNode(n1.getNodeValue());
	    t2 = doc.createTextNode(n2.getNodeValue());
	    attr.setTextContent("comment_value");
	    break;
	case TAG_NAME_DIF:
	    t1 = doc.createTextNode(n1.getNodeName());
	    t2 = doc.createTextNode(n2.getNodeName());
	    attr.setTextContent("tag_name");
	    break;
	case CHILD_COUNT:
	    t1 = doc.createTextNode("" + n1.getChildNodes().getLength());
	    t2 = doc.createTextNode("" + n2.getChildNodes().getLength());
	    attr.setTextContent("number_of_children");
	    break;
	case ATTR_COUNT:
	    t1 = doc.createTextNode("" + n1.getAttributes().getLength());
	    t2 = doc.createTextNode("" + n2.getAttributes().getLength());
	    attr.setTextContent("number_of_attributes");
	    break;
	case ATTR_COUNT_ZERO:
	    if (n1.hasAttributes()) {
		t1 = doc.createTextNode("" + n1.getAttributes().getLength());
		t2 = doc.createTextNode("" + 0);
	    } else {
		t1 = doc.createTextNode("" + 0);
		t2 = doc.createTextNode("" + n2.getAttributes().getLength());
	    }
	    attr.setTextContent("number_of_attributes");
	    break;
	case ATTR_DIF:
	    t1 = doc.createElement("attributes");
	    t2 = doc.createElement("attributes");
	    Attr att1 = doc.createAttribute("attrcount");
	    Attr att2 = doc.createAttribute("attrcount");
	    att1.setNodeValue(attributeDifferences.get(0).size() + "");
	    att2.setNodeValue(attributeDifferences.get(1).size() + "");
	    ((Element) t1).setAttributeNode(att1);
	    ((Element) t2).setAttributeNode(att2);
	    Node[] ts = { t1, t2 };

	    for (int j = 0; j < 2; j++) {
		for (int i = 0; i < attributeDifferences.get(j).size(); i++) {
		    Element index = doc.createElement("attr");
		    Attr indexNo = doc.createAttribute("alphabetical_index");
		    indexNo.setNodeValue(""
			    + attributeDifferencesIndices.get(j).get(i));
		    index.setAttributeNode(indexNo);

		    String fullAttr = attributeDifferences.get(j).get(i);
		    String attrName = "a";
		    String attrVal = "v";

		    Pattern pattern = Pattern.compile("\"(.*?)\"");
		    Matcher matcher = pattern.matcher(fullAttr);
		    if (matcher.find())
			attrVal = matcher.group(1);

		    pattern = Pattern.compile("(.*?)=");
		    matcher = pattern.matcher(fullAttr);
		    if (matcher.find())
			attrName = matcher.group(1);

		    Element aName = doc.createElement("name");
		    Element aValue = doc.createElement("value");
		    aName.appendChild(doc.createTextNode(attrName));
		    aValue.appendChild(doc.createTextNode(attrVal));

		    index.appendChild(aName);
		    index.appendChild(aValue);

		    ts[j].appendChild(index);
		}
	    }
	    attr.setTextContent("attributes");
	    break;
	}
	e1.setAttributeNode(attr);
	e1.appendChild(t1);
	e2.appendChild(t2);
	return new Element[] { e1, e2 };
    }

    private static String getNodeType(short nodeType) {
	switch (nodeType) {
	case Document.ATTRIBUTE_NODE:
	    return "attribute node";
	case Document.COMMENT_NODE:
	    return "comment node";
	case Document.DOCUMENT_NODE:
	    return "document node";
	case Document.ELEMENT_NODE:
	    return "element node";
	case Document.TEXT_NODE:
	    return "text node";
	default:
	    return "unknown";
	}
    }

    public static void printDocument(Document doc) throws Exception {
	TransformerFactory tf = TransformerFactory.newInstance();

	Transformer transformer = tf.newTransformer();
	transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	transformer.setOutputProperty(
		"{http://xml.apache.org/xslt}indent-amount", "1");

	StreamResult res = new StreamResult(new StringWriter());
	DOMSource src = new DOMSource(doc);
	transformer.transform(src, res);
    }

    public static Document startComparing(String x1, String x2)
	    throws ParserConfigurationException, UnsupportedEncodingException,
	    SAXException, IOException {
	differenceCount = 0;
	Document doc = null;
	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	factory.setValidating(false);
	DocumentBuilder builder = factory.newDocumentBuilder();
	builder.setErrorHandler(new SimpleErrorHandler());

	doc1 = builder.parse(new InputSource(new ByteArrayInputStream(x1
		.getBytes("utf-8"))));
	doc2 = builder.parse(new InputSource(new ByteArrayInputStream(x2
		.getBytes("utf-8"))));

	isDifferent = !isEqual(doc1.getDocumentElement(),
		doc2.getDocumentElement());

	doc = buildXMLDifference();

	return doc;
    }

    public static Document startComparing()
	    throws ParserConfigurationException, SAXException, IOException {
	differenceCount = 0;
	Document doc = null;
	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	DocumentBuilder builder = factory.newDocumentBuilder();
	builder.setErrorHandler(new SimpleErrorHandler());

	doc1 = builder.parse(file1);
	doc2 = builder.parse(file2);

	isDifferent = !isEqual(doc1.getDocumentElement(),
		doc2.getDocumentElement());

	doc = buildXMLDifference();

	return doc;

    }

}

class SimpleErrorHandler implements ErrorHandler {

    public void warning(SAXParseException e) throws SAXException {
	XMLDiff.errorMessage += "WARNING: " + e.getMessage() + "\n";
    }

    public void error(SAXParseException e) throws SAXException {
	XMLDiff.errorMessage = "ERROR: " + e.getMessage() + "\n";
    }

    public void fatalError(SAXParseException e) throws SAXException {
	XMLDiff.errorMessage = "FATAL ERROR: " + e.getMessage() + "\n";
    }

}

class ClassPathHack {

    public static void addURL(String u) throws IOException {
	String swtFileName = u;
	try {
	    URLClassLoader cl = (URLClassLoader) SWTLoader.class
		    .getClassLoader();
	    Method addUrlMethod = URLClassLoader.class.getDeclaredMethod(
		    "addURL", URL.class);
	    addUrlMethod.setAccessible(true);

	    URL swtFileUrl = new URL("rsrc:" + swtFileName);
	    addUrlMethod.invoke(cl, swtFileUrl);

	    return;
	} catch (Exception exx) {
	    // throw new Exception(exx.getClass().getSimpleName() + ": " +
	    // exx.getMessage());
	}
    }
}
