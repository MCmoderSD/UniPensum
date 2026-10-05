package de.mcmodersd.unipensum.domain.logic;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.LocalDate;

import de.mcmodersd.unipensum.domain.logic.SemesterNamer.Label;
import de.mcmodersd.unipensum.domain.logic.SemesterNamer.Season;

public class SemesterNamerTest {

    private static Label label(int year, int month, int day) {
        return SemesterNamer.label(LocalDate.of(year, month, day));
    }

    @Test
    public void october_isWinterOfThatYear() {
        assertEquals(new Label(Season.WINTER, 2026), label(2026, 10, 5));
    }

    @Test
    public void september_startsWinter() {
        assertEquals(new Label(Season.WINTER, 2026), label(2026, 9, 1));
    }

    @Test
    public void december_isStillWinterOfThatYear() {
        assertEquals(new Label(Season.WINTER, 2026), label(2026, 12, 31));
    }

    @Test
    public void januaryAndFebruary_belongToWinterOfPreviousYear() {
        assertEquals(new Label(Season.WINTER, 2026), label(2027, 1, 5));
        assertEquals(new Label(Season.WINTER, 2026), label(2027, 2, 28));
    }

    @Test
    public void marchToAugust_isSummer() {
        assertEquals(new Label(Season.SUMMER, 2027), label(2027, 3, 1));
        assertEquals(new Label(Season.SUMMER, 2027), label(2027, 4, 13));
        assertEquals(new Label(Season.SUMMER, 2027), label(2027, 8, 31));
    }
}
