package com.company;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;


/**
 * Производит вычисление переданного объекта в переданном окружении
 * является набором статических полей и методов - экземпляры класса не создается,
 * все требуемые параметры передаются напрямую в методы
 */
public class Eval {

    /**
     * объект - пустой список, все списки заканчиваются им, чтобы не плодить их много
     */
    public static ConsList emptyList = new ConsList(null, null);

    /**
     * объект - строка "ОК", возвращается когда все сделано и нечего вернуть,
     * чтобы не плодить их много
     */
    public static String stringOK = "OK"; //new RawString("OK");

    /**
     * интерфейс, реализующий ввод-вывод информации в строковом виде в процессе вычисления
     * выражения. Один из параметров, передаваемых методу eval должен реализовывать этот интерфейс
     */
    public interface InOutable {
        void out(boolean newLine, String string);

        String in();
    }

    /**
     * перечисление - список особых форм языка
     */
    public enum SpecialForm {
        AR_ADD, AR_SUB, AR_MUL, AR_DIV, AR_MOD,
        AR_GR, AR_GR_EQ, AR_LOW, AR_LOW_EQ, AR_EQ, AR_NOT_EQ,
        ST_CONCAT,
        EQUAL, DEF, SET, GET, CONS, CAR, CDR, QUOTE, COND, EVAL, EVAL_IN_CONTEXT,
        TYPEOF, PRINT, READ, LAMBDA, MACRO, BEGIN, TRAY,
        CLASS, JAVA, COMPILE
    }

    /**
     * словарь - список соответствий ключевых слов текста скрипта особым формам языка
     */
    public static HashMap<String, SpecialForm> specialFormWords = setSpecialFormWords();

    private static HashMap<String, SpecialForm> setSpecialFormWords() {
        HashMap<String, SpecialForm> keyWords = new HashMap<String, SpecialForm>();

        keyWords.put("+", SpecialForm.AR_ADD);
        keyWords.put("-", SpecialForm.AR_SUB);
        keyWords.put("*", SpecialForm.AR_MUL);
        keyWords.put("/", SpecialForm.AR_DIV);
        keyWords.put("mod", SpecialForm.AR_MOD);
        keyWords.put(">", SpecialForm.AR_GR);
        keyWords.put(">=", SpecialForm.AR_GR_EQ);
        keyWords.put("<", SpecialForm.AR_LOW);
        keyWords.put("<=", SpecialForm.AR_LOW_EQ);
        keyWords.put("=", SpecialForm.AR_EQ);
        keyWords.put("/=", SpecialForm.AR_NOT_EQ);
        keyWords.put("++", SpecialForm.ST_CONCAT);

        keyWords.put("eq?", SpecialForm.EQUAL);
        keyWords.put("def", SpecialForm.DEF);
        keyWords.put("set!", SpecialForm.SET);
        keyWords.put("get", SpecialForm.GET);
        keyWords.put("cons", SpecialForm.CONS);
        keyWords.put("car", SpecialForm.CAR);
        keyWords.put("cdr", SpecialForm.CDR);
        keyWords.put("quote", SpecialForm.QUOTE);
        keyWords.put("cond", SpecialForm.COND);
        keyWords.put("eval", SpecialForm.EVAL);
        keyWords.put("eval-in", SpecialForm.EVAL_IN_CONTEXT);
        keyWords.put("typeof", SpecialForm.TYPEOF);
        keyWords.put("print", SpecialForm.PRINT);
        keyWords.put("read", SpecialForm.READ);
        keyWords.put("lambda", SpecialForm.LAMBDA);
        keyWords.put("macro", SpecialForm.MACRO);

        keyWords.put("begin", SpecialForm.BEGIN);
        keyWords.put("tray", SpecialForm.TRAY);
        keyWords.put("class", SpecialForm.CLASS);
        keyWords.put("java", SpecialForm.JAVA);
        keyWords.put("compile", SpecialForm.COMPILE);

        return keyWords;
    }


    //---------------------- TYPE LispVal AND ITS INSTANCES --------------------

    /**
     * тип языка Liscript - односвязный список
     */
    public static class ConsList {
        /**
         * объект - значение головы текущего списка
         */
        public Object car;
        /**
         * список - значение хвоста текущего списка
         */
        public ConsList cdr;

        /**
         * Конструктор со значениями головы и хвоста.
         *
         * @param h объект - голова списка
         * @param t список - хвост списка
         */
        ConsList(Object h, ConsList t) {
            car = h;
            cdr = t;
        }

        /**
         * проверяет, является ли список пустым
         *
         * @return истина/ложь
         */
        public boolean isEmpty() {
            return this.car == null && this.cdr == null;
        }

        /**
         * возвращает размер списка
         *
         * @return размер
         */
        public int size() {
            int r = 0;
            ConsList p = this;
            while (!p.isEmpty()) {
                r += 1;
                p = p.cdr;
            }
            return r;
        }

        /**
         * @return строковое представление текущего списка
         */
        @Override
        public String toString() {
            return showVal(this);
        }
    }

    /**
     * тип языка Liscript - функция
     */
    public static class Func {
        /**
         * односвязный список имен параметров функции
         */
        public ConsList pars;
        /**
         * тело функции
         */
        public Object body;
        /**
         * окружение, в котором создана функция
         */
        public Env clojure;

        /**
         * Конструктор
         *
         * @param p односвязный список имен параметров функции
         * @param b тело функции
         * @param c окружение, в котором создана функция
         */
        Func(ConsList p, Object b, Env c) {
            pars = p;
            body = b;
            clojure = c;
        }

