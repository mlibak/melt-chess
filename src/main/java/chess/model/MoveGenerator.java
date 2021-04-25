package chess.model;

import java.util.ArrayList;

/**
 * Encapsulates the rules for moving pieces on the chess board
 * and offers functions for finding and validating moves.
 */
public class MoveGenerator {

    // constants for directions: newPosition = currentPosition + <direction>
    public static final int UP = -8;
    public static final int DOWN = 8;
    public static final int LEFT = -1;
    public static final int RIGHT = 1;
    public static final int UPLEFT = -7;
    public static final int UPRIGHT = -9;
    public static final int DOWNLEFT = 7;
    public static final int DOWNRIGHT = 9;

    private Board board;
    private ArrayList<Integer> whitePiecePositions;
    private ArrayList<Integer> blackPiecePositions;
    private int teamColor, opponentColor;

    MoveGenerator(Board board) {
        this.board = board;
        whitePiecePositions = board.getPiecePositionsFor(Piece.White);
        blackPiecePositions = board.getPiecePositionsFor(Piece.Black);
        teamColor = board.getTurnColor();
        opponentColor = teamColor == Piece.White ? Piece.Black : Piece.White;
    }


    /**
     * Generates moves according to the rules for the pawn piece
     * @param startSquare the position of the pawn
     * @return ArrayList of Move objects
     */
    public ArrayList<Move> generatePawnMoves(int startSquare) {

        ArrayList<Move> generatedMoves = new ArrayList<>();
        int direction = teamColor == Piece.Black ? DOWN : UP;

        // move forward if possible
        int forwardPosition = startSquare + direction;
        if (!Coordinate.isUpMost(forwardPosition) && !Coordinate.isDownMost(forwardPosition)
                && board.getPieceAt(forwardPosition) == Piece.None) {
            generatedMoves.add(new Move(startSquare, forwardPosition));
        }

        // if moving forward lead to the last square in the file, exchanging the piece is possible
        if (board.getPieceAt(forwardPosition) == Piece.None
                && (Coordinate.isUpMost(forwardPosition) || Coordinate.isDownMost(forwardPosition))) {
            // Make the default queen exchange
            generatedMoves.add(new Move(startSquare, forwardPosition, Move.PromoteToQueen));
        }

        // if still in starting row, move two squares forward
        forwardPosition = startSquare + 2 * direction;
        int rank = Coordinate.fromIndex(startSquare)[1];
        if (rank == 1 && direction == DOWN || rank == 6 && direction == UP) {
            if (board.getPieceAt(forwardPosition) == Piece.None)
                generatedMoves.add(new Move(startSquare, forwardPosition, Move.PawnTwoForward));
        }

        // if possible, capture diagonal pieces
        for (int diagonalPosition : new int[]{startSquare+direction+LEFT, startSquare+direction+RIGHT}) {

            if (!Coordinate.isOnBorder(startSquare)) {
                if (Piece.isColor(board.getPieceAt(diagonalPosition), opponentColor))
                    generatedMoves.add(new Move(startSquare, diagonalPosition));
                if (board.getEnPassantSquare() == diagonalPosition)
                    generatedMoves.add(new Move(startSquare, diagonalPosition, Move.EnPassantCapture));
            }
        }

        return generatedMoves;
    }

    /**
     * Walks in each direction until border of the board or another piece is reached
     * @param startSquare the position of the piece
     * @param directions ArrayList of {UP, DOWNRIGHT, ...}
     * @return ArrayList of Move objects
     */
    private ArrayList<Move> generateDirectionalMoves(int startSquare, ArrayList<Integer> directions) {
        // walk in each direction until meeting border or piece
        ArrayList<Move> generatedMoves = new ArrayList<>();
        int currentSquare;
        int piece;
        for (int direction : directions) {
            currentSquare = startSquare;
            do {
                currentSquare += direction;
                piece = board.getPieceAt(currentSquare);
                if (Piece.getType(piece) == Piece.None || Piece.isColor(piece, opponentColor)) {
                    generatedMoves.add(new Move(startSquare, currentSquare));
                    if (Piece.isColor(piece, opponentColor))
                        break;
                }
                if (Piece.isColor(piece, teamColor))
                    break;
            } while (!Coordinate.isOnBorder(currentSquare));

        }
        return generatedMoves;
    }


    private ArrayList<Integer> generateStartingDirectionsAcross(int startSquare) {
        ArrayList<Integer> directions = new ArrayList<>();
        // do not walk off the board
        if (!Coordinate.isLeftMost(startSquare))
            directions.add(LEFT);
        if (!Coordinate.isRightMost(startSquare))
            directions.add(RIGHT);
        if (!Coordinate.isUpMost(startSquare))
            directions.add(UP);
        if (!Coordinate.isDownMost(startSquare))
            directions.add(DOWN);
        return directions;
    }


    /**
     * Generates moves across the current file and rank
     * @param startSquare the position of the piece
     * @return ArrayList of Move objects
     */
    public ArrayList<Move> generateAcrossMoves(int startSquare) {
        ArrayList<Integer> directions = generateStartingDirectionsAcross(startSquare);
        return generateDirectionalMoves(startSquare, directions);
    }

    private ArrayList<Integer> generateStartingDirectionsDiagonal(int startSquare) {
        ArrayList<Integer> directions = new ArrayList<>();
        // do not walk off the board
        if (!Coordinate.isLeftMost(startSquare) && !Coordinate.isUpMost(startSquare))
            directions.add(UPLEFT);
        if (!Coordinate.isRightMost(startSquare) && !Coordinate.isUpMost(startSquare))
            directions.add(UPRIGHT);
        if (!Coordinate.isLeftMost(startSquare) && !Coordinate.isDownMost(startSquare))
            directions.add(DOWNLEFT);
        if (!Coordinate.isRightMost(startSquare) && !Coordinate.isDownMost(startSquare))
            directions.add(DOWNRIGHT);
        return directions;
    }

    /**
     * Generates diagonal moves
     * @param startSquare the position of the piece
     * @return ArrayList of Move objects
     */
    public ArrayList<Move> generateDiagonalMoves(int startSquare) {
        ArrayList<Integer> directions = generateStartingDirectionsDiagonal(startSquare);
        return generateDirectionalMoves(startSquare, directions);
    }

    /**
     * Generate moves for the rook
     * @param startSquare the position of the rook
     * @return ArrayList of Move objects
     */
    public ArrayList<Move> generateRookMoves(int startSquare) {
        return generateAcrossMoves(startSquare);
    }


    /**
     * Generate moves for the bishop
     * @param startSquare the position of the rook
     * @return ArrayList of Move objects
     */
    public ArrayList<Move> generateBishopMoves(int startSquare) {
        return generateDiagonalMoves(startSquare);
    }


    /**
     * Generate moves for the queen
     * @param startSquare the position of the queen
     * @return ArrayList of Move objects
     */
    public ArrayList<Move> generateQueenMoves(int startSquare) {
        ArrayList<Move> moves = generateDiagonalMoves(startSquare);
        moves.addAll(generateAcrossMoves(startSquare));
        return moves;
    }

    /**
     * Checks whether castling for current color is possible
     * @return array of two booleans {x1Possible, x8Possible}
     */
    private boolean[] isCastlingPossible(int startSquare) {
        if (startSquare != 4 && startSquare != 60)
            return new boolean[]{false, false};
        boolean[] isPossible = new boolean[]{true, true};

        if (board.isCastlingA1Possible() && teamColor == Piece.White || board.isCastlingH1Possible() && teamColor == Piece.Black) {
            for (int i = 1; i < 4; i++) {
                if (Piece.getType(board.getPieceAt(startSquare - i)) != Piece.None) {
                    isPossible[0] = false;
                    break;
                }
            }
            if (Piece.getType(board.getPieceAt(startSquare - 4)) != Piece.Rook)
                isPossible[0] = false;
        } else {
            isPossible[0] = false;
        }
        if (board.isCastlingA8Possible() && teamColor == Piece.White || board.isCastlingH8Possible() && teamColor == Piece.Black) {
            for (int i = 1; i < 3; i++) {
                if (Piece.getType(board.getPieceAt(startSquare + i)) != Piece.None) {
                    isPossible[1] = false;
                    break;
                }
            }
            if (Piece.getType(board.getPieceAt(startSquare + 3)) != Piece.Rook)
                isPossible[1] = false;
        } else {
            isPossible[1] = false;
        }
        return isPossible;
    }

    /**
     * Generate moves for the king
     * @param startSquare the position of the king
     * @return ArrayList of Move objects
     */
    public ArrayList<Move> generateKingMoves(int startSquare) {
        ArrayList<Move> generatedMoves = new ArrayList<>();
        ArrayList<Integer> directions = generateStartingDirectionsAcross(startSquare);
        directions.addAll(generateStartingDirectionsDiagonal(startSquare));

        // normal movement
        int targetSquare, piece;
        for (int direction : directions) {
            targetSquare = startSquare + direction;
            piece = board.getPieceAt(targetSquare);
            if (Piece.getColor(piece) == teamColor)
                continue;
            generatedMoves.add(new Move(startSquare, targetSquare));
        }

        // castling
        boolean[] castlingPossible = isCastlingPossible(startSquare);
        if (castlingPossible[0]) {
            targetSquare = (teamColor == Piece.Black) ? 2 : 58;
            generatedMoves.add(new Move(startSquare, targetSquare, Move.Castling));
        }
        if (castlingPossible[1]) {
            targetSquare = (teamColor == Piece.Black) ? 6 : 62;
            generatedMoves.add(new Move(startSquare, targetSquare, Move.Castling));
        }
        return generatedMoves;
    }


    /**
     * Generate a list of all possible moves
     */
    public ArrayList<Move> generateMoves() {
        // TODO write tests (king, knight)
        ArrayList<Move> generatedMoves = new ArrayList<>();
        ArrayList<Integer> teamPositions = teamColor == Piece.Black ? blackPiecePositions : whitePiecePositions;

        for (int position : teamPositions) {
            generatedMoves.addAll(generateMovesFrom(position));
        }
        return generatedMoves;
    }


    /**
     * Returns true if the move is legal according to the rules of chess
     * @param move the move to check
     */
    public boolean isValid(Move move) {
        // TODO write tests
        // TODO write function
        return false;
    }


    /**
     * Generate a list of all possible moves starting on a given square
     * @param startSquare the starting square
     * @return ArrayList of Move objects
     */
    public ArrayList<Move> generateMovesFrom(int startSquare) {
        // gets tested via generateMoves()
        // TODO write function

        switch (board.getPieceAt(startSquare)) {
            case Piece.Pawn:
                return generatePawnMoves(startSquare);
            case Piece.Queen:
                return generateQueenMoves(startSquare);
            case Piece.Bishop:
                return generateBishopMoves(startSquare);
            case Piece.Rook:
                return generateRookMoves((startSquare));
        }
        return new ArrayList<>();
    }

}
