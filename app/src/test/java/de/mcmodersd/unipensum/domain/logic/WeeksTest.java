package de.mcmodersd.unipensum.domain.logic;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;

public class WeeksTest {

    @Test
    public void anchor_isAMonday() {
        assertEquals(DayOfWeek.MONDAY, Weeks.ANCHOR.getDayOfWeek());
    }

    @Test
    public void mondayOfAndPositionOf_areInverse() {
        for (int position : new int[]{0, 1, 300, 1559}) {
            LocalDate monday = Weeks.mondayOf(position);
            assertEquals(DayOfWeek.MONDAY, monday.getDayOfWeek());
            assertEquals(position, Weeks.positionOf(monday));
            assertEquals(position, Weeks.positionOf(monday.plusDays(6)));
            assertEquals(position + 1, Weeks.positionOf(monday.plusDays(7)));
        }
    }

    @Test
    public void positionBeforeTheAnchor_isNegative() {
        assertEquals(-1, Weeks.positionOf(LocalDate.of(2020, 1, 5)));
    }

    @Test
    public void initialPosition_onAWeekday_isTheCurrentWeek() {
        LocalDate wednesday = LocalDate.of(2026, 10, 7);
        assertEquals(Weeks.positionOf(wednesday), Weeks.initialPosition(wednesday));
    }

    @Test
    public void initialPosition_onTheWeekend_isTheComingWeek() {
        LocalDate sunday = LocalDate.of(2026, 10, 4);
        LocalDate nextMonday = LocalDate.of(2026, 10, 5);
        assertEquals(Weeks.positionOf(nextMonday), Weeks.initialPosition(sunday));
        assertEquals(Weeks.positionOf(nextMonday), Weeks.initialPosition(LocalDate.of(2026, 10, 3)));
    }

    @Test
    public void initialPosition_isClampedToThePagerRange() {
        assertEquals(0, Weeks.initialPosition(LocalDate.of(2001, 1, 1)));
        assertEquals(Weeks.PAGE_COUNT - 1, Weeks.initialPosition(LocalDate.of(2099, 1, 1)));
    }

    @Test
    public void isoWeekNumber() {
        assertEquals(41, Weeks.isoWeekNumber(LocalDate.of(2026, 10, 5)));
        assertEquals(1, Weeks.isoWeekNumber(LocalDate.of(2025, 12, 29)));
    }
}