        /**
         * @return строковое представление функции
         */
        @Override
        public String toString() {
            return showVal(this);
        }
    }

    /**
     * тип языка Liscript - макрос
     */
    public static class Macr {
        /**
         * односвязный список имен параметров макроса
         */
        public ConsList pars;
        /**
         * тело макроса
         */
        public Object body;

        /**
         * Конструктор
         *
         * @param p односвязный список имен параметров макроса
         * @param b тело макроса
         */
        Macr(ConsList p, Object b) {
            pars = p;
            body = b;
        }

        /**
         * @return строковое представление макроса
         */
        @Override
        public String toString() {
            return showVal(this);
        }
    }

    /**
     * тип языка Liscript - символ
     */
    public static class Symbol {
        /**
         * строка - имя символа
         */
        public String name = "";

        /**
         * Конструктор
         *
         * @param n строка - имя символа
         */
        Symbol(String n) {
            name = n;
        }

        /**
         * проверяет, является ли символ пустым
         *
         * @return истина/ложь
         */
        public boolean isEmpty() {
            return name.isEmpty();
        }

        /**
         * @return строковое представление текущего типа
         */
        @Override
        public String toString() {
            return name;
        }
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

        } else if (p instanceof String) {
            sb.append("\"");
            sb.append(p);
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

    //--------------------------- recursive EVAL ----------------------------------

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
            if (o instanceof Symbol)
                throw new RuntimeException("не связанная переменная: " + o.toString());
            else
                throw new RuntimeException("ошибка преобразования в число: "
                        + o.getClass().getSimpleName() + ": " + o.toString());
        }
    }

    private static boolean object2boolean(Object o) throws RuntimeException {
        try {
            return (boolean) o;
        } catch (Throwable ex) {
            if (o instanceof Symbol)
                throw new RuntimeException("не связанная переменная: " + o.toString());
            else
                throw new RuntimeException("ошибка преобразования в булевский тип: "
                        + o.getClass().getSimpleName() + ": " + o.toString());
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

        if (a instanceof Symbol) {
            String pa = a.toString(), pb = b.toString();
            return pa.equals(pb);
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
            ConsList p = (ConsList) o;
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

        if (o instanceof Symbol) {
            String s = o.toString();
            return map.containsKey(s) ? map.get(s) : o;
        } else if (o instanceof ConsList) {
            ConsList p = (ConsList) o;
            return p.isEmpty() ? emptyList :
                    new ConsList(macroexpand(map, p.car), (ConsList) macroexpand(map, p.cdr));
        } else return o;
    }

    private static class FuncCall {
        public Func f;
        public HashMap<String, Object> args;

        FuncCall(Func _f, HashMap<String, Object> _args) {
            f = _f;
            args = _args;
        }

        @Override
        public String toString() {
            return "FUNCALL: " + args.toString();
        }
    }

    private static HashMap<String, Object> getMapArgsVals(
            int d, InOutable io, Env env, ConsList pa, ConsList pv, boolean evalVals) {

        HashMap<String, Object> m = new HashMap<String, Object>();
        while (!pa.isEmpty() && !pv.isEmpty()) {
            if (pa.cdr.isEmpty() && !pv.cdr.isEmpty())
                m.put(pa.car.toString(), evalVals ? evalCons(d, io, env, pv) : pv);
            else
                m.put(pa.car.toString(), evalVals ? eval(d, true, io, env, pv.car) : pv.car);
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
        io.out(false, cntd(d) + Character.toString((char) 8592) + " ");
        io.out(true, o.toString());
    }

    private static String cntd(int d) {
        return String.format("%" + 2 * d + "s", " ") + d + " ";
    }

    private static Class classForName(String name) throws RuntimeException {
        switch (name) {
            case "boolean":
                return boolean.class;
            case "byte":
                return byte.class;
            case "char":
                return char.class;
            case "short":
                return short.class;
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            default:
                try {
                    return Class.forName(name);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
        }
    }

    private static Class compileClassForCode_(String code) throws Exception {
        // create the source
        //File sourceFile   = new File("/temp/Hello.java");
        File sourceFile = new File("Hello.java");
        FileWriter writer = new FileWriter(sourceFile);
/*
        writer.write(
                "public class Hello{ \n" +
                        " public void doit() { \n" +
                        "   System.out.println(\"Hello world\") ;\n" +
                        " }\n" +
                        "}"
        );
        (compile "public class Hello {public int doit() {return 33;}}")
        */
        writer.write("package com.company;\n" + code);
        writer.close();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager =
                compiler.getStandardFileManager(null, null, null);

//        fileManager.setLocation(StandardLocation.CLASS_OUTPUT,
//                //Arrays.asList(new File("/temp")));

        // Compile the file
        boolean callRes = compiler.getTask(null,
                fileManager,
                null,
                null,
                null,
                fileManager.getJavaFileObjectsFromFiles(Arrays.asList(sourceFile)))
                .call();
        fileManager.close();

        // delete the source file
        // sourceFile.deleteOnExit();

        if (callRes) {
            //System.out.println("Yipe");
            // Create a new custom class loader, pointing to the directory that contains the compiled
            // classes, this should point to the top of the package structure!
            URLClassLoader classLoader =
                    new URLClassLoader(new URL[]{new File("./").toURI().toURL()});
            // Load the class from the classloader by name....
            //Class<?> loadedClass = classLoader.loadClass("testcompile.HelloWorld");
            Class<?> loadedClass = classLoader.loadClass("Hello");

            /*
            // Create a new instance...
            Object obj = loadedClass.newInstance();
            // Santity check
            if (obj instanceof DoStuff) {
                // Cast to the DoStuff interface
                DoStuff stuffToDo = (DoStuff)obj;
                // Run it baby
                stuffToDo.doStuff();
            }
            */
            return loadedClass;

        } else throw new RuntimeException("Не смогли загрузить класс " + code);

        //runIt();
    }

    private static Class compileClassForCode__(String code) throws Exception {

        // create the source
        //File sourceFile   = new File("/temp/Hello.java");
//        File sourceFile = new File("Hello.java");
//        FileWriter writer = new FileWriter(sourceFile);
/*
(def cls (compile "
public class Hello {
    public int doit() {return 33;}
}
"))
(def cls-obj (java cls "new"))
(java cls-obj "doit")

        (compile "public class Hello {public int doit() {return 33;}}")
        */
//        writer.write("package com.company;\n" + code);
//        writer.close();

        File root = new File("/java"); // On Windows running on C:\, this is C:\java.
        File sourceFile = new File(root, "test/Hello.java");
        sourceFile.getParentFile().mkdirs();
        FileWriter writer = new FileWriter(sourceFile);
/*
        (compile "public class Hello {public int doit() {return 33;}}")
        */
        writer.write("package test;\n" + code);
        writer.close();


// Compile source file.
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        compiler.run(null, null, null, sourceFile.getPath());

// Load and instantiate compiled class.
        URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{root.toURI().toURL()});
        //Class<?> cls = Class.forName("test.Test", true, classLoader); // Should print "hello".
        Class<?> cls = Class.forName("test.Hello", true, classLoader);
        //Object instance = cls.newInstance(); // Should print "world".
        //System.out.println(instance); // Should print "test.Test@hashcode".

        // delete the source file
        sourceFile.deleteOnExit();

        return cls;
    }

    private static Class compileClassForCode(String code) throws Exception {
        File root = new File("/java");
        File sourceFile = new File(root, "test/Hello.java");
        sourceFile.getParentFile().mkdirs();
        FileWriter writer = new FileWriter(sourceFile);
        writer.write("package test;\n" + code);
        writer.close();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        compiler.run(null, null, null, sourceFile.getPath());

        URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{root.toURI().toURL()});
        Class<?> cls = Class.forName("test.Hello", true, classLoader);

        sourceFile.deleteOnExit();
        return cls;
    }

    private static Class compileClassForCode_____(String code) throws Exception {

        File root = new File("/java"); // On Windows running on C:\, this is C:\java.

// Load and instantiate compiled class.
        URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{root.toURI().toURL()});
        //Class<?> cls = Class.forName("test.Test", true, classLoader); // Should print "hello".
        Class<?> cls = Class.forName("test.Hello", true, classLoader);

        return cls;
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
        for (Constructor cn : cns) {
            if (!Modifier.isPublic(cn.getModifiers())
                    || (!cn.isVarArgs() && paramTypes.length != cn.getParameterCount())
                    || (paramTypes.length != cn.getParameterCount())
                    ) continue;
            f1.add(cn);
        }
        if (f1.size() == 0)
            throw new RuntimeException("У класса " + c.getName()
                    + " нет подходящего конструктора");
        else if (f1.size() == 1) {
            //methodsHash.put(keyHash, f1.get(0));
            return f1.get(0);
        }

        //methodsHash.put(keyHash, f1.get(0));
        //return f1.get(0);

        ArrayList<Constructor> f2 = new ArrayList<Constructor>();
        for (Constructor cn : f1) {
            if (!Arrays.equals(cn.getParameterTypes(), paramTypes)) continue;
            f2.add(cn);
        }
        if (f2.size() == 0)
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
        for (Method m : ms) {
            if (!Modifier.isPublic(m.getModifiers())
                    || !m.getName().equals(name)
                    || (!m.isVarArgs() && paramTypes.length != m.getParameterCount())
                    || (paramTypes.length > m.getParameterCount())
                    ) continue;
            f1.add(m);
        }
        if (f1.size() == 0)
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
        if (v instanceof Boolean) return boolean.class;
        if (v instanceof Byte) return byte.class;
        if (v instanceof Character) return char.class;
        if (v instanceof Short) return short.class;
        if (v instanceof Integer) return int.class;
        if (v instanceof Long) return long.class;
        if (v instanceof Float) return float.class;
        if (v instanceof Double) return double.class;
        else return v.getClass();
    }

    private static Object invokeMethod(int d, InOutable io, Env env,
                                       Class c, Object o, String name, ConsList params) {
        //if (name.equals("castComponent")) return (Component) o;

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

        if (name.equals("new")) {
            Constructor cn = constructorForParams(c, paramTypes);
            try {
                return cn.newInstance(paramValues);
            } catch (Throwable e) {throw new RuntimeException(e.getMessage(), e);}
        }

        Method m = methodForName(c, name, paramTypes);
        try {
            Object r = m.invoke(Modifier.isStatic(m.getModifiers()) ? null : o, paramValues);
            return r == null ? stringOK : r;
        } catch (Throwable e) {throw new RuntimeException(e.getMessage(), e);}
    }

    /**
     * вычисляет значение переданного выражения
     *
     * @param sl     текущий уровень вложенности рекурсивных вызовов, для трассировки
     * @param strict строгое/ленивое вычисление применения функции к аргументам - для ТСО
     * @param io     объект, реализующий интерфейс InOutable, для ввода-вывода при вычислении
     * @param env    окружение (иерархическое), в котором производится вычисление
     * @param inobj  объект, который надо вычислить
     * @return вычисленное значение
     */
    public static Object eval(int sl, boolean strict, InOutable io, Env env, Object inobj) {

        int d = sl + (sl < 0 ? 0 : 1);
        //tray(d, io, inobj);
/*
        if (1 == 2) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("commented recursive Eval.....");

        } else
*/
        if (Thread.currentThread().isInterrupted()) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("interrupted recursive Eval.....");
            //return ret(d, io, emptyList);

        } else if (inobj == null) {
            Thread.currentThread().interrupt();
            throw new Error("interrupted recursive Eval: inobj == null.....");
            //return ret(d, io, emptyList);

        } else if (inobj instanceof Symbol) {
            Object v = env.getVar(inobj.toString());
            if (v.equals(inobj.toString()))
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
            Object op = eval(d, ls.isEmpty() ? strict : true, io, env, l.car), v;
            String name;

            if (op instanceof SpecialForm) {
                SpecialForm sf = (SpecialForm) op;

                switch (sf) {
                    case CLASS:
                        name = (ls.car instanceof String) ?
                                (String) ls.car :
                                eval(d, true, io, env, ls.car).toString();
                        return ret(d, io, classForName(name));
/*
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
*/
                    case JAVA:
                        v = eval(d, true, io, env, ls.car);
                        name = (ls.cdr.car instanceof String) ?
                                (String) ls.cdr.car :
                                eval(d, true, io, env, ls.cdr.car).toString();
                        if (v instanceof Class)
                            return invokeMethod(d, io, env, (Class) v, null, name, ls.cdr.cdr);
                        else
                            return invokeMethod(d, io, env, v.getClass(), v, name, ls.cdr.cdr);

                    case COMPILE:
                        name = (ls.car instanceof String) ?
                                (String) ls.car :
                                eval(d, true, io, env, ls.car).toString();
                        try {
                            //compileClassForCode(name);
                            //return ret(d, io, stringOK);
                            return ret(d, io, compileClassForCode(name));

                        } catch (Throwable e) {
                            throw new RuntimeException(e.getMessage(), e);
                        }
/*
                    case METHOD:
                        name = (ls.cdr.car instanceof String) ?
                                (String) ls.cdr.car :
                                eval(d, true, io, env, ls.cdr.car).toString();
                        try {
                            Class cls = (Class) eval(d, true, io, env, ls.car);
                            int paramCnt = 0;
                            ConsList p = ls.cdr.cdr;
                            Class[] paramTypes = new Class[p.size()];
                            while (!p.isEmpty()) {
                                String n = (p.car instanceof String) ?
                                        (String) p.car :
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
*/
                    case AR_ADD:
                    case AR_SUB:
                    case AR_MUL:
                    case AR_DIV:
                    case AR_MOD:
                        return ret(d, io, foldBinOp(d, sf, io, env, ls));

                    case AR_GR:
                    case AR_GR_EQ:
                    case AR_LOW:
                    case AR_LOW_EQ:
                    case AR_EQ:
                    case AR_NOT_EQ:
                        return ret(d, io, foldCompOp(d, sf, io, env, ls));

                    case ST_CONCAT:
                        String rs = "";
                        while (!ls.isEmpty()) {
                            rs = rs + eval(d, true, io, env, ls.car).toString();
                            ls = ls.cdr;
                        }
                        return ret(d, io, rs);

                    case EQUAL:
                        return ret(d, io, foldEqObject(d, io, env, ls));

                    case DEF:
                        while (!ls.isEmpty() && !ls.cdr.isEmpty()) {
                            name = (ls.car instanceof Symbol) ?
                                    ls.car.toString() :
                                    eval(d, true, io, env, ls.car).toString();
                            env.defVar(name, eval(d, true, io, env, ls.cdr.car));
                            ls = ls.cdr.cdr;
                        }
                        return ret(d, io, stringOK);

                    case SET:
                        while (!ls.isEmpty() && !ls.cdr.isEmpty()) {
                            name = (ls.car instanceof Symbol) ?
                                    ls.car.toString() :
                                    eval(d, true, io, env, ls.car).toString();
                            env.setVar(name, eval(d, true, io, env, ls.cdr.car));
                            ls = ls.cdr.cdr;
                        }
                        return ret(d, io, stringOK);

                    case GET:
                        name = (ls.car instanceof Symbol) ?
                                ls.car.toString() :
                                eval(d, true, io, env, ls.car).toString();
                        v = env.getVar(name);
                        //if (v.equals(ls.car))
                        //    return v;
                        //else
                        return ret(d, io, v);

                    case CONS:
                        return ret(d, io, evalCons(d, io, env, ls));

                    case CAR:
                        v = eval(d, true, io, env, ls.car);
                        if (v instanceof ConsList) {
                            ConsList lv = (ConsList) v;
                            return ret(d, io, lv.isEmpty() ? emptyList : lv.car);
                        } else return ret(d, io, v);

                    case CDR:
                        v = eval(d, true, io, env, ls.car);
                        if (v instanceof ConsList) {
                            ConsList lv = (ConsList) v;
                            return ret(d, io, lv.isEmpty() ? emptyList : lv.cdr);
                        } else return ret(d, io, emptyList);

                    case QUOTE:
                        return ret(d, io, ls.car);

                    case TRAY:
                        return ret(d, io, eval(0, strict, io, env, getbody(ls)));

                    case COND:
                        while (!ls.isEmpty() && !ls.cdr.isEmpty()) {
                            if (object2boolean(eval(d, true, io, env, ls.car)))
                                return ret(d, io, eval(d, strict, io, env, ls.cdr.car));
                            ls = ls.cdr.cdr;
                        }
                        return ret(d, io,
                                !ls.isEmpty() ? eval(d, strict, io, env, ls.car) : stringOK);
/*
                    case WHILE:
                        while (object2boolean(eval(d, true, io, env, ls.car)))
                            eval(d, true, io, env, ls.cdr);
                        return ret(d, io, stringOK);
*/
                    case EVAL:
                        return ret(d, io, eval(d, true, io, env, eval(d, true, io, env, ls.car)));

                    case EVAL_IN_CONTEXT:
                        v = eval(d, true, io, env, ls.car);
                        if (v instanceof Func) {
                            Func f = (Func) v;
                            return ret(d, io, eval(d, true, io, f.clojure, ls.cdr));
                        } else
                            throw new RuntimeException("eval-in - 1 argument isn't lambda");

                    case TYPEOF:
                        //v = ls.car;
                        v = eval(d, true, io, env, ls.car);
                        /*
                        if (v instanceof String) v = eval(d, true, io, env, ls.car);
                        switch (v.getClass().getSimpleName()) {
                            case "RawString":
                                return new RawString("String");
                            case "String":
                                return new RawString("Symbol");
                            default:
                                return new RawString(v.getClass().getSimpleName());}
                                */
                        return v.getClass().getSimpleName();

                    case PRINT:
                        while (!ls.isEmpty()) {
                            io.out(false, eval(d, true, io, env, ls.car).toString());
                            ls = ls.cdr;
                        }
                        return ret(d, io, stringOK);

                    case READ:
                        return ret(d, io, Read.string2LispVal(io.in()));

                    case LAMBDA:
                        return ret(d, io, new Func((ConsList) ls.car, getbody(ls.cdr), env));

                    case MACRO:
                        return ret(d, io, new Macr((ConsList) ls.car, getbody(ls.cdr)));

                    //case "rec":
                    //    Func f = (Func) eval(d, true, io, env, ls.car);
                    //    return ret(d, io, new FuncCall(f,
                    //            getEvalMapArgsVals(io, env, f.pars, ls.cdr)));
                    //    return ret(d, io, eval(d, true, io, env, ls));

                    case BEGIN:
                    default:
                        v = op;
                        while (!ls.isEmpty()) {
                            v = eval(d, ls.cdr.isEmpty() ? strict : true, io, env, ls.car);
                            ls = ls.cdr;
                        }
                        return ret(d, io, v);
                }

            } else if (op instanceof Func) {
                Func f = (Func) op;
                FuncCall fcall = new FuncCall(f, getMapArgsVals(d, io, env, f.pars, ls, true));
                if (strict) {
                    v = fcall;
                    while (v instanceof FuncCall) {
                        FuncCall fc = (FuncCall) v;
                        v = eval(d, false, io, new Env(fc.args, fc.f.clojure), fc.f.body);
                    }
                    return ret(d, io, v);
                } else return ret(d, io, fcall);

                /*
                v = eval(d, false, io,
                        new Env(getMapArgsVals(d, io, env, f.pars, ls, true), f.clojure), f.body);
                return ret(d, io, v);
                */


            } else if (op instanceof Macr) {
                Macr f = (Macr) op;
                Object me = macroexpand(getMapArgsVals(d, io, env, f.pars, ls, false), f.body);
                //io.out(false, me.toString());
                return ret(d, io, eval(d, true, io, env, me));

            } else if (op instanceof Method) {
                Method m = (Method) op;
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
                    return ret(d, io, r == null ? stringOK : r);
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
                    v = eval(d, ls.cdr.isEmpty() ? strict : true, io, env, ls.car);
                    ls = ls.cdr;
                }
                return ret(d, io, v);
            }

        } else if (inobj instanceof FuncCall) {

            tray(d, io, inobj);

            Object v = inobj;
            while (v instanceof FuncCall) {
                FuncCall fc = (FuncCall) v;
                v = eval(d, false, io, new Env(fc.args, fc.f.clojure), fc.f.body);
            }
            return ret(d, io, v);

        } else
            //return ret(d, io, inobj);
            return inobj;
    }


    //--------------------------- iterative EVAL ----------------------------------


    public static class StackItem {
        public Env env;
        public ConsList p;
        public ArrayDeque<Object> l;

        StackItem(Env _env, ConsList _p) {
            env = _env;
            p = _p;
            l = new ArrayDeque<>();
        }

        public void upd(ConsList p) {
            this.p = p;
            this.l.clear();
        }

        public void upd(ConsList p, Env env) {
            this.upd(p);
            this.env = env;
        }
    }

    private static String showStack(ArrayDeque<StackItem> stack) {
        String s = "";
        for (StackItem si : stack) {
            s = s + si.l.toString() + " - " + si.p.toString() + "\n";
        }
        return s;
    }

    private static Number foldBinOpNoeval(SpecialForm op, ArrayDeque<Object> l)
            throws RuntimeException {
        if (l.isEmpty())
            throw new RuntimeException("нет аргументов для арифметической операции: " + op);

        Number r = object2Number(l.removeFirst());
        while (!l.isEmpty()) r = BinOp(op, r, object2Number(l.removeFirst()));
        return r;
    }

    private static boolean foldCompOpNoeval(SpecialForm op, ArrayDeque<Object> l) {
        if (l.isEmpty()) return true;

        Number a = object2Number(l.removeFirst());
        while (!l.isEmpty()) {
            Number b = object2Number(l.removeFirst());
            if (!CompOp(op, a, b)) return false;
            a = b;
        }
        return true;
    }

    private static boolean foldEqObjectNoeval(ArrayDeque<Object> l) {
        if (l.isEmpty()) return true;

        Object a = l.removeFirst();
        while (!l.isEmpty()) {
            Object b = l.removeFirst();
            if (!isMyEqual(a, b)) return false;
            a = b;
        }
        return true;
    }

    private static Object invokeMethodNoeval(
            Class c, Object o, String name, ArrayDeque<Object> params) {
        //if (name.equals("castComponent")) return (Component) o;

        int paramCnt = 0, paramsSize = params.size();
        Class[] paramTypes = new Class[paramsSize];
        Object[] paramValues = new Object[paramsSize];
        for(Object v : params) {
            paramValues[paramCnt] = v;
            paramTypes[paramCnt] = classValue(v); //v.getClass();Class.forName(n);
            paramCnt += 1;
        }

        if (name.equals("new")) {
            Constructor cn = constructorForParams(c, paramTypes);
            try {
                return cn.newInstance(paramValues);
            } catch (Throwable e) {throw new RuntimeException(e.getMessage(), e);}
        }

        Method m = methodForName(c, name, paramTypes);
        try {
            Object r = m.invoke(Modifier.isStatic(m.getModifiers()) ? null : o, paramValues);
            return r == null ? stringOK : r;
        } catch (Throwable e) {throw new RuntimeException(e.getMessage(), e);}
    }

    private static ConsList convertLinkedToConsList(ArrayDeque<Object> l) {
        ConsList r = emptyList;
        while (!l.isEmpty()) r = new ConsList(l.removeLast(), r);
        return r;
    }

    private static HashMap<String, Object> getMapArgsValsNoeval(
            ConsList pa, ArrayDeque<Object> pv) {
        HashMap<String, Object> m = new HashMap<>();
        while (!pa.isEmpty() && !pv.isEmpty()) {
            if (pa.cdr.isEmpty() && pv.size() > 1)
                m.put(pa.car.toString(), convertLinkedToConsList(pv));
            else
                m.put(pa.car.toString(), pv.removeFirst());
            pa = pa.cdr;
            //pv = pv.cdr;
        }
        return m;
    }

    private static ConsList evalConsNoeval(ArrayDeque<Object> l) {
        if (l.isEmpty()) return emptyList;

        Object o = l.removeLast();
        ConsList r = (o instanceof ConsList) ? (ConsList) o : new ConsList(o, emptyList);
        while (!l.isEmpty()) r = new ConsList(l.removeLast(), r);
        return r;
    }

    public static Object evalLinkedList(InOutable io, Env env, ArrayDeque<Object> ls) {

        if (ls.isEmpty()) return ls;
        Object op = ls.removeFirst(), v;
        String name;

        if (op instanceof SpecialForm) {
            SpecialForm sf = (SpecialForm) op;

            switch (sf) {
                case CLASS:
                    name = ls.getFirst().toString();
                    return classForName(name);

                case JAVA:
                    v = ls.removeFirst();
                    name = ls.removeFirst().toString();
                    if (v instanceof Class)
                        return invokeMethodNoeval((Class) v, null, name, ls);
                    else
                        return invokeMethodNoeval(v.getClass(), v, name, ls);

                case COMPILE:
                    name = ls.getFirst().toString();
                    try {
                        //compileClassForCode(name);
                        //return ret(d, io, stringOK);
                        return compileClassForCode(name);
                    } catch (Throwable e) {
                        throw new RuntimeException(e.getMessage(), e);
                    }

                case AR_ADD:
                case AR_SUB:
                case AR_MUL:
                case AR_DIV:
                case AR_MOD:
                    return foldBinOpNoeval(sf, ls);

                case AR_GR:
                case AR_GR_EQ:
                case AR_LOW:
                case AR_LOW_EQ:
                case AR_EQ:
                case AR_NOT_EQ:
                    return foldCompOpNoeval(sf, ls);

                case ST_CONCAT:
                    String rs = "";
                    while (!ls.isEmpty()) rs = rs + ls.removeFirst().toString();
                    return rs;

                case EQUAL: return foldEqObjectNoeval(ls);

                case DEF:
                    //while (ls.size() > 1) {
                    //    name = ls.removeFirst().toString();
                    //    env.defVar(name, ls.removeFirst());
                    //}
                    return stringOK;

                case SET:
                    //while (ls.size() > 1) {
                    //    name = ls.removeFirst().toString();
                    //    env.setVar(name, ls.removeFirst());
                    //}
                    return stringOK;

                case GET: return env.getVar(ls.getFirst().toString());

                case CONS: return evalConsNoeval(ls);

                case CAR:
                    v = ls.getFirst();
                    if (v instanceof ConsList) {
                        ConsList lv = (ConsList) v;
                        return lv.isEmpty() ? emptyList : lv.car;
                    } else return v;

                case CDR:
                    v = ls.getFirst();
                    if (v instanceof ConsList) {
                        ConsList lv = (ConsList) v;
                        return lv.isEmpty() ? emptyList : lv.cdr;
                    } else return emptyList;

                case QUOTE: return ls.getFirst();

//                case TRAY: return ret(d, io, eval(0, strict, io, env, getbody(ls)));

                case COND: return ls.isEmpty() ? stringOK : ls.getLast();
/*
                case EVAL:
                    return ret(d, io, eval(d, true, io, env, eval(d, true, io, env, ls.car)));
*/
                case TYPEOF: return ls.getFirst().getClass().getSimpleName();

                case PRINT:
                    //while (!ls.isEmpty()) io.out(false, ls.removeFirst().toString());
                    return stringOK;

                case READ: return Read.string2LispVal(io.in());

                case LAMBDA:
                    return new Func((ConsList) ls.getFirst(), getbody(ls.getLast()), env);

                case MACRO:
                    return new Macr((ConsList) ls.getFirst(), getbody(ls.getLast()));

                case BEGIN:
                default:
                    return ls.isEmpty() ? op : ls.getLast();
            }
        } else
            return ls.isEmpty() ? op : ls.getLast();
    }

    public static boolean isTailForm(Object op) {
/*
        if (op instanceof SpecialForm) {
            SpecialForm sf = (SpecialForm) op;

            switch (sf) {
                case CLASS:
                case NEW:
                case JAVA:
                case COMPILE:
                case METHOD:

                case AR_ADD:
                case AR_SUB:
                case AR_MUL:
                case AR_DIV:
                case AR_MOD:
                case AR_GR:
                case AR_GR_EQ:
                case AR_LOW:
                case AR_LOW_EQ:
                case AR_EQ:
                case AR_NOT_EQ:
                case ST_CONCAT:
                case EQUAL:

                case DEF:
                case SET:
                case GET:
                case CONS:
                case CAR:
                case CDR:
                case QUOTE:
                case TRAY:
                    return false;

                case COND:
                    return true;

                case WHILE:
                case EVAL:
                case TYPEOF:
                case PRINT:
                case READ:
                case LAMBDA:
                case MACRO:
                    return false;

                case BEGIN:
                default:
                    return true;
            }
        } else if (op instanceof Func) {
            return false;
        } else if (op instanceof Macr) {
            return false;
        }

        return true;
*/
        if (op instanceof SpecialForm) return op == SpecialForm.COND;
        else if (op instanceof Func || op instanceof Macr) return false;
        else return true;
    }

    public static void updsi(Object o, StackItem si) {

        if (o instanceof ConsList)
            si.upd((ConsList) o);
        else {
            Object r = o instanceof Symbol ? si.env.getVar(o.toString()) : o;
            si.l.clear();
            si.l.addLast(SpecialForm.BEGIN);
            si.l.addLast(r);
            si.p = emptyList;
        }
    }


    public static Object evalIter(InOutable io, Env env, Object inobj) throws
            FileNotFoundException {

        PrintWriter writer = new PrintWriter("StackLog.txt");
        boolean log = false; //true;
        int maxstacksize = 0;
        Main.maxstacksize = 0;

        if (inobj == null) {
            Thread.currentThread().interrupt();
            throw new Error("interrupted inobj == null.....");

        } else if (inobj instanceof Symbol) return env.getVar(inobj.toString());

        else if (inobj instanceof ConsList) {

            StackItem si = new StackItem(env, (ConsList) inobj);
            ArrayDeque<StackItem> stack = new ArrayDeque<>();
            stack.addFirst(si);

            while (true) {
                //io.out(true, "" + stack.size());
                //    writer.println("" + stack.size());
                //writer.append("\n");
                //writer.flush();
                //    cnt++;

                while (true) {
                    if (Thread.currentThread().isInterrupted()) {

                        if (log) writer.close();
                        io.out(true, "max stack size = " + maxstacksize);

                        Thread.currentThread().interrupt();
                        throw new RuntimeException("interrupted ITER lalalala.....");
                    }

                    // головная форма уже рассчитана - в голове si.l
                    if (!si.l.isEmpty()) {
                        Object op = si.l.getFirst();

                        if (op instanceof SpecialForm) {
                            SpecialForm sf = (SpecialForm) op;

                            switch (sf) {
/*
                                case CLASS:
                                case JAVA:
                                case COMPILE:
                                    break;

                                case AR_ADD:
                                case AR_SUB:
                                case AR_MUL:
                                case AR_DIV:
                                case AR_MOD:

                                    if (si.l.size() > 2) {
                                        Number r2 = object2Number(si.l.removeLast());
                                        Number r1 = object2Number(si.l.removeLast());
                                        si.l.addLast(BinOp(sf, r1, r2));
                                    }
                                    if (si.p.isEmpty()) {

                                        if (si.l.size() == 2) {
                                            //si.l.set(0, SpecialForm.BEGIN);
                                            si.l.addFirst(SpecialForm.BEGIN);
                                            si.p = emptyList;
                                        } else
                                            throw new RuntimeException(
                                                    "нет аргументов для арифметической операции: "
                                                            + op);
                                    }
                                    break;

                                case AR_GR:
                                case AR_GR_EQ:
                                case AR_LOW:
                                case AR_LOW_EQ:
                                case AR_EQ:
                                case AR_NOT_EQ:

                                    boolean allright = true;

                                    if (si.l.size() > 2) {
                                        Number r2 = object2Number(si.l.removeLast());
                                        Number r1 = object2Number(si.l.removeLast());

                                        allright = CompOp(sf, r1, r2);
                                        if (!allright) {
                                            si.p = emptyList;
                                        } else
                                            si.l.addLast(r2);
                                    }
                                    if (si.p.isEmpty()) {
                                        si.l.clear();
                                        si.l.addLast(SpecialForm.BEGIN);
                                        si.l.addLast(allright);
                                        si.p = emptyList;
                                    }
                                    break;

                                case ST_CONCAT:
                                case EQUAL:
                                    break;
*/
                                case DEF:
                                case SET:
                                    // обработать имеющиеся имя/значение
                                    if (si.l.size() == 3) {
                                        Object o = si.l.removeLast();
                                        String name = si.l.removeLast().toString();
                                        if (sf == SpecialForm.DEF) si.env.defVar(name, o);
                                        else si.env.setVar(name, o);
                                    }
                                    if (si.l.size() == 1 && si.p.car instanceof Symbol) {
                                        si.l.addLast(si.p.car);
                                        si.p = si.p.cdr;
                                    }
                                    break;
/*
                                case GET:
                                case CONS:
                                case CAR:
                                case CDR:
                                    break;
*/
                                case QUOTE:
                                    si.l.addLast(si.p.car);
                                    si.p = emptyList;
                                    break;

//                                case TRAY:
//                                    break;

                                case COND:
                                    if (si.l.size() > 1 && !si.p.isEmpty()) {
                                        if (object2boolean(si.l.getLast())) {
                                            updsi(si.p.car, si);
                                        } else {
                                            si.l.removeLast();
                                            si.p = si.p.cdr;
                                        }
                                    }
                                    break;
                                //cond (= 1 2) (print 1) (= 1 3) (print 2) (= 1 4) (print 3)

                                case EVAL:
                                    if (si.l.size() > 1) updsi(si.l.getLast(), si);
                                    break;

                                case EVAL_IN_CONTEXT:
                                    if (si.l.size() == 2) {
                                        Object o = si.l.getLast();
                                        if (o instanceof Func) {
                                            Func f = (Func) o;
                                            si.upd(si.p, f.clojure);
                                        } else
                                            throw new RuntimeException(
                                                    "eval-in - 1 argument isn't lambda");
                                    }
                                    break;

//                                case TYPEOF:
//                                    break;

                                case PRINT:
                                    if (si.l.size() > 1)
                                        io.out(false, si.l.removeLast().toString());
                                    break;

//                                case READ:
//                                    break;

                                case LAMBDA:
                                case MACRO:
                                    si.l.addLast(si.p.car);
                                    si.l.addLast(si.p.cdr);
                                    si.p = emptyList;
                                    break;

//                                case BEGIN:
//                                default:
//                                    break;
                            }
                            //} else if (op instanceof Func) {
                        } else if (op instanceof Macr) {
                            while (!si.p.isEmpty()) {
                                si.l.addLast(si.p.car);
                                si.p = si.p.cdr;
                            }
                            si.p = emptyList;
                        }
                    }

                    // вычисляем дальше текущий список
                    if (si.p.isEmpty()) {
                        break;

                    } else if (si.p.car instanceof Symbol) {
                        si.l.addLast(si.env.getVar(si.p.car.toString()));
                        si.p = si.p.cdr;

                    } else if (si.p.car instanceof ConsList) {
                        // TCO
                        if ((si.l.isEmpty() || isTailForm(si.l.getFirst()))
                                && si.p.cdr.isEmpty()) {
                            si.upd((ConsList) si.p.car);
                        } else {
                            si = new StackItem(si.env, (ConsList) si.p.car);
                            stack.addFirst(si);

                            maxstacksize =
                                    stack.size() > maxstacksize ? stack.size() : maxstacksize;
                            if (log) writer.println("" + stack.size());
                        }
                    } else {
                        si.l.addLast(si.p.car);
                        si.p = si.p.cdr;
                    }
                } // while true по списку

                // дошли до конца текущего списка
                Object op = si.l.getFirst();

                if (op instanceof Func) {
                    Func f = (Func) op;
                    si.l.removeFirst();
                    si.upd((ConsList) f.body,
                            new Env(getMapArgsValsNoeval(f.pars, si.l), f.clojure));

                } else if (op instanceof Macr) {
                    Macr f = (Macr) op;
                    si.l.removeFirst();
                    Object me = macroexpand(getMapArgsValsNoeval(f.pars, si.l), f.body);
                    si.upd((ConsList) me);

                } else {
                    Object r = evalLinkedList(io, si.env, si.l);
                    stack.removeFirst();

                    if (log) writer.println("" + stack.size());

                    if (stack.size() > 0) {
                        si = stack.getFirst();
                        si.l.addLast(r);
                        si.p = si.p.cdr;
                    } else {
                        if (log) writer.close();
                        //io.out(true, "max stack size = " + maxstacksize);
                        Main.maxstacksize = maxstacksize;
                        return r;
                    }
                }

            } // while true по стеку

        } else return inobj;
    }
}

/*
                                case WHILE:
                                    if (si.l.size() == 2) {
                                        if (!object2boolean(si.l.getLast())) {
                                            si.l.clear();
                                            si.l.addLast(stringOK);
                                            si.p = emptyList;
                                        }
                                    } else if (si.p.isEmpty())
                                        si.upd(si.p0);
                                    break;
*/

