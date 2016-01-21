package com.company;

import java.util.HashMap;

public class Eval {

    public static ConsList emptyList = new ConsList(null, null);
    public static RawString rawStringOK = new RawString("OK");

    public interface InOutable {
        void out(boolean newLine, String string);
        String in();
    }

    //---------------------- TYPE LispVal AND ITS INSTANCES --------------------

    public static class ConsList {
        public Object car;
        public ConsList cdr;
        public String s="";

        ConsList(Object h, ConsList t) { car = h; cdr = t; }
        public boolean isEmpty() { return this.car == null && this.cdr == null; }

        @Override
        public String toString() { return showVal(this); }
    }
    public static class Func {
        public ConsList pars;
        public Object body;
        public Env clojure;
        public String s="";

        Func(ConsList p, Object b, Env c) { pars = p; body = b; clojure = c; }

        @Override
        public String toString() { return showVal(this); }
    }
    public static class Macr {
        public ConsList pars;
        public Object body;
        public String s="";

        Macr(ConsList p, Object b) { pars = p; body = b; }

        @Override
        public String toString() { return showVal(this); }
    }
    public static class RawString {
        public String string = "";

        RawString(String s) { string = s; }

        public boolean isEmpty() { return string.isEmpty(); }

        @Override
        public String toString() { return string; }
    }
    private static String showConsList_go(ConsList p) {
        String r;
        if (p==null) r="null";
        else {

            //if (!p.s.isEmpty()) return "SSSSS: " + p.s;

            r = showVal(p.car);
            ConsList t = p.cdr;
            if (p.isEmpty()) r="";
            else if (t != null && !t.isEmpty()) r += " " + showConsList_go(t);
        }
        return r;
    }
    private static String showConsList(ConsList p) {

        //if (r.length()>50) {} //r = "\n" + r;
        //else r = r.replace('\n',' ');

        //String r = showConsList_go(p);

        //String r = "";
        //while (!p.isEmpty()) {
        //    if (Thread.currentThread().isInterrupted()) {
        //        Thread.currentThread().interrupt();
        //        throw new RuntimeException("interrupted lalalala.....");
        //    }
        //    r = r + " " + showVal(p.car);
        //    p = p.cdr;
        //}
        //return r.trim();

        final StringBuilder sb = new StringBuilder();
        if (!p.isEmpty()) {
            sb.append(showVal(p.car));
            p = p.cdr;
        }
        while (!p.isEmpty()) {
            //if (Thread.currentThread().isInterrupted()) {
            //    Thread.currentThread().interrupt();
            //    throw new RuntimeException("interrupted lalalala.....");
            //}
            sb.append(" ");
            sb.append(showVal(p.car));
            p = p.cdr;
        }
        return sb.toString();
    }
    private static String showVal(Object p) {
        final StringBuilder sb = new StringBuilder();

        if (p == null) sb.append("null");

        else if (p instanceof RawString) {
            sb.append("\"");
            sb.append(((RawString) p).string);
            sb.append("\"");

        } else if (p instanceof ConsList) {
            //r = "(" + showConsList((ConsList) p) + ")";
            sb.append("(");
            sb.append(showConsList((ConsList) p));
            sb.append(")");

        } else if (p instanceof Func) {
            Func f = (Func) p;

            //if (!f.s.isEmpty()) return "SSSSS: " + f.s;

            //r = "(LAMBDA " + showVal(f.pars) + " " + showVal(f.body) + ")";

            sb.append("(lambda ");
            sb.append(showVal(f.pars));
            sb.append(" ");
            sb.append(showVal(f.body));
            sb.append(")");

        } else if (p instanceof Macr) {
            Macr f = (Macr) p;

            //if (!f.s.isEmpty()) return "SSSSS: " + f.s;

            //r = "(MACRO " + showVal(f.pars) + " " + showVal(f.body) + ")";

            sb.append("(macro ");
            sb.append(showVal(f.pars));
            sb.append(" ");
            sb.append(showVal(f.body));
            sb.append(")");

        } else sb.append(p.toString());

        return sb.toString();
    }

    //---------------------------------- EVAL ----------------------------------

    private static Number BinOp(String op, Number ina, Number inb) {
        if (ina instanceof Double || inb instanceof Double) {
            double a = ina.doubleValue();
            double b = inb.doubleValue();
            return op.equals("+") ? a+b : op.equals("-") ? a-b :
                    op.equals("*") ? a*b : op.equals("/") ? a/b : op.equals("mod") ? a%b : a;
        } else {
            int a = ina.intValue();
            int b = inb.intValue();
            return op.equals("+") ? a+b : op.equals("-") ? a-b :
                    op.equals("*") ? a*b : op.equals("/") ? a/b : op.equals("mod") ? a%b : a;
        }
    }
    private static boolean CompOp(String op, Number ina, Number inb) {
        if (ina instanceof Double || inb instanceof Double) {
            double a = ina.doubleValue();
            double b = inb.doubleValue();
            return  op.equals(">") ? a>b : op.equals(">=") ? a>=b :
                    op.equals("<") ? a<b : op.equals("<=") ? a<=b :
                            op.equals("=") ? a==b : op.equals("/=") ? a!=b : true;
        } else {
            int a = ina.intValue();
            int b = inb.intValue();
            return  op.equals(">") ? a>b : op.equals(">=") ? a>=b :
                    op.equals("<") ? a<b : op.equals("<=") ? a<=b :
                            op.equals("=") ? a==b : op.equals("/=") ? a!=b : true;
        }
    }
    private static Number foldBinOp(int d, String op, InOutable io, Env env, ConsList l) {
        if (l.isEmpty()) return (Number) null;

        Number r = (Number) eval(d, true, io, env, l.car);
        l = l.cdr;
        while (!l.isEmpty()) {
            r = BinOp(op, r, (Number) eval(d, true, io, env, l.car));
            l = l.cdr;
        }
        return r;
    }
    private static boolean foldCompOp(int d, String op, InOutable io, Env env, ConsList l) {
        if (l.isEmpty()) return true;

        Number a = (Number) eval(d, true, io, env, l.car);
        l = l.cdr;
        while (!l.isEmpty()) {
            Number b = (Number) eval(d, true, io, env, l.car);
            if (!CompOp(op, a, b)) return false;
            a = b;
            l = l.cdr;
        }
        return true;
    }
    private static boolean foldEqObject(int d, InOutable io, Env env, ConsList l) {
        //if (l.isEmpty()) return true;
        //Object b = eval(d, true, io, env, l.car);
        //return  (a == null || isMyEqual(a, b)) && foldEqObject(env, l.cdr, b);

        if (l.isEmpty()) return true;
        Object a = eval(d, true, io, env, l.car);
        l = l.cdr;
        while (!l.isEmpty()) {
            Object b = eval(d, true, io, env, l.car);
            if (!isMyEqual(a, b)) return false;
            a = b;
            l = l.cdr;
        }
        return true;
    }
    private static boolean isMyEqual(Object a, Object b) {
        if (a == null || b == null) return false;
        if (!(a.getClass().equals(b.getClass()))) return false;

        if (a instanceof RawString) {
            RawString pa = (RawString) a, pb = (RawString) b;
            return pa.string.equals(pb.string);
        } else if (a instanceof ConsList) {
                ConsList pa = (ConsList) a, pb = (ConsList) b;
                boolean ae = pa.isEmpty(), be = pb.isEmpty();
                //if (ae && be) return true;
                //else if (!ae && !be)
                //    return isMyEqual(pa.car, pb.car) && isMyEqual(pa.cdr, pb.cdr);
                //else return false;
                return (ae && be) ||
                        (!ae && !be && isMyEqual(pa.car, pb.car) && isMyEqual(pa.cdr, pb.cdr));
        } else if (a instanceof Macr) {
            Macr ma = (Macr) a, mb = (Macr) b;
            return isMyEqual(ma.pars, mb.pars) && isMyEqual(ma.body, mb.body);
        } else return a.equals(b);
    }

