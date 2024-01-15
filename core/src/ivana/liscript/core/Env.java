package ivana.liscript.core;

import java.util.HashMap;

//class Env:
//        def __init__(self, m, p):
//        self.frame, self.parent = m, p
//
//        def setvar(self, k, v):
//        e = self
//        while e is not None:
//        if k in e.frame:
//        e.frame[k] = v
//        break
//        e = e.parent
//
//        def getvar(self, k, s):
//        e = self
//        while e is not None:
//        if k in e.frame:
//        return e.frame[k]
//        e = e.parent
//        return s if isinstance(s, Symbol) else Symbol(k)
//
//        def defvar(self, k, v):
//        self.frame[k] = v

/**
 * Иерархическая структура окружения - словарь связей: строковый ключ - значение, и ссылка на
 * родительское окружение. Структура для хранения словаря НЕ является потокобезопасной.
 */
public class Env {
    /** словарь связей строковый ключ - объектное значение */
    public HashMap<String, Object> map;
    /** ссылка на родительское окружение */
    public Env parent;

    /** Конструктор со словарем и родителем.
     * @param m словарь строковый ключ-значение
     * @param p родительское окружение
     */
    public Env (HashMap<String, Object> m, Env p) { map = m; parent = p; }

    /** Конструктор без параметров. Возвращает окружение с пустым словарем и пустым родительским
     * окружением.
     */
    public Env () { this(new HashMap<>(), null); }

    //public Env subEnv(HashMap<String, Object> m) { return new Env(m, this); }

    /** устанавливает значение по ключу в ближайшем словаре из иерархической структуры, где
     * существует значение с данным ключом.
     * @param var строка-ключ
     * @param value объект-значение
     */
    public void setVar(String var, Object value) {
        Env env = this;
        while (env != null) {
            if (env.map.containsKey(var)) {env.map.put(var, value); break;}
            env = env.parent;
        }
    }

    /** получает значение по ключу в ближайшем словаре из иерархической структуры, где
     * существует значение с данным ключом. Если не находит - возвращает сам ключ в качестве
     * значения.
     * @param var строка-ключ
     * @return объект-значение
     */
    public Object getVar(Eval.Symbol var) {
        String key = var.toString();
        Env env = this;
        while (env != null) {
            if (env.map.containsKey(key)) return env.map.get(key);
            env = env.parent;
        }
        return var;
    }

    /** устанавливает значение по ключу в текущаем словаре
     * @param var строка-ключ
     * @param value объект-значение
     */
    public void defVar(String var, Object value) { this.map.put(var, value); }

    /** возвращает истину, если данный ключ связан со значением в любом словаре из иерархии вверх
     *  от текущего.
     * @param var строка-ключ
     * @return истина/ложь
     */
    public boolean isBounded(String var) {
        Env env = this;
        while (env != null) {
            if (env.map.containsKey(var)) return true;
            env = env.parent;
        }
        return false;
    }
}
