package de.mcmodersd.unipensum.domain.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import de.mcmodersd.unipensum.domain.logic.WeekLayout.Block;
import de.mcmodersd.unipensum.domain.logic.WeekLayout.Placement;

public class WeekLayoutTest {

    private static Block block(long id, int startHour, int endHour) {
        return new Block(id, startHour * 60, endHour * 60);
    }

    private static Placement placement(ArrayList<Placement> all, long id) {
        for (var p : all) {
            if (p.id() == id) return p;
        }
        throw new AssertionError("No placement for " + id);
    }

    @Test
    public void empty_yieldsNothing() {
        assertTrue(WeekLayout.layout(List.of()).isEmpty());
    }

    @Test
    public void single_fillsFullWidth() {
        var result = WeekLayout.layout(List.of(block(1, 8, 10)));
        assertEquals(new Placement(1, 0, 1, false), result.get(0));
    }

    @Test
    public void touching_doesNotOverlap() {
        var result = WeekLayout.layout(List.of(block(1, 8, 10), block(2, 10, 12)));
        assertEquals(new Placement(1, 0, 1, false), placement(result, 1));
        assertEquals(new Placement(2, 0, 1, false), placement(result, 2));
    }

    @Test
    public void overlappingPair_sharesTwoColumns() {
        var result = WeekLayout.layout(List.of(block(1, 8, 10), block(2, 9, 11)));
        assertEquals(new Placement(1, 0, 2, true), placement(result, 1));
        assertEquals(new Placement(2, 1, 2, true), placement(result, 2));
    }

    @Test
    public void chain_reusesFreedColumnButStaysOneGroup() {
        // 1 overlaps 2, 2 overlaps 3, but 1 and 3 do not overlap each other.
        var result = WeekLayout.layout(List.of(block(1, 8, 10), block(2, 9, 11), block(3, 10, 12)));
        assertEquals(new Placement(1, 0, 2, true), placement(result, 1));
        assertEquals(new Placement(2, 1, 2, true), placement(result, 2));
        assertEquals(new Placement(3, 0, 2, true), placement(result, 3));
    }

    @Test
    public void tripleOverlap_usesThreeColumns() {
        var result = WeekLayout.layout(List.of(block(1, 8, 12), block(2, 9, 11), block(3, 10, 13)));
        assertEquals(3, placement(result, 1).columns());
        assertEquals(0, placement(result, 1).column());
        assertEquals(1, placement(result, 2).column());
        assertEquals(2, placement(result, 3).column());
    }

    @Test
    public void separateGroups_areLaidOutIndependently() {
        var result = WeekLayout.layout(List.of(block(1, 8, 10), block(2, 9, 11), block(3, 14, 16)));
        assertEquals(2, placement(result, 1).columns());
        assertEquals(new Placement(3, 0, 1, false), placement(result, 3));
        assertFalse(placement(result, 3).overlapping());
    }

    @Test
    public void unsortedInput_givesSameResult() {
        var result = WeekLayout.layout(List.of(block(3, 10, 12), block(2, 9, 11), block(1, 8, 10)));
        assertEquals(new Placement(1, 0, 2, true), placement(result, 1));
        assertEquals(new Placement(2, 1, 2, true), placement(result, 2));
        assertEquals(new Placement(3, 0, 2, true), placement(result, 3));
    }
}