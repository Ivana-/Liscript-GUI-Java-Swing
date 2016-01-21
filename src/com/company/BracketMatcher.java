package com.company;

import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.*;
import java.awt.*;

public class BracketMatcher implements CaretListener
{
    /** The tags returned from the highlighter, used for clearing the
     current highlight. */
    Object start, end;
    Highlighter highlighter;
    Highlighter.HighlightPainter goodPainter;
    Highlighter.HighlightPainter badPainter;


    //Object word=null;


    /** Highlights using a good painter for matched parens, and a bad
     painter for unmatched parens */
    BracketMatcher(Highlighter.HighlightPainter goodHighlightPainter,
                   Highlighter.HighlightPainter badHighlightPainter)
    {
        this.goodPainter = goodHighlightPainter;
        this.badPainter = badHighlightPainter;
    }

    /** A BracketMatcher with the default highlighters (cyan and magenta) */
    BracketMatcher()
    {
        this(new DefaultHighlighter.DefaultHighlightPainter(Color.cyan),
        //new Color(153, 204, 255)),
                new DefaultHighlighter.DefaultHighlightPainter(Color.magenta));
    }

    public void clearHighlights() {
        if(highlighter != null) {
            if(start != null) highlighter.removeHighlight(start);
            if(end   != null) highlighter.removeHighlight(end);

            //if(word != null) highlighter.removeHighlight(word);
            //word = null;

            start = end = null;
            highlighter = null;
        }
    }

    /** Returns the character at position p in the document*/
    public static char getCharAt(Document doc, int p)
            throws BadLocationException { return doc.getText(p, 1).charAt(0); }

    /** Returns the position of the matching parenthesis (bracket,
     * whatever) for the character at paren.  It counts all kinds of
     * brackets, so the "matching" parenthesis might be a bad one.  For
     * this demo, we're not going to take quotes or comments into account
     * since that's not the point.
     *
     * It's assumed that paren is the position of some parenthesis
     * character
     *
     * @return the position of the matching paren, or -1 if none is found
     **/
    public static int findMatchingParen__(Document d, int paren)
            throws BadLocationException
    {
        int parenCount = 1;
        int i = paren-1;
        for(; i >= 0; i--) {
            char c = getCharAt(d, i);
            switch(c) {
                case ')':
                case '}':
                case ']':
                    parenCount++;
                    break;
                case '(':
                case '{':
                case '[':
                    parenCount--;
                    break;
            }
            if(parenCount == 0)
                break;
        }
        return i;
    }



/*
    private Action getAction(JTextComponent c, String name)
    {
        Action action = null;
        Action[] actions = c.getActions();

        for (int i = 0; i < actions.length; i++)
        {
            if (name.equals( actions[i].getValue(Action.NAME).toString() ) )
            {
                action = actions[i];
                break;
            }
        }

        return action;
    }
*/



    //DefaultStyledDocument doc__;


    public static int firstNonspaseOffset(Document d, int p, int dir)
            throws BadLocationException
    {
        int i = p, length = d.getLength();
        for(; i >= 0 && i < length; i+=dir) {
            char c = getCharAt(d, i);
            if ( !Character.isWhitespace(c) || c=='\n' ) break;
        }
        return i >= length ? -1 : i;
    }

    public static int findMatchingParen(Document d, int p, char c)
            throws BadLocationException
    {
        int dir;
        if      (c == '(') dir = 1;
        else if (c == ')') dir = -1;
        else return -1;

        int length = d.getLength(), parenLevel = dir, i = p+dir;
        for(; i >= 0 && i < length; i+=dir) {
            switch (getCharAt(d, i)) {
                case ')':
                //case '}':
                //case ']':
                    parenLevel--;
                    break;
                case '(':
                //case '{':
                //case '[':
                    parenLevel++;
                    break;
            }
            if (parenLevel == 0) break;
        }
        return i >= length ? -1 : i;
    }

    public boolean checkHighlightParens(Document d, int p)
            throws BadLocationException
    {
        if (p < 0 || p >= d.getLength()) return false;

        char c = getCharAt(d, p);
        if (c == '(' || c == ')') {
            int mp = findMatchingParen(d, p, c);
            if (mp >= 0) {
                //char c2 = getCharAt(d, mp);
                //if((c2 == '(' && c == ')') ||
                //        (c2 == '{' && c == '}') ||
                //        (c2 == '[' && c == ']')) {
                    start = highlighter.addHighlight(p, p+1, goodPainter);
                    end = highlighter.addHighlight(mp, mp+1, goodPainter);
                //} else {
                //    start = highlighter.addHighlight(openParen,
                //            openParen+1,
                //            badPainter);
                //    end = highlighter.addHighlight(closeParen,
                //            closeParen+1,
                //            badPainter);
                //}
            } else
                end = highlighter.addHighlight(p, p+1, badPainter);

            return true;
        }
        return false;
    }

    /** Called whenever the caret moves, it updates the highlights */
    public void caretUpdate(CaretEvent e) {

        clearHighlights();
        int p = e.getDot();
        if (p != e.getMark()) return;

        JTextComponent source = (JTextComponent) e.getSource();
        highlighter = source.getHighlighter();
        Document doc = source.getDocument();
        //DefaultStyledDocument doc = (DefaultStyledDocument) source.getDocument();


        //Action aaa = getAction(source, DefaultEditorKit.selectLineAction);
        //try {
            //word = highlighter.addHighlight(
            //        Utilities.getWordStart(source, e.getDot()),
            //        Utilities.getWordEnd(source, e.getDot()),
            //        goodPainter);

            //word = highlighter.addHighlight(
            //        Utilities.getRowStart(source, e.getDot()),
            //        Utilities.getRowEnd(source, e.getDot()),
            //        new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 250, 227)));


            //aaa.actionPerformed( null );
            //doc.set

        try {
            if (! checkHighlightParens(doc, p-1)) // near left
            if (! checkHighlightParens(doc, p))   // near right
            if (! checkHighlightParens(doc, firstNonspaseOffset(doc, p-1, -1))) // far left
            if (! checkHighlightParens(doc, firstNonspaseOffset(doc, p, 1)) ) {}// far right
        } catch (BadLocationException ex) { throw new Error(ex); }
    }
}

