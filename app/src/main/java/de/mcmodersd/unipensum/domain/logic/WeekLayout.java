package de.mcmodersd.unipensum.domain.logic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Places the sessions of one day side by side where they overlap. Touching sessions
 * (one ends exactly when the next begins) do not overlap.
 */
public final class WeekLayout {

    public record Block(long id, int startMin, int endMin) {
    }

    /**
     * @param column      zero-based column inside the overlap group
     * @param columns     number of columns of the overlap group, the block is {@code 1/columns} wide
     * @param overlapping true if the block shares time with at least one other block
     */
    public record Placement(long id, int column, int columns, boolean overlapping) {
    }

    private WeekLayout() {
    }

    /** Result is ordered by start time, then end time, then id. */
    public static List<Placement> layout(List<Block> blocks) {
        List<Block> sorted = new ArrayList<>(blocks);
        sorted.sort(Comparator.comparingInt(Block::startMin)
                .thenComparingInt(Block::endMin)
                .thenComparingLong(Block::id));

        List<Placement> result = new ArrayList<>();
        List<Block> group = new ArrayList<>();
        List<Integer> groupColumns = new ArrayList<>();
        List<Integer> columnEnds = new ArrayList<>();
        int groupEnd = Integer.MIN_VALUE;

        for (Block block : sorted) {
            if (!group.isEmpty() && block.startMin() >= groupEnd) {
                flush(group, groupColumns, columnEnds.size(), result);
                group.clear();
                groupColumns.clear();
                columnEnds.clear();
            }

            int column = -1;
            for (int i = 0; i < columnEnds.size(); i++) {
                if (columnEnds.get(i) <= block.startMin()) {
                    column = i;
                    break;
                }
            }
            if (column < 0) {
                column = columnEnds.size();
                columnEnds.add(block.endMin());
            } else {
                columnEnds.set(column, block.endMin());
            }

            group.add(block);
            groupColumns.add(column);
            groupEnd = group.size() == 1 ? block.endMin() : Math.max(groupEnd, block.endMin());
        }
        flush(group, groupColumns, columnEnds.size(), result);
        return result;
    }

    private static void flush(List<Block> group, List<Integer> columns, int columnCount, List<Placement> out) {
        boolean overlapping = group.size() > 1;
        for (int i = 0; i < group.size(); i++) {
            out.add(new Placement(group.get(i).id(), columns.get(i), columnCount, overlapping));
        }
    }
}
