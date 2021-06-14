package chess.gui.game;

import java.util.List;

import chess.gui.settings.SettingsModel;
import chess.gui.util.TextManager;
import chess.model.Board;
import chess.model.Coordinate;
import chess.model.Game;
import chess.model.Move;
import chess.model.MoveValidator;
import chess.model.Piece;
import javafx.animation.RotateTransition;
import javafx.beans.binding.Bindings;
import javafx.concurrent.Service;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;

/**
 * Controls behaviour of GUI chessboard.
 */
public class BoardController {

	private GameController gameController;
	private Service service;
	protected boolean isRotated = false;

	/**
	 * Create new BoardController instance of
	 * 
	 * @param gameController the current game controller
	 */
	public BoardController(GameController gameController) {
		this.gameController = gameController;
	}

	/**
	 * Initialize chessboard in GameView
	 */
	protected void initialize() {
		if (service == null) {
			service = new PerformEngineMoveService();
		} else {
			// cancel a running service
			service.cancel();
		}

		gameController.boardGrid.setDisable(false);
		gameController.boardGrid.prefHeightProperty().bind(Bindings.min(
				gameController.rootPane.widthProperty().divide(1.94), gameController.rootPane.heightProperty().divide(1.09)));
		gameController.boardGrid.prefWidthProperty().bind(Bindings.min(gameController.rootPane.widthProperty().divide(1.94),
				gameController.rootPane.heightProperty().divide(1.09)));
		gameController.boardGrid.maxHeightProperty().bind(gameController.boardGrid.prefHeightProperty());
		gameController.boardGrid.maxWidthProperty().bind(gameController.boardGrid.prefWidthProperty());

		gameController.lineNumbersPane.prefHeightProperty().bind(gameController.boardGrid.heightProperty());
		gameController.lineNumbersPane.prefWidthProperty().bind(Bindings.min(
				gameController.rootPane.widthProperty().divide(42.66), gameController.rootPane.heightProperty().divide(24)));

		gameController.columnLettersPane.prefHeightProperty().bind(gameController.lineNumbersPane.widthProperty());
		gameController.columnLettersPane.prefWidthProperty().bind(gameController.boardGrid.widthProperty());

		gameController.boardGrid.getChildren().forEach(s -> {
			s.getStyleClass().removeAll("focused", "possibleMove", "checkMove", "captureMove");
		});

		if ((GameModel.getCurrentGame().getCurrentPosition().getTurnColor() == Piece.Black && !isRotated
				&& SettingsModel.isFlipBoard())
				|| (GameModel.getGameMode() != GameModel.ChessMode.Player
						&& GameModel.getColor() == GameModel.ChessColor.Black))
			flipBoard(false);

		checkForGameOver();
	}

	/**
	 * Moves the selected Piece to the selected destination if allowed
	 * 
	 * @param startIndex  the index of the selected piece
	 * @param targetIndex the selected destination
	 */
	protected void movePieceOnBoard(int startIndex, int targetIndex) {
		Move move = new Move(startIndex, targetIndex);
		Board board = GameModel.getCurrentGame().getCurrentPosition();
		Move testMove = new Move(startIndex, targetIndex);
		Game testGame = GameModel.getCurrentGame();
		testGame.addFlag(testMove);

		if (Piece.isColor(board.getPieceAt(startIndex), board.getTurnColor())
				&& MoveValidator.validateMove(testGame.getCurrentPosition(), testMove)) {
			// if promotion is possible
			if ((Coordinate.isOnUpperBorder(targetIndex) || Coordinate.isOnLowerBorder(targetIndex))
					&& Piece.isType(board.getPieceAt(startIndex), Piece.Pawn)) {
				// open the PopupMenu to choose promotion
				gameController.gamePopup.showPromotionPopup(GameModel.getCurrentGame().getCurrentPosition().getTurnColor(),
						move);
				return;
			}
		}
		finishMove(move);
	}

	/**
	 * Displays the possible moves on the View
	 * 
	 * @param startPosition from where to look for moves
	 */
	protected void showPossibleMoves(int startPosition) {

		Board board = GameModel.getCurrentGame().getCurrentPosition();
		if (!Piece.isColor(board.getPieceAt(startPosition), board.getTurnColor()))
			return;

		for (Move move : GameModel.getPossibleMoves(startPosition)) {
			Node squareNode = gameController.boardGrid.getChildren().get(move.getTargetSquare());

			if (!(squareNode instanceof AnchorPane))
				return;

			AnchorPane square = (AnchorPane) squareNode;
			String styleClass = square.getChildren().isEmpty() ? "possibleMove" : "captureMove";
			if (MoveValidator.getPossibleCheckMoves(board, board.getTurnColor(), move).contains(move)) {
				styleClass = "checkMove";
			}
			square.getStyleClass().addAll(styleClass);
		}
	}

