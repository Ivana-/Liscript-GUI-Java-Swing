package ivana.liscript.gui.swing;

import ivana.liscript.core.Eval;
import ivana.liscript.core.Read;
import ivana.liscript.gui.swing.Main.InterThread;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;

public class WorkPanel extends JPanel implements Eval.InOutable {

    public JTextArea outputPane;
    public JTextPane inputPane, editPane;

    public JLabel lastLoadFileNameLabel, lastOpenFileNameLabel;
    //public JCheckBox clearinputPane;

    public static HashMap<String, Object> styleOutputPane = new HashMap<>();
    public static HashMap<String, Object> styleInputPane = new HashMap<>();
    public static HashMap<String, Object> styleEditPane = new HashMap<>();
    public static Color inputPane_BackgroundIn;

    public static void setDefaultStyle(HashMap<String, Object> s) {
        s.put("font", new Font("Monospaced", Font.PLAIN, 20));
        s.put("foreground", Color.black);
        s.put("background", Color.white);
        s.put("caretColor", Color.black);
    }
    public static void setDefaultSettings() {
        setDefaultStyle(styleOutputPane);
        setDefaultStyle(styleInputPane);
        setDefaultStyle(styleEditPane);
        inputPane_BackgroundIn = new Color(255, 237, 197);
    }

    public InterThread thread;
    public volatile boolean isCin;
    public String cinString;

    ArrayList<String> inputStrings;
    int inputStringsIndex;

    Action sendUserInputAction, restoreUserInputAction,
            sendEditPaneInputAction, interruptAction,
            formatUserInputAction, formatEditPaneInputAction;

