package userinterface;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import logic.UniqueBoundedStack;
import logic.XMLDiff;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.wb.swt.SWTResourceManager;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.eclipse.swt.widgets.Spinner;

import etinyplugins.commons.swt.UndoRedoImpl;

import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;

public class Windows extends Composite {
    private CCombo xml1_path;
    private CCombo xml2_path;
    private StyledText xml1_text;
    private StyledText xml2_text;
    private StyledText xml_diff;
    private Label xml1_label;
    private Label xml2_label;
    private StyledText terminal_text;
    private Boolean xml1_edited = false, xml2_edited = false;
    private Boolean xml1_browsed = false, xml2_browsed = false;
    private Boolean xml1_focused = false, xml2_focused = false;
    private Boolean syntaxColoring = true;
    Color orange = new Color(getDisplay(), 255, 127, 0);
    Color comment_color = orange;
    Color lime = new Color(getDisplay(), 127, 255, 127);
    Color pink = new Color(getDisplay(), 204, 0, 152);
    Color blue = new Color(getDisplay(), 37, 73, 183);
    Color tag_color = blue;
    Color purple = new Color(getDisplay(), 102, 0, 102);
    Color attrName_color = purple;
    Color green = new Color(getDisplay(), 25, 108, 26);
    Color attrValue_color = green;
    Color black = new Color(getDisplay(), 0, 0, 0);
    Color highlight_color = lime;
    public static int fontSize = 10;
    UniqueBoundedStack<String> recentFile = new UniqueBoundedStack<String>(5);
    private UndoRedoImpl xml1_undoredo;
    private UndoRedoImpl xml2_undoredo;
    private Button xml2_expander;
    private Button xml1_expander;
    private Button xml1_fileBrowseButton;
    private Button xml2_fileBrowseButton;
    private Composite composite;
    String textFromDialog;
    private Button btnSyntaxColoring;
    private Button btnColorScheme;

