package com.company;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.util.HashMap;

public class SyntaxHighlighter implements DocumentListener {

    boolean beenModified = false;
    DocumentEvent documentEvent;
    JTextComponent textComponent;

    static HashMap<String, SimpleAttributeSet> keyWordStyles;
    static SimpleAttributeSet styleEmpty, styleUtil, styleNumber,
            styleString, styleStringError, styleEnvBounds, styleComment;

    SyntaxHighlighter(JTextComponent _textComponent) {
        textComponent = _textComponent;

        //styleEmpty =        makeStyle(new Color(109, 163, 189), false, false, false);
        if (textComponent.getBackground().equals(Color.black))
            styleEmpty =        makeStyle(Color.lightGray, false, false, false);
        else
            styleEmpty =        makeStyle(Color.black, false, false, false);

        styleUtil =         makeStyle(new Color(102, 204, 102), false, false, false);
        styleNumber =       makeStyle(Color.magenta, false, false, false);
        styleString =       makeStyle(new Color(0, 128, 0), true, false, false);
        styleStringError =  makeStyle(new Color(0, 128, 0), true, true, true);
        styleComment =      makeStyle(Color.gray, false, false, true);
        styleEnvBounds =    makeStyle(new Color(177, 177, 0), false, false, false);
        SimpleAttributeSet styleKeyWord;
        styleKeyWord =      makeStyle(new Color(177, 177, 0), true, false, false);
        SimpleAttributeSet styleBoolean;
        styleBoolean =      makeStyle(new Color(0, 0, 128), true, false, false);

        keyWordStyles = new HashMap<String, SimpleAttributeSet>();
        keyWordStyles.put("true",       styleBoolean);
        keyWordStyles.put("false",      styleBoolean);

        keyWordStyles.put("+",          styleEnvBounds);
        keyWordStyles.put("-",          styleKeyWord);
        keyWordStyles.put("*",          styleKeyWord);
        keyWordStyles.put("/",          styleKeyWord);
        keyWordStyles.put("mod",        styleKeyWord);
        keyWordStyles.put(">",          styleKeyWord);
        keyWordStyles.put(">=",         styleKeyWord);
        keyWordStyles.put("<",          styleKeyWord);
        keyWordStyles.put("<=",         styleKeyWord);
        keyWordStyles.put("=",          styleKeyWord);
        keyWordStyles.put("/=",         styleKeyWord);
        keyWordStyles.put("++",         styleKeyWord);

        keyWordStyles.put("eq?",        styleKeyWord);
        keyWordStyles.put("def",        styleKeyWord);
        keyWordStyles.put("set!",       styleKeyWord);
        keyWordStyles.put("get",        styleKeyWord);
        keyWordStyles.put("cons",       styleKeyWord);
        keyWordStyles.put("car",        styleKeyWord);
        keyWordStyles.put("cdr",        styleKeyWord);
        keyWordStyles.put("quote",      styleKeyWord);
        keyWordStyles.put("cond",       styleKeyWord);
        keyWordStyles.put("while",      styleKeyWord);
        keyWordStyles.put("eval",       styleKeyWord);
        keyWordStyles.put("typeof",      styleKeyWord);
        //keyWordStyles.put("atom?",      styleKeyWord);
        //keyWordStyles.put("list?",      styleKeyWord);
        //keyWordStyles.put("func?",      styleKeyWord);
        //keyWordStyles.put("macr?",      styleKeyWord);
        keyWordStyles.put("print",      styleKeyWord);
        keyWordStyles.put("read",       styleKeyWord);
        keyWordStyles.put("lambda",     styleKeyWord);
        keyWordStyles.put("macro",      styleKeyWord);
        //keyWordStyles.put("rec",        styleKeyWord);
    }

    public static SimpleAttributeSet makeStyle(
        Color foreground, boolean bold, boolean underline, boolean italic) {

        SimpleAttributeSet style = new SimpleAttributeSet();
        style.addAttribute(StyleConstants.Foreground, foreground);
        style.addAttribute(StyleConstants.Bold, bold);
        style.addAttribute(StyleConstants.Underline, underline);
        style.addAttribute(StyleConstants.Italic, italic);
        return style;
    }

    public void insertUpdate(DocumentEvent e) {highlightText(e);}
    public void removeUpdate(DocumentEvent e) {highlightText(e);}
    public void changedUpdate(DocumentEvent e) {}

    public void highlightText(DocumentEvent e) {

        if (beenModified) return;
        //beenModified = true;
        documentEvent = e;

    Runnable doRunnableHighlight = new Runnable() {
        @Override
        public void run() {
            DefaultStyledDocument doc = (DefaultStyledDocument) documentEvent.getDocument();
            try {
                int offset = documentEvent.getOffset();

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
                //System.out.println("" + ifrom + " " + ito);

                // set attributes in determined interval
                int STATE_TOKEN = 0, STATE_UTIL = 1, STATE_STRING = 2, STATE_COMMENT = 3;
                int state = STATE_TOKEN, ib = ifrom;
                boolean replace = false;

                for (int i = ifrom; i <= ito; ++i) {
                    char c = i < ito ? doc.getText(i, 1).charAt(0) : 0;

                    if (state == STATE_STRING &&
                            (c == '"' || c == '\n' || c == 0)) {
                        SimpleAttributeSet style = c == '"' ? styleString : styleStringError;
                        doc.setCharacterAttributes(ib, i - ib + 1, style, replace);
                        ib = i + 1;
                        state = STATE_TOKEN;
                    }
                    else if (state == STATE_COMMENT &&
                            (c == ';' || c == '\n' || c == 0)) {
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
                            Double test = new Double(word);
                            style = styleNumber;
                        } catch (NumberFormatException errorDouble) {
                            style = Env.isBounded(Main.globalEnv, word) ?
                                    styleEnvBounds :
                                    keyWordStyles.containsKey(word) ?
                                    keyWordStyles.get(word) :
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
        }
    };

        SwingUtilities.invokeLater(doRunnableHighlight);
    }
}



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