    private static Object getbody(Object o) {
        if (o instanceof ConsList) {
            ConsList p = (ConsList)o;
            if (!p.isEmpty() && p.cdr.isEmpty()) {
                if (p.car instanceof ConsList) return getbody(p.car);
            }
        }
        return o;
    }
    private static ConsList evalCons(int d, InOutable io, Env env, ConsList l) {
        //return l.isEmpty() ? emptyList : new ConsList(eval(d, true, io, env, l.car), evalCons(env, l.cdr));
/*
        if (l.isEmpty()) return emptyList;
        else {
            Object v = eval(d, true, io, env, l.car);
            return l.cdr.isEmpty() && v instanceof ConsList ?
                    (ConsList) v :
                    new ConsList(v, evalCons(env, l.cdr));
            }
*/
        if (l.isEmpty()) return emptyList;

        ConsList r = new ConsList(null, null), t = r;
        while (!l.isEmpty()) {
            Object v = eval(d, true, io, env, l.car);
            if (l.cdr.isEmpty()) {

                if (v instanceof ConsList) {
                    ConsList pv = (ConsList) v;
                    t.car = pv.car;
                    t.cdr = pv.cdr;
                } else {
                    t.car = v;
                    t.cdr = emptyList;
                    t = t.cdr;
                }
            } else {
                t.car = v;
                t.cdr = new ConsList(null, null);
                t = t.cdr;
            }
            l = l.cdr;
        }
        return r;
    }
    private static Object macroexpand(HashMap<String, Object> map, Object o) {
        //macroexpand :: [(String, LispVal)] -> LispVal -> LispVal
        //macroexpand kv = go where
        //go (Atom a) = fromMaybe (Atom a) $ lookup a kv
        //go (List l) = List $ map go l
        //go        x = x

        if (o == null) return null;

        if (o instanceof String) {
            String s = (String)o;
            return map.containsKey(s) ? map.get(s) : s;
        } else if (o instanceof ConsList) {
            ConsList p = (ConsList)o;
            return p.isEmpty() ? emptyList :
                new ConsList(macroexpand(map, p.car), (ConsList)macroexpand(map, p.cdr));
        } else return o;
    }

    private static class TailCall {
        public Func f;
        public HashMap<String, Object> args;

        TailCall(Func _f, HashMap<String, Object> _args) { f = _f; args = _args; }

        @Override
        //public String toString() { return "FUNCALL: " + f.toString() + " " + args.toString(); }
        public String toString() { return "FUNCALL: " + args.toString(); }
    }

    private static HashMap<String, Object> getEvalMapArgsVals(
            int d, InOutable io, Env env, ConsList pa, ConsList pv) {

        HashMap<String, Object> m = new HashMap<String, Object>();
        while (!pa.isEmpty() && !pv.isEmpty()) {
            if (pa.cdr.isEmpty() && !pv.cdr.isEmpty())
                m.put((String) pa.car, evalCons(d, io, env, pv));
            else
                m.put((String) pa.car, eval(d, true, io, env, pv.car));
            pa = pa.cdr;
            pv = pv.cdr;
        }
        return m;
    }

    //throws Throwable { // InterruptedException {
    public static Object ret(int d, InOutable io, Object o) {
        //io.out(false, cntd(d) + Character.toString((char)8594) + " ");
        //io.out(true, o.toString());
        return o;
    }

    public static void tray(int d, InOutable io, Object o) {
        //io.out(false, cntd(d) + Character.toString((char)8592) + " ");
        //io.out(true, o.toString());
    }

    public static String cntd(int d) {
        String r = "";
        for (int i = d; i>1; i--) r+="  ";
        return r + d + " ";
    }

    public static Object eval(int din, boolean strike, InOutable io, Env env, Object inobj) {

        int d = din+1;
        //tray(d, io, inobj);

        if (Thread.currentThread().isInterrupted()) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("interrupted lalalala.....");
            //return ret(d, io, emptyList);
        }
        if (inobj == null) {
            Thread.currentThread().interrupt();
            throw new Error("interrupted inobj == null.....");
            //return ret(d, io, emptyList);
        }

