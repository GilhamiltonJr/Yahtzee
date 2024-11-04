package org.example;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.font.TextAttribute;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        JFrame f = new JFrame("Hello, world!");
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        YahtzeeGame game = new YahtzeeGame(List.of(new YahtzeePlayer("Player 1"), new YahtzeePlayer("Player 2")));

        // hold dice actions
        var holdActions = new ArrayList<Action>();
        for (int i = 0; i < game.getDice().getDice().size(); i++) {
            holdActions.add(new HoldDieAction(game, i));
        }

        var l = new GridBagLayout();
        f.getContentPane().setLayout(l);

        var c = new GridBagConstraints();
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridwidth = 5;
        MakeMoveAction makeMoveAction = new MakeMoveAction(game);
        JTable moveTable = getMoveTable(game);
        moveTable.getSelectionModel().addListSelectionListener(e -> {
            Combination comboToPlay = null;
            int row = moveTable.getSelectedRow();
            if (row != -1) {
                Object o = moveTable.getValueAt(row, 0);
                if (o instanceof Combination) {
                    comboToPlay = (Combination) o;
                }
            }
            makeMoveAction.setCombinationToPlay(comboToPlay);
        });

        var scrollPane = new JScrollPane(moveTable);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new Dimension((int) moveTable.getPreferredSize().getWidth(),
                5 + (int) (moveTable.getPreferredSize().getHeight()
                        + moveTable.getTableHeader().getPreferredSize().getHeight())));
        f.getContentPane().add(scrollPane, c);

        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        c.gridy++;
        c.gridwidth = 1;
        for (int i = 0; i < 5; i++) {
            var button = new JDiceToggleButton(game, i);
            button.setAction(holdActions.get(i));
            f.getContentPane().add(button, c);
        }

        c.gridy++;
        c.weighty = 0;
        c.anchor = GridBagConstraints.CENTER;
        for (int i = 0; i < 5; i++) {
            var checkbox = new JCheckBox(holdActions.get(i));
            checkbox.setHorizontalAlignment(SwingConstants.CENTER);
            f.getContentPane().add(checkbox, c);
        }

        JButton rollButton = new JButton(new RollAction(game));
        c.gridy++;
        c.gridwidth = 2;
        f.getContentPane().add(rollButton, c);

        c.gridwidth = GridBagConstraints.REMAINDER;
        f.getContentPane().add(new JButton(makeMoveAction), c);

        f.pack();
        f.setVisible(true);
        f.setResizable(false);
    }

    private static JTable getMoveTable(YahtzeeGame game) {
        JTable moveTable = new JTable(new ScoreTableModel(game)) {
            private final Font regularFont;
            private final Font boldFont;
            private final Font strikethroughFont;

            {
                regularFont = getFont();
                boldFont = regularFont.deriveFont(Collections.singletonMap(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD));
                strikethroughFont = regularFont.deriveFont(Collections.singletonMap(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON));
            }

            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component result = super.prepareRenderer(renderer, row, column);

                if (row == -1 || column == -1) {
                    return result;
                }

                result.setFont(regularFont);
                result.setEnabled(true);

                ScoreTableModel.Row modelRow = ((ScoreTableModel) getModel()).getRow(row);
                Object o = modelRow.getValue(0);

                if (modelRow.style() == ScoreTableModel.RowStyle.TOTAL) {
                    result.setFont(boldFont);
                } else if (modelRow.style() == ScoreTableModel.RowStyle.COMBO) {
                    Combination combo = o instanceof Combination ? (Combination) o : null;
                    if (column == 0 && combo != null && game.getPlayerMoves(game.getWhoseTurn()).containsKey(combo)) {
                        // Current player has already played this combo
                        result.setFont(strikethroughFont);
                        result.setEnabled(false);
                    } else if (column > 0 && combo != null && game.getPlayerMoves(column - 1).containsKey(combo)) {
                        // Player for this column has already locked in this combo.
                        result.setFont(regularFont);
                        result.setEnabled(false);
                    }
                }

                return result;
            }
        };

        moveTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        return moveTable;
    }

    private static class MakeMoveAction extends AbstractAction implements YahtzeeGame.GameStateListener {
        private final YahtzeeGame game;
        private Combination combinationToPlay;

        public MakeMoveAction(YahtzeeGame game) {
            this.game = game;
            setEnabled(false);
            updateName();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            game.makeMove(combinationToPlay);
        }

        public void setCombinationToPlay(Combination combo) {
            this.combinationToPlay = combo;
            setEnabled(combo != null && !game.getPlayerMoves(game.getWhoseTurn()).containsKey(combo));
            updateName();
        }

        @Override
        public void gameStateChanged() {
            updateName();
        }

        private void updateName() {
            if (combinationToPlay == null) {
                putValue(AbstractAction.NAME, "Select combination to play...");
            } else {
                putValue(AbstractAction.NAME, "Play " + combinationToPlay.getName() + " for " + combinationToPlay.score() + " points");
            }
        }
    }

    private static class HoldDieAction extends AbstractAction implements YahtzeeGame.GameStateListener {
        private final YahtzeeGame game;
        private final int die;

        public HoldDieAction(YahtzeeGame game, int die) {
            this.game = game;
            this.die = die;
            putValue(AbstractAction.SELECTED_KEY, false);
            putValue(AbstractAction.SHORT_DESCRIPTION, "Hold die " + (die + 1));
            game.addGameStateListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            game.setDieHeld(die, (boolean) getValue(AbstractAction.SELECTED_KEY));
        }

        @Override
        public void gameStateChanged() {
            putValue(AbstractAction.SELECTED_KEY, game.getDice().isHeld(die));
        }
    }

    private static class RollAction extends AbstractAction implements YahtzeeGame.GameStateListener {
        private final YahtzeeGame game;

        public RollAction(YahtzeeGame game) {
            this.game = game;
            setEnabled(game.getRollsRemaining() > 0);
            putValue(AbstractAction.NAME, "Roll, " + game.getRollsRemaining() + " remaining");
            game.addGameStateListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            game.rollDice();
        }

        @Override
        public void gameStateChanged() {
            putValue(AbstractAction.NAME, "Roll, " + game.getRollsRemaining() + " remaining");
            setEnabled(game.getRollsRemaining() > 0);
        }
    }
}