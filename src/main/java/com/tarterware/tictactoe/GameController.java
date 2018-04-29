package com.tarterware.tictactoe;

import java.util.Random;

import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.tarterware.tictactoe.GameState.GameMode;
import com.tarterware.tictactoe.GameState.GameStage;

@Controller
public class GameController {
		
	private static final Logger log = LoggerFactory.getLogger(GameController.class);

	/**
	 * Starts new Tic Tac Toe game.
	 * 
	 * @param session 
	 * @param model Spring framework Model
	 * @return Spring framework View name
	 */
	@RequestMapping(value = "/tictactoe", method = RequestMethod.GET)
	public String game(
			HttpSession session,
			Model model) {
		
		GameState gameState = getStateFromSession(session);
		if(gameState == null) {
			log.info("gameState is null; starting new game");
			gameState = new GameState();
			putStateInSession(session, gameState);
		}
		model.addAttribute(Constants.GAME_STATE, gameState);
		
		return Constants.VIEW_GAME;
	}
	
	/**
	 * Resets the game to it's initial state, and allows selection of
	 * either play against the computer, or against another human.
	 * 
	 * @param session 
	 * @param model Spring framework Model
	 * @return Spring framework View name
	 */
	@RequestMapping(value = "/tictactoe/reset", method = RequestMethod.GET)
	public String reset(
			HttpSession session,
			Model model) {
		
		log.info("Resetting new game");
		GameState gameState = new GameState();
		putStateInSession(session, gameState);
		model.addAttribute(Constants.GAME_STATE, gameState);
		
		return Constants.VIEW_GAME;
	}
	
	/**
	 * Starts a new game in the current mode.
	 * 
	 * @param session 
	 * @param model Spring framework Model
	 * @return Spring framework View name
	 */
	@RequestMapping(value = "/tictactoe/new", method = RequestMethod.GET)
	public String gameNew(
			HttpSession session,
			Model model) {
		
		log.info("Starting new game");
		GameState gameState = getStateFromSession(session);
		gameState.startNewGame();
		model.addAttribute(Constants.GAME_STATE, gameState);
		
		return Constants.VIEW_GAME;
	}
	/**
	 * Choose whether to play the game against the computer, or a human oponent.
	 * 
	 * @param session 
	 * @param mode String representing the desired mode: "ai" for play against the computer; "twoplayer" for multiplayer mode.
	 * @param model Spring framework Model
	 * @return Spring framework View name
	 */
	@RequestMapping(value = "/tictactoe/modeselection", method = RequestMethod.GET)
	public String modeSelected(
			HttpSession session,
			@RequestParam(value = "mode", required = true) String mode,
			Model model) {
		
		GameState gameState = getStateFromSession(session);
		if(mode.equals("ai")) {
			gameState.setGameMode(GameMode.AI_VS_HUMAN);
		}
		else if(mode.equals("twoplayer")) {
			gameState.setGameMode(GameMode.HUMAN_VS_HUMAN);
		}
		else {
			throw new RuntimeException("Invalid selected game mode:" + mode);
		}
		model.addAttribute(Constants.GAME_STATE, gameState);
		
		return "redirect:/tictactoe/new";
	}
	
