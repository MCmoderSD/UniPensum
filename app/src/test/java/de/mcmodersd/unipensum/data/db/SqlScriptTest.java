package de.mcmodersd.unipensum.data.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class SqlScriptTest {

    /** The unit tests run in the folder of the module. */
    private static final Path RAW = Path.of("src", "main", "res", "raw");

    @Test
    public void aSemicolonEndsAStatement() {
        assertEquals(List.of("SELECT 1", "SELECT 2"), SqlScript.statements("SELECT 1;\nSELECT 2;\n"));
    }

    @Test
    public void theLastStatementNeedsNoSemicolon() {
        assertEquals(List.of("SELECT 1", "SELECT 2"), SqlScript.statements("SELECT 1;\nSELECT 2"));
    }

    @Test
    public void aLineThatBeginsWithTwoDashesIsAComment() {
        assertEquals(
                List.of("SELECT 1"),
                SqlScript.statements("-- the first\n   -- indented\nSELECT 1;\n-- after it\n")
        );
    }

    @Test
    public void aStatementMayTakeSeveralLinesAndTheFileMayUseWindowsLineBreaks() {
        assertEquals(
                List.of("SELECT a,\n       b\nFROM t"),
                SqlScript.statements("-- comment\r\nSELECT a,\r\n       b\r\nFROM t;\r\n")
        );
    }

    @Test
    public void emptyPartsAreDropped() {
        assertEquals(List.of(), SqlScript.statements("\n -- nothing\n;;\n  ;\n"));
    }

    @Test
    public void theSchemaCreatesFiveTablesAndSixIndexes() throws IOException {
        var statements = SqlScript.statements(Files.readString(RAW.resolve("schema.sql")));

        assertEquals(11, statements.size());
        for (var i = 0; i < 5; i++) assertTrue(statements.get(i), statements.get(i).startsWith("CREATE TABLE "));
        for (var i = 5; i < 11; i++) assertTrue(statements.get(i), statements.get(i).startsWith("CREATE INDEX "));
    }
}