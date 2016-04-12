package com.company;

import java.awt.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Производит вычисление переданного объекта в переданном окружении
 * является набором статических полей и методов - экземпляры класса не создается,
 * все требуемые параметры передаются напрямую в методы
 */
public class Eval {

    /** объект - пустой список, все списки заканчиваются им, чтобы не плодить их много*/
    public static ConsList emptyList = new ConsList(null, null);

    /** объект - сырая строка "ОК", возвращается когда все сделано и нечего вернуть,
     * чтобы не плодить их много*/
    public static RawString rawStringOK = new RawString("OK");

    /** интерфейс, реализующий ввод-вывод информации в строковом виде в процессе вычисления
     * выражения. Один из параметров, передаваемых методу eval должен реализовывать этот интерфейс*/
    public interface InOutable {
        void out(boolean newLine, String string);
        String in();
    }

    /** перечисление - список особых форм языка*/
    public enum SpecialForm {
        AR_ADD, AR_SUB, AR_MUL, AR_DIV, AR_MOD,
        AR_GR, AR_GR_EQ, AR_LOW, AR_LOW_EQ, AR_EQ, AR_NOT_EQ,
        ST_CONCAT,
        EQUAL, DEF, SET, GET, CONS, CAR, CDR, QUOTE, COND, WHILE, EVAL,
        TYPEOF, PRINT, READ, LAMBDA, MACRO
        , BEGIN
        , TRAY
        , CLASS
        , NEW
        , METHOD
        , JAVA
    }

    /** словарь - список соответствий ключевых слов текста скрипта особым формам языка*/
    public static HashMap<String, SpecialForm> specialFormWords = setSpecialFormWords();

    private static HashMap<String, SpecialForm> setSpecialFormWords() {
        HashMap<String, SpecialForm> keyWords = new HashMap<String, SpecialForm>();

        keyWords.put("+",          SpecialForm.AR_ADD);
        keyWords.put("-",          SpecialForm.AR_SUB);
        keyWords.put("*",          SpecialForm.AR_MUL);
        keyWords.put("/",          SpecialForm.AR_DIV);
        keyWords.put("mod",        SpecialForm.AR_MOD);
        keyWords.put(">",          SpecialForm.AR_GR);
        keyWords.put(">=",         SpecialForm.AR_GR_EQ);
        keyWords.put("<",          SpecialForm.AR_LOW);
        keyWords.put("<=",         SpecialForm.AR_LOW_EQ);
        keyWords.put("=",          SpecialForm.AR_EQ);
        keyWords.put("/=",         SpecialForm.AR_NOT_EQ);
        keyWords.put("++",         SpecialForm.ST_CONCAT);

        keyWords.put("eq?",        SpecialForm.EQUAL);
        keyWords.put("def",        SpecialForm.DEF);
        keyWords.put("set!",       SpecialForm.SET);
        keyWords.put("get",        SpecialForm.GET);
        keyWords.put("cons",       SpecialForm.CONS);
        keyWords.put("car",        SpecialForm.CAR);
        keyWords.put("cdr",        SpecialForm.CDR);
        keyWords.put("quote",      SpecialForm.QUOTE);
        keyWords.put("cond",       SpecialForm.COND);
        keyWords.put("while",      SpecialForm.WHILE);
        keyWords.put("eval",       SpecialForm.EVAL);
        keyWords.put("typeof",     SpecialForm.TYPEOF);
        keyWords.put("print",      SpecialForm.PRINT);
        keyWords.put("read",       SpecialForm.READ);
        keyWords.put("lambda",     SpecialForm.LAMBDA);
        keyWords.put("macro",      SpecialForm.MACRO);

        keyWords.put("begin",      SpecialForm.BEGIN);
        keyWords.put("tray",       SpecialForm.TRAY);
        keyWords.put("class",      SpecialForm.CLASS);
        keyWords.put("new",        SpecialForm.NEW);
        keyWords.put("method",     SpecialForm.METHOD);
        keyWords.put("java",       SpecialForm.JAVA);

        return keyWords;
    }