	/**
	 * Places a marker for the current player in the requested position.
	 * 
	 * @param session 
	 * @param row Number of row to place marker
	 * @param col Number of column to place marker
	 * @param model Spring framework Model
	 * @return Spring framework View name
	 */
	@RequestMapping(value = "/tictactoe/move", method = RequestMethod.GET)
	public String playerMove(
			HttpSession session,
			@RequestParam(value = "row", required = true) Integer row, 
			@RequestParam(value = "col", required = true) Integer col, 
			Model model) {
		
		GameState gameState = getStateFromSession(session);
		model.addAttribute(Constants.GAME_STATE, gameState);
		log.info("move=(" + row + ", " + col + ")");
		
		// If not in the midst of a game, don't allow move.
		if(!gameState.getGameStage().equals(GameStage.IN_GAME)) {
			log.info("Game not in progress); ignoring move request.");
			return Constants.VIEW_GAME;
		}
		
		Board board = gameState.getBoard();
		try {
			board.move(row, col, gameState.getTurn());
			evaluateBoard(gameState);
			
			// If game has not ended one way or another, and the game is 
			// against the computer, determine where it will move.
			if(gameState.getGameStage().equals(GameStage.IN_GAME) &&
					gameState.getGameMode().equals(GameMode.AI_VS_HUMAN)) {
				determineBestMove(gameState);
				evaluateBoard(gameState);
			}
		}
		catch( Exception e )
		{
			// TODO: Add message to user.  As it is now, move request is
			// ignored, but letting them know would probably be better
			log.error("Cannot complete move", e);
		}
		
		return Constants.VIEW_GAME;
	}
	
	/**
	 * Evaluate the game board to see if a winner can be declared, or if there is a draw.
	 * If neither of these conditions is detected, switch active player.
	 * 
	 * @param gameState
	 */
	public void evaluateBoard(GameState gameState) {
		Board board = gameState.getBoard();
		// First, check for a draw
		if(board.isDraw()) {
			gameState.setGameMessage("It's a draw!");
			gameState.setGameStage(GameStage.POST_GAME);
		}
		else if(board.isWinner(gameState.getTurn())) {
			if(gameState.getTurn().equals(Board.Marker.O)) {
				gameState.setGameMessage("O wins!");
			}
			else {
				gameState.setGameMessage("X wins!");
			}
			gameState.setGameStage(GameStage.POST_GAME);
		}
		else
		{
			if(gameState.getTurn() == Board.Marker.X) {
				gameState.setTurn(Board.Marker.O);
				gameState.setTurnMessage("Turn: O");
			}
			else {
				gameState.setTurn(Board.Marker.X);
				gameState.setTurnMessage("Turn: X");
			}
		}
	}
	
