package ivana.liscript.gui.swing;

import ivana.liscript.core.Eval;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.util.HashSet;

public class SyntaxHighlighter implements DocumentListener {

    boolean beenModified = false;
    DocumentEvent documentEvent;
    JTextComponent textComponent;
    //SimpleAttributeSet styleEmpty;

    //static HashMap<String, SimpleAttributeSet> keyWordStyles;
    static HashSet<String> booleanKeyWords = new HashSet<>();
    static SimpleAttributeSet styleUtil = new SimpleAttributeSet();
    static SimpleAttributeSet styleNumber = new SimpleAttributeSet();
    static SimpleAttributeSet styleBoolean = new SimpleAttributeSet();
    static SimpleAttributeSet styleString = new SimpleAttributeSet();
    static SimpleAttributeSet styleStringError = new SimpleAttributeSet();
    static SimpleAttributeSet styleComment = new SimpleAttributeSet();
    static SimpleAttributeSet styleEnvBounds = new SimpleAttributeSet();
    static SimpleAttributeSet styleKeyWord = new SimpleAttributeSet();

    SyntaxHighlighter(JTextComponent _textComponent) { textComponent = _textComponent; }

    public static void makeStyle(SimpleAttributeSet style,
            Color foreground, boolean bold, boolean underline, boolean italic) {
        style.addAttribute(StyleConstants.Foreground, foreground);
        style.addAttribute(StyleConstants.Bold, bold);
        style.addAttribute(StyleConstants.Underline, underline);
        style.addAttribute(StyleConstants.Italic, italic);
    }

    public static void setDefaultSettings() {
        //makeStyle(styleEmpty, textComponent.getForeground(), false, false, false);
        makeStyle(styleUtil, new Color(102, 204, 102), false, false, false);
        makeStyle(styleNumber, Color.magenta, false, false, false);
        makeStyle(styleBoolean, new Color(0, 150, 150), true, false, false);
        makeStyle(styleString, new Color(0, 150, 0), true, false, false);
        makeStyle(styleStringError, new Color(0, 150, 0), true, true, true);
        makeStyle(styleComment, Color.gray, false, false, true);
        makeStyle(styleEnvBounds, new Color(177, 177, 0), false, false, false);
        makeStyle(styleKeyWord, new Color(177, 177, 0), true, false, false);

        //SimpleAttributeSet styleBoolean;
        //styleBoolean =      makeStyle(new Color(0, 150, 150), true, false, false);
        //keyWordStyles = new HashMap<String, SimpleAttributeSet>();
        //keyWordStyles.put("true",       styleBoolean);
        //keyWordStyles.put("false",      styleBoolean);

        booleanKeyWords = new HashSet<>();
        booleanKeyWords.add("true");
        booleanKeyWords.add("false");
    }

    public void insertUpdate(DocumentEvent e) {highlightText(e);}
    public void removeUpdate(DocumentEvent e) {highlightText(e);}
    public void changedUpdate(DocumentEvent e) {}