    //---------------------- TYPE LispVal AND ITS INSTANCES --------------------

    /** тип языка Liscript - односвязный список */
    public static class ConsList {
        /** объект - значение головы текущего списка */
        public Object car;
        /** список - значение хвоста текущего списка */
        public ConsList cdr;

        /** Конструктор со значениями головы и хвоста.
         * @param h объект - голова списка
         * @param t список - хвост списка
         */
        ConsList(Object h, ConsList t) { car = h; cdr = t; }

        /** проверяет, является ли список пустым
         * @return истина/ложь
         */
        public boolean isEmpty() { return this.car == null && this.cdr == null; }

        /** возвращает размер списка
         * @return размер
         */
        public int size() {
            int r = 0;
            ConsList p = this;
            while (!p.isEmpty()) {r += 1; p = p.cdr;}
            return r;
        }

        /** @return строковое представление текущего списка */
        @Override
        public String toString() { return showVal(this); }
    }

    /** тип языка Liscript - функция */
    public static class Func {
        /** односвязный список имен параметров функции */
        public ConsList pars;
        /** тело функции */
        public Object body;
        /** окружение, в котором создана функция */
        public Env clojure;

        /** Конструктор
         * @param p односвязный список имен параметров функции
         * @param b тело функции
         * @param c окружение, в котором создана функция
         */
        Func(ConsList p, Object b, Env c) { pars = p; body = b; clojure = c; }

        /** @return строковое представление функции */
        @Override
        public String toString() { return showVal(this); }
    }

    /** тип языка Liscript - макрос */
    public static class Macr {
        /** односвязный список имен параметров макроса */
        public ConsList pars;
        /** тело макроса */
        public Object body;

        /** Конструктор
         * @param p односвязный список имен параметров макроса
         * @param b тело макроса
         */
        Macr(ConsList p, Object b) { pars = p; body = b; }

        /** @return строковое представление макроса */
        @Override
        public String toString() { return showVal(this); }
    }

    /** тип языка Liscript - строка */
    public static class RawString {
        /** строка - содержимое */
        public String string = "";

        /** Конструктор
         * @param s строка - содержимое
         */
        RawString(String s) { string = s; }

        /** проверяет, является ли строка пустой
         * @return истина/ложь
         */
        public boolean isEmpty() { return string.isEmpty(); }

        /** @return строковое представление текущего типа */
        @Override
        public String toString() { return string; }
    }

    private static String showConsList(ConsList p) {
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

        else if (p instanceof SpecialForm) {
            String r = p.toString();
            for (String key : specialFormWords.keySet()) {
                if (p.equals(specialFormWords.get(key))) {
                    r = key;
                    break;
                }
            }
            sb.append(r);

        } else if (p instanceof RawString) {
            sb.append("\"");
            sb.append(((RawString) p).string);
            sb.append("\"");

        } else if (p instanceof ConsList) {
            sb.append("(");
            sb.append(showConsList((ConsList) p));
            sb.append(")");

        } else if (p instanceof Func) {
            Func f = (Func) p;
            sb.append("(lambda ");
            sb.append(showVal(f.pars));
            sb.append(" ");
            sb.append(showVal(f.body));
            sb.append(")");

        } else if (p instanceof Macr) {
            Macr f = (Macr) p;
            sb.append("(macro ");
            sb.append(showVal(f.pars));
            sb.append(" ");
            sb.append(showVal(f.body));
            sb.append(")");

        } else sb.append(p.toString());

        return sb.toString();
    }

    //---------------------------------- EVAL ----------------------------------

