package ivana.liscript.ui.console;

import ivana.liscript.core.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class Main implements Eval.InOutable {

    public static Env globalEnv;

    private static String readFileToString (String fileAbsolutePath) throws IOException {
        byte[] fileBytes = Files.readAllBytes(Paths.get(fileAbsolutePath));
        return new String(fileBytes, StandardCharsets.UTF_8);
    }

    private void loadFile (String fileName) {
        try {
            String expression = readFileToString(fileName);
            Object lv = Read.string2LispVal(expression);
            //this.out(true, Eval.evalIter(this, globalEnv, lv).toString());
            Eval.evalIter(this, globalEnv, lv);
            this.out(true, "Тест интерпретатора в консоли - простой однопоточный циклический вызов");
        } catch (IOException ex) {
            this.out(true, ex.getLocalizedMessage());
        }
    }

    Main() {
        globalEnv = new Env();
        loadFile("standard_library.liscript");
    }

    @Override
    public void out(boolean ln, String s) {
        if (s == null) return;
        if (ln) System.out.println(s); else System.out.print(s);
    }

    @Override
    public void outFromRead(String s) { out(false, s); }

    @Override
    public String in() {
        Scanner in = new Scanner(System.in);
        //String s = in.next();
        //String s = "";
        //while (in.hasNextLine()) s += "\n" + in.nextLine();
        return in.nextLine();
    }

    public static void main(String[] args) {
        Main app = new Main();
        while (true) {
            app.out(false, "> ");
            String expression = app.in();
            //if (expression.equals("q")) break;
            try {
                Object lv = Read.string2LispVal(expression);
                app.out(true, Eval.evalIter(app, globalEnv, lv).toString());
            } catch (Throwable e) {
                app.out(true, e.getLocalizedMessage());
                break;
            }
        }
    }

}
