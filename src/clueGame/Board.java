package clueGame;

import java.util.*;
import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

//Board Class
//Author: William Warner
//Created March 5th, 2023
//No Collaboraters or outside sources

//Board represents a Board in the Clue game
//Contains all BoardCells and Rooms in the game
//Handles the setup of the board from data files
//Initializes Each BoardCell based on data files
//Calculates targets given a starting cell and dice roll



public class Board extends JPanel implements MouseListener{
    private int cols;
    private int rows;
    private BoardCell[][] board;
    private Set<BoardCell> targets;
    private Set<BoardCell> visited;
    private File setupFile;
    private File layoutFile;
    private ArrayList<Room> rooms;
    private ArrayList<Player> players;
    private ArrayList<Card> cards;
    private Solution solution;
    private int turnNum = 0;
    

    private Set<Card> weaponCards = new HashSet<Card>();
    private Set<Card> roomCards = new HashSet<Card>();
    private Set<Card> personCards = new HashSet<Card>();

    private static Board theInstance = new Board();

    //private constructor so can only be created once, directly above this comment
    private Board() {
        super();
        addMouseListener(this);
        visited = new HashSet<BoardCell>();
        rooms = new ArrayList<Room>();
        targets = new HashSet<BoardCell>();
        players = new ArrayList<Player>();
        cards = new ArrayList<Card>();
        solution = new Solution();
    }

    //returns this instance
    public static Board getInstance() {
        return theInstance;
    }
    
    //calculates BoardCell targets given a starting cell and dice roll
    public void calcTargets(BoardCell startCell, int pathLength, BoardCell initialCell) {
        if (startCell == initialCell) {
            clearTargets();
        }
        visited.add(startCell);
        for (BoardCell adjCell : startCell.getAdjList()) {
            //no need to visit a cell that has already been visited in this path
            if (!visited.contains(adjCell)) {
                //if an adjacent cell is a room, we can just add it to the targets
                if (adjCell.isRoom()) {
                    targets.add(adjCell);
                }
                else if (adjCell.isOccupied()) {
                    
                }
                else if (pathLength == 1) {
                    targets.add(adjCell);
                }
                else {
                    //recursive call with a pathLength that is one less
                    calcTargets(adjCell, pathLength - 1, initialCell);
                    visited.remove(adjCell);
                } 
            }
        }
    }

    //returns a BoardCell based on the given row and collumn
    public BoardCell getCell(int row, int col) {
        return board[row][col];
    }

    //returns targets (must calculate targets first)
    public Set<BoardCell> getTargets() {
        return targets;
    }
    //sets the config files
    public void setConfigFiles(String layoutF, String setupF) {
        this.layoutFile = new File(layoutF);
        this.setupFile = new File(setupF);
    }