    /**
     * Create the composite.
     * 
     * @param parent
     * @param style
     */
    public Windows(Composite parent, int style) {
	super(parent, SWT.NONE);
	GridLayout gridLayout = new GridLayout();
	gridLayout.numColumns = 9;
	gridLayout.marginLeft = 5;
	gridLayout.marginBottom = 5;
	gridLayout.marginTop = 5;
	gridLayout.marginRight = 5;
	setLayout(gridLayout);

	/* XML1 label and expander */
	xml1_label = new Label(this, SWT.NONE);
	xml1_label.setText("XML1");
	GridData gridData = new GridData(GridData.FILL, GridData.CENTER, true,
		false);
	gridData.horizontalSpan = 1;
	xml1_label.setLayoutData(gridData);

	xml1_expander = new Button(this, SWT.ARROW);
	xml1_expander.addSelectionListener(new SelectionAdapter() {

	    @Override
	    public void widgetSelected(SelectionEvent e) {
		(new TextEditorDialog(getShell(), xml1_label.getText(),
			xml1_text.getText(), xml1_text, 1)).open();

	    }
	});
	xml1_expander.setToolTipText("expand XML1 text editor");
	gridData = new GridData(GridData.END, GridData.CENTER, false, false);
	gridData.horizontalSpan = 1;
	xml1_expander.setLayoutData(gridData);

	/* XML2 label and expander */
	xml2_label = new Label(this, SWT.NONE);
	xml2_label.setText("XML2");
	gridData = new GridData(GridData.FILL, GridData.CENTER, true, false);
	gridData.horizontalSpan = 1;
	xml2_label.setLayoutData(gridData);

	xml2_expander = new Button(this, SWT.ARROW);
	xml2_expander.addSelectionListener(new SelectionAdapter() {

	    @Override
	    public void widgetSelected(SelectionEvent e) {
		(new TextEditorDialog(getShell(), xml2_label.getText(),
			xml2_text.getText(), xml2_text, 2)).open();

	    }

	});
	xml2_expander.setToolTipText("expand XML2 text editor");
	gridData = new GridData(GridData.END, GridData.CENTER, false, false);
	gridData.horizontalSpan = 1;
	xml2_expander.setLayoutData(gridData);

	/* INPUT OUTPUT SEPARATOR */
	Label input_output_separator = new Label(this, SWT.SEPARATOR);
	input_output_separator.setBounds(625, 13, 13, 599);
	gridData = new GridData(GridData.CENTER, GridData.FILL, false, true);
	gridData.verticalSpan = 6;
	input_output_separator.setLayoutData(gridData);

	/* ignore whitespace */
	Button whitespace_check = new Button(this, SWT.CHECK);
	whitespace_check.setSelection(true);
	whitespace_check.addSelectionListener(new SelectionAdapter() {
	    @Override
	    public void widgetSelected(SelectionEvent e) {
		XMLDiff.ignoreWhitespace = !XMLDiff.ignoreWhitespace;
	    }
	});
	whitespace_check.setText("Ignore whitespace");
	gridData = new GridData(GridData.BEGINNING, GridData.CENTER, false,
		false);
	whitespace_check.setLayoutData(gridData);

	/* ignore comment */
	Button comment_check = new Button(this, SWT.CHECK);
	comment_check.addSelectionListener(new SelectionAdapter() {
	    @Override
	    public void widgetSelected(SelectionEvent e) {
		XMLDiff.ignoreComment = !XMLDiff.ignoreComment;
	    }
	});
	comment_check.setText("Ignore comment");
	comment_check.setSelection(true);
	gridData = new GridData(GridData.BEGINNING, GridData.CENTER, false,
		false);
	comment_check.setLayoutData(gridData);

	/* highlight separator */
	Label color_highlight_separator = new Label(this, SWT.SEPARATOR
		| SWT.SHADOW_OUT);
	gridData = new GridData(GridData.CENTER, GridData.FILL, false, false);
	gridData.heightHint = comment_check.getSize().y;
	color_highlight_separator.setLayoutData(gridData);

	/* highlight button */
	Button highlight_check = new Button(this, SWT.CHECK);
	highlight_check.addSelectionListener(new SelectionAdapter() {
	    @Override
	    public void widgetSelected(SelectionEvent arg0) {
	    }
	});
	highlight_check.setText("Show highlight");
	gridData = new GridData(GridData.FILL, GridData.CENTER, true, false);
	highlight_check.setLayoutData(gridData);
	highlight_check.setVisible(false);

	/* XML 1 Textarea */
	xml1_text = new StyledText(this, SWT.BORDER | SWT.H_SCROLL
		| SWT.V_SCROLL);
	xml1_text.addFocusListener(new FocusAdapter() {
	    @Override
	    public void focusGained(FocusEvent e) {
		xml1_focused = true;
	    }

	    @Override
	    public void focusLost(FocusEvent e) {
		xml1_focused = false;
	    }
	});
	xml1_text.addModifyListener(new ModifyListener() {
	    public void modifyText(ModifyEvent e) {
		if (xml1_browsed && (xml1_focused)) {
		    if (xml1_undoredo.hasUndo()
			    || !xml1_label.getText().toString().endsWith("*")) {
			//System.out.println("has undo");
			xml1_edited = true;
			if (!xml1_label.getText().toString().endsWith("*"))
			    xml1_label.setText(xml1_label.getText().toString()
				    + "*");
		    } else {
			//System.out.println("has no undo");
			xml1_edited = false;
			if (xml1_label.getText().toString().endsWith("*"))
			    xml1_label.setText(xml1_label.getText().substring(
				    0, xml1_label.getText().length() - 1));
		    }
		}
		if (syntaxColoring)
		    adjustColor(xml1_text);
	    }
	});
	xml1_text.setFont(SWTResourceManager.getFont(XMLDiff.fontName, 10,
		SWT.NORMAL));
	gridData = new GridData(GridData.FILL, GridData.FILL, true, true);
	gridData.heightHint = 428;
	gridData.widthHint = 301;
	gridData.horizontalSpan = 2;
	xml1_text.setLayoutData(gridData);

	/* XML2 Text Area */
	xml2_text = new StyledText(this, SWT.BORDER | SWT.H_SCROLL
		| SWT.V_SCROLL);
	xml2_text.addFocusListener(new FocusAdapter() {
	    @Override
	    public void focusGained(FocusEvent e) {
		xml2_focused = true;
	    }

	    @Override
	    public void focusLost(FocusEvent e) {
		xml2_focused = false;
	    }
	});
	xml2_text.addModifyListener(new ModifyListener() {
	    public void modifyText(ModifyEvent e) {
		if (xml2_browsed && xml2_focused) {
		    if (xml2_undoredo.hasUndo()
			    || !xml2_label.getText().toString().endsWith("*")) {
			xml2_edited = true;
			if (!xml2_label.getText().toString().endsWith("*"))
			    xml2_label.setText(xml2_label.getText().toString()
				    + "*");
		    } else {
			xml2_edited = false;
			if (xml2_label.getText().toString().endsWith("*"))
			    xml2_label.setText(xml2_label.getText().substring(
				    0, xml2_label.getText().length() - 1));
		    }
		}
		if (syntaxColoring)
		    adjustColor(xml2_text);

	    }
	});
	xml2_text.setFont(SWTResourceManager.getFont(XMLDiff.fontName, 10,
		SWT.NORMAL));
	gridData = new GridData(GridData.FILL, GridData.FILL, true, true);
	gridData.heightHint = 428;
	gridData.widthHint = 301;
	gridData.horizontalSpan = 2;
	xml2_text.setLayoutData(gridData);

	/* XML Output Text */
	xml_diff = new StyledText(this, SWT.BORDER | SWT.H_SCROLL
		| SWT.V_SCROLL);
	xml_diff.addKeyListener(new KeyAdapter() {
		@Override
		public void keyPressed(KeyEvent e) {
		    if(e.stateMask == SWT.CTRL && e.keyCode == 'a'){     
		            xml_diff.selectAll();
		          }
		}
	});
	xml_diff.addModifyListener(new ModifyListener() {
	    public void modifyText(ModifyEvent e) {
		if (syntaxColoring)
		    adjustColor(xml_diff);
	    }
	});
	xml_diff.setEditable(false);
	xml_diff.setFont(SWTResourceManager.getFont(XMLDiff.fontName, 10,
		SWT.NORMAL));
	gridData = new GridData(GridData.FILL, GridData.FILL, true, true);
	gridData.horizontalSpan = 4;
	gridData.verticalSpan = 4;
	xml_diff.setLayoutData(gridData);

	/* XML 1 path and browse */
	xml1_path = new CCombo(this, SWT.BORDER | SWT.READ_ONLY);
	xml1_path.addSelectionListener(new SelectionAdapter() {
	    @Override
	    public void widgetSelected(SelectionEvent e) {
		String path = xml1_path.getText();
		if (path != null) {
		    File file = new File(path);
		    if (file.isFile())
			displayFiles(new String[] { file.toString() }, 1);
		    else
			displayFiles(file.list(), 1);
		}
	    }
	});
	gridData = new GridData(GridData.FILL, GridData.CENTER, true, false);
	xml1_path.setLayoutData(gridData);

	xml1_fileBrowseButton = new Button(this, SWT.PUSH);
	xml1_fileBrowseButton.addSelectionListener(new SelectionAdapter() {
	    @Override
	    public void widgetSelected(SelectionEvent e) {
		FileDialog dialog = new FileDialog(getShell(), SWT.NULL);
		String path = dialog.open();
		if (path != null) {
		    File file = new File(path);
		    if (file.isFile())
			displayFiles(new String[] { file.toString() }, 1);
		    else
			displayFiles(file.list(), 1);
		}
	    }
	});
	xml1_fileBrowseButton.setText("Browse..");
	gridData = new GridData(GridData.END, GridData.CENTER, false, false);
	xml1_fileBrowseButton.setLayoutData(gridData);

	/* XML 2 Path and browse */
	xml2_path = new CCombo(this, SWT.BORDER | SWT.READ_ONLY);
	xml2_path.addSelectionListener(new SelectionAdapter() {
	    @Override
	    public void widgetSelected(SelectionEvent e) {
		String path = xml2_path.getText();
		if (path != null) {
		    File file = new File(path);
		    if (file.isFile())
			displayFiles(new String[] { file.toString() }, 2);
		    else
			displayFiles(file.list(), 2);
		}
	    }
	});
	gridData = new GridData(GridData.FILL, GridData.CENTER, true, false);
	xml2_path.setLayoutData(gridData);

	xml2_fileBrowseButton = new Button(this, SWT.PUSH);
	xml2_fileBrowseButton.addSelectionListener(new SelectionAdapter() {
	    @Override
	    public void widgetSelected(SelectionEvent e) {
		FileDialog dialog = new FileDialog(getShell(), SWT.NULL);
		String path = dialog.open();
		if (path != null) {
		    File file = new File(path);
		    if (file.isFile())
			displayFiles(new String[] { file.toString() }, 2);
		    else
			displayFiles(file.list(), 2);
		}
	    }
	});
	xml2_fileBrowseButton.setText("Browse..");
	gridData = new GridData(GridData.END, GridData.CENTER, false, false);
	xml2_fileBrowseButton.setLayoutData(gridData);

	/* font size label and spinner */

	composite = new Composite(this, SWT.NONE);
	composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false,
		4, 1));
	composite.setLayout(new GridLayout(4, false));
	Label lblFontSize = new Label(composite, SWT.NONE);
	lblFontSize.setText("Font size:");

	final Spinner fontSizeSpinner = new Spinner(composite, SWT.BORDER);
	fontSizeSpinner.addModifyListener(new ModifyListener() {
	    public void modifyText(ModifyEvent e) {
		fontSize = fontSizeSpinner.getSelection();
		FontData[] fD = xml1_text.getFont().getFontData();
		fD[0].setHeight(fontSize);
		xml1_text.setFont(new Font(getDisplay(), fD[0]));
		xml2_text.setFont(new Font(getDisplay(), fD[0]));
		xml_diff.setFont(new Font(getDisplay(), fD[0]));
		terminal_text.setFont(new Font(getDisplay(), fD[0]));
	    }
	});

	btnSyntaxColoring = new Button(composite, SWT.CHECK);
	btnSyntaxColoring.setSelection(true);
	btnSyntaxColoring.addSelectionListener(new SelectionAdapter() {
	    @Override
	    public void widgetSelected(SelectionEvent e) {
		syntaxColoring = !syntaxColoring;
		if (syntaxColoring) {
		    adjustAllColor();
		} else {
		    setAllToBlack();
		}
	    }
	});
	btnSyntaxColoring.setText("Syntax Coloring");

	btnColorScheme = new Button(composite, SWT.NONE);
	btnColorScheme.addSelectionListener(new SelectionAdapter() {
	    @Override
	    public void widgetSelected(SelectionEvent arg0) {
		(new ColorSchemeDialog(getShell())).open();
	    }
	});
	btnColorScheme.setText("Color Scheme");

	/* terminal separator */
	Label terminal_separator = new Label(this, SWT.SEPARATOR
		| SWT.HORIZONTAL | SWT.SHADOW_IN);
	gridData = new GridData(GridData.FILL, GridData.CENTER, true, false, 4,
		1);
	terminal_separator.setLayoutData(gridData);

	/* terminal text */
	terminal_text = new StyledText(this, SWT.BORDER);
	terminal_text.setEnabled(false);
	terminal_text.setEditable(false);
	terminal_text.setFont(SWTResourceManager.getFont(XMLDiff.fontName, 10,
		SWT.NORMAL));
	gridData = new GridData(GridData.FILL, GridData.FILL, true, false, 4, 1);
	gridData.heightHint = 83;
	gridData.widthHint = 614;
	terminal_text.setLayoutData(gridData);
	fontSizeSpinner.setValues(10, 8, 60, 0, 1, 1);
	/* button generate differences */
	Button btnGenerateDifferences = new Button(this, SWT.NONE);
	btnGenerateDifferences.addSelectionListener(new SelectionAdapter() {
	    @Override
	    public void widgetSelected(SelectionEvent e) {
		int result = 1;
		if (xml1_edited || xml2_edited) {
		    //System.out.println("here");
		    MessageDialog dialog = new MessageDialog(
			    getShell(),
			    "XML has been edited.",
			    null,
			    "You've edited an xml previously loaded from a file. Do you want to use the original xml file or the one you have edited?",
			    MessageDialog.QUESTION, new String[] {
				    "Original one", "Edited one" }, 0);
		    result = dialog.open();
		}

		Document doc;
		if (result == 1) {
		    if (xml1_text.getText().isEmpty()
			    || xml2_text.getText().isEmpty()) {
			terminal_text
				.setText("You haven't choose one of the xml files.");
		    } else {
			if (xml1_edited) {
			    xml1_browsed = xml1_edited = false;
			    XMLDiff.xmlFileName1 = "XML1";
			    xml1_label.setText("XML1");
			    xml1_path.setText("");
			}
			if (xml2_edited) {
			    xml2_browsed = xml2_edited = false;
			    XMLDiff.xmlFileName2 = "XML2";
			    xml2_label.setText("XML2");
			    xml2_path.setText("");
			}

			try {
			    XMLDiff.attributeDifferencesIndices.clear();
			    doc = XMLDiff
				    .startComparing(xml1_text.getText()
					    .toString(), xml2_text.getText()
					    .toString());
			    printDocument(doc);
			} catch (ParserConfigurationException e1) {
			    terminal_text
				    .setText("ERROR: Cannot parse XML. Check your syntax.");
			    //e1.printStackTrace();
			} catch (TransformerException e1) {
			    terminal_text
				    .setText("ERROR: Trouble building final XML Difference. Please try again.");
			    //e1.printStackTrace();
			} catch (IOException e1) {
			    terminal_text
				    .setText("ERROR: Problem opening XML file/directory.");
			    //e1.printStackTrace();
			} catch (DOMException e1) {
			    terminal_text
				    .setText("ERROR: Illegal character in XML. Please check your syntax.");
			} catch (SAXException e1) {
			    terminal_text
			    .setText("ERROR: Cannot parse XML. Check your syntax.");
		    //e1.printStackTrace();
			}
		    }
		} else {
		    if (XMLDiff.xmlFileName1.equals("XML1")
			    || XMLDiff.xmlFileName2.equals("XML2")) {
			terminal_text
				.setText("ERROR: You haven't choose one of the xml files.");
		    } else {
			try {
			    if (xml1_edited) {
				xml1_text.setText(readFile(
					XMLDiff.xmlFilePath1,
					StandardCharsets.UTF_8));
				if (xml1_label.getText().endsWith("*"))
				    xml1_label.setText(xml1_label.getText()
					    .substring(
						    0,
						    xml1_label.getText()
							    .length() - 1));
			    }
			    if (xml2_edited) {
				xml2_text.setText(readFile(
					XMLDiff.xmlFilePath2,
					StandardCharsets.UTF_8));
				if (xml2_label.getText().endsWith("*"))
				    xml2_label.setText(xml2_label.getText()
					    .substring(
						    0,
						    xml2_label.getText()
							    .length() - 1));
			    }

			    xml1_edited = xml2_edited = false;

			    doc = XMLDiff.startComparing();
			    printDocument(doc);
			} catch (ParserConfigurationException e1) {
			    terminal_text
				    .setText("ERROR: Cannot parse XML. Check your syntax.");
			    //e1.printStackTrace();
			} catch (TransformerException e1) {
			    terminal_text
				    .setText("ERROR: Trouble building final XML Difference. Please try again.");
			    //e1.printStackTrace();
			} catch (IOException e1) {
			    terminal_text
				    .setText("ERROR: Problem opening XML file/directory.");
			    //e1.printStackTrace();
			} catch (DOMException e1) {
			    terminal_text
				    .setText("ERROR: Illegal character in XML. Please check your syntax.");
			} catch (SAXException e1) {
			    terminal_text
			    .setText("ERROR: Cannot parse XML. Check your syntax.");
		    //e1.printStackTrace();
			}
		    }
		}
	    }
	});
	btnGenerateDifferences.setFont(SWTResourceManager.getFont("Segoe UI",
		9, SWT.BOLD));
	btnGenerateDifferences.setForeground(SWTResourceManager
		.getColor(SWT.COLOR_WIDGET_FOREGROUND));
	btnGenerateDifferences.setText("Generate differences!");
	gridData = new GridData(GridData.BEGINNING, GridData.BEGINNING, false,
		false, 4, 1);
	btnGenerateDifferences.setLayoutData(gridData);

	xml1_undoredo = new UndoRedoImpl(xml1_text);
	xml2_undoredo = new UndoRedoImpl(xml2_text);
    }

    protected void adjustColor(StyledText xml_text, Color tag, Color attrName,
	    Color attrValue, Color comment, Color highlihgt) {
	Pattern pattern = Pattern.compile("<.*?>");
	Matcher matcher = pattern.matcher(xml_text.getText());
	while (matcher.find()) {
	    StyleRange styleRange = new StyleRange();
	    styleRange.start = matcher.start();
	    styleRange.length = matcher.end() - matcher.start();
	    styleRange.foreground = tag;
	    xml_text.setStyleRange(styleRange);
	}

	pattern = Pattern.compile(" \\S*?=");
	matcher = pattern.matcher(xml_text.getText());
	while (matcher.find()) {
	    StyleRange styleRange = new StyleRange();
	    styleRange.start = matcher.start();
	    styleRange.length = matcher.end() - matcher.start();
	    styleRange.foreground = attrName;
	    styleRange.fontStyle = SWT.BOLD;
	    xml_text.setStyleRange(styleRange);
	}

	pattern = Pattern.compile("\".*?\"");
	matcher = pattern.matcher(xml_text.getText());
	while (matcher.find()) {
	    StyleRange styleRange = new StyleRange();
	    styleRange.start = matcher.start();
	    styleRange.length = matcher.end() - matcher.start();
	    styleRange.foreground = attrValue;
	    styleRange.fontStyle = SWT.ITALIC;
	    xml_text.setStyleRange(styleRange);
	}

	pattern = Pattern.compile("<!--.*?-->");
	matcher = pattern.matcher(xml_text.getText());
	while (matcher.find()) {
	    StyleRange styleRange = new StyleRange();
	    styleRange.start = matcher.start();
	    styleRange.length = matcher.end() - matcher.start();
	    styleRange.foreground = comment;
	    xml_text.setStyleRange(styleRange);
	}
    }

    protected void adjustColor(StyledText xml_text) {
	adjustColor(xml_text, tag_color, attrName_color, attrValue_color,
		comment_color, highlight_color);
    }

    protected void adjustAllColor() {
	adjustColor(xml1_text);
	adjustColor(xml2_text);
	adjustColor(xml_diff);
    }

    protected void setToBlack(StyledText xml_text) {

	if (xml_text.getText().length() > 0) {
	    StyleRange styleRange = new StyleRange();
	    styleRange.start = 0;
	    styleRange.length = xml_text.getText().length() - 1;
	    styleRange.foreground = black;
	    xml_text.setStyleRange(styleRange);
	}

    }

    protected void setAllToBlack() {
	setToBlack(xml1_text);
	setToBlack(xml2_text);
	setToBlack(xml_diff);

    }

    @Override
    protected void checkSubclass() {
	// Disable the check that prevents subclassing of SWT components
    }

    public void displayFiles(String[] files, int xmlnum) {
	CCombo tpath;
	StyledText txml;
	Label lxml;
	UndoRedoImpl xml_undoredo;
	StringBuffer eMessage = new StringBuffer();
	String tempPath;

	if (xmlnum == 1) {
	    tpath = xml1_path;
	    txml = xml1_text;
	    lxml = xml1_label;
	    xml1_browsed = true;
	    xml1_edited = false;
	    xml_undoredo = xml1_undoredo;
	    tempPath = xml2_path.getText();
	} else {
	    tpath = xml2_path;
	    txml = xml2_text;
	    lxml = xml2_label;
	    xml2_browsed = true;
	    xml2_edited = false;
	    xml_undoredo = xml2_undoredo;
	    tempPath = xml1_path.getText();
	}

	for (int i = 0; files != null && i < files.length; i++) {

	    try {
		txml.setText(readFile(files[i], StandardCharsets.UTF_8));
		String filename = (new File(files[i])).getName();
		lxml.setText(filename);

		if (!filename.endsWith(".xml")) {
		    eMessage.append("WARNING: " + filename
			    + " is not an xml file.\n");
		}

		XMLDiff.setXMLFiles(files[i], xmlnum);
		xml_undoredo.clearUndoRedo();
		recentFile.push(files[i]);
		xml1_path.setItems(recentFile
			.toRevertArray(new String[recentFile.size()]));
		xml2_path.setItems(recentFile
			.toRevertArray(new String[recentFile.size()]));
		tpath.setText(files[i]);
		if (xmlnum == 1)
		    xml2_path.setText(tempPath);
		else
		    xml1_path.setText(tempPath);
		terminal_text.setText(eMessage.toString());

	    } catch (IOException e) {
		//e.printStackTrace();
	    }
	}
    }

    String readFile(String path, Charset encoding) throws IOException {
	byte[] encoded = Files.readAllBytes(Paths.get(path));
	return encoding.decode(ByteBuffer.wrap(encoded)).toString();
    }

    void printDocument(Document doc) throws TransformerException {

	TransformerFactory tf = TransformerFactory.newInstance();

	Transformer transformer = tf.newTransformer();
	transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	transformer.setOutputProperty(
		"{http://xml.apache.org/xslt}indent-amount", "1");

	StreamResult res = new StreamResult(new StringWriter());
	DOMSource src = new DOMSource(doc);
	transformer.transform(src, res);

	xml_diff.setText(res.getWriter().toString());
	int i = 0;
	StringTokenizer st = new StringTokenizer(xml_diff.getText(), "\n");

	while (st.hasMoreTokens()) {
	    if (st.nextToken().contains("difference_type"))
		break;
	    i++;
	}
	xml_diff.setLineBackground(i, 1, highlight_color);

	if (XMLDiff.isDifferent) {
	    terminal_text.setText("The 2 xmls are different.");
	} else {
	    terminal_text.setText("The 2 xmls are equal.");
	}

    }

    class TextEditorDialog extends TitleAreaDialog {

	private StyledText textEditor;
	private StyledText source;

	private String title;
	private String initialText;
	private int srcCode;

	public TextEditorDialog(Shell parentShell, String title,
		String initialText, StyledText source, int srcCode) {
	    super(parentShell);
	    this.title = title;
	    this.initialText = initialText;
	    this.source = source;
	    this.srcCode = srcCode;
	}

	@Override
	public void create() {
	    super.create();
	    setTitle(title);
	    setMessage("Edit your XML here", IMessageProvider.INFORMATION);
	    Image img;
	    try {
		img = new Image(this.getShell().getDisplay(), XMLDiff.class
			.getClassLoader().getResourceAsStream(
				"images/text-editor.png"));
	    } catch (Exception e) {
		img = new Image(this.getShell().getDisplay(), XMLDiff.class
			.getClassLoader()
			.getResourceAsStream("text-editor.png"));
	    }
	    ImageData imgData = img.getImageData().scaledTo(75, 75);
	    img = new Image(this.getShell().getDisplay(), imgData);
	    this.setTitleImage(img);
	    this.getShell().setImage(img);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
	    Composite area = (Composite) super.createDialogArea(parent);
	    Composite container = new Composite(area, SWT.NONE);
	    container.setLayoutData(new GridData(GridData.FILL_BOTH));
	    GridLayout layout = new GridLayout(2, false);
	    container
		    .setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	    container.setLayout(layout);

	    toolBarArea(container);
	    textEditorArea(container);

	    return area;
	}

	private void textEditorArea(Composite container) {
	    textEditor = new StyledText(container, SWT.BORDER | SWT.H_SCROLL
		    | SWT.V_SCROLL);
	    textEditor.setFont(SWTResourceManager.getFont(XMLDiff.fontName, 10,
		    SWT.NORMAL));
	    GridData gridData = new GridData(GridData.FILL, GridData.FILL,
		    true, true);
	    gridData.horizontalSpan = 2;
	    textEditor.setLayoutData(gridData);
	    textEditor.setText(initialText);
	    textEditor.setFont(SWTResourceManager.getFont(XMLDiff.fontName,
		    fontSize, SWT.NORMAL));
	    if (syntaxColoring) {
		adjustColor(textEditor);
	    }
	    textEditor.addModifyListener(new ModifyListener() {
		public void modifyText(ModifyEvent e) {
		    if (syntaxColoring) {
			adjustColor(textEditor);
		    }
		}
	    });
	    new UndoRedoImpl(textEditor);
	}

	private void toolBarArea(Composite container) {
	    Composite composite = new Composite(container, SWT.NONE);
	    composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false,
		    false, 4, 1));
	    composite.setLayout(new GridLayout(3, false));
	    Label lblFontSize = new Label(composite, SWT.NONE);
	    lblFontSize.setText("Font size:");

	    final Spinner fontSizeSpinner = new Spinner(composite, SWT.BORDER);
	    fontSizeSpinner.addModifyListener(new ModifyListener() {
		public void modifyText(ModifyEvent e) {
		    fontSize = fontSizeSpinner.getSelection();
		    FontData[] fD = xml1_text.getFont().getFontData();
		    fD[0].setHeight(fontSize);
		    xml1_text.setFont(new Font(getDisplay(), fD[0]));
		    xml2_text.setFont(new Font(getDisplay(), fD[0]));
		    xml_diff.setFont(new Font(getDisplay(), fD[0]));
		    terminal_text.setFont(new Font(getDisplay(), fD[0]));
		    if (textEditor != null)
			textEditor.setFont(new Font(getDisplay(), fD[0]));
		}
	    });
	    fontSizeSpinner.setValues(fontSize, 8, 60, 0, 1, 1);

	    final Button btnSyntaxColoring = new Button(composite, SWT.CHECK);
	    btnSyntaxColoring.setSelection(true);
	    btnSyntaxColoring.addSelectionListener(new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent e) {
		    syntaxColoring = !syntaxColoring;
		    if (syntaxColoring) {
			adjustAllColor();
			adjustColor(textEditor);

		    } else {
			setAllToBlack();
			setToBlack(textEditor);
		    }
		    btnSyntaxColoring.setSelection(syntaxColoring);
		}
	    });
	    btnSyntaxColoring.setText("Syntax Coloring");
	}

	@Override
	protected boolean isResizable() {
	    return true;
	}

	// save content of the Text fields because they get disposed
	// as soon as the Dialog closes
	private void saveInput() {
	    textFromDialog = textEditor.getText();
	    if (!(source.getText().hashCode() == textFromDialog.hashCode())) {
		source.setText(textFromDialog);
		if (srcCode == 1 && xml1_browsed) {
		    if (!xml1_label.getText().toString().endsWith("*"))
			xml1_label.setText(xml1_label.getText().toString()
				+ "*");
		    xml1_edited = true;
		} else if (srcCode == 2 && xml2_browsed) {
		    if (!xml2_label.getText().toString().endsWith("*"))
			xml2_label.setText(xml2_label.getText().toString()
				+ "*");
		    xml2_edited = true;
		}
	    }

	}

	@Override
	protected void okPressed() {
	    saveInput();
	    super.okPressed();
	}
    }

    class ColorSchemeDialog extends TitleAreaDialog {

	private Label tagColor;
	private Label attrNameColor;
	private Label attrValueColor;
	private Label commentColor;
	private Label highlightColor;
	private StyledText sample;

	public ColorSchemeDialog(Shell parentShell) {
	    super(parentShell);
	}

	@Override
	public void create() {
	    super.create();
	    setTitle("Color Picker");
	    setMessage("Choose your own color scheme.",
		    IMessageProvider.INFORMATION);
	    Image img;
	    try {
		img = new Image(this.getShell().getDisplay(), XMLDiff.class
			.getClassLoader().getResourceAsStream(
				"images/colorpicker.png"));
	    } catch (Exception e) {
		img = new Image(this.getShell().getDisplay(), XMLDiff.class
			.getClassLoader()
			.getResourceAsStream("colorpicker.png"));
	    }
	    ImageData imgData = img.getImageData().scaledTo(75, 75);
	    img = new Image(this.getShell().getDisplay(), imgData);
	    this.setTitleImage(img);
	    this.getShell().setImage(img);
	}

	@Override
	protected boolean isResizable() {
	    return false;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
	    Composite area = (Composite) super.createDialogArea(parent);
	    Composite container = new Composite(area, SWT.NONE);
	    container.setLayoutData(new GridData(GridData.FILL_BOTH));
	    GridLayout layout = new GridLayout(9, false);
	    container.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true,
		    true));
	    container.setLayout(layout);

	    Label tag = new Label(container, SWT.NONE);
	    tag.setText("    Tag    ");
	    GridData gridData = new GridData(GridData.CENTER, GridData.CENTER,
		    true, false);
	    tag.setLayoutData(gridData);

	    Label s1 = new Label(container, SWT.SEPARATOR | SWT.SHADOW_OUT);
	    gridData = new GridData(GridData.CENTER, GridData.CENTER, false,
		    true, 1, 2);
	    s1.setLayoutData(gridData);

	    Label attrName = new Label(container, SWT.NONE);
	    attrName.setText("Attribute Name");
	    gridData = new GridData(GridData.CENTER, GridData.CENTER, true,
		    false);
	    attrName.setLayoutData(gridData);

	    Label s2 = new Label(container, SWT.SEPARATOR | SWT.SHADOW_OUT);
	    gridData = new GridData(GridData.CENTER, GridData.FILL, false,
		    true, 1, 2);
	    s2.setLayoutData(gridData);

	    Label attrValue = new Label(container, SWT.NONE);
	    attrValue.setText("Attribute Value");
	    gridData = new GridData(GridData.CENTER, GridData.CENTER, true,
		    false);
	    attrValue.setLayoutData(gridData);

	    Label s3 = new Label(container, SWT.SEPARATOR | SWT.SHADOW_OUT);
	    gridData = new GridData(GridData.CENTER, GridData.FILL, false,
		    true, 1, 2);
	    s3.setLayoutData(gridData);

	    Label comment = new Label(container, SWT.NONE);
	    comment.setText("Comment");
	    gridData = new GridData(GridData.CENTER, GridData.CENTER, true,
		    false);
	    comment.setLayoutData(gridData);

	    Label s4 = new Label(container, SWT.SEPARATOR | SWT.SHADOW_OUT);
	    gridData = new GridData(GridData.CENTER, GridData.FILL, false,
		    true, 1, 2);
	    s4.setLayoutData(gridData);

	    Label highlight = new Label(container, SWT.NONE);
	    highlight.setText("Highlight");
	    gridData = new GridData(GridData.CENTER, GridData.CENTER, true,
		    false);
	    highlight.setLayoutData(gridData);

	    /* PALLETE */
	    tagColor = new Label(container, SWT.BORDER);
	    tagColor.setText("        ");
	    tagColor.setBackground(tag_color);
	    gridData = new GridData(GridData.CENTER, GridData.CENTER, false,
		    false);
	    tagColor.setLayoutData(gridData);
	    tagColor.addMouseListener(new MouseAdapter() {
		@Override
		public void mouseDown(MouseEvent arg0) {
		    showColorDialog(tagColor);
		}
	    });

	    attrNameColor = new Label(container, SWT.BORDER);
	    attrNameColor.setText("        ");
	    attrNameColor.setBackground(attrName_color);
	    gridData = new GridData(GridData.CENTER, GridData.CENTER, false,
		    false);
	    attrNameColor.setLayoutData(gridData);
	    attrNameColor.addMouseListener(new MouseAdapter() {
		@Override
		public void mouseDown(MouseEvent arg0) {
		    showColorDialog(attrNameColor);
		}
	    });

	    attrValueColor = new Label(container, SWT.BORDER);
	    attrValueColor.setText("        ");
	    attrValueColor.setBackground(attrValue_color);
	    gridData = new GridData(GridData.CENTER, GridData.CENTER, false,
		    false);
	    attrValueColor.setLayoutData(gridData);
	    attrValueColor.addMouseListener(new MouseAdapter() {
		@Override
		public void mouseDown(MouseEvent arg0) {
		    showColorDialog(attrValueColor);
		}
	    });

	    commentColor = new Label(container, SWT.BORDER);
	    commentColor.setText("        ");
	    commentColor.setBackground(comment_color);
	    gridData = new GridData(GridData.CENTER, GridData.CENTER, false,
		    false);
	    commentColor.setLayoutData(gridData);
	    commentColor.addMouseListener(new MouseAdapter() {
		@Override
		public void mouseDown(MouseEvent arg0) {
		    showColorDialog(commentColor);
		}
	    });

	    highlightColor = new Label(container, SWT.BORDER);
	    highlightColor.setText("        ");
	    highlightColor.setBackground(highlight_color);
	    gridData = new GridData(GridData.CENTER, GridData.CENTER, false,
		    false);
	    highlightColor.setLayoutData(gridData);
	    highlightColor.addMouseListener(new MouseAdapter() {
		@Override
		public void mouseDown(MouseEvent arg0) {
		    showColorDialog(highlightColor);
		}
	    });

	    sample = new StyledText(container, SWT.NONE);
	    sample.setEditable(false);
	    sample.setEnabled(false);
	    gridData = new GridData(GridData.CENTER, GridData.CENTER, true,
		    false, 9, 1);
	    sample.setLayoutData(gridData);
	    sample.setText("<tag attributeName=\"attributeValue\">text</tag> <!--super comment-->");
	    sample.setFont(SWTResourceManager.getFont(XMLDiff.fontName, 10,
		    SWT.NORMAL));
	    adjustColor(sample);

	    Button restoreDefault = new Button(container, SWT.NONE);
	    gridData = new GridData(GridData.CENTER, GridData.CENTER, true,
		    false, 9, 1);
	    restoreDefault.setLayoutData(gridData);
	    restoreDefault.setText("Restore default");
	    restoreDefault.addSelectionListener(new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent e) {
		    tagColor.setBackground(blue);
		    attrNameColor.setBackground(purple);
		    attrValueColor.setBackground(green);
		    commentColor.setBackground(orange);
		    highlightColor.setBackground(lime);
		    adjustColor(sample, blue, purple, green, orange, lime);
		}
	    });

	    return area;
	}

	private void showColorDialog(Label colorLabel) {
	    // Create the color-change dialog
	    ColorDialog dlg = new ColorDialog(this.getShell());

	    // Set the selected color in the dialog from
	    // user's selected color
	    dlg.setRGB(colorLabel.getBackground().getRGB());

	    // Change the title bar text
	    dlg.setText("Choose a Color");

	    // Open the dialog and retrieve the selected color
	    RGB rgb = dlg.open();
	    if (rgb != null) {
		Color color = new Color(this.getShell().getDisplay(), rgb);
		colorLabel.setBackground(color);
		adjustColor(sample, tagColor.getBackground(),
			attrNameColor.getBackground(),
			attrValueColor.getBackground(),
			commentColor.getBackground(),
			highlightColor.getBackground());
	    }
	}

	@Override
	protected void okPressed() {
	    tag_color = tagColor.getBackground();
	    attrName_color = attrNameColor.getBackground();
	    attrValue_color = attrValueColor.getBackground();
	    comment_color = commentColor.getBackground();
	    highlight_color = highlightColor.getBackground();
	    adjustAllColor();
	    super.okPressed();
	}
    }
}