	/**
	 * This method is called during play against the computer, and 
	 * attempts to find the best possible move.
	 * 
	 * @param gameState
	 */
	public void determineBestMove(GameState gameState)
	{
		Board.Marker board[][] = gameState.getBoard().board;
		Board.Marker playerMarker = gameState.getTurn() ;
		Board.Marker opponentMarker = playerMarker.equals(Board.Marker.X) ? Board.Marker.O : Board.Marker.X ;
		
		// First, determine if there is a block that needs to be made.
		// Check the center first, if empty, blocker-wise
		if( board[1][1].equals(Board.Marker.BLANK)) {
			if((board[0][1].equals(opponentMarker) &&
				board[2][1].equals(opponentMarker)) ||
			   (board[1][0].equals(opponentMarker) &&
				board[1][2].equals(opponentMarker)) ||
			   (board[0][0].equals(opponentMarker) &&  
				board[2][2].equals(opponentMarker)) ||
			   (board[0][2].equals(opponentMarker) &&  
				board[2][0].equals(opponentMarker))) {
					
				try {
					gameState.getBoard().move(1, 1, playerMarker );
					return;
				}
				catch(Exception e) {
					// Since we already checked, swallow
				}
			}
		}
		
		// Next, check if there is a block move in the verticals.
		for(int r = 0; r < 3; ++r) {
			int bCount = 0;
			int oCount = 0;
			for(int c = 0; c < 3; ++c) {
				if(board[r][c].equals(opponentMarker)) {
					++oCount;
				}
				if(board[r][c].equals(Board.Marker.BLANK)) {
					++bCount;
				}
			}
			
			// If there were two opponent markers and a blank,
			// move to the blank spot.
			if((oCount == 2) && (bCount == 1)) {
				for(int c = 0; c < 3; ++c) {
					if(board[r][c].equals(Board.Marker.BLANK)) {
						try {
							gameState.getBoard().move(r, c, playerMarker);
							return;
						}
						catch(Exception e) {
							// Since we already checked, swallow
						}
					}
				}
			}
		}
		
		// Next, check rows for blockers.
		for(int c = 0; c < 3; ++c) {
			int bCount = 0;
			int oCount = 0;
			for(int r = 0; r < 3; ++r) {
				if(board[r][c].equals(opponentMarker)) {
					++oCount;
				}
				if(board[r][c].equals(Board.Marker.BLANK)) {
					++bCount;
				}
			}
			
			// If there were two opponent markers and a blank,
			// move to the blank spot.
			if((oCount == 2) && (bCount == 1)) {
				for(int r = 0; r < 3; ++r) {
					if(board[r][c].equals(Board.Marker.BLANK)) {
						try {
							gameState.getBoard().move(r, c, playerMarker);
							return;
						}
						catch(Exception e) {
							// Since we already checked, swallow
						}
					}
				}
			}
		}
		
		// And lastly for blockers, check for diagonals
		int bCount = 0;
		int oCount = 0;
		int r = 0;
		int c = 0;
		for(int i = 0; i < 3; ++i) {
			if(board[r][c].equals(opponentMarker)) {
				++oCount;
			}
			if(board[r][c].equals(Board.Marker.BLANK)) {
				++bCount;
			}
			++r;
			++c;
		}
		if((oCount == 2) && (bCount == 1)) {
			r = 0;
			c = 0;
			for(int i = 0; i < 3; ++i) {
				if(board[r][c].equals(Board.Marker.BLANK)) {
					try {
						gameState.getBoard().move(r, c, playerMarker);
						return;
					}
					catch(Exception e) {
						// Since we already checked, swallow
					}
				}
				++r;
				++c;
			}
		}
		r = 0;
		c = 2;
		bCount = 0;
		oCount = 0;
		for(int i = 0; i < 3; ++i) {
			if(board[r][c].equals(opponentMarker)) {
				++oCount;
			}
			if(board[r][c].equals(Board.Marker.BLANK)) {
				++bCount;
			}
			++r;
			--c;
		}
		if((oCount == 2) && (bCount == 1)) {
			r = 0;
			c = 2;
			for(int i = 0; i < 3; ++i) {
				if(board[r][c].equals(Board.Marker.BLANK)) {
					try {
						gameState.getBoard().move(r, c, playerMarker);
						return;
					}
					catch(Exception e) {
						// Since we already checked, swallow
					}
				}
				++r;
				--c;
			}
		}
		
		// If still available, take the center; always a good move.
		if(board[1][1].equals(Board.Marker.BLANK)) {
			try {
				gameState.getBoard().move(1, 1, playerMarker );
				return;
			}
			catch(Exception e) {
				// Since we already checked, swallow
			}			
		}
		
		// TODO: Add logic that moves in such a way to force
		// human to make a block move.
		
		// Keep generating random positions until a blank spot is found
		boolean found = false;
		Random random = new Random();
		while(!found) {
			r = random.nextInt(3);
			c = random.nextInt(3);
			if(board[r][c].equals(Board.Marker.BLANK)) {
				try {
					gameState.getBoard().move(r, c, playerMarker );
					found = true;
				}
				catch(Exception e) {
					log.error("Problem making random move!", e);
				}			
			}
		}
	}
	
	/**
	 * Convenience method to retrieve game state from session.
	 * 
	 * @param session
	 * @return Current game state.
	 */
	private GameState getStateFromSession(HttpSession session)
	{
		GameState gameState = (GameState)session.getAttribute(Constants.GAME_STATE);
		if(gameState == null) {
			log.info("New GameState created and put in session");
			gameState = new GameState();
			putStateInSession(session, gameState);
		}
		return gameState;
	}
	
	/**
	 * Convenience method to save game state in session.
	 * 
	 * @param session
	 */
	private void putStateInSession(HttpSession session, GameState gameState) {
		session.setAttribute(Constants.GAME_STATE, gameState);
	}
}