    WorkPanel() {
        //super(JSplitPane.VERTICAL_SPLIT);

        this.setLayout(new BorderLayout());

        //WorkPanel thisPane = this;
        lastLoadFileNameLabel = new JLabel();
        lastOpenFileNameLabel = new JLabel();
        //clearinputPane = new JCheckBox();

        inputStrings = new ArrayList<>();
        inputStringsIndex = 0;

        thread = null;
        isCin = false;
        cinString = "";

        setActions();
        //Font textPaneFont = new Font("Courier New", Font.PLAIN, 12);
        //Font textPaneFont = new Font("Monospaced", Font.PLAIN, 12);

        //--------------------------------------

        outputPane = new JTextArea(5, 30);
        outputPane.setLineWrap(true);
        outputPane.setWrapStyleWord(true);
        //outputPane.setFont(textPaneFont);
        //outputPane.setEditable(false);
        outputPane.addCaretListener(new BracketMatcher());

        /*
        DefaultStyledDocument docoutputPane = new DefaultStyledDocument();
        outputPane = new JTextPane(docoutputPane);

        //outputPane.setContentType("text/html");

        outputPane.setFont(textPaneFont);
        //inputPane.setParagraphAttributes(textPanestyle, false);
        //inputPane.setCaret(c);
        //inputPane.setBackground(Color.black);
        outputPane.addCaretListener(new BracketMatcher());
        docoutputPane.addDocumentListener(new SyntaxHighlighter(outputPane));
        */


        JScrollPane scrollPane = new JScrollPane(outputPane);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        //--------------------------------------

        //SimpleAttributeSet textPanestyle = new SimpleAttributeSet();
        //textPanestyle.addAttribute(StyleConstants.SpaceAbove, 1.0f);
        //textPanestyle.addAttribute(StyleConstants.LeftIndent, 2.0f);
        //textPanestyle.addAttribute(StyleConstants.LineSpacing, 0.15f);

        //Caret c = new DefaultCaret();
        //c.setBlinkRate(0);

        //final StyleContext scinputPane = new StyleContext();
        //final DefaultStyledDocument docinputPane = new DefaultStyledDocument(scinputPane);
        DefaultStyledDocument docinputPane = new DefaultStyledDocument();
        inputPane = new JTextPane(docinputPane);
        inputPane.setFocusTraversalKeysEnabled(false);
        //inputPane.setFont(textPaneFont);
        //inputPane.setParagraphAttributes(textPanestyle, false);
        //inputPane.setCaret(c);
        //inputPane.setBackground(Color.black);
        inputPane.addCaretListener(new BracketMatcher());
        docinputPane.addDocumentListener(new SyntaxHighlighter(inputPane));

        JScrollPane scrollPaneIn = new JScrollPane(inputPane);
        scrollPaneIn.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        inputPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK),
                        "sendUserInputAction");
        inputPane.getActionMap().put("sendUserInputAction", sendUserInputAction);

        inputPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK)
                        , "restoreUserInputAction");
        inputPane.getActionMap().put("restoreUserInputAction", restoreUserInputAction);

        inputPane.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK)
                        , "formatUserInputAction");
        inputPane.getActionMap().put("formatUserInputAction", formatUserInputAction);

        //--------------------------------------

        DefaultStyledDocument docEditPane = new DefaultStyledDocument();
        editPane = new JTextPane(docEditPane);
        editPane.setFocusTraversalKeysEnabled(false);
        //editPane.setFont(textPaneFont);
        //editPane.setParagraphAttributes(textPanestyle, false);
        //editPane.setCaret(c);
        //editPane.setBackground(Color.black);
        editPane.addCaretListener(new BracketMatcher());
        docEditPane.addDocumentListener(new SyntaxHighlighter(editPane));

        JScrollPane scrollEditPane = new JScrollPane(editPane);
        scrollEditPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        editPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F1, InputEvent.CTRL_DOWN_MASK)
                        , "sendEditPaneInputAction");
        editPane.getActionMap().put("sendEditPaneInputAction", sendEditPaneInputAction);

        editPane.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK)
                        , "formatEditPaneInputAction");
        editPane.getActionMap().put("formatEditPaneInputAction", formatEditPaneInputAction);

        //--------------------------------------

        Rectangle wa = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();

        JSplitPane splitPaneREPL = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPaneREPL.add(scrollPane);
        splitPaneREPL.add(scrollPaneIn);
        splitPaneREPL.setDividerLocation((int)(wa.height * 0.7));

        JSplitPane splitPaneAll = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPaneAll.add(splitPaneREPL);
        splitPaneAll.add(scrollEditPane);
        splitPaneAll.setDividerLocation((int)(wa.width * 0.8));

        this.add(splitPaneAll, BorderLayout.CENTER);

        JPanel labelsPanel = new JPanel(new GridLayout(1,2));
        labelsPanel.add(lastLoadFileNameLabel);
        labelsPanel.add(lastOpenFileNameLabel);
        //this.add(lastLoadFileNameLabel, BorderLayout.SOUTH);
        this.add(labelsPanel, BorderLayout.SOUTH);

        //this.add(clearinputPane, BorderLayout.WEST);
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "interruptAction");
        this.getActionMap().put("interruptAction", interruptAction);
        //this.setPreferredSize(new Dimension(wa.width, wa.height));

        applySettings();
    }

    private static void formatTextPane (JTextPane textPane) {
        final String text = textPane.getText().trim();
        final String formattedText = Read.prettyPrint(text);
        final int cp = Math.min(textPane.getCaretPosition(), formattedText.length());

        Runnable doIt = () -> {
            textPane.setText("");
            try {
                textPane.getStyledDocument().insertString(0, formattedText, null);
                textPane.setCaretPosition(cp);
            } catch (BadLocationException ex) {}
        };
        if (SwingUtilities.isEventDispatchThread()) doIt.run(); else SwingUtilities.invokeLater(doIt);
    }

    private void setActions() {
        WorkPanel thisPane = this;

        sendUserInputAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //String input = inputPane.getText().trim();
                String input = inputPane.getText();

                if (isCin) {
                    cinString = input;
                    isCin = false;
                } else if (thread == null)
                    Main.startNewThread(thisPane, true, input);
                else return;

                out(true, input);
                if(!input.trim().isEmpty()) {
                    while (inputStrings.size() >= 10)
                        inputStrings.remove(inputStrings.size() - 1);
                    inputStrings.add(0, input);
                    inputStringsIndex = 0;
                }

                if (SwingUtilities.isEventDispatchThread()) {
                    inputPane.setText("");
                } else {
                    SwingUtilities.invokeLater(() -> inputPane.setText(""));
                }
            }
        };

        restoreUserInputAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (inputStrings.isEmpty()) return;

                inputStringsIndex =
                        inputStringsIndex >= inputStrings.size() ? 0 : inputStringsIndex;

                String s = inputStrings.get(inputStringsIndex);
                inputStringsIndex += 1;

                Runnable doIt = () -> {
                    inputPane.setText("");
                    try {
                        inputPane.getStyledDocument().insertString(0, s, null);
                    } catch (BadLocationException ex) {}
                };
                if (SwingUtilities.isEventDispatchThread()) doIt.run(); else SwingUtilities.invokeLater(doIt);
            }
        };

        sendEditPaneInputAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isCin || thread != null) return;
                String input = editPane.getText().trim();
                Main.startNewThread(thisPane, true, input);
            }
        };

        interruptAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (thread == null) return;
                out(true, "interrupt " + thread.getName());
                thread.interrupt();
            }
        };

        formatUserInputAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                formatTextPane (inputPane);
            }
        };
        formatEditPaneInputAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                formatTextPane (editPane);
            }
        };
    }

    public void applySettings() {

        outputPane.setFont((Font) styleOutputPane.get("font"));
        outputPane.setForeground((Color) styleOutputPane.get("foreground"));
        outputPane.setBackground((Color) styleOutputPane.get("background"));
        outputPane.setCaretColor((Color) styleOutputPane.get("caretColor"));

        inputPane.setFont((Font) styleInputPane.get("font"));
        inputPane.setForeground((Color) styleInputPane.get("foreground"));
        inputPane.setBackground((Color) styleInputPane.get("background"));
        inputPane.setCaretColor((Color) styleInputPane.get("caretColor"));

        editPane.setFont((Font) styleEditPane.get("font"));
        editPane.setForeground((Color) styleEditPane.get("foreground"));
        editPane.setBackground((Color) styleEditPane.get("background"));
        editPane.setCaretColor((Color) styleEditPane.get("caretColor"));

        String s = inputPane.getText();
        inputPane.selectAll();
        inputPane.replaceSelection(s);

        s = editPane.getText();
        editPane.selectAll();
        editPane.replaceSelection(s);
    }

    @Override
    public void out(boolean ln, String s) {
        if (s == null) return;

        Runnable doIt = () -> {

            outputPane.append(s);
            if (ln) outputPane.append("\n");
            /*
            try {
                outputPane.getDocument().insertString(
                        outputPane.getDocument().getLength(), s, null);
                if (ln) outputPane.getDocument().insertString(
                        outputPane.getDocument().getLength(), "\n", null);

            } catch (BadLocationException ex) {}
            */

            outputPane.setCaretPosition(outputPane.getDocument().getLength());
        };

        if (SwingUtilities.isEventDispatchThread()) doIt.run(); else SwingUtilities.invokeLater(doIt);
    }

    @Override
    public void outFromRead(String s) { out(false, s); }

    @Override
    public String in() {
        Main.setPaneTabState(this, 2);
        cinString = "";
        isCin = true;
        while (isCin) {
            if (Thread.currentThread().isInterrupted()) {
                isCin = false;
                Thread.currentThread().interrupt();
                throw new RuntimeException();
                //break;
            }
        }
        Main.setPaneTabState(this, 1);

        return cinString;
    }
}
