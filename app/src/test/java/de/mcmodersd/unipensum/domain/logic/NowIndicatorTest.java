package de.mcmodersd.unipensum.domain.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public class NowIndicatorTest {

    /** A Monday. */
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 5);

    private static Optional<NowIndicator.Position> at(LocalDateTime now) {
        return NowIndicator.at(MONDAY, now, 7, 22);
    }

    @Test
    public void duringTheShownWeek_isTheColumnOfToday() {
        for (var day = 0; day < 5; day++) {
            var position = at(MONDAY.plusDays(day).atTime(10, 30)).orElseThrow();
            assertEquals(day, position.dayIndex());
            assertEquals(10 * 60 + 30, position.minutes());
        }
    }

    @Test
    public void onTheWeekend_thereIsNoColumn() {
        assertFalse(at(MONDAY.plusDays(5).atTime(10, 30)).isPresent());
        assertFalse(at(MONDAY.plusDays(6).atTime(10, 30)).isPresent());
    }

    @Test
    public void inAnotherWeek_thereIsNoMarker() {
        assertFalse(at(MONDAY.minusDays(1).atTime(10, 30)).isPresent());
        assertFalse(at(MONDAY.plusWeeks(1).atTime(10, 30)).isPresent());
        assertFalse(at(MONDAY.minusWeeks(1).plusDays(2).atTime(10, 30)).isPresent());
    }

    @Test
    public void outsideTheVisibleHours_thereIsNoMarker() {
        assertFalse(at(MONDAY.atTime(6, 59)).isPresent());
        assertFalse(at(MONDAY.atTime(22, 1)).isPresent());
        assertFalse(at(MONDAY.atTime(0, 0)).isPresent());
        assertFalse(at(MONDAY.atTime(23, 59)).isPresent());
    }

    @Test
    public void theFirstAndTheLastMinuteOfTheGrid_arePartOfIt() {
        assertTrue(at(MONDAY.atTime(7, 0)).isPresent());
        assertTrue(at(MONDAY.atTime(22, 0)).isPresent());
    }

    @Test
    public void followsTheVisibleHours() {
        var evening = MONDAY.atTime(20, 15);
        assertTrue(NowIndicator.at(MONDAY, evening, 7, 22).isPresent());
        assertFalse(NowIndicator.at(MONDAY, evening, 7, 18).isPresent());
        assertTrue(NowIndicator.at(MONDAY, evening, 18, 24).isPresent());
    }

    @Test
    public void seconds_doNotMatter() {
        assertEquals(at(MONDAY.atTime(9, 41, 3)), at(MONDAY.atTime(9, 41, 58)));
    }
}