package org.eso.ias.asce.test.transfer;

import jep.Interpreter;
import jep.SharedInterpreter;
import org.eso.ias.types.Priority;
import jep.SharedInterpreter;
import org.junit.jupiter.api.Test;

public class TestJep {
    public static void main(String[] args) {
        try (Interpreter interp = new SharedInterpreter()) {
            interp.exec("from java.lang import System");
            interp.exec("s = 'Hello World'");
            interp.exec("System.out.println(s)");
            interp.exec("print(s)");
            interp.exec("print(s[1:-1])");
            interp.set("id", "Identifier");

            Priority p = Priority.CRITICAL;
            interp.set("prio", p);
            Object obj = interp.getValue("prio");
            System.out.println("prio "+obj.getClass().getName());


        }
    }


    @Test
    void canInitSharedInterpreter() {
        try (SharedInterpreter py = new SharedInterpreter()) {
            py.eval("import sys, locale");
            py.eval("print('Python:', sys.version)");
            py.eval("print('filesystem encoding:', sys.getfilesystemencoding())");
        }
    }


}
