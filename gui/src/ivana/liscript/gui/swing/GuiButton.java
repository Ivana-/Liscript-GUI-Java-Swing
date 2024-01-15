package ivana.liscript.gui.swing;

import ivana.liscript.core.Eval;

import javax.swing.*;

public class GuiButton extends JButton {
    private Eval.ConsList liscriptCode;

    public void setLambda(ivana.liscript.core.Eval.Func lambda) {
        liscriptCode = new Eval.ConsList(lambda, Eval.emptyList);
        this.addActionListener(e -> onClick());
    }

    public void onClick() {
        Thread thread = new Thread(() -> {
            try {
                //System.out.println(liscriptCode.toString());
                Eval.evalIter(null, Main.globalEnv, liscriptCode);
            } catch (Throwable e) {
                Thread.currentThread().interrupt();
                //pane.out(true, e.getLocalizedMessage());
            }
        });
        thread.start();
    }
}