        if (inobj instanceof String) {
            //return ret(d, io, Env.getVar(env, (String) inobj));
            //return Env.getVar(env, (String) inobj);

            Object v = Env.getVar(env, (String) inobj);
            if (v.equals(inobj))
                return v;
            else {
                tray(d, io, inobj);
                return ret(d, io, v);
            }
        }

        else if (inobj instanceof ConsList) {

            tray(d, io, inobj);

            ConsList l = (ConsList) inobj;
            if (l.isEmpty()) return ret(d, io, l);

            ConsList ls = l.cdr;
            Object op = eval(d, ls.isEmpty() ? strike : true, io, env, l.car), v;

            if (op instanceof String) {
                String command = (String) op;

                switch (command) {
                    case "+": case "-": case "*": case "/": case "mod":
                        return ret(d, io, foldBinOp(d, command, io, env, ls));

                    case ">": case ">=": case "<": case "<=": case "=": case "/=":
                        return ret(d, io, foldCompOp(d, command, io, env, ls));

                    case "++":
                        String rs = "";
                        while (!ls.isEmpty())
                            {rs = rs + eval(d, true, io, env, ls.car).toString(); ls = ls.cdr;}
                        return ret(d, io, new RawString(rs));

                    case "eq?": return ret(d, io, foldEqObject(d, io, env, ls));

                    case "def":
                        while (!ls.isEmpty() && !ls.cdr.isEmpty()) {
                            String name = (ls.car instanceof String) ?
                                    (String)ls.car :
                                    eval(d, true, io, env, ls.car).toString();
                            //Env.defVar(env, (String) ls.car, eval(d, true, io, env, ls.cdr.car));
                            Env.defVar(env, name, eval(d, true, io, env, ls.cdr.car));
                            ls = ls.cdr.cdr;
                        }
                        return ret(d, io, rawStringOK);

                    case "set!":
                        while (!ls.isEmpty() && !ls.cdr.isEmpty()) {
                            String name = (ls.car instanceof String) ?
                                    (String)ls.car :
                                    eval(d, true, io, env, ls.car).toString();
                            //Env.setVar(env, (String) ls.car, eval(d, true, io, env, ls.cdr.car));
                            Env.setVar(env, name, eval(d, true, io, env, ls.cdr.car));
                            ls = ls.cdr.cdr;
                        }
                        return ret(d, io, rawStringOK);

                    case "get":
                        String name = (ls.car instanceof String) ?
                                (String)ls.car :
                                eval(d, true, io, env, ls.car).toString();
                        v = Env.getVar(env, name);
                        if (v.equals(ls.car))
                            return v;
                        else
                            return ret(d, io, v);

                    case "cons": return ret(d, io, evalCons(d, io, env, ls));

                    case "car":
                        v = eval(d, true, io, env, ls.car);
                        if (v instanceof ConsList) {
                            ConsList lv = (ConsList) v;
                            return ret(d, io, lv.isEmpty() ? emptyList : lv.car);
                        }
                        else return ret(d, io, v);

                    case "cdr":
                        v = eval(d, true, io, env, ls.car);
                        if (v instanceof ConsList) {
                            ConsList lv = (ConsList) v;
                            return ret(d, io, lv.isEmpty() ? emptyList : lv.cdr);
                        }
                        else return ret(d, io, emptyList);

                    case "quote": return ret(d, io, l.cdr.car);

                    case "cond":
                        while (!ls.isEmpty() && !ls.cdr.isEmpty()) {
                            if ((boolean)eval(d, true, io, env, ls.car))
                                return ret(d, io, eval(d, strike, io, env, ls.cdr.car));
                            ls = ls.cdr.cdr;
                        }
                        return ret(d, io, !ls.isEmpty() ? eval(d, strike, io, env, ls.car) : "");

                    case "while":
                        while ((boolean)eval(d, true, io, env, ls.car))
                            eval(d, true, io, env, ls.cdr);
                        return ret(d, io, rawStringOK);

                    case "eval":
                        return ret(d, io,
                                eval(d, true, io, env, eval(d, true, io, env, ls.car)));
/*
                    case "atom?":
                        v = eval(d, true, io, env, ls.car);
                        return ret(d, io, !(   v instanceof ConsList
                                ||  v instanceof Func
                                ||  v instanceof Macr));
                    case "list?":
                        return ret(d, io,
                                eval(d, true, io, env, ls.car) instanceof ConsList);
                    case "func?":
                        return ret(d, io,
                                eval(d, true, io, env, ls.car) instanceof Func);
                    case "macr?":
                        return ret(d, io, eval(d, true, io, env, ls.car) instanceof Macr);
*/
                    case "typeof":
                        v = ls.car;
                        if (v instanceof String) v = eval(d, true, io, env, ls.car);
                        switch (v.getClass().getSimpleName()) {
                            case "RawString":
                                return new RawString("String");
                            case "String":
                                return new RawString("Symbol");
                            default:
                                return new RawString(v.getClass().getSimpleName());}

                    case "print":
                        while (!ls.isEmpty()) {
                            io.out(false, eval(d, true, io, env, ls.car).toString());
                            ls = ls.cdr;
                        }
                        return ret(d, io, rawStringOK);

                    case "read":
                        String expression = io.in();
                        return ret(d, io, Read.tokens2LispVal(Read.tokens(expression)));

                    case "lambda":
                        return ret(d, io, new Func((ConsList) ls.car, getbody(ls.cdr), env));

                    case "macro":
                        return ret(d, io, new Macr((ConsList) ls.car, getbody(ls.cdr)));

                    //case "rec":
                    //    Func f = (Func) eval(d, true, io, env, ls.car);
                    //    return ret(d, io, new TailCall(f,
                    //            getEvalMapArgsVals(io, env, f.pars, ls.cdr)));
                    //    return ret(d, io, eval(d, true, io, env, ls));

                    default:
                        v = op;
                        while (!ls.isEmpty()) {
                            v = eval(d, ls.cdr.isEmpty() ? strike : true, io, env, ls.car);
                            ls = ls.cdr;
                        }
                        return ret(d, io, v);
                }

            } else if (op instanceof Func) {
                Func f = (Func)op;
                TailCall tcall = new TailCall(f, getEvalMapArgsVals(d, io, env, f.pars, ls));
                if (strike) {
                    v = tcall;
                    while (v instanceof TailCall) {
                        TailCall tc = (TailCall) v;
                        v = eval(d, false, io, new Env(tc.args, tc.f.clojure), tc.f.body);
                    }
                    return ret(d, io, v);
                } else return ret(d, io, tcall);

            } else if (op instanceof Macr) {
                Macr f = (Macr)op;

                //System.out.println(showVal(f));
                //System.out.println(showVal(l));

                HashMap <String, Object> m = new HashMap <String, Object> ();
                ConsList pa = f.pars;
                ConsList pv = ls;
                //System.out.println(ls.toString());
                while (!pa.isEmpty() && !pv.isEmpty()) {
                    if (pa.cdr.isEmpty() && !pv.cdr.isEmpty())
                        m.put((String) pa.car, pv);
                    else
                        m.put((String) pa.car, pv.car);
                    pa = pa.cdr;
                    pv = pv.cdr;
                }
                //System.out.println(m.toString());

                Object me = macroexpand(m, f.body);
                //System.out.println(showVal(me));

                return ret(d, io, eval(d, true, io, env, me));

            } else {
                v = op;
                while (!ls.isEmpty()) {
                    v = eval(d, ls.cdr.isEmpty() ? strike : true, io, env, ls.car);
                    ls = ls.cdr;
                }
                return ret(d, io, v);
            }

        } else if (inobj instanceof TailCall) {

            tray(d, io, inobj);

            Object v = inobj;
            while (v instanceof TailCall) {
                TailCall tc = (TailCall) v;
                v = eval(d, false, io, new Env(tc.args, tc.f.clojure), tc.f.body);
            }
            return ret(d, io, v);

        } else
            //return ret(d, io, inobj);
            return inobj;
    }

}

    /*
    private static void prepareCin(InterThreadJSplitPane io) throws BadLocationException {
        //Main.cout(io.textArea, false, ">> ");
        if (SwingUtilities.isEventDispatchThread()) {
            io.textArea.setEditable(true);
            //io.textArea.setBackground(Color.yellow);

            Document doc = io.textArea.getDocument();
            Highlighter highlighter = io.textArea.getHighlighter();
            Highlighter.HighlightPainter goodPainter =
                    new DefaultHighlighter.DefaultHighlightPainter(Color.cyan);

            int promptPosition = io.textArea.getLineEndOffset(io.textArea.getLineCount()-1);
            Object start = highlighter.addHighlight(
                    promptPosition - 10, promptPosition, goodPainter);

            io.textArea.requestFocus();
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    io.textArea.setEditable(true);
                    //io.textArea.setBackground(Color.yellow);


                    Document doc = io.textArea.getDocument();
                    Highlighter highlighter = io.textArea.getHighlighter();
                    Highlighter.HighlightPainter goodPainter =
                            new DefaultHighlighter.DefaultHighlightPainter(Color.cyan);

                    try {
                        int promptPosition = io.textArea.getLineEndOffset(io.textArea.getLineCount() - 1);
                        Object start = highlighter.addHighlight(
                                promptPosition - 10, promptPosition, goodPainter);
                    } catch(BadLocationException ex) {}


                    io.textArea.requestFocus();
                }
            });
        }
    }
    private static void releaseCin(InterThreadJSplitPane io) throws BadLocationException {
        if (SwingUtilities.isEventDispatchThread()) {
            io.textArea.setEditable(false);
            //io.textArea.setBackground(Color.white);
            io.textArea.append("\n");
            io.textAreaIn.requestFocus();
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    io.textArea.setEditable(false);
                    //io.textArea.setBackground(Color.white);
                    io.textArea.append("\n");
                    io.textAreaIn.requestFocus();
                }
            });
        }
    }
    private static String cin(InterThreadJSplitPane io) throws BadLocationException {

        prepareCin(io);
        int promptPosition = io.textArea.getLineEndOffset(io.textArea.getLineCount()-1);

        class Filter extends DocumentFilter {
            public void insertString(final FilterBypass fb, final int offset, final String string, final AttributeSet attr)
                    throws BadLocationException {
                if (offset >= promptPosition) {
                    super.insertString(fb, offset, string, attr);
                }
            }
            public void remove(final FilterBypass fb, final int offset, final int length)
                    throws BadLocationException {
                if (offset >= promptPosition) {
                    super.remove(fb, offset, length);
                }
            }
            public void replace(final FilterBypass fb, final int offset, final int length, final String text, final AttributeSet attrs)
                    throws BadLocationException {
                if (offset >= promptPosition) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
            //else Toolkit.getDefaultToolkit().beep();
        }

        AbstractDocument doc = (AbstractDocument)io.textArea.getDocument();
                //new DocumentSizeFilter(MAX_CHARACTERS)

        doc.setDocumentFilter(new Filter());






        Main.setPaneTabState(io, 2);
        io.cinString = "";
        io.isCin = true;
        while (io.isCin) {
            if (Thread.currentThread().isInterrupted()) {
                io.isCin = false;
                releaseCin(io);
                Thread.currentThread().interrupt();
                throw new RuntimeException("interrupted lalalala.....");
                //break;
            }
        }
        Main.setPaneTabState(io, 1);

        //return io.cinString;

        int endDoc = io.textArea.getLineEndOffset(io.textArea.getLineCount() - 1);

        doc.setDocumentFilter(null);
        releaseCin(io);

        return io.textArea.getText(promptPosition, endDoc-promptPosition).trim();
    }
    */