    //parses the setup file to initialize rooms
    public void loadSetupConfig() throws BadConfigFormatException {
        rooms.clear();
        players.clear();
        cards.clear();
        Scanner scanner;

        try {
            String line = "";
            scanner = new Scanner(this.setupFile);
            while (scanner.hasNextLine()) {
                line = scanner.nextLine();
                if (!line.contains("//")) {
                    String[] setupWords = line.split(", ");
                    //setup file must be in format: Room, Name, 'N'
                    if (!(setupWords[0].equals("Room") || setupWords[0].equals("Space") || setupWords[0].equals("Player") || setupWords[0].equals("Weapon"))) {
                        scanner.close();
                        throw new BadConfigFormatException("Bad format for Setup file");
                    }
                    if (setupWords[0].equals("Room") || setupWords[0].equals("Space")) {
                        rooms.add(new Room(setupWords[1], setupWords[2].charAt(0)));
                        if (setupWords[0].equals("Room")) {
                            cards.add(new Card(setupWords[1], CardType.ROOM));
                            roomCards.add(new Card(setupWords[1], CardType.ROOM));
                        }
                        
                    }
                    //Player layout in Setup file
                    //Player, Name, Color, row, col, true/false
                    else if (setupWords[0].equals("Player")) {
                        int tempRow = Integer.valueOf(setupWords[3]);
                        int tempCol = Integer.valueOf(setupWords[4]);
                        boolean tempIsHuman = Boolean.parseBoolean(setupWords[5]);

                        cards.add(new Card(setupWords[1], CardType.PERSON));
                        personCards.add(new Card(setupWords[1], CardType.PERSON));

                        if (setupWords[5].equals("true")) {
                            players.add(new HumanPlayer(setupWords[1], setupWords[2], tempRow, tempCol, tempIsHuman));
                        }
                        else {
                            players.add(new ComputerPlayer(setupWords[1], setupWords[2], tempRow, tempCol, tempIsHuman));
                        }
                    }
                    else if (setupWords[0].equals("Weapon")) {
                        cards.add(new Card(setupWords[1], CardType.WEAPON));
                        weaponCards.add(new Card(setupWords[1], CardType.WEAPON));
                    }
                    else {
                        scanner.close();
                        throw new BadConfigFormatException("Unrecognized type in Setup file");
                    }
                }
            }

        } catch (FileNotFoundException e) {
            System.err.println("Setup file not found");
            System.exit(1);
        }
    }

