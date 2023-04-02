package clueGame;

import java.util.ArrayList;

public abstract class Player {
    private String name;
    private String color;
    private int row;
    private int col;
    private ArrayList<Card> cards;
    private boolean isHuman;

    public Player() {

    }

    public Player(String name, String color, int row, int col, boolean isHuman) {
        this.name = name;
        this.color = color;
        this.row = row;
        this.col = col;
        this.isHuman = isHuman;
        cards = new ArrayList<Card>();
    }

    public void addCard(Card card) {
        cards.add(card);
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public ArrayList<Card> getCards() {
        return cards;
    }

    public boolean isHuman() {
        return isHuman;
    }

    public void clearHand() {
        cards.clear();
    }
}
