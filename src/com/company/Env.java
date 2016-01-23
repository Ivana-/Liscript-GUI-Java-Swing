package com.company;

import java.util.HashMap;

public class Env {
    public HashMap<String, Object> map;
    public Env parent;

    Env (HashMap<String, Object> m, Env p) { map = m; parent = p; }

    public static void setVar(Env env, String var, Object value) {
        //if (env != null) {
        //    if (env.map.containsKey(var)) env.map.put(var, value);
        //    else setVar(env.parent, var, value);
        //}

        Env curenv = env;
        while (curenv != null) {
            if (curenv.map.containsKey(var)) {curenv.map.put(var, value); break;}
            curenv = curenv.parent;
        }
    }

    public static Object getVar(Env env, String var) {
        //return env == null ? var : env.map.containsKey(var) ? env.map.get(var) :
        // getVar(env.parent, var);

        Env curenv = env;
        while (curenv != null) {
            if (curenv.map.containsKey(var)) return curenv.map.get(var);
            curenv = curenv.parent;
        }
        return var;
    }

    public static void defVar(Env env, String var, Object value) {
        if (env != null) env.map.put(var, value);
    }

    public static boolean isBounded(Env env, String var) {
        Env curenv = env;
        while (curenv != null) {
            if (curenv.map.containsKey(var)) return true;
            curenv = curenv.parent;
        }
        return false;
    }
}