    private static Number BinOp(SpecialForm op, Number ina, Number inb) {
        if (ina instanceof Double || inb instanceof Double) {
            double a = ina.doubleValue();
            double b = inb.doubleValue();
            switch (op) {
                case AR_ADD:
                    return a + b;
                case AR_SUB:
                    return a - b;
                case AR_MUL:
                    return a * b;
                case AR_DIV:
                    return a / b;
                case AR_MOD:
                    return a % b;
                default:
                    return a;
            }
        } else {
            int a = ina.intValue();
            int b = inb.intValue();
            switch (op) {
                case AR_ADD:
                    return a + b;
                case AR_SUB:
                    return a - b;
                case AR_MUL:
                    return a * b;
                case AR_DIV:
                    return a / b;
                case AR_MOD:
                    return a % b;
                default:
                    return a;
            }
        }
    }
    private static boolean CompOp(SpecialForm op, Number ina, Number inb) {
        if (ina instanceof Double || inb instanceof Double) {
            double a = ina.doubleValue();
            double b = inb.doubleValue();
            switch (op) {
                case AR_GR:
                    return a > b;
                case AR_GR_EQ:
                    return a >= b;
                case AR_LOW:
                    return a < b;
                case AR_LOW_EQ:
                    return a <= b;
                case AR_EQ:
                    return a == b;
                case AR_NOT_EQ:
                    return a != b;
                default:
                    return true;
            }
        } else {
            int a = ina.intValue();
            int b = inb.intValue();
            switch (op) {
                case AR_GR:
                    return a > b;
                case AR_GR_EQ:
                    return a >= b;
                case AR_LOW:
                    return a < b;
                case AR_LOW_EQ:
                    return a <= b;
                case AR_EQ:
                    return a == b;
                case AR_NOT_EQ:
                    return a != b;
                default:
                    return true;
            }
        }
    }
    private static Number object2Number(Object o) throws RuntimeException {
        try {
            return (Number) o;
        } catch (Throwable ex) {
            if (o instanceof String)
                throw new RuntimeException("не связанная переменная: " + o.toString());
            else
                throw new RuntimeException("ошибка преобразования в число: " + o.getClass().getSimpleName());
        }
    }
    private static boolean object2boolean(Object o) throws RuntimeException {
        try {
            return (boolean) o;
        } catch (Throwable ex) {
            if (o instanceof String)
                throw new RuntimeException("не связанная переменная: " + o.toString());
            else
                throw new RuntimeException("ошибка преобразования в булевский тип: "
                        + o.getClass().getSimpleName());
        }
    }
    private static Number foldBinOp(int d, SpecialForm op, InOutable io, Env env, ConsList l)
        throws RuntimeException {

        if (l.isEmpty())
            throw new RuntimeException("нет аргументов для арифметической операции: " + op);

        Number r = object2Number(eval(d, true, io, env, l.car));
        l = l.cdr;
        while (!l.isEmpty()) {
            r = BinOp(op, r, object2Number(eval(d, true, io, env, l.car)));
            l = l.cdr;
        }
        return r;
    }
    private static boolean foldCompOp(int d, SpecialForm op, InOutable io, Env env, ConsList l) {
        if (l.isEmpty()) return true;

        Number a = object2Number(eval(d, true, io, env, l.car));
        l = l.cdr;
        while (!l.isEmpty()) {
            Number b = object2Number(eval(d, true, io, env, l.car));
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

    private static HashMap<String, Object> getMapArgsVals(
            int d, InOutable io, Env env, ConsList pa, ConsList pv, boolean evalVals) {

        HashMap<String, Object> m = new HashMap<String, Object>();
        while (!pa.isEmpty() && !pv.isEmpty()) {
            if (pa.cdr.isEmpty() && !pv.cdr.isEmpty())
                m.put((String) pa.car, evalVals ? evalCons(d, io, env, pv) : pv);
            else
                m.put((String) pa.car, evalVals ? eval(d, true, io, env, pv.car) : pv.car);
            pa = pa.cdr;
            pv = pv.cdr;
        }
        return m;
    }

    private static Object ret(int d, InOutable io, Object o) {
        if (d >= 0) {
            io.out(false, cntd(d) + Character.toString((char) 8594) + " ");
            io.out(true, o.toString());
        }
        return o;
    }

    private static void tray(int d, InOutable io, Object o) {
        if (d < 0) return;
        io.out(false, cntd(d) + Character.toString((char)8592) + " ");
        io.out(true, o.toString());
    }

    private static String cntd(int d) { return String.format("%" + 2*d + "s", " ") + d + " "; }

    private static Class classForName(String name) throws RuntimeException {
        switch (name) {
            case "boolean": return boolean.class;
            case "byte":    return byte.class;
            case "char":    return char.class;
            case "short":   return short.class;
            case "int":     return int.class;
            case "long":    return long.class;
            case "float":   return float.class;
            case "double":  return double.class;
            default:
                try {
                    return Class.forName(name);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
        }
    }

    private static HashMap<String, Method> methodsHash = new HashMap<>();

    private static Constructor constructorForParams(Class c, Class[] paramTypes)
            throws RuntimeException {
/*
        String keyHash = c.getName() + name + Arrays.hashCode(paramTypes);
        //System.out.println(keyHash);
        if (methodsHash.containsKey(keyHash)) {
            //System.out.println("Нашли!");
            return methodsHash.get(keyHash);
        }
        */

        //Method[] ms = c.getDeclaredMethods();
        Constructor[] cns = c.getConstructors();
        ArrayList<Constructor> f1 = new ArrayList<Constructor>();
        for(Constructor cn : cns) {
            if (    !Modifier.isPublic(cn.getModifiers())
                    || (!cn.isVarArgs() && paramTypes.length != cn.getParameterCount())
                    || (paramTypes.length != cn.getParameterCount())
                    ) continue;
            f1.add(cn);
        }
        if(f1.size() == 0)
            throw new RuntimeException("У класса " + c.getName()
                    + " нет подходящего конструктора");
        else if (f1.size() == 1) {
            //methodsHash.put(keyHash, f1.get(0));
            return f1.get(0);
        }

        //methodsHash.put(keyHash, f1.get(0));
        //return f1.get(0);

        ArrayList<Constructor> f2 = new ArrayList<Constructor>();
        for(Constructor cn : f1) {
            if (!Arrays.equals(cn.getParameterTypes(), paramTypes)) continue;
            f2.add(cn);
        }
        if(f2.size() == 0)
            throw new RuntimeException("У класса " + c.getName()
                    + " конструктор не подходит по типам параметров");
        else if (f2.size() == 1) {
            //methodsHash.put(keyHash, f2.get(0));
            return f2.get(0);
        } else throw new RuntimeException("У класса " + c.getName()
                    + " " + f2.size() + " конструктора с данными типами параметров");
    }

    private static Method methodForName(Class c, String name, Class[] paramTypes)
            throws RuntimeException {

        String keyHash = c.getName() + name + Arrays.hashCode(paramTypes);
        //System.out.println(keyHash);
        if (methodsHash.containsKey(keyHash)) {
            //System.out.println("Нашли!");
            return methodsHash.get(keyHash);
        }

        //Method[] ms = c.getDeclaredMethods();
        Method[] ms = c.getMethods();
        ArrayList<Method> f1 = new ArrayList<Method>();
        for(Method m : ms) {
            if (    !Modifier.isPublic(m.getModifiers())
                    || !m.getName().equals(name)
                    || (!m.isVarArgs() && paramTypes.length != m.getParameterCount())
                    || (paramTypes.length > m.getParameterCount())
                    ) continue;
                f1.add(m);
        }
        if(f1.size() == 0)
            throw new RuntimeException("У класса " + c.getName()
                    + " нет подходящего метода с именем " + name);
        else if (f1.size() == 1) {
            methodsHash.put(keyHash, f1.get(0));
            return f1.get(0);
        }

        methodsHash.put(keyHash, f1.get(0));
        return f1.get(0);
/*
        ArrayList<Method> f2 = new ArrayList<Method>();
        for(Method m : f1) {
            System.out.println(name + "----------------------------");
            System.out.println(Arrays.toString(m.getParameterTypes()));
            System.out.println(Arrays.toString(paramTypes));
            if (!Arrays.equals(m.getParameterTypes(), paramTypes)) continue;
            f2.add(m);
        }
        if(f2.size() == 0)
            throw new RuntimeException("У класса " + c.getName()
                    + " метод " + name + " не подходит по типам параметров");
        else if (f2.size() == 1) {
            methodsHash.put(keyHash, f2.get(0));
            return f2.get(0);
        } else throw new RuntimeException("У класса " + c.getName()
                    + " " + f2.size() + " метода " + name + " с данными типами параметров");
*/
    }

    private static Class classValue(Object v) {
        if (v instanceof Boolean)   return boolean.class;
        if (v instanceof Byte)      return byte.class;
        if (v instanceof Character) return char.class;
        if (v instanceof Short)     return short.class;
        if (v instanceof Integer)   return int.class;
        if (v instanceof Long)      return long.class;
        if (v instanceof Float)     return float.class;
        if (v instanceof Double)    return double.class;
        else return v.getClass();
    }

    private static Object invokeMethod(int d, InOutable io, Env env,
                Class c, Object o, String name, ConsList params) {

        if (name.equals("castComponent")) return (Component) o;
        //else

        int paramCnt = 0, paramsSize = params.size();
        ConsList p = params;
        Class[] paramTypes = new Class[paramsSize];
        Object[] paramValues = new Object[paramsSize];
        while (!p.isEmpty()) {
            Object v = eval(d, true, io, env, p.car);
            //paramTypes[paramCnt] = Class.forName(n);
            //paramTypes[paramCnt] = classForName(n);
            paramValues[paramCnt] = v;
            paramTypes[paramCnt] = classValue(v); //v.getClass();
            paramCnt += 1;
            p = p.cdr;
        }

        if(name.equals("new")) {
            Constructor cn = constructorForParams(c, paramTypes);
            try {
                Object r = null;
                if (paramsSize == 0) {
                    r = cn.newInstance();
                } else if (paramsSize == 1) {
                    r = cn.newInstance(paramValues[0]);
                } else if (paramsSize == 2) {
                    r = cn.newInstance(paramValues[0], paramValues[1]);
                } else if (paramsSize == 3) {
                    r = cn.newInstance(paramValues[0], paramValues[1], paramValues[2]);
                } else if (paramsSize == 4) {
                    r = cn.newInstance(paramValues[0], paramValues[1], paramValues[2], paramValues[3]);
                } else if (paramsSize == 5) {
                    r = cn.newInstance(paramValues[0],
                            paramValues[1],
                            paramValues[2],
                            paramValues[3],
                            paramValues[4]);
                } else throw new RuntimeException("Конструктор " + c.getName()
                        + " - параметров больше чем реализовано");
                return ret(d, io, r);
            } catch (Throwable e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        Method m = methodForName(c, name, paramTypes);
        Object mobj = o;
        if (Modifier.isStatic(m.getModifiers())) mobj = null;

        try {
            Object r = null;
            if (paramsSize == 0) {
                r = m.invoke(mobj);
            } else if (paramsSize == 1) {
                r = m.invoke(mobj, paramValues[0]);
            } else if (paramsSize == 2) {
                r = m.invoke(mobj, paramValues[0], paramValues[1]);
            } else if (paramsSize == 3) {
                r = m.invoke(mobj, paramValues[0], paramValues[1], paramValues[2]);
            } else if (paramsSize == 4) {
                r = m.invoke(mobj, paramValues[0], paramValues[1], paramValues[2], paramValues[3]);
            } else if (paramsSize == 5) {
                r = m.invoke(mobj, paramValues[0], paramValues[1], paramValues[2],
                        paramValues[3], paramValues[4]);
            } else if (paramsSize == 6) {
                r = m.invoke(mobj, paramValues[0], paramValues[1], paramValues[2],
                        paramValues[3], paramValues[4], paramValues[5]);
            } else throw new RuntimeException("Метод " + name + "параметров больше чем " +
                    "реализовано");
            return ret(d, io, r == null ? rawStringOK : r);
        } catch (Throwable e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }


        /** вычисляет значение переданного выражения
         * @param ind текущий уровень вложенности рекурсивных вызовов, для трассировки
         * @param strike строгое/ленивое вычисление применения функции к аргументам - для ТСО
         * @param io объект, реализующий интерфейс InOutable, для ввода-вывода при вычислении
         * @param env окружение (иерархическое), в котором производится вычисление
         * @param inobj объект, который надо вычислить
         * @return вычисленное значение
         */
    public static Object eval(int ind, boolean strike, InOutable io, Env env, Object inobj) {

        int d = ind + (ind < 0 ? 0 : 1);
        //tray(d, io, inobj);

        if (Thread.currentThread().isInterrupted()) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("interrupted lalalala.....");
            //return ret(d, io, emptyList);

        } else if (inobj == null) {
            Thread.currentThread().interrupt();
            throw new Error("interrupted inobj == null.....");
            //return ret(d, io, emptyList);

        } else if (inobj instanceof String) {
            //return ret(d, io, Env.getVar(env, (String) inobj));
            //return Env.getVar(env, (String) inobj);

            //Object v = Env.getVar(env, (String) inobj);
            Object v = env.getVar((String) inobj);
            if (v.equals(inobj))
                return v;
            else {
                tray(d, io, inobj);
                return ret(d, io, v);
            }

        } else if (inobj instanceof ConsList) {

            tray(d, io, inobj);

            ConsList l = (ConsList) inobj;
            if (l.isEmpty()) return ret(d, io, l);

            ConsList ls = l.cdr;
            Object op = eval(d, ls.isEmpty() ? strike : true, io, env, l.car), v;
            String name;

            if (op instanceof SpecialForm) {
                SpecialForm sf = (SpecialForm) op;

                switch (sf) {
                    case CLASS:
                        name = (ls.car instanceof String) ?
                                (String)ls.car :
                                eval(d, true, io, env, ls.car).toString();
                        return ret(d, io, classForName(name));

                    case NEW:
                        try {
                            Class cls = (Class) eval(d, true, io, env, ls.car);
                            return ret(d, io, cls.newInstance());
                            //} catch (InstantiationException e) {
                            //    throw new RuntimeException(e.getMessage(), e);
                            //} catch (IllegalAccessException e) {
                            //    throw new RuntimeException(e.getMessage(), e);
                        } catch (Throwable e) {
                            throw new RuntimeException(e.getMessage(), e);
                        }

                    case JAVA:
                        v = eval(d, true, io, env, ls.car);
                        name = (ls.cdr.car instanceof String) ?
                                (String)ls.cdr.car :
                                eval(d, true, io, env, ls.cdr.car).toString();
                        if (v instanceof Class)
                            return invokeMethod(d, io, env, (Class) v, null, name, ls.cdr.cdr);
                        else
                            return invokeMethod(d, io, env, v.getClass(), v, name, ls.cdr.cdr);

                    case METHOD:
                        name = (ls.cdr.car instanceof String) ?
                                (String)ls.cdr.car :
                                eval(d, true, io, env, ls.cdr.car).toString();
                        try {
                            Class cls = (Class) eval(d, true, io, env, ls.car);
                            int paramCnt = 0;
                            ConsList p = ls.cdr.cdr;
                            Class[] paramTypes = new Class[p.size()];
                            while (!p.isEmpty()) {
                                String n = (p.car instanceof String) ?
                                        (String)p.car :
                                        eval(d, true, io, env, p.car).toString();
                                //paramTypes[paramCnt] = Class.forName(n);
                                paramTypes[paramCnt] = classForName(n);
                                paramCnt += 1;
                                p = p.cdr;
                            }

                            //Class[] paramTypes = new Class[2];
                            //paramTypes[0] = Component.class; // JFrame.class;
                            //paramTypes[1] = Object.class; //String.class;
                            //String methodName = name;
                            //io.out(true, name);
                            //io.out(true, v.getClass().toString());
                            //JOptionPane.showMessageDialog(Main.application, "test param");
                            //Instantiate an object of type method that returns you method name

                        //    Method m = v.getClass().getDeclaredMethod(methodName, paramTypes);
                            Method m = cls.getDeclaredMethod(name, paramTypes);

                            //invoke method with actual params
                            //Object r = m.invoke(v, Main.application, mess);
                            //return ret(d, io, r==null ? rawStringOK : r);
                            return ret(d, io, m);

                            //} catch (NoSuchMethodException e) {
                            //    throw new RuntimeException(e.getMessage(), e);
                            //} catch (InvocationTargetException e) {
                            //    throw new RuntimeException(e.getMessage(), e);
                            //} catch (IllegalAccessException e) {
                            //   throw new RuntimeException(e.getMessage(), e);
                        } catch (Throwable e) {
                            throw new RuntimeException(e.getMessage(), e);
                        }

                    case AR_ADD: case AR_SUB: case AR_MUL: case AR_DIV: case AR_MOD:
                        return ret(d, io, foldBinOp(d, sf, io, env, ls));

                    case AR_GR: case AR_GR_EQ: case AR_LOW: case AR_LOW_EQ: case AR_EQ: case AR_NOT_EQ:
                        return ret(d, io, foldCompOp(d, sf, io, env, ls));

                    case ST_CONCAT:
                        String rs = "";
                        while (!ls.isEmpty()) {
                            rs = rs + eval(d, true, io, env, ls.car).toString();
                            ls = ls.cdr;}
                        return ret(d, io, new RawString(rs));

                    case EQUAL: return ret(d, io, foldEqObject(d, io, env, ls));

                    case DEF:
                        while (!ls.isEmpty() && !ls.cdr.isEmpty()) {
                            name = (ls.car instanceof String) ?
                                    (String)ls.car :
                                    eval(d, true, io, env, ls.car).toString();
                            env.defVar(name, eval(d, true, io, env, ls.cdr.car));
                            ls = ls.cdr.cdr;
                        }
                        return ret(d, io, rawStringOK);

                    case SET:
                        while (!ls.isEmpty() && !ls.cdr.isEmpty()) {
                            name = (ls.car instanceof String) ?
                                    (String)ls.car :
                                    eval(d, true, io, env, ls.car).toString();
                            env.setVar(name, eval(d, true, io, env, ls.cdr.car));
                            ls = ls.cdr.cdr;
                        }
                        return ret(d, io, rawStringOK);

                    case GET:
                        name = (ls.car instanceof String) ?
                                (String)ls.car :
                                eval(d, true, io, env, ls.car).toString();
                        v = env.getVar(name);
                        //if (v.equals(ls.car))
                        //    return v;
                        //else
                        return ret(d, io, v);

                    case CONS: return ret(d, io, evalCons(d, io, env, ls));

                    case CAR:
                        v = eval(d, true, io, env, ls.car);
                        if (v instanceof ConsList) {
                            ConsList lv = (ConsList) v;
                            return ret(d, io, lv.isEmpty() ? emptyList : lv.car);
                        }
                        else return ret(d, io, v);

                    case CDR:
                        v = eval(d, true, io, env, ls.car);
                        if (v instanceof ConsList) {
                            ConsList lv = (ConsList) v;
                            return ret(d, io, lv.isEmpty() ? emptyList : lv.cdr);
                        }
                        else return ret(d, io, emptyList);

                    case QUOTE: return ret(d, io, ls.car);

                    case TRAY: return ret(d, io, eval(0, strike, io, env, getbody(ls)));

                    case COND:
                        while (!ls.isEmpty() && !ls.cdr.isEmpty()) {
                            if (object2boolean(eval(d, true, io, env, ls.car)))
                                return ret(d, io, eval(d, strike, io, env, ls.cdr.car));
                            ls = ls.cdr.cdr;
                        }
                        return ret(d, io,
                                !ls.isEmpty() ? eval(d, strike, io, env, ls.car) : rawStringOK);

                    case WHILE:
                        while (object2boolean(eval(d, true, io, env, ls.car)))
                            eval(d, true, io, env, ls.cdr);
                        return ret(d, io, rawStringOK);

                    case EVAL:
                        return ret(d, io, eval(d, true, io, env, eval(d, true, io, env, ls.car)));

                    case TYPEOF:
                        //v = ls.car;
                        v = eval(d, true, io, env, ls.car);
                        if (v instanceof String) v = eval(d, true, io, env, ls.car);
                        switch (v.getClass().getSimpleName()) {
                            case "RawString":
                                return new RawString("String");
                            case "String":
                                return new RawString("Symbol");
                            default:
                                return new RawString(v.getClass().getSimpleName());}

                    case PRINT:
                        while (!ls.isEmpty()) {
                            io.out(false, eval(d, true, io, env, ls.car).toString());
                            ls = ls.cdr;
                        }
                        return ret(d, io, rawStringOK);

                    case READ: return ret(d, io, Read.string2LispVal(io.in()));

                    case LAMBDA:
                        return ret(d, io, new Func((ConsList) ls.car, getbody(ls.cdr), env));

                    case MACRO:
                        return ret(d, io, new Macr((ConsList) ls.car, getbody(ls.cdr)));

                    //case "rec":
                    //    Func f = (Func) eval(d, true, io, env, ls.car);
                    //    return ret(d, io, new TailCall(f,
                    //            getEvalMapArgsVals(io, env, f.pars, ls.cdr)));
                    //    return ret(d, io, eval(d, true, io, env, ls));

                    case BEGIN:
                    default:
                        v = op;
                        while (!ls.isEmpty()) {
                            v = eval(d, ls.cdr.isEmpty() ? strike : true, io, env, ls.car);
                            ls = ls.cdr; }
                        return ret(d, io, v);
                }

            } else if (op instanceof Func) {
                Func f = (Func)op;
                TailCall tcall = new TailCall(f, getMapArgsVals(d, io, env, f.pars, ls, true));
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
                Object me = macroexpand(getMapArgsVals(d, io, env, f.pars, ls, false), f.body);
                //io.out(false, me.toString());
                return ret(d, io, eval(d, true, io, env, me));

            } else if (op instanceof Method) {
                Method m = (Method)op;
                ConsList params;
                if (Modifier.isStatic(m.getModifiers())) {
                    v = null;
                    params = ls;
                } else {
                    v = eval(d, true, io, env, ls.car);
                    params = ls.cdr;
                }
                try {
                    int paramSize = params.size();
                    Object r = null;
                    if (paramSize == 0) {
                        r = m.invoke(v);
                    } else if (paramSize == 1) {
                        r = m.invoke(v, eval(d, true, io, env, params.car));
                    } else if (paramSize == 2) {
                        r = m.invoke(v,
                                eval(d, true, io, env, params.car),
                                eval(d, true, io, env, params.cdr.car)
                        );
                    }
                    return ret(d, io, r == null ? rawStringOK : r);
                    //} catch (NoSuchMethodException e) {
                    //    throw new RuntimeException(e.getMessage(), e);
                    //} catch (InvocationTargetException e) {
                    //    throw new RuntimeException(e.getMessage(), e);
                    //} catch (IllegalAccessException e) {
                    //   throw new RuntimeException(e.getMessage(), e);
                } catch (Throwable e) {
                    throw new RuntimeException(e.getMessage(), e);
                }

            } else {
                v = op;
                while (!ls.isEmpty()) {
                    v = eval(d, ls.cdr.isEmpty() ? strike : true, io, env, ls.car);
                    ls = ls.cdr; }
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