    //parses layout file, initializes each BoardCell from that file
    //adds info for each BoardCell based on layout file
    public void loadLayoutConfig() throws BadConfigFormatException {

        Scanner scanner;
        try {
            scanner = new Scanner(this.layoutFile);
            int tempRows = 0;

            String line = "";
            while (scanner.hasNextLine()) {
                tempRows++;
                line = scanner.nextLine();

            }
            String[] cellNames = line.split(",");

            this.cols = cellNames.length;
            this.rows = tempRows;
            scanner.close();

            board = new BoardCell[rows][cols];

            // reads the info of each cell
            scanner = new Scanner(layoutFile);
            for (int r = 0; r < rows; r++) {
                line = scanner.nextLine();
                cellNames = line.split(",");
                for (int c = 0; c < cols; c++) {
                    // Throws exception if each collumn is not expected length
                    if (cols != cellNames.length) {
                        scanner.close();
                        System.err.println("Collumns are not consistent among each row");
                        throw new BadConfigFormatException("Collumns are not consistent among each row");
                    }

                    board[r][c] = new BoardCell(r, c, cellNames[c]);
                    //sets the doorDirection of each BoardCell
                    if (cellNames[c].contains(">")) {
                        board[r][c].setDoorDirection(DoorDirection.RIGHT);
                    } else if (cellNames[c].contains("<")) {
                        board[r][c].setDoorDirection(DoorDirection.LEFT);
                    } else if (cellNames[c].contains("^")) {
                        board[r][c].setDoorDirection(DoorDirection.UP);
                    } else if (cellNames[c].contains("v")) {
                        board[r][c].setDoorDirection(DoorDirection.DOWN);
                    } else {
                        board[r][c].setDoorDirection(DoorDirection.NONE);
                    }

                    Room tempRoom = this.getRoom(cellNames[c].charAt(0));
                    if (tempRoom.getName().equals("error")) {
                        scanner.close();
                        throw new BadConfigFormatException("BoardCell contains room not specified in setup file [" + r + ", " + c + "]");
                    }
                    board[r][c].setRoom(tempRoom);
                    //sets label cells for each room
                    if (board[r][c].isLabel()) {
                        board[r][c].getRoom().setlabelCell(board[r][c]);
                    }
                    //sets center cells for each room
                    if (board[r][c].isRoomCenter()) {
                        board[r][c].getRoom().setCenterCell(board[r][c]);
                    }
                    //sets secret passage destination for each room that has one
                    //this if statement got quite complicated
                    //a boardCell with a length greater than one and the 2nd char is alphabetical means that it is a secret passage
                    if (board[r][c].getName().length() > 1 && Character.isAlphabetic(board[r][c].getName().charAt(1))) {
                        board[r][c].setSecretPassage(board[r][c].getName().charAt(1));
                        board[r][c].getRoom().setPassageDestination(this.getRoom(board[r][c].getName().charAt(1)));
                    }

                }
            }

        } catch (FileNotFoundException e) {
            System.err.println("Layout File not found");
            System.exit(1);
        }

    }
    //calls functions to load setup and layout configs
    //adds adjacencies to Each BoardCell
    public void initialize() {
        targets.clear();
        try {
            this.loadSetupConfig();
        } catch (BadConfigFormatException e) {
            System.err.println("Bad Configuration for setup file");
        }
        try {
            this.loadLayoutConfig();
        } catch (BadConfigFormatException e) {
            System.err.println("Bad Configuration for layout file");
        }

       //solution must be set before cards are dealt
        setSolution();
        dealCards();

        for (int i = 0; i < players.size(); i++) {
            players.get(i).setNumber(i);
        }

        //adds adjacencies, might be good practice to make this a seperate method later
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (board[r][c].isWalkway()) {

                    if (r != 0 && board[r-1][c].isWalkway()) {
                        board[r][c].addAdjacency(board[r - 1][c]);
                    }
                    if (r < rows - 1 && board[r+1][c].isWalkway()) {
                        board[r][c].addAdjacency(board[r + 1][c]);
                    }
                    if (c != 0 && board[r][c-1].isWalkway()) {
                        board[r][c].addAdjacency(board[r][c - 1]);
                    }
                    if (c < cols - 1 && board[r][c+1].isWalkway()) {
                        board[r][c].addAdjacency(board[r][c + 1]);
                    }

                    if (board[r][c].isDoorway()) {
                        DoorDirection tempDirection = board[r][c].getDoorDirection();
                        Room tempRoom = null;
                        if (tempDirection == DoorDirection.UP) {
                            tempRoom = this.getRoom(board[r-1][c].getName().charAt(0));
                        }
                        else if (tempDirection == DoorDirection.DOWN) {
                            tempRoom = this.getRoom(board[r+1][c].getName().charAt(0));
                        }
                        else if (tempDirection == DoorDirection.LEFT) {
                            tempRoom = this.getRoom(board[r][c-1].getName().charAt(0));
                        }
                        else if (tempDirection == DoorDirection.RIGHT) {
                            tempRoom = this.getRoom(board[r][c+1].getName().charAt(0));
                        }
                        else {
                            System.err.print("door direction error");
                        }
                        if (tempRoom.getCenterCell() == null) {
                            System.err.println(tempRoom.getName());
                            System.err.println("(" + r + ", " + c + ")");
                            System.err.println();
                        }
        
                        BoardCell tempCenter = tempRoom.getCenterCell();
                        //if current cell in double for loop is a door, we add an adjacency
                        //to the room center, and also add an adjacency from the room center to the doorway
                        board[r][c].addAdjacency(tempCenter);
                        tempCenter.addAdjacency(board[r][c]);
                    }

                }
                //if current cell is a room center and it has a secret passage, we add an adjacency from
                //the cell to the center cell of the room which the passage goes to
                if (board[r][c].isRoomCenter() && board[r][c].getRoom().getPassageDestination() != null) {
                    board[r][c].addAdjacency(board[r][c].getRoom().getPassageDestination().getCenterCell());
                }

            }
        }

    }

    //deals the cards to the players, then adds the solution cards back to the deck
    public void dealCards() {
        for (int i = 0; i < 3; i++) {
            players.get(i).clearHand();
        }
        Collections.shuffle(cards);
        
        for (int p = 0; p < 18; p+= 3) {
            players.get(p / 3).addCard(cards.get(p));
            players.get(p / 3).addCard(cards.get(p + 1));
            players.get(p / 3).addCard(cards.get(p + 2));
        }

        //now we add the previosly removed solution cards back to the deck to pass tests
        cards.add(solution.getPersonCard());
        cards.add(solution.getRoomCard());
        cards.add(solution.getWeaponCard());
    }

    //sets a random solution and removes the cards from the deck
    public void setSolution() {
        Collections.shuffle(cards);
        
        int index = 0;
        while (!(cards.get(index).getCardType() == CardType.PERSON)) {
            index++;
        }
        cards.get(index).setSolution(true);
        solution.setPersonCard(cards.get(index));
        cards.remove(index);

        index = 0;
        while (!(cards.get(index).getCardType() == CardType.ROOM)) {
            index++;
        }
        cards.get(index).setSolution(true);
        solution.setRoomCard(cards.get(index));
        cards.remove(index);

        index = 0;
        while (!(cards.get(index).getCardType() == CardType.WEAPON)) {
            index++;
        }
        cards.get(index).setSolution(true);
        solution.setWeaponCard(cards.get(index));
        cards.remove(index);
    }

    //returns the number of rows
    public int getNumRows() {
        return this.rows;
    }
    //returns the number of cols
    public int getNumColumns() {
        return this.cols;
    }
    //returns a room given its character symbol
    public Room getRoom(char c) {
        for (Room r : rooms) {
            if (r.getSymbol() == c) {
                return r;
            }
        }
        return new Room("error", 'e');
    }
    //returns a room given a cell
    public Room getRoom(BoardCell bc) {
        return bc.getRoom();
    }
    //returns the adjacency list of a given cell
    public Set<BoardCell> getAdjList(int r, int c) {
        return board[r][c].getAdjList();
    }
    //clears targets
    public void clearTargets() {
        targets.clear();
        visited.clear();
    }
    //returns the Players
    public ArrayList<Player> getPlayers() {
        return players;
    }
    //returns all the cards
    public ArrayList<Card> getCards() {
        return cards;
    }
    //returns the solution to the game
    public Solution getSolution() {
        return solution;
    }

    //checks whether a given accusation is correct
    public boolean checkAccusation(Solution accusation) {
        return this.solution.equals(accusation);
    }

    //handles a suggestion, calls the method disproveSuggestion from each player
    public Card handleSuggestion(ArrayList<Card> suggestion, int playerNum) {

        for (int i = playerNum + 1; i < playerNum + 6; i++) {
            int adjustedPlayerNum = i % 6;
            if (players.get(adjustedPlayerNum).disproveSuggestion(suggestion) != null) {
                return players.get(adjustedPlayerNum).disproveSuggestion(suggestion);
            }
        }
        return null;
    }

    //allows to set the solution for testing
    public void setSolution(Solution sol) {
        this.solution = sol;
    }
    //returns all the weapon cards
    public Set<Card> getWeaponCards() {
        return weaponCards;
    }
    //returns all the person cards
    public Set<Card> getPersonCards() {
        return personCards;
    }
    //returns all the room cards
    public Set<Card> getRoomCards() {
        return roomCards;
    }

    public void paintComponent(Graphics g) {
        
        super.paintComponent(g);
        int width = getWidth();
        int height = getHeight();
        int cellWidth = width / cols;
        int cellHeight = height / rows;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                board[r][c].draw(g, cellWidth, cellHeight);
            }
        }
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                board[r][c].drawDoor(g, cellWidth, cellHeight);
            }
        }
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                board[r][c].drawRoomName(g, cellWidth, cellHeight);
            }
        }
        for (Player p : players) {
            p.draw(g, cellWidth, cellHeight);
        }
       

        
    }


    //handles when mouse clicks on the game board
    @Override
    public void mousePressed(MouseEvent e) {
        ArrayList<BoardCell> validTargetCells = new ArrayList<BoardCell>();
        Player currentPlayer = players.get(turnNum);

        if (currentPlayer instanceof HumanPlayer) {

            //calculates validTargetCells
            if (!((HumanPlayer)currentPlayer).isFinished()) {
                for (BoardCell target : targets) {
                    if (target.isRoomCenter()) {
                        ArrayList<BoardCell> otherRoomCells = getRoomCells(target);
                        for (BoardCell roomCell : otherRoomCells) {
                            validTargetCells.add(roomCell);
                        }
                    }
                    else {
                        validTargetCells.add(target);
                    }
                }
            }
            else {
                return; //ignores click when turn is already finished
            }
            //end generating validTargetCells

            if (validTargetCells.isEmpty()) {
                //handle case with no possible moves
                JOptionPane.showMessageDialog(null, "Your turn is skipped", "You have no moves!", JOptionPane.ERROR_MESSAGE);
                currentPlayer.setFinished(true);
            }

            int width = getWidth();
            int height = getHeight();
            int cellWidth = width / cols;
            int cellHeight = height / rows;
            BoardCell clickedCell = null;
            //determines clicked cell
            for (BoardCell validTargetCell : validTargetCells) {
                if (validTargetCell.containsClick(e.getX(), e.getY(), cellWidth, cellHeight)) {
                    if (validTargetCell.isRoom()) {
                        clickedCell = validTargetCell.getRoom().getCenterCell();
                    }
                    else {
                        clickedCell = validTargetCell;
                    }
                    break;
                }
            }
            if (clickedCell == null) {
                //clicked cell is not a valid target
                return;
            }
            else {
                currentPlayer.move(clickedCell);
                //if the player moves into a room, handle suggestion
                if (clickedCell.isRoomCenter()) {
                    //show popup window and get suggestion from user input
                    ArrayList<Card> tempSuggestion = getSuggestion();


                    //move suggestedPlayer into room
                    Card personCard = null;
                    for (Card c : tempSuggestion) {
                        if (c.getCardType() == CardType.PERSON) {
                            personCard = c;
                            break;
                        }
                    }
                    for (Player p : getPlayers()) {
                        if (p.getName().equals(personCard.getName())) {
                            BoardCell dest = players.get(turnNum).getRoom().getCenterCell();
                            p.move(dest);
                            break;
                        }
                    }
                    //end moving suggested player into room

                    //display guess in gameControlPanel
                    String guess = tempSuggestion.get(0).getName() + ", " + tempSuggestion.get(1).getName() + ", " + tempSuggestion.get(2).getName();
                    GameControlPanel.getInstance().setGuess(guess);

                    Card disprovedCard = this.handleSuggestion(tempSuggestion, turnNum);
                    if (disprovedCard == null) {
                        //suggestion could not be disproved
                        GameControlPanel.getInstance().setGuessResult("Could not be disproved!");
                    }
                    else {
                        //suggestion was disproved, show in gameControlPanel
                        //if human player, show disproved card and who disproved it
                        Player disprovingPlayer = null;
                        for (Player p : players) {
                            if (p.getCards().contains(disprovedCard)) {
                                disprovingPlayer = p;
                                break;
                            }
                        }
                        GameControlPanel.getInstance().setGuessResult("\"" + disprovedCard.getName() + "\"" + " card was shown by " + disprovingPlayer.getName());
                        CardsPanel.getInstance().seeCard(disprovedCard); //make player see disproved card
                    }
                    GameControlPanel.getInstance().refresh(); //TODO: make this not bug out the game control panel
                    //GameControlPanel.getInstance().repaint();

                }
                currentPlayer.setFinished(true);
                //unihighlights the target cells
                for (BoardCell bc : validTargetCells) {
                    bc.unhighlight();
                }
                repaint();
            }

        }
        else {

        }
    }

    //empty overriden methods for MouseListener
    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) {}
    @Override
    public void mouseClicked(MouseEvent e) {}
    @Override
    public void mouseReleased(MouseEvent e) {}

    //given a room center cell, returns an arraylist of all cells in the room
    public ArrayList<BoardCell> getRoomCells(BoardCell roomCenter) {

        ArrayList<BoardCell> roomCells = new ArrayList<BoardCell>();
        Room room = roomCenter.getRoom();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (board[r][c].getRoom().equals(room)) {
                    roomCells.add(board[r][c]);
                }
            }
        }
        return roomCells;
    }

    //handles logic when nextButton is pressed, called from NextButtonListener in GameControlPanel
    public void nextButtonPressed() {
        if (players.get(turnNum).isFinished()) {
            nextPlayerTurn();
            players.get(turnNum).handleTurn();
        }
        else {
            //error, turn is not over
            JOptionPane.showMessageDialog(null, "Please finish your turn", "Your turn is not over!", JOptionPane.ERROR_MESSAGE);
        }
        GameControlPanel.getInstance().repaint();
    }

    //incriments turnNum correctly
    public void nextPlayerTurn() {
        if (turnNum == 5) {
            turnNum = 0;
        }
        else {
            turnNum++;
        }
    }

    //starts first players turn
    public void startGame() {
        turnNum = 0;
        players.get(turnNum).handleTurn();
    }

    public ArrayList<Card> getSuggestion() {
        ArrayList<Card> suggestionTemp = new ArrayList<Card>();

        //make a popup where you can select the suggestion
        SuggestionPopup suggestionPopup = new SuggestionPopup(players.get(turnNum).getRoom());
        String personString = suggestionPopup.getPersonCard();
        String weaponString = suggestionPopup.getWeaponCard();
        
        for (Card c : this.getPersonCards()) {
            if (c.getName().equals(personString)) {
                suggestionTemp.add(c);
                break;
            }
        }
        for (Card c : this.getWeaponCards()) {
            if (c.getName().equals(weaponString)) {
                suggestionTemp.add(c);
                break;
            }
        }
        String roomName = players.get(turnNum).getRoom().getName();
        for (Card c : this.getRoomCards()) {
            if (c.getName().equals(roomName)) {
                suggestionTemp.add(c);
                break;
            }
        }
        return suggestionTemp;
    }

    public int getTurnNum() {
        return turnNum;
    }

    //accusation button was pressed
    public void accusationButtonPressed() {
        if (players.get(turnNum) instanceof HumanPlayer && !players.get(turnNum).isFinished()) {

        }
        else {
            return;
        }
        AccusationPopup acc = new AccusationPopup();
        Solution sol = acc.getSolution();
        boolean correctAcc = this.checkAccusation(sol);

        if (correctAcc) {
            //player wins game
            humanWinGame(players.get(0), sol);
        }
        else {
            //player loses game
            humanLoseGame(players.get(0), sol);
        }
    }

    public void humanWinGame(Player player, Solution sol) {
        String message = "You guessed the correct solution of: " + sol.getRoomCard().getName() + ", " + sol.getPersonCard().getName() + ", " + sol.getWeaponCard().getName();
        JOptionPane.showMessageDialog(null, message, "You won the game!", JOptionPane.INFORMATION_MESSAGE);
        System.exit(0);
    }
    public void humanLoseGame(Player player, Solution sol) {
        String message = "You guessed the incorrect solution of: " + sol.getRoomCard().getName() + ", " + sol.getPersonCard().getName() + ", " + sol.getWeaponCard().getName() + "\nThe correct solution was: " + solution.getRoomCard().getName() + ", " + solution.getPersonCard().getName() + ", " + solution.getWeaponCard().getName();
        JOptionPane.showMessageDialog(null, message, "You lost the game!", JOptionPane.INFORMATION_MESSAGE);
        System.exit(0);
    }

    public void computerWinGame(Player player, Solution sol) {
        String message = player.getName() + " guessed the correct solution of: " + sol.getRoomCard().getName() + ", " + sol.getPersonCard().getName() + ", " + sol.getWeaponCard().getName();
        JOptionPane.showMessageDialog(null, message, "Computer won the game!", JOptionPane.INFORMATION_MESSAGE);
        System.exit(0);
    }

}