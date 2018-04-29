package com.tarterware.tictactoe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Board {
	enum Marker { BLANK, X, O };
	
	public Marker[][] board = new Marker[3][3];
	
	private static final Logger log = LoggerFactory.getLogger(Board.class);

	/**
	 * Clears the TicTacTow board of all of the markers.
	 */
	public void clear() {
		for(int r = 0;  r < 3;  ++r ) {
			for(int c = 0;  c < 3;  ++c) {
				board[r][c] = Marker.BLANK;
			}
		}
	}
	
	public String markAt(int row, int col)
	{
		Marker marker = board[row][col];
		if(marker.equals(Marker.X)) {
			return "X";
		}
		else if(marker.equals(Marker.O)) {
			return "O";
		}
		else if(marker.equals(Marker.BLANK)) {
			return " ";
		}
		return "#";
	}
	
	/**
	 * Place specified marker on the board at the specified row and column.
	 * @param row Row index to place marker
	 * @param col Column index to place marker
	 * @param marker Marker type
	 * @throws Exception
	 */
	public void move(int row, int col, Marker marker) throws Exception {
		if( board[row][col] != Marker.BLANK) {
			throw new Exception( "Square @ (" + row + ", " + col + ") is not empty");
		}
		if(marker == Marker.BLANK) {
			throw new IllegalArgumentException("Playing a BLANK marker is not valid");
		}
		board[row][col] = marker;
	}
	
	/**
	 * Determine if the requested marker type has won the game.
	 * 
	 * @param marker Marker type to check
	 * @return true if the indicated marker has won the game.
	 */
	public boolean isWinner(Marker marker) {
		// Check for three in a row down
		for(int r = 0; r < 3;  ++r) {
			boolean isWinner = true;
			for(int c = 0; isWinner && (c < 3); ++c) {
				if(board[r][c] != marker) {
					isWinner = false;
				}
			}
			if(isWinner) {
				return true;
			}
		}
		
		// Check for three in a row across
		for(int c = 0; c < 3;  ++c) {
			boolean isWinner = true;
			for(int r = 0; isWinner && (r < 3); ++r) {
				if(board[r][c] != marker) {
					isWinner = false;
				}
			}
			if(isWinner) {
				return true;
			}
		}
		
		// Check the diagonals
		if((board[0][0] == marker) && (board[1][1] == marker) && (board[2][2] == marker)) {
			return true;
		}
		if((board[2][0] == marker) && (board[1][1] == marker) && (board[0][2] == marker)) {
			return true;
		}
		
		return false;
	}
	/**
	 * Determine if the game cannot be won
	 * @return true if the game cannot be won.
	 */
	public boolean isDraw() {
		// If all squares are filled, and a winner not declared, it's a draw pure and simple
		for(int r = 0 ;  r < 3;  ++r) {
			for(int c = 0 ;  c < 3;  ++c) {
				if(board[r][c].equals(Marker.BLANK)) {
					return false;
				}
			}
		}
		return true;
	}
}
