package ivana.liscript.core;

import ivana.liscript.core.Eval.ConsList;

import java.util.ArrayList;
import java.util.LinkedList;

public class Read {

    private static void addToken(LinkedList<String> r, String s) {
        if (!r.isEmpty() && r.getLast().equals("'")) {
            r.remove(r.size() - 1);
            r.add("'" + s);
        } else r.add(s);
    }

    public static LinkedList<String> tokens(String s) {
        LinkedList<String> r = new LinkedList<>();

        int STATE_TOKEN = 0, STATE_STRING = 2, STATE_COMMENT = 3;
        int state = STATE_TOKEN;
        int ifrom = 0, ito = s.length(), ib = ifrom, lev = 0;

        for (int i = ifrom; i <= ito; ++i) {
            char c = i < ito ? s.charAt(i) : 0;

            if (state == STATE_STRING &&
                    //(c == '"' || c == '\n' || c == 0)) {
                    (c == '"' || c == 0)) {
                if (lev == 0) {
                    addToken(r, s.substring(ib, i + 1));
                    ib = i + 1;
                }
                state = STATE_TOKEN;
            } else if (state == STATE_COMMENT &&
                    //(c == ';' || c == '\n' || c == 0)) {
                    (c == ';' || c == 0)) {
                if (lev == 0) {
                    //r.add(s.substring(ib, i + 1));
                    ib = i + 1;
                }
                state = STATE_TOKEN;
            } else if (state == STATE_TOKEN) {
                if (c == ')') lev -= 1;
                if (lev == 0 &&
                        (Character.isWhitespace(c) || c == '(' || c == ')'
                                || c == '"' || c == ';' || c == 0)) {
                    int ie = c == ')' ? i + 1 : i;
                    if (ie > ib) addToken(r, s.substring(ib, ie));
                    ib = ie;
                    if (Character.isWhitespace(c)) ib += 1;
                }
                if (c == '(') lev += 1;
                else if (c == '"') state = STATE_STRING;
                else if (c == ';') state = STATE_COMMENT;
            }
        }
        return r;
    }

    private static Object readToken(String t) {
        int length = t.length();
        char fst = t.charAt(0), lst = t.charAt(length - 1);
        if (fst == '(' && lst == ')')
            return tokens2ConsList(tokens(t.substring(1, length - 1)));
        else if (fst == '"' && lst == '"')
            return t.substring(1, length - 1);
        else if (fst == '\'') {
            Object v = tokens2LispVal(tokens(t.substring(1, length)));
            return new ConsList(Eval.SpecialForm.QUOTE, new ConsList(v, Eval.emptyList));
        } else if (t.equals("true")) return true;
        else if (t.equals("false")) return false;
        else
            try { return Integer.parseInt(t);
            } catch (NumberFormatException eInteger) {
                try { return Long.parseLong(t);
                } catch (NumberFormatException eLong) {
                    try { return Double.parseDouble(t);
                    } catch (NumberFormatException eDouble) {
                        return Eval.specialFormWords.containsKey(t) ?
                                Eval.specialFormWords.get(t) : new Eval.Symbol(t);
                    }
                }
            }
    }

    private static ConsList tokens2ConsList(LinkedList<String> ts) {
        if (ts.isEmpty()) return Eval.emptyList;
        else {
            String h = ts.removeFirst();
            return new ConsList(readToken(h), tokens2ConsList(ts));
        }
    }

    public static Object tokens2LispVal(LinkedList<String> ts) {
        if (ts.size() == 1) return readToken(ts.getFirst());
        else return tokens2ConsList(ts);
    }

    public static Object string2LispVal(String s) {
        return tokens2LispVal(tokens(s));
    }

    private static String getShift(String s, int i) {
        if      (s.startsWith("cond", i+1)) return "      ";
        else if (s.startsWith("(", i+1)) return " ";
        else return "    ";
    }

    public static String prettyPrint(String s) {
        final StringBuilder sb = new StringBuilder();
        ArrayList<String> shifts = new ArrayList<>();

        int i = 0, wsFrom = -1, wsTo = -1, wsLines = 0; // lev = 0,
        while (i < s.length()) {
            char c = s.charAt(i);

            if (Character.isWhitespace(c)) {
                if (wsFrom < 0) wsFrom = i;
                wsTo = i;
                if (c == '\n') wsLines += 1;
                i++;
            } else {
                // append space
                char p = sb.length() == 0 ? 0 : sb.charAt(sb.length() - 1);
                if (!(sb.length() == 0 || c == ')' || p == '(')) {
                    if (wsLines > 0) {
                        sb.append("\n".repeat(wsLines));
                        for (String shift : shifts) sb.append(shift);
                    } else if (wsFrom >= 0) {
                        sb.append(s, wsFrom, wsTo + 1);
                    } else {
                        if (p == '"' || c == '"' || p != '(' && p != '\'' && c == '(' || p == ')' && c != ')')
                            sb.append(' ');
                    }
                }
                wsFrom = -1;
                wsTo = -1;
                wsLines = 0;
                // ----------------------------

                if (c == '"') {
                    int j = s.indexOf('"', i + 1);
                    j = j < 0 ? s.length() - 1 : j;
                    sb.append(s, i, j + 1);
                    i = j + 1;
                } else if (c == ';') {
                    int j = s.indexOf(';', i + 1);
                    j = j < 0 ? s.length() - 1 : j;
                    sb.append(s, i, j + 1);
                    i = j + 1;
                } else {
                    if (c == '(') shifts.add(getShift(s, i));
                    else if (c == ')') shifts.remove(shifts.size() - 1);
                    sb.append(c);
                    i++;
                }
            }
        }
        return sb.toString();
    }
}

/*
    public static LinkedList<String> tokens_(String s) {
        LinkedList<String> r = new LinkedList<String>();

        String t = "";
        Integer lev = 0;
        boolean f = false;
        for (Character c : (s + " ").toCharArray()) {
            if (c.toString().trim().isEmpty()) {}//continue;
            else if (c == '(' && !f) lev += 1;
            else if (c == ')' && !f) lev -= 1;
            else if (c == '"') f = !f;

            t += c.toString();
            if (    lev == 0 &&
                    !t.trim().isEmpty() &&
                    (c.toString().trim().isEmpty() || c == ')') &&
                    !f) {
                if (!t.trim().isEmpty()) r.add(t.trim());
                t = "";
                f = false;
            }
        }
        return r;
    }
*/