    public void highlightText(DocumentEvent e) {

        if (beenModified) return;
        //beenModified = true;
        documentEvent = e;

        Runnable doRunnableHighlight = () -> {
            DefaultStyledDocument doc = (DefaultStyledDocument) documentEvent.getDocument();
            try {
                int offset = documentEvent.getOffset();
                SimpleAttributeSet styleEmpty = new SimpleAttributeSet();
                makeStyle(styleEmpty, textComponent.getForeground(), false, false, false);

                // add close paren ) or "
            /*
            if (documentEvent.getType() == DocumentEvent.EventType.INSERT
                    && documentEvent.getLength() == 1) {
                char c = doc.getText(offset, 1).charAt(0);
                if (c == '(' || c == '"') {
                    doc.insertString(offset + 1, c == '(' ? ")" : "\"", null);
                    textComponent.setCaretPosition(offset + 1);
                }
            }
            */

                // determine start and end offsets where needed to reset attributes
                int ifrom = doc.getParagraphElement(offset).getStartOffset(), ito;

                if (documentEvent.getType() == DocumentEvent.EventType.INSERT)
                    ito = doc.getParagraphElement(offset
                            + documentEvent.getLength()).getEndOffset();
                else
                    ito = doc.getParagraphElement(offset).getEndOffset();

//                System.out.println("Highlight " + ifrom + " - " + ito + " (" + doc.getLength() + ")");

                // set attributes in determined interval
                int STATE_TOKEN = 0, STATE_UTIL = 1, STATE_STRING = 2, STATE_COMMENT = 3;
                int state = STATE_TOKEN, ib = ifrom;
                boolean replace = false;

                for (int i = ifrom; i <= ito; ++i) {
                    char c = i < ito ? doc.getText(i, 1).charAt(0) : 0;

                    if (state == STATE_STRING &&
                            (c == '"'
//                                    || c == '\n'
                                    || c == 0)) {
                        SimpleAttributeSet style = c == '"' ? styleString : styleStringError;
                        doc.setCharacterAttributes(ib, i - ib + 1, style, replace);
                        ib = i + 1;
                        state = STATE_TOKEN;
                    }
                    else if (state == STATE_COMMENT &&
                            (c == ';'
//                                    || c == '\n'
                                    || c == 0)) {
                        doc.setCharacterAttributes(ib, i - ib + 1, styleComment, replace);
                        ib = i + 1;
                        state = STATE_TOKEN;
                    }
                    else if (state == STATE_TOKEN &&
                            (c == '(' || c == ')'  || c == '"' || c == ';'
                                    || c == 0 || Character.isWhitespace(c))) {
                        String word = doc.getText(ib, i - ib);
                        SimpleAttributeSet style;
                        try {
                            double test = Double.parseDouble(word); // needs for check if word is number!
                            style = styleNumber;
                        } catch (NumberFormatException errorDouble) {
                            style = Main.globalEnv.isBounded(word) ? styleEnvBounds :
                                    //keyWordStyles.containsKey(word) ? keyWordStyles.get(word) :
                                    booleanKeyWords.contains(word) ? styleBoolean :
                                            Eval.specialFormWords.containsKey(word) ? styleKeyWord :
                                                    styleEmpty;
                        }
                        doc.setCharacterAttributes(ib, i - ib, style, replace);
                        state = c == '"' ? STATE_STRING :
                                c == ';' ? STATE_COMMENT : STATE_UTIL;
                        ib = i;
                    }
                    else if (state == STATE_UTIL &&
                            (c == 0 || !(c == '(' || c == ')' || Character.isWhitespace(c)))) {
                        doc.setCharacterAttributes(ib, i - ib, styleUtil, replace);
                        state = c == '"' ? STATE_STRING :
                                c == ';' ? STATE_COMMENT : STATE_TOKEN;
                        ib = i;
                    }
                }
            } catch (BadLocationException ex) {throw new Error(ex);}

            beenModified = false;
        };

        SwingUtilities.invokeLater(doRunnableHighlight);
    }
}
/*
    public void game() {

        if (newGame) {
            resources.free();
        }

        s = FILENAME + 3;
        setLocation();
        load(s);
        loadDialog.process();

        try {
            setGamerColor(RED);
        } catch (Exception e) {
            reset();
        }

        while (notReady) {
            objects.make();
            if (resourceNotFound) {
                break;
            }
        }

        byte result; // сменить на int!
        music();
        System.out.print("");
    }
*/
    /*
    if (!beenModified) {
        beenModified = true;

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {

                String keyword = wordSearchField.getText().trim();
                DefaultComboBoxModel<String> m = new DefaultComboBoxModel<String>();
                for (int i = 0; i < 10; i++) {
                    m.addElement(i + "");
                }
                wordSearchTips.setModel(m);
                wordSearchTips.setSelectedIndex(-1);
                ((JTextField) wordSearchTips.getEditor().getEditorComponent())
                        .setText(keyword);
                wordSearchTips.showPopup();

                beenModified = false;
            }
        });
    }


                private void assistDateText() {
                    Runnable doAssist = new Runnable() {
                        @Override
                        public void run() {
                            // when input "2013",will add to "2013-";when
                            // input "2013-10",will add to "2013-10-"
                            String input = expiration_timeTF.getText();
                            if (input.matches("^[0-9]{4}")) {
                                expiration_timeTF.setText(input + "-");
                            } else if (input.matches("^[0-9]{4}-[0-9]{2}")) {
                                expiration_timeTF.setText(input + "-");
                            }
                        }
                    };
                    SwingUtilities.invokeLater(doAssist);
                }


    */

