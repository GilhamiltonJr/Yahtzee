package org.example;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ScoreTableModel extends AbstractTableModel implements YahtzeeGame.GameStateListener {
    private final YahtzeeGame game;
    private final List<Row> rows;

    public ScoreTableModel(YahtzeeGame game) {
        this.game = game;
        rows = new ArrayList<>();
        for (Combination combo : game.getUpperCombinations()) {
            rows.add(getComboRow(game, combo));
        }

        rows.add(new Row("Upper section bonus", RowStyle.TOTAL,
                game::getUpperSectionBonusScore));
        rows.add(new Row("Upper section total", RowStyle.TOTAL,
                game::getUpperSectionScore));

        for (Combination combo : game.getLowerCombinations()) {
            rows.add(getComboRow(game, combo));
        }

        rows.add(new Row("Bonus Yahtzees", RowStyle.TOTAL, game::getBonusYahtzeeCount));
        rows.add(new Row("Bonus Yahtzee score", RowStyle.TOTAL, game::getBonusYahtzeeScore));
        rows.add(new Row("Lower section score", RowStyle.TOTAL, game::getLowerSectionScore));
        rows.add(new Row("Grand total", RowStyle.TOTAL, game::getPlayerScore));

        game.addGameStateListener(this);
    }

    public Row getRow(int row) {
        return rows.get(row);
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return 1 + game.getPlayers().size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return rows.get(rowIndex).getValue(columnIndex);
    }

    @Override
    public String getColumnName(int column) {
        if (column == 0) {
            return "";
        }

        return (column - 1 == game.getWhoseTurn() ? "* " : "") + game.getPlayers().get(column - 1).getName();
    }

    @Override
    public void gameStateChanged() {
        fireTableStructureChanged();
    }

    public enum RowStyle {
        COMBO,
        TOTAL
    }

    private static Row getComboRow(YahtzeeGame game, Combination combo) {
        return new Row(combo, RowStyle.COMBO, (player) -> {
            Map<Combination, Integer> playerMoves = game.getPlayerMoves(player);
            int score = playerMoves.getOrDefault(combo, -1);
            if (score != -1) {
                return score;
            }

            // If it's this player's turn, calculate what the combo would be worth.
            if (player == game.getWhoseTurn()) {
                return combo.score();
            }

            // Otherwise, nothing.
            return "";
        });
    }

    public record Row(Object leftColumnValue, RowStyle style, Function<Integer, Object> valueCalc) {
        Object getValue(int column) {
            if (column == 0) {
                return leftColumnValue;
            }

            return valueCalc.apply(column - 1);
        }
    }
}