	/**
	 * Finishes the current move after flags for promotion are set.
	 * 
	 * @param move the current move containing flags for promotion
	 */
	protected void finishMove(Move move) {
		GameModel.getCurrentGame().addFlag(move);
		System.out.println("Called: movePieceOnBorad(" + move.toString() + ")");
		List<Integer> capturedPieces = GameModel.getCurrentGame().getCurrentPosition().getCapturedPieces();
		if (GameModel.getCurrentGame().attemptMove(move)) {

			if (GameModel.getCurrentGame().checkWinCondition() == 0) {
				if (GameModel.getCurrentGame().checkCheck() && SettingsModel.isShowInCheck()) {
					GameModel.playSound(GameModel.ChessSound.Check, true);
				} else if (GameModel.getCurrentGame().getCurrentPosition().getCapturedPieces().equals(capturedPieces)) {
					GameModel.playSound(GameModel.ChessSound.Move, true);
				} else {
					GameModel.playSound(GameModel.ChessSound.Capture, true);
				}
			}

			System.out.println("Move happened: " + move.toString());
			GameModel.getMovesHistory().add(0, move);
			if (SettingsModel.isFlipBoard() && GameModel.getGameMode() != GameModel.ChessMode.Computer)
				flipBoard(true);
		} else {
			System.out.println("Game.attemptMove() did not allow " + move.toString());
			GameModel.playSound(GameModel.ChessSound.Failure, true);
			return;
		}

		gameController.updateUI();
		if (checkForGameOver())
			return;

		if (GameModel.getGameMode() == GameModel.ChessMode.Computer) {

			gameController.activityIndicator.visibleProperty().bind(service.runningProperty());
			gameController.boardGrid.setDisable(true);

			service.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
				@Override
				public void handle(WorkerStateEvent workerStateEvent) {
					finishEngineMove();
				}
			});
			service.restart();
		}

	}

	/**
	 * Updates a performed move of the Engine to the view
	 * 
	 * @return null
	 */
	private EventHandler<WorkerStateEvent> finishEngineMove() {

		System.out.println("finishEngineMove() was called");

		GameModel.setSelectedIndex(-1);
		gameController.boardGrid.getChildren().forEach(s -> {
			s.getStyleClass().removeAll("focused", "possibleMove", "checkMove", "captureMove");
		});
		GameModel.setAllowedToMove(true);
		gameController.boardGrid.setDisable(false);
		checkForGameOver();
		gameController.updateUI();
		return null;
	}

	/**
	 * Checks whether the current game is over if that is the case the endGame()
	 * function will be called
	 */
	protected boolean checkForGameOver() {
		gameController.checkLabel.setVisible(false);
		Game game = GameModel.getCurrentGame();
		if (game.checkCheck()) {
			System.out.println("checkForGameOver was called");

			String key;
			if (game.getCurrentPosition().getTurnColor() == Piece.White) {
				key = "game.whiteInCheck";
			} else {
				key = "game.blackInCheck";
			}
			if (SettingsModel.isShowInCheck())
				gameController.checkLabel.setVisible(true);
			TextManager.computeText(gameController.checkLabel, key);
		}
		if (game.checkWinCondition() != 0) {
			endGame();
			return true;
		}
		return false;
	}

	/**
	 * Ends the current game
	 */
	public void endGame() {
		Game game = GameModel.getCurrentGame();
		int winCondition = game.checkWinCondition();

		// only end the game when it should be ended
		if (winCondition == 0)
			return;

		String key = "";
		if (winCondition == 1) {
			if (game.getCurrentPosition().getTurnColor() == Piece.White) {
				key = "game.whiteInCheckmate";
			} else {
				key = "game.blackInCheckmate";
			}
		} else if (winCondition == 2) {
			if (game.getCurrentPosition().getTurnColor() == Piece.White) {
				key = "game.whiteInRemis";
			} else {
				key = "game.blackInRemis";
			}
		}
		TextManager.computeText(gameController.checkLabel, key);
		gameController.checkLabel.setVisible(true);
		gameController.gamePopup.showGameOverPopup(winCondition);
	}

	/**
	 * Flips the board
	 * 
	 * @param animate whether should animate rotate
	 */
	protected void flipBoard(boolean animate) {
		String lines = isRotated ? "87654321" : "12345678";
		String columns = isRotated ? "abcdefgh" : "hgfedcba";

		for (int i = 0; i < 8; i++) {
			((Label) gameController.lineNumbersPane.getChildren().get(i)).setText(lines.charAt(i) + "");
			((Label) gameController.columnLettersPane.getChildren().get(i)).setText(columns.charAt(i) + "");
		}

		if (animate) {
			// -------- rotate with animation --------
			RotateTransition transition = new RotateTransition(Duration.seconds(1.2), gameController.boardGrid);
			transition.setToAngle(isRotated ? 0 : 180);
			// transition.setDelay(Duration.seconds(0.5));
			transition.play();
		} else {
			// -------- rotate without animation --------
			gameController.boardGrid.setRotate(isRotated ? 0 : 180);
		}

		for (Node squareNode : gameController.boardGrid.getChildren()) {
			squareNode.setRotate(isRotated ? 0 : 180);
		}
		isRotated = !isRotated;
	}

}