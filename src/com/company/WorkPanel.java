package com.company;

import com.company.Main.InterThread;

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

    public JTextArea textArea;
    public JTextPane textAreaIn;
    public JTextPane editPane;

    public JLabel lastLoadFileNameLabel;
    public JLabel lastOpenFileNameLabel;
    //public JCheckBox cleartextAreaIn;

    public static HashMap<String, Object> styleTextArea = new HashMap<>();
    public static HashMap<String, Object> styleTextAreaIn = new HashMap<>();
    public static HashMap<String, Object> styleEditPane = new HashMap<>();
    public static Color textAreaIn_BackgroundIn;

    public static void setDefaultStyle(HashMap<String, Object> s) {
        HashMap<String, Object> style = new HashMap<>();
        s.put("font", new Font("Monospaced", Font.PLAIN, 12));
        s.put("foreground", Color.black);
        s.put("background", Color.white);
        s.put("caretColor", Color.black);
    }
    public static void setDefaultSettings() {
        setDefaultStyle(styleTextArea);
        setDefaultStyle(styleTextAreaIn);
        setDefaultStyle(styleEditPane);
        textAreaIn_BackgroundIn = new Color(255, 237, 197);
    }

    public InterThread thread = null;
    public volatile boolean isCin = false;
    public String cinString = "";

    ArrayList<String> inputStrings;
    int inputStringsIndex;

    Action sendUserInputAction, restoreUserInputAction,
            sendEditPaneInputAction, interruptAction;

    WorkPanel() {
        //super(JSplitPane.VERTICAL_SPLIT);

        this.setLayout(new BorderLayout());

        //WorkPanel thisPane = this;
        lastLoadFileNameLabel = new JLabel();
        lastOpenFileNameLabel = new JLabel();
        //cleartextAreaIn = new JCheckBox();

        inputStrings = new ArrayList<String>();
        inputStringsIndex = 0;

        thread = null;
        isCin = false;
        cinString = "";

        setActions();
        //Font textPaneFont = new Font("Courier New", Font.PLAIN, 12);
        //Font textPaneFont = new Font("Monospaced", Font.PLAIN, 12);

        //--------------------------------------

        textArea = new JTextArea(5, 30);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        //textArea.setFont(textPaneFont);
        //textArea.setEditable(false);
        textArea.addCaretListener(new BracketMatcher());

        /*
        DefaultStyledDocument doctextArea = new DefaultStyledDocument();
        textArea = new JTextPane(doctextArea);

        //textArea.setContentType("text/html");

        textArea.setFont(textPaneFont);
        //textAreaIn.setParagraphAttributes(textPanestyle, false);
        //textAreaIn.setCaret(c);
        //textAreaIn.setBackground(Color.black);
        textArea.addCaretListener(new BracketMatcher());
        doctextArea.addDocumentListener(new SyntaxHighlighter(textArea));
        */


        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        //--------------------------------------

        //SimpleAttributeSet textPanestyle = new SimpleAttributeSet();
        //textPanestyle.addAttribute(StyleConstants.SpaceAbove, 1.0f);
        //textPanestyle.addAttribute(StyleConstants.LeftIndent, 2.0f);
        //textPanestyle.addAttribute(StyleConstants.LineSpacing, 0.15f);

        //Caret c = new DefaultCaret();
        //c.setBlinkRate(0);

        //final StyleContext sctextAreaIn = new StyleContext();
        //final DefaultStyledDocument doctextAreaIn = new DefaultStyledDocument(sctextAreaIn);
        DefaultStyledDocument doctextAreaIn = new DefaultStyledDocument();
        textAreaIn = new JTextPane(doctextAreaIn);
        //textAreaIn.setFont(textPaneFont);
        //textAreaIn.setParagraphAttributes(textPanestyle, false);
        //textAreaIn.setCaret(c);
        //textAreaIn.setBackground(Color.black);
        textAreaIn.addCaretListener(new BracketMatcher());
        doctextAreaIn.addDocumentListener(new SyntaxHighlighter(textAreaIn));

        JScrollPane scrollPaneIn = new JScrollPane(textAreaIn);
        scrollPaneIn.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        textAreaIn.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_MASK),
                        "sendUserInputAction");
        textAreaIn.getActionMap().put("sendUserInputAction", sendUserInputAction);

        textAreaIn.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.CTRL_MASK)
                        , "restoreUserInputAction");
        textAreaIn.getActionMap().put("restoreUserInputAction", restoreUserInputAction);

        //--------------------------------------

        DefaultStyledDocument docEditPane = new DefaultStyledDocument();
        editPane = new JTextPane(docEditPane);
        //editPane.setFont(textPaneFont);
        //editPane.setParagraphAttributes(textPanestyle, false);
        //editPane.setCaret(c);
        //editPane.setBackground(Color.black);
        editPane.addCaretListener(new BracketMatcher());
        docEditPane.addDocumentListener(new SyntaxHighlighter(editPane));

        JScrollPane scrollEditPane = new JScrollPane(editPane);
        scrollEditPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        int krpiyu = KeyEvent.VK_F1;
        editPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(krpiyu, InputEvent.CTRL_MASK)
                        , "sendEditPaneInputAction");
        editPane.getActionMap().put("sendEditPaneInputAction", sendEditPaneInputAction);

        //--------------------------------------

        Rectangle wa = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();

        JSplitPane splitPaneREPL = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPaneREPL.add(scrollPane);
        splitPaneREPL.add(scrollPaneIn);
        splitPaneREPL.setDividerLocation(wa.height - 200);

        JSplitPane splitPaneAll = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPaneAll.add(splitPaneREPL);
        splitPaneAll.add(scrollEditPane);
        splitPaneAll.setDividerLocation(wa.width - 100);

        this.add(splitPaneAll, BorderLayout.CENTER);

        JPanel labelsPanel = new JPanel(new GridLayout(1,2));
        labelsPanel.add(lastLoadFileNameLabel);
        labelsPanel.add(lastOpenFileNameLabel);
        //this.add(lastLoadFileNameLabel, BorderLayout.SOUTH);
        this.add(labelsPanel, BorderLayout.SOUTH);

        //this.add(cleartextAreaIn, BorderLayout.WEST);
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "interruptAction");
        this.getActionMap().put("interruptAction", interruptAction);
        //this.setPreferredSize(new Dimension(wa.width, wa.height));

        applySettings();
    }

    private void setActions() {
        WorkPanel thisPane = this;

        sendUserInputAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //String input = textAreaIn.getText().trim();
                String input = textAreaIn.getText();

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
                    textAreaIn.setText("");
                } else {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            textAreaIn.setText("");
                        }
                    });
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

                Runnable doIt = new Runnable() {
                    public void run() {
                        textAreaIn.setText("");
                        try {
                            textAreaIn.getStyledDocument().insertString(0, s, null);
                        } catch (BadLocationException ex) {}
                    }};
                if (SwingUtilities.isEventDispatchThread()) doIt.run();
                else SwingUtilities.invokeLater(doIt);
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
                out(true, "interrupt");
                thread.interrupt();
                out(true, thread.getName());
            }
        };
    }

    public void applySettings() {

        textArea.setFont((Font) styleTextArea.get("font"));
        textArea.setForeground((Color) styleTextArea.get("foreground"));
        textArea.setBackground((Color) styleTextArea.get("background"));
        textArea.setCaretColor((Color) styleTextArea.get("caretColor"));

        textAreaIn.setFont((Font) styleTextAreaIn.get("font"));
        textAreaIn.setForeground((Color) styleTextAreaIn.get("foreground"));
        textAreaIn.setBackground((Color) styleTextAreaIn.get("background"));
        textAreaIn.setCaretColor((Color) styleTextArea.get("caretColor"));

        editPane.setFont((Font) styleEditPane.get("font"));
        editPane.setForeground((Color) styleEditPane.get("foreground"));
        editPane.setBackground((Color) styleEditPane.get("background"));
        editPane.setCaretColor((Color) styleTextArea.get("caretColor"));

        String s = textAreaIn.getText();
        textAreaIn.selectAll();
        textAreaIn.replaceSelection(s);

        s = editPane.getText();
        editPane.selectAll();
        editPane.replaceSelection(s);
    }

    @Override
    public void out(boolean ln, String s) {
        if (s == null) return;

        Runnable doIt = new Runnable() {
            public void run() {

                textArea.append(s);
                if (ln) textArea.append("\n");
                /*
                try {
                    textArea.getDocument().insertString(
                            textArea.getDocument().getLength(), s, null);
                    if (ln) textArea.getDocument().insertString(
                            textArea.getDocument().getLength(), "\n", null);

                } catch (BadLocationException ex) {}
                */

                textArea.setCaretPosition(textArea.getDocument().getLength());
            }};

        if (SwingUtilities.isEventDispatchThread()) doIt.run();
        else SwingUtilities.invokeLater(doIt);
    }

    @Override
    public String in() {
        Main.setPaneTabState(this, 2);
        cinString = "";
        isCin = true;
        while (isCin) {
            if (Thread.currentThread().isInterrupted()) {
                isCin = false;
                Thread.currentThread().interrupt();
                throw new RuntimeException("interrupted lalalala.....");
                //break;
            }
        }
        Main.setPaneTabState(this, 1);

        return cinString;
    }
}
