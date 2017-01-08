package com.company;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GuiButton extends JButton {
    private com.company.Eval.ConsList liscriptCode;

    public void setLambda(com.company.Eval.Func lambda) {
        liscriptCode = new Eval.ConsList(lambda, Eval.emptyList);

        this.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onClick();
            }
        });
    }

    public void onClick() {
        Thread thread = new Thread() {
            public void run() {
                try {
                    //System.out.println(liscriptCode.toString());
                    Eval.evalIter(null, Main.globalEnv, liscriptCode);
                } catch (Throwable e) {
                    Thread.currentThread().interrupt();
                    //pane.out(true, e.getLocalizedMessage());
                }
            }
        };
        thread.start();
    }
}
