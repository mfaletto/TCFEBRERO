package tc.dec;
import org.antlr.v4.runtime.*;

import java.util.ArrayList;
import java.util.List;

public class CustomErrorListener extends BaseErrorListener {
    private static final List<String> errores = new ArrayList<>();

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine, String msg, RecognitionException e) {
        String error = "❌ Error sintáctico en línea " + line + ":" + charPositionInLine + " - " + msg;
        errores.add(error);
        //System.err.println(error);
    }

    public static List<String> getErrores() {
        return errores;
    }

    public static boolean hasErrors() {
        return !errores.isEmpty();
    }

    public static void clearErrors() {
        errores.clear();
    }
}
