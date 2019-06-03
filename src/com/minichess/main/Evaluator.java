package com.minichess.main;

import java.io.FileNotFoundException;
import java.io.IOException;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.*;

/* The Evaluator Class.
 * 	Evaluates the current position, and decides
 * 	the computer's move.
 */
public class Evaluator {
	static int pInfinity, nInfinity;
	static int posNum, pruneNum;
	static String token;
	public static void eval(String in, int depth) throws MoveGeneratorException {
		System.out.print("\u001b[0m");
		
		Move givenMove;
		MoveList moves = MoveGenerator.generateLegalMoves(MiniChess.board);
		boolean isLegal = false;
		pInfinity = (int) Double.POSITIVE_INFINITY;
		nInfinity = (int) Double.NEGATIVE_INFINITY;
		posNum = 0;
		pruneNum = 0;
		token = "";
		char[] input = in.toCharArray();
		boolean nextSquare = false;
		boolean isCommand = false;
		Square[] squares = new Square[2];
		
		/* Parsing the input */
		for (int i = 0; i < input.length; i++) {
			token+=input[i];
			if(input[i] == '/') {
				isCommand = true;
				token = "";
			}
			if(isCommand) {
				String param = getParam(input, i);
				switch(token) {
				default: break;
				case "printBoard": MiniChess.printBoard(); break;
				case "setDepth":MiniChess.depth = Integer.parseInt(param); break;
				case "exit": System.exit(1); break;
				case "save":
					try {
						MiniChess.saveBoard(param);
					} catch (IOException e) {
						System.out.println("Could not create file.");
					}
					break;
				case "load":
					try {
						MiniChess.loadBoard(param);
					} catch (FileNotFoundException e) {
						System.out.println("Could not find file.");
					}
					break;
				case "version":
					System.out.println("MiniChess AI v2.0 Created 5/17/19\n\n"
							+ "Credits:\n"
							+ " | MiniChess v2.0 - Copyright (C) 2019 Ryan Danver\n"
							+ " | chesslib - Copyright 2017 Ben-Hur Carlos Vieira Langoni Junior");
					break;
				case "cmds":
					System.out.println("List of all commands:\n"
							+ " | /save [name] - Saves the current board state to a file.\n"
							+ " | /load [name] - Loads board state from file.\n"
							+ " | /setDepth [depth] - Sets the search depth for the minimax tree.\n"
							+ " | /printBoard - Prints a representation of the current board.\n"
							+ " | /exit - Exits the program."
							+ " | /cmds - Prints all commands.\n"
							+ " | /version - Prints version and credits.");
					break;
				}
			} else {
				if(Character.isAlphabetic(input[i])) {
					squares[(nextSquare) ? 1 : 0] = Square.valueOf(formatMove(input, i));
					nextSquare = !nextSquare;
				}
			}
		}
		if(isCommand) return;
		givenMove = new Move(squares[0], squares[1]);
		
		/* Determining whether the player's move was legal. If so, update the board. */
		for (Move m : moves)
			if (m.equals(givenMove))
				isLegal = true;
		if (!isLegal) {
			System.out.println("Illegal Move! Please enter again.");
			return;
		} else
			MiniChess.board.doMove(givenMove);
		makeComputerMove(moves,depth);
	}
	
	/* Deciding, and applying the computer's move. */
	public static void makeComputerMove(MoveList moves, int depth) throws MoveGeneratorException {
		moves = MoveGenerator.generateLegalMoves(MiniChess.board);
		Move cMove = rootMiniMax(true,depth);
		MiniChess.board.doMove(cMove);
		System.out.println("\u001b[0m | Positions evaled: " + posNum);
		System.out.println(" | Positions pruned: " + pruneNum);
		System.out.println(" | (\u001b[36m" + cMove + "\u001b[0m)");
		MiniChess.move++;
	}
	
	/* Minimax algorithm with alpha-beta pruning.
	 * 	For more information, go to https://en.wikipedia.org/wiki/Minimax.
	 */
	
	/* For the root node. */
	public static Move rootMiniMax(boolean maximizing, int depth) throws MoveGeneratorException {
		Move bestMove = null;
		MoveList newMoves = MoveGenerator.generateLegalMoves(MiniChess.board);
		int bestValue = (int) Double.NEGATIVE_INFINITY;
		for(int i = 0; i < newMoves.size(); i++) {
			MiniChess.board.doMove(newMoves.get(i));
			int miniMaxValue = miniMax(!maximizing, depth - 1, nInfinity,pInfinity);
			MiniChess.board.undoMove();
			if(miniMaxValue >= bestValue) {
				bestValue = miniMaxValue;
				bestMove = newMoves.get(i);
			}
		}
		return bestMove;
	}
	
	/* For all child nodes. */
	public static int miniMax(boolean maximizing, int depth, int alpha, int beta) throws MoveGeneratorException {
		posNum++;
		if(depth == 0) 
			return -getBoardValue(MiniChess.board);
		MoveList newMoves = MoveGenerator.generateLegalMoves(MiniChess.board);
		if(maximizing) {
			int bestValue = nInfinity;
			for(int i = 0; i < newMoves.size(); i++) {
				MiniChess.board.doMove(newMoves.get(i));
				int value = miniMax(!maximizing, depth - 1, alpha, beta);
				bestValue = Math.max(bestValue, value);
				MiniChess.board.undoMove();
				alpha = Math.max(alpha, value);
				if(beta <= alpha) {
					pruneNum++;
					break;
				}
			}
			return bestValue;
		} else {
			int bestValue = pInfinity;
			for(int i = 0; i < newMoves.size(); i++) {
				MiniChess.board.doMove(newMoves.get(i));
				int value = miniMax(!maximizing, depth - 1, alpha, beta);
				bestValue = Math.min(bestValue, value);
				MiniChess.board.undoMove();
				beta = Math.min(beta, value);
				if(beta <= alpha) {
					pruneNum++;
					break;
				}
			}
			return bestValue;
		}
	}

	/* Get the total board value */
	public static int getBoardValue(Board b) {
		int value = 0;
		for (Piece p : b.boardToArray()) {
			value += getPieceValue(p);
		}
		return value;
	}

	/* Get the value for one piece */
	public static int getPieceValue(Piece p) {
		boolean side = false;
		if(p.getPieceSide() == Side.WHITE) side = true;
		
		if(p.getPieceType() != null) {
			switch (p.getPieceType()) {
			default: return 0;
			case PAWN: if(side) return 100; else return -100;
			case KNIGHT: if(side) return 300; else return -300;
			case BISHOP: if(side) return 350; else return -350;
			case ROOK: if(side) return 500; else return -500;
			case QUEEN: if(side) return 1000; else return -1000;
			case KING: if(side) return pInfinity; else return nInfinity;
			}
		} else return 0;
	}
	
	/* Functions for parser */
	public static String getParam(char[] chars, int index) {
		String command = "";
		for(int i = index + 1; i < chars.length; i++) {
			if(chars[i] == '\n') break;
			else if(chars[i] != ' ')
				command+=chars[i];
		}
		return command;
	}
	
	public static String formatMove(char[] chars, int index) {
		return Character.toString(Character.toUpperCase(chars[index]))
				+ Character.toString(Character.toUpperCase(chars[index + 1]));
	}
}
