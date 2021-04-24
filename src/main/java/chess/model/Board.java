package chess.model;

import java.util.ArrayList;

/**
 * The Board class contains all information about the current state of the game.
 * Instances of Board will be used as game history and as parameter for the engine.
 */
public class Board {
    // representing the chess board where squares[0] is "a1" and squares[63] is "h8"
    private int[] squares;
    private int turnColor;


    /**
     * Construct empty Board instance
     */
    public Board() {
        this.squares = new int[64];
        this.turnColor = Piece.White;
    }


    /**
     * Construct Board instance from fen string
     * @param fenString Integer representation starting square
     */
    public Board(String fenString) {
        // TODO write tests
        // TODO write function
    }


    /**
     * Construct Board instance from fen string
     * @param fenString board position in Forsyth–Edwards Notation
     * @param turnColor which color is next turn
     */
    public Board(String fenString, int turnColor) {
        // TODO write tests
        // TODO write function
    }


    /**
     * Returns new Board instance after making move
     * @param move The move to be made.
     */
    public Board makeMove(Move move) {
        // TODO write tests
        // TODO write function
        return this;
    }


    /**
     * Returns the index in square array for position
     * @param position position string e.g.: "e5"
     */
    public int positionToIndex(String position){
        // TODO write tests
        // TODO write function
        return 0;
    }


    /**
     * Returns the string representation of the board as used by the console client
     */
    public String toString() {
        // TODO write tests
        // TODO write function
        return "";
    }


    /**
     * Returns the current piece at position squareIndex
     * @param squareIndex where to look for piece
     */
    public int getPieceAt(int squareIndex) {
        // TODO write tests
        // TODO write function
        return 0;
    }


    /**
     * Returns the indices for all pieces of a certain color
     * @param color of the pieces
     */
    public ArrayList<Integer> getPiecePositionsFor(int color){
        // TODO write tests
        // TODO write function
        return null;
    }

}
