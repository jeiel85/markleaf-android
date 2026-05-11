package com.markleaf.notes.core.markdown.syntax

/**
 * Lightweight regex-based syntax highlighter for fenced code blocks in the
 * preview pane. Supports the top ~10 languages Markleaf users write notes
 * about; everything else falls through as plain TEXT (monospace, no color).
 *
 * Design constraints:
 *  - Zero external dependencies (§2.7 lightweight bias). 10 hand-rolled
 *    rule sets are ~30 LOC each, no Java reflection, no APK bloat.
 *  - Non-overlapping tokens. Earlier rule wins on conflict, longest match
 *    wins on tie. Strings/comments take precedence over keywords so
 *    "fun" inside `"a fun day"` is not colored as Kotlin's keyword.
 *  - Output is rendering-agnostic: [Token]s carry [TokenType], which the
 *    Compose renderer maps to colors from [CodeBlockColors].
 */
object SyntaxHighlighter {

    enum class TokenType {
        TEXT,        // default — uncolored
        KEYWORD,     // class, fun, def, if, return, …
        STRING,      // "..." or '...' or `...`
        NUMBER,      // 42, 3.14, 0xff
        COMMENT,     // // line, # line, /* block */
        FUNCTION,    // identifier followed by `(`
        TYPE,        // capitalized identifier or annotation
        PUNCTUATION  // operators / brackets — used sparingly
    }

    data class Token(val text: String, val type: TokenType)

    fun tokenize(code: String, language: String?): List<Token> {
        val rules = rulesFor(language) ?: return listOf(Token(code, TokenType.TEXT))
        return applyRules(code, rules)
    }

    private fun rulesFor(language: String?): List<Rule>? = when (language?.lowercase()?.trim()) {
        "kotlin", "kt" -> KOTLIN_RULES
        "java" -> JAVA_RULES
        "python", "py" -> PYTHON_RULES
        "javascript", "js" -> JAVASCRIPT_RULES
        "typescript", "ts" -> TYPESCRIPT_RULES
        "bash", "sh", "shell", "zsh" -> SHELL_RULES
        "json" -> JSON_RULES
        "yaml", "yml" -> YAML_RULES
        "xml", "html" -> XML_RULES
        "sql" -> SQL_RULES
        else -> null
    }

    private data class Rule(val regex: Regex, val type: TokenType)

    private data class Match(val start: Int, val end: Int, val type: TokenType)

    private fun applyRules(text: String, rules: List<Rule>): List<Token> {
        // Collect every regex match across every rule. Rules are checked in
        // declaration order, and the first rule with a non-empty match at a
        // given index wins — this is why comments/strings come first in the
        // rule lists below.
        val matches = mutableListOf<Match>()
        rules.forEach { rule ->
            rule.regex.findAll(text).forEach { m ->
                matches += Match(m.range.first, m.range.last + 1, rule.type)
            }
        }
        // Sort by start, then by descending length, then by stable order.
        matches.sortWith(compareBy({ it.start }, { -(it.end - it.start) }))
        // Resolve overlaps — first match wins, later ones inside its range drop.
        val resolved = mutableListOf<Match>()
        var cursor = 0
        for (m in matches) {
            if (m.start < cursor) continue
            resolved += m
            cursor = m.end
        }
        // Emit tokens: TEXT for gaps, typed tokens for resolved matches.
        val out = mutableListOf<Token>()
        var pos = 0
        for (m in resolved) {
            if (m.start > pos) out += Token(text.substring(pos, m.start), TokenType.TEXT)
            out += Token(text.substring(m.start, m.end), m.type)
            pos = m.end
        }
        if (pos < text.length) out += Token(text.substring(pos), TokenType.TEXT)
        return out
    }

    // -------------------------------------------------------------------
    // Rule sets
    // -------------------------------------------------------------------
    // Each list is checked in order: comments / strings first so they
    // shadow keywords inside them. Numbers / keywords / function-shape /
    // type-shape last.

    private val LINE_COMMENT_DOUBLE_SLASH = Rule(Regex("""//[^\n]*"""), TokenType.COMMENT)
    private val LINE_COMMENT_HASH = Rule(Regex("""#[^\n]*"""), TokenType.COMMENT)
    private val BLOCK_COMMENT_C = Rule(Regex("""/\*[\s\S]*?\*/"""), TokenType.COMMENT)
    private val STRING_DOUBLE = Rule(Regex(""""(?:\\.|[^"\\\n])*""""), TokenType.STRING)
    private val STRING_SINGLE = Rule(Regex("""'(?:\\.|[^'\\\n])*'"""), TokenType.STRING)
    private val STRING_BACKTICK = Rule(Regex("""`(?:\\.|[^`\\])*`"""), TokenType.STRING)
    private val NUMBER = Rule(
        Regex("""\b(?:0x[0-9A-Fa-f_]+|0b[01_]+|\d[\d_]*(?:\.\d+)?(?:[eE][+-]?\d+)?[fFLlUu]?)\b"""),
        TokenType.NUMBER
    )
    private val FUNCTION_CALL = Rule(Regex("""\b([a-zA-Z_][a-zA-Z0-9_]*)(?=\()"""), TokenType.FUNCTION)
    private val ANNOTATION = Rule(Regex("""@[A-Za-z_][A-Za-z0-9_]*"""), TokenType.TYPE)
    private val TYPE_CAPITALIZED = Rule(Regex("""\b[A-Z][A-Za-z0-9_]*\b"""), TokenType.TYPE)

    private fun keywords(words: List<String>): Rule =
        Rule(Regex("""\b(?:${words.joinToString("|")})\b"""), TokenType.KEYWORD)

    private val KOTLIN_RULES = listOf(
        LINE_COMMENT_DOUBLE_SLASH, BLOCK_COMMENT_C,
        STRING_DOUBLE, STRING_BACKTICK,
        ANNOTATION,
        keywords(listOf(
            "abstract", "actual", "as", "break", "by", "catch", "class", "companion",
            "const", "constructor", "continue", "crossinline", "data", "do", "else",
            "enum", "expect", "external", "false", "final", "finally", "for", "fun",
            "get", "if", "import", "in", "infix", "init", "inline", "inner", "interface",
            "internal", "is", "lateinit", "noinline", "null", "object", "open", "operator",
            "out", "override", "package", "private", "protected", "public", "reified",
            "return", "sealed", "set", "super", "suspend", "tailrec", "this", "throw",
            "true", "try", "typealias", "val", "var", "vararg", "when", "where", "while"
        )),
        NUMBER, FUNCTION_CALL, TYPE_CAPITALIZED
    )

    private val JAVA_RULES = listOf(
        LINE_COMMENT_DOUBLE_SLASH, BLOCK_COMMENT_C,
        STRING_DOUBLE, STRING_SINGLE,
        ANNOTATION,
        keywords(listOf(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "default", "do", "double", "else", "enum",
            "extends", "false", "final", "finally", "float", "for", "goto", "if",
            "implements", "import", "instanceof", "int", "interface", "long", "native",
            "new", "null", "package", "private", "protected", "public", "record",
            "return", "short", "static", "strictfp", "super", "switch", "synchronized",
            "this", "throw", "throws", "transient", "true", "try", "var", "void",
            "volatile", "while", "yield"
        )),
        NUMBER, FUNCTION_CALL, TYPE_CAPITALIZED
    )

    private val PYTHON_RULES = listOf(
        LINE_COMMENT_HASH,
        STRING_DOUBLE, STRING_SINGLE,
        keywords(listOf(
            "False", "None", "True", "and", "as", "assert", "async", "await", "break",
            "class", "continue", "def", "del", "elif", "else", "except", "finally",
            "for", "from", "global", "if", "import", "in", "is", "lambda", "nonlocal",
            "not", "or", "pass", "raise", "return", "try", "while", "with", "yield",
            "match", "case"
        )),
        Rule(Regex("""@[A-Za-z_][A-Za-z0-9_.]*"""), TokenType.TYPE),
        NUMBER, FUNCTION_CALL, TYPE_CAPITALIZED
    )

    private val JAVASCRIPT_RULES = listOf(
        LINE_COMMENT_DOUBLE_SLASH, BLOCK_COMMENT_C,
        STRING_DOUBLE, STRING_SINGLE, STRING_BACKTICK,
        keywords(listOf(
            "async", "await", "break", "case", "catch", "class", "const", "continue",
            "debugger", "default", "delete", "do", "else", "export", "extends",
            "false", "finally", "for", "from", "function", "if", "import", "in",
            "instanceof", "let", "new", "null", "of", "return", "static", "super",
            "switch", "this", "throw", "true", "try", "typeof", "undefined", "var",
            "void", "while", "with", "yield"
        )),
        NUMBER, FUNCTION_CALL, TYPE_CAPITALIZED
    )

    private val TYPESCRIPT_RULES = listOf(
        LINE_COMMENT_DOUBLE_SLASH, BLOCK_COMMENT_C,
        STRING_DOUBLE, STRING_SINGLE, STRING_BACKTICK,
        keywords(listOf(
            "any", "as", "async", "await", "boolean", "break", "case", "catch", "class",
            "const", "continue", "debugger", "declare", "default", "delete", "do",
            "else", "enum", "export", "extends", "false", "finally", "for", "from",
            "function", "if", "implements", "import", "in", "instanceof", "interface",
            "is", "keyof", "let", "namespace", "never", "new", "null", "number", "of",
            "private", "protected", "public", "readonly", "return", "satisfies",
            "static", "string", "super", "switch", "this", "throw", "true", "try",
            "type", "typeof", "undefined", "unknown", "var", "void", "while", "with",
            "yield"
        )),
        NUMBER, FUNCTION_CALL, TYPE_CAPITALIZED
    )

    private val SHELL_RULES = listOf(
        LINE_COMMENT_HASH,
        STRING_DOUBLE, STRING_SINGLE, STRING_BACKTICK,
        keywords(listOf(
            "if", "then", "else", "elif", "fi", "case", "esac", "for", "in", "do",
            "done", "while", "until", "function", "return", "exit", "break",
            "continue", "local", "readonly", "declare", "export", "unset", "set",
            "shift", "trap", "true", "false"
        )),
        // bash variable references: $var, ${var}, $1, $@
        Rule(Regex("""\$\{[^}]+}|\$[A-Za-z_][A-Za-z0-9_]*|\$\d|\$[@*#?!${'$'}_]"""), TokenType.TYPE),
        NUMBER, FUNCTION_CALL
    )

    private val JSON_RULES = listOf(
        STRING_DOUBLE,
        keywords(listOf("true", "false", "null")),
        NUMBER
    )

    private val YAML_RULES = listOf(
        LINE_COMMENT_HASH,
        STRING_DOUBLE, STRING_SINGLE,
        keywords(listOf("true", "false", "null", "yes", "no", "on", "off", "True", "False", "Null")),
        // YAML keys: `^key:` at the start of a line
        Rule(Regex("""(?m)^\s*[A-Za-z_][A-Za-z0-9_-]*(?=\s*:)"""), TokenType.FUNCTION),
        NUMBER
    )

    private val XML_RULES = listOf(
        Rule(Regex("""<!--[\s\S]*?-->"""), TokenType.COMMENT),
        STRING_DOUBLE, STRING_SINGLE,
        // Tags: <name and </name
        Rule(Regex("""</?[A-Za-z][A-Za-z0-9:_-]*"""), TokenType.KEYWORD),
        // Attribute names just before =
        Rule(Regex("""\b[A-Za-z_][A-Za-z0-9:_-]*(?=\s*=)"""), TokenType.FUNCTION),
        NUMBER
    )

    private val SQL_RULES = listOf(
        LINE_COMMENT_DOUBLE_SLASH, // some dialects
        Rule(Regex("""--[^\n]*"""), TokenType.COMMENT),
        BLOCK_COMMENT_C,
        STRING_SINGLE, STRING_DOUBLE,
        keywords(listOf(
            "SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES", "UPDATE", "SET",
            "DELETE", "CREATE", "TABLE", "INDEX", "VIEW", "ALTER", "ADD", "DROP",
            "JOIN", "INNER", "LEFT", "RIGHT", "OUTER", "ON", "USING", "GROUP", "BY",
            "ORDER", "ASC", "DESC", "LIMIT", "OFFSET", "HAVING", "AS", "DISTINCT",
            "UNION", "ALL", "AND", "OR", "NOT", "IN", "EXISTS", "BETWEEN", "LIKE",
            "IS", "NULL", "TRUE", "FALSE", "PRIMARY", "KEY", "FOREIGN", "REFERENCES",
            "DEFAULT", "UNIQUE", "CONSTRAINT", "CASCADE", "BEGIN", "COMMIT", "ROLLBACK",
            "TRANSACTION", "WITH", "CASE", "WHEN", "THEN", "ELSE", "END",
            // lowercase mirrors
            "select", "from", "where", "insert", "into", "values", "update", "set",
            "delete", "create", "table", "join", "on", "and", "or", "not", "in",
            "is", "null", "as"
        )),
        NUMBER, FUNCTION_CALL
    )
}
