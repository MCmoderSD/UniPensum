package de.mcmodersd.unipensum.data.db;

import java.util.ArrayList;

/**
 * Reads the text of an {@code .sql} file. A line that begins with two dashes is a comment, a semicolon ends a
 * statement. Neither is looked for inside a text in quotes, so the files do not use semicolons or lines like that in
 * their texts.
 */
final class SqlScript {

    private SqlScript() { }

    static ArrayList<String> statements(String script) {
        var code = new StringBuilder();
        for (var line : script.split("\r?\n")) {
            if (!line.trim().startsWith("--")) code.append(line).append('\n');
        }
        var statements = new ArrayList<String>();
        for (var part : code.toString().split(";")) {
            var statement = part.trim();
            if (!statement.isEmpty()) statements.add(statement);
        }
        return statements;
    }
}