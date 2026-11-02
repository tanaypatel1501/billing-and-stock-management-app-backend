package com.gst.billingandstockmanagement.chatbot;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Layer 2 of the defence. The real backstops are the database itself (read-only user that can
 * only SELECT from 4 self-filtering views), so treat this as "reject obviously bad SQL early
 * and give the model a clear error to retry with".
 */
@Component
public class SqlGuard {

    public static class RejectedSqlException extends RuntimeException {
        public RejectedSqlException(String message) { super(message); }
    }

    private static final int MAX_LENGTH = 2000;

    private static final Pattern ALLOWED_VIEW =
            Pattern.compile("\\b(v_bills|v_bill_items|v_stock|v_purchasers)\\b", Pattern.CASE_INSENSITIVE);

    /** Characters that never belong in a single clean SELECT. */
    private static final Pattern BAD_CHARS = Pattern.compile("(;|@|--|/\\*|\\*/|#|\\\\)");

    private static final Pattern BAD_WORDS = Pattern.compile(
            "\\b(into|outfile|dumpfile|load_file|sleep|benchmark|get_lock|release_lock|"
                    + "information_schema|performance_schema|mysql|sys)\\b", Pattern.CASE_INSENSITIVE);

    /** Returns the cleaned SQL, or throws RejectedSqlException with a model-friendly reason. */
    public String validate(String raw) {
        if (raw == null || raw.isBlank()) throw new RejectedSqlException("The query was empty.");

        String sql = raw.trim().replace("`", "");
        if (sql.endsWith(";")) sql = sql.substring(0, sql.length() - 1).trim();

        if (sql.length() > MAX_LENGTH) throw new RejectedSqlException("The query is too long.");
        if (BAD_CHARS.matcher(sql).find())
            throw new RejectedSqlException("Do not use semicolons, comments, variables (@) or backslashes.");
        if (BAD_WORDS.matcher(sql).find())
            throw new RejectedSqlException("The query uses a forbidden keyword or schema.");
        if (!ALLOWED_VIEW.matcher(sql).find())
            throw new RejectedSqlException("The query must read from v_bills, v_bill_items, v_stock or v_purchasers.");

        Statement parsed;
        try {
            parsed = CCJSqlParserUtil.parse(sql);
        } catch (JSQLParserException e) {
            throw new RejectedSqlException("The query is not valid MySQL SELECT syntax.");
        }
        if (!(parsed instanceof Select)) throw new RejectedSqlException("Only SELECT statements are allowed.");

        return sql;
    }
}