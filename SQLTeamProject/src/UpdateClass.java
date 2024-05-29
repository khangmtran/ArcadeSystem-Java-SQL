
/**
 * @author Khang Tran, Yen Lai, Alex Ponce Ayala
 * Class to control the functionalities of Game and Prize. 
 * Include primary methods to add and delete a game, and add and delete a prize. In adding a game to the system, 
 * also add the information of the game to the system. Such as adding a scorethreshold for a particular game that user 
 * can get rewards from hitting that scorethreshold. Additionally, there's a playGame method that let user play a game,
 * after playing a game, add the gameplay to GameHistory and HighScore relations.  In removing a game, all prior scores, 
 * as well as any other information stored about the game should be deleted. 
 * 
 * Prize: The prize counter will constantly be emptied and restocked, as players win tickets. When adding a new prize, 
 * the cost in tickets of the prize must be specified so that it can be properly logged in the database.
 * When a member redeems their tickets for a prize, it should be removed from the system. Before this deletion can occur, 
 * it must be checked that the member has enough tickets for this transaction.
 */
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.InputMismatchException;
import java.util.Random;
import java.util.Scanner;

public class UpdateClass {
	private Connection conn;
	private IDGenerator idGenerator;

	/*
	 * Constructor
	 */
	public UpdateClass(Connection dbconn) {
		this.conn = dbconn;
		this.idGenerator = new IDGenerator(conn);

	}

	/**
	 * Method to add new game to the system. When adding a new game machine, ask
	 * user to set game name, tokens erquired to play the game and max score of the
	 * game. After that, proceed to next method that ask to set the number of
	 * tickets that are won by achieving different scores on the game.
	 * 
	 * @param kb
	 */
	public void addGame(Scanner kb) {
		// Ask user to provide information for game.
		System.out.println("Provide name of the game: ");
		String name = kb.nextLine();
		System.out.println("Provide the tokens that cost for each game play: ");
		int tokenCost = kb.nextInt();
		System.out.println("Provide the max score for the game");
		int maxScore = kb.nextInt();

		// Get next gameID, which's the PK. Every game has a unique GameID.
		int nextGameID = idGenerator.getNextGameID();

		// Add game to the system.
		String insertGame = "INSERT INTO ylai1.Game (GameID, GameName, TokenCost, MaxScore) VALUES (?, ?, ?, ?)";
		try {
			PreparedStatement statement = conn.prepareStatement(insertGame);
			statement.setInt(1, nextGameID);
			statement.setString(2, name);
			statement.setInt(3, tokenCost);
			statement.setInt(4, maxScore);

			int rowsInserted = statement.executeUpdate();
			if (rowsInserted > 0)
				System.out.println("New game registered successfully with ID: " + nextGameID);

		} catch (SQLException e) {
			e.printStackTrace();
		}

		// After adding the game to the system, proceed to next method that ask to set
		// the number of tickets that are won by achieving different scores on the game.
		System.out.println("Add tickets earned from hitting a score? (Answer 'y' for yes)");
		// Consume the newline left-over
		kb.nextLine();

		String ans = kb.nextLine();
		if (!ans.equalsIgnoreCase("y")) {
			System.out.println("Exiting program. You have to add tickets and scores to determine rewards. "
					+ "How can we know what score earns how many tickets if you don't tell us?");
			System.exit(0);
		} else {
			setGameInfoForRecentlyAddedGame(nextGameID, kb);
		}
	}

	/**
	 * Helper Method that ask the user to set the number of tickets that are won by
	 * achieving different scores on the game. Each game can have different
	 * scorethresholds the user can hit to earn different tickets.
	 * 
	 * @param gameID
	 * @param kb
	 */
	private void setGameInfoForRecentlyAddedGame(int gameID, Scanner kb) {
		// Get the scorethreshold and tickets can get from above that score.
		System.out.println("Provide the score the player should hit to earn tickets from the game: ");
		int score = kb.nextInt();
		System.out.println("Provide how many tickets the player can earn from that: ");
		int ticket = kb.nextInt();
		kb.nextLine(); // consume newline left-over

		// Insert to the gameinfo relation.
		String query = "INSERT INTO ylai1.GameInfo (GameID, ScoreThreshold, TicketsEarned) VALUES (?, ?, ?)";
		try {
			PreparedStatement statement = conn.prepareStatement(query);
			statement.setInt(1, gameID);
			statement.setInt(2, score);
			statement.setInt(3, ticket);

			int rowsInserted = statement.executeUpdate();
			if (rowsInserted > 0)
				System.out.println("New info for game registered successfully with ID: " + gameID);
		} catch (SQLException e) {
			e.printStackTrace();
		} // end try-catch

	}

	/**
	 * Method to add more scorethresholds and tickets can get from above that score.
	 * Method is called from the menu in main.
	 * 
	 * @param gameID
	 * @param kb
	 */
	public void setMoreGameInfo(int gameID, Scanner kb) {
		// Get the scorethreshold and tickets can get from above that score.
		System.out.println("Provide the score the player should hit to earn tickets from the game: ");
		int score = kb.nextInt();

		// Handle existing score. Whenever the user provide the same scorethreshold that
		// is already existed, ask them to either provide another rewards can get from
		// that scorethreshold or keep it the same.
		String getExistThreshold = "SELECT ScoreThreshold FROM ylai1.GameInfo WHERE ScoreThreshold = " + score
				+ " AND GameID = " + gameID;
		try {
			Statement stm = conn.createStatement();
			ResultSet result = stm.executeQuery(getExistThreshold);
			if (result.next()) {
				System.out.println("The given score is already existed for the given GameID."
						+ " Do you want to replace this score with a different type of reward?");
				System.out.println("Enter 'y' to replace");
				kb.nextLine(); // consume newline left-over
				String replace = kb.nextLine();

				// If the user accept to change the reward, ask to provide new rewards.
				if (replace.equalsIgnoreCase("y")) {
					System.out.println("Provide how many tickets the player can earn from that: ");
					int ticket = kb.nextInt();
					kb.nextLine(); // consume newline left-over

					String query = "UPDATE ylai1.GameInfo SET TicketsEarned = ? WHERE GameID = ? AND ScoreThreshold = ?";
					try {
						PreparedStatement statement = conn.prepareStatement(query);
						statement.setInt(1, ticket);
						statement.setInt(2, gameID);
						statement.setInt(3, score);

						int rowsUpdated = statement.executeUpdate();
						if (rowsUpdated > 0)
							System.out.println("New info for game updated successfully with ID: " + gameID);
					} catch (SQLException e) {
						e.printStackTrace();
					} // end try-catch
						// If the user doesn't want to update new reward, simply print nothing updated.
				} else {
					System.out.println("Nothing was updated.");
				}
			}
			// If is a new scorethreshold, proceed to ask for the reward from that score.
			else {
				System.out.println("Provide how many tickets the player can earn from that: ");
				int ticket = kb.nextInt();
				kb.nextLine(); // consume newline left-over

				// Insert information to gameinfo relation.
				String query = "INSERT INTO ylai1.GameInfo (GameID, ScoreThreshold, TicketsEarned) VALUES (?, ?, ?)";
				try {
					PreparedStatement statement = conn.prepareStatement(query);
					statement.setInt(1, gameID);
					statement.setInt(2, score);
					statement.setInt(3, ticket);

					int rowsInserted = statement.executeUpdate();
					if (rowsInserted > 0)
						System.out.println("New info for game registered successfully with ID: " + gameID);
					else
						System.out.println("Something went wrong while inserting game info");
				} catch (SQLException e) {
					e.printStackTrace();
				} // end try-catch
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} // end try-catch

	}

	/**
	 * Method to delete an existing scorethreshold and reward from the gameinfo
	 * relation. Pre-asked the user from the main menu to provide which game and
	 * which scorethreshold they want to delete. Then this method proceed to delete.
	 * 
	 * @param gameID
	 * @param score
	 * @param kb
	 */
	public void deleteGameInfo(int gameID, int score, Scanner kb) {
		// Delete query.
		String deleteGameInfoQuery = "DELETE FROM ylai1.GameInfo WHERE GameID = " + gameID + " AND ScoreThreshold = "
				+ score;
		try {
			Statement stmt = conn.createStatement();
			int rowCount = stmt.executeUpdate(deleteGameInfoQuery);
			if (rowCount > 0) {
				System.out.println("Deletion successful.");
			} else {
				System.out.println("No records were deleted.");
				System.out.println("Maybe the Game ID and the Score Threshold were wrong?");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} // end try-catch
	}

	/**
	 * Method to delete an existing game. In removing a game, all prior scores, as
	 * well as any other information stored about the game are deleted. Which
	 * include deleting from highscore, gamehistory, gameinfo, and game relations.
	 * 
	 * @param gameID
	 */
	public void deleteGame(int gameID) {
		// ( do this step by step, do not change the order!!!)
		// 1. FIRST, DELETE THE CORRESPONDING ROWS FROM highscore
		String deleteHighScoreQuery = "DELETE FROM ylai1.HighScore WHERE GameID = ?";
		try {
			PreparedStatement stmt = conn.prepareStatement(deleteHighScoreQuery);
			stmt.setInt(1, gameID); // set the parameter
			int rowsDeleted = stmt.executeUpdate();
			if (rowsDeleted > 0)
				System.out.println("Successfully deleted the game high score for gameID: " + gameID);
			// If there is no highscore, which means nobody played this game before, print
			// nothing was deleted.
			else
				System.out.println("Nothing was deleted from HighScore, "
						+ "no one played the game before or the game doesn't exist.");
		} catch (SQLException e) {
			e.printStackTrace();
		}

		// 2. DELETE THE CORRESPONDING ROWS FROM gamehistory
		String deleteGameHistoryQuery = "DELETE FROM ylai1.GameHistory WHERE GameID = ?";
		try {
			PreparedStatement stmt = conn.prepareStatement(deleteGameHistoryQuery);
			stmt.setInt(1, gameID); // set the parameter
			int rowsDeleted = stmt.executeUpdate();
			if (rowsDeleted > 0)
				System.out.println("Successfully deleted the game history for gameID: " + gameID);
			// If there is nothing deleted from gamehistory, which means nobody played this
			// game before, print nothing was deleted.
			else
				System.out.println("Nothing was deleted from GameHistory, "
						+ "no one played the game before or the game doesn't exist.");
		} catch (SQLException e) {
			e.printStackTrace();
		}

		// 3. DELETE THE CORRESPONDING ROWS FROM gameinfo
		String deleteGameInfoQuery = "DELETE FROM ylai1.GameInfo WHERE GameID = ?";
		try {
			PreparedStatement stmt = conn.prepareStatement(deleteGameInfoQuery);
			stmt.setInt(1, gameID); // set the parameter
			int rowsDeleted = stmt.executeUpdate();
			if (rowsDeleted > 0)
				System.out.println("Successfully deleted the game info for gameID: " + gameID);
			// If there is nothing deleted from gameinfo, which means no scorethreshold was
			// set for the game before, print nothing was deleted.
			else
				System.out.println("Nothing was deleted from GameInfo, "
						+ "no ScoreThreshold was set for the game or the game doesn't exist.");
		} catch (SQLException e) {
			e.printStackTrace();
		}

		// 4. THEN, DELETE THE GAME FROM GAME!
		String deleteGameQuery = "DELETE FROM ylai1.Game WHERE GameID = ?";

		try {
			PreparedStatement stmt = conn.prepareStatement(deleteGameQuery);
			stmt.setInt(1, gameID); // set the parameter
			int rowsDeleted = stmt.executeUpdate();
			if (rowsDeleted > 0)
				System.out.println("Successfully deleted the game with gameID: " + gameID);
			// If the game doesn't exist, print unsuccessful.
			else
				System.out.println("Unsuccessful to delete the game. "
						+ "Maybe the given id is wrong, or the game doesn't exist.");

		} catch (SQLException e) {
			e.printStackTrace();
		} // end try catch
	}

	/**
	 * Method to play game. Pre-asked the user to input their memberID, and gameID
	 * from which game they want to play. Then check if the information they
	 * provided were valid. If the information was valid, generate a score the
	 * player hit and insert it into gamehistory relation, which holds every
	 * transaction of game play. Also, if they hit a highscore, which is their
	 * highest score they have reached, update it to highscorer relation. If the
	 * information was not valid, tell them which information was wrong and return.
	 * 
	 * @param playerID
	 * @param gameID
	 * @param date
	 */
	public void playGame(int playerID, int gameID, String date) {
		// check if the game exist
		int maxScore = checkGameExistAndGetMaxScore(gameID);
		if (maxScore == 0) {
			return;
		}

		// check if the player registered membership
		if (!memberExist(playerID))
			return;

		// Generate a score the player hit and get ticketsEarned from that score.
		Random rand = new Random();
		int playerScore = rand.nextInt(maxScore + 1);
		String getTicketsEarned = "SELECT TicketsEarned FROM ylai1.GameInfo WHERE GameID = " + gameID
				+ " AND ScoreThreshold <= " + playerScore + " ORDER BY ScoreThreshold DESC";
		int ticketsEarned = 0;

		try {
			Statement stmt = conn.createStatement();
			ResultSet result = stmt.executeQuery(getTicketsEarned);

			// if there's a threshold for the score the person got, get the tickets earned.
			// Else, ticketsEarned will be 0
			if (result.next())
				ticketsEarned = result.getInt(1);

			// Find TokenCost from the Game table to calculate
			int tokenCost = 0;
			String getTokenCostQuery = "SELECT TokenCost FROM ylai1.Game WHERE GameID = ?";
			try (PreparedStatement costStmt = conn.prepareStatement(getTokenCostQuery)) {
				costStmt.setInt(1, gameID);
				ResultSet costResult = costStmt.executeQuery();
				if (costResult.next()) {
					tokenCost = costResult.getInt("TokenCost");
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}

			// Update tokens after the game
			updateTokensAfterGame(playerID, tokenCost);

			// After having had all the information, insert to game history table.
			int nextPlayID = idGenerator.getNextPlayID();
			String insertGameHistory = "INSERT INTO ylai1.GameHistory "
					+ "(PlayID, MemberID, GameID, Score, TicketsEarned, GDate) VALUES (?, ?, ?, ?, ?, TO_DATE(?, 'DD-MM-YYYY'))";

			PreparedStatement pstmt = conn.prepareStatement(insertGameHistory);
			pstmt.setInt(1, nextPlayID);
			pstmt.setInt(2, playerID);
			pstmt.setInt(3, gameID);
			pstmt.setInt(4, playerScore);
			pstmt.setInt(5, ticketsEarned);
			pstmt.setString(6, date);
			int rowsInserted = pstmt.executeUpdate();
			if (rowsInserted > 0) {
				System.out.println("Successfully inserted a game play into gameHistory with playID: " + nextPlayID);
			} else {
				System.out.println("Failed to insert a game play into gameHistory with playID: " + nextPlayID);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		} // catch for select a maxScore from a game

		// Check if the score is the highest score that the player got from the game
		checkHighestScore(playerScore, playerID, gameID, date);
	}

	// UPDATE TOKEN WHEN A MEMBER PLAYS A GAME
	/**
	 * This method is used to update the total tokens for a member after they play a
	 * game. It constructs a SQL UPDATE statement and executes it using the provided
	 * member ID and tokens used. The TotalTokens in the MemberRecord is decreased
	 * by the number of tokens used. If the update is successful, it prints a
	 * success message.
	 *
	 * @param memberID   The ID of the member.
	 * @param tokensUsed The number of tokens used by the member.
	 * @throws SQLException If an SQL error occurs.
	 */
	public void updateTokensAfterGame(int memberID, int tokensUsed) {
		String updateTokens = "UPDATE ylai1.MemberRecord SET TotalTokens = TotalTokens - ? WHERE MemberID = ?";

		try (PreparedStatement statement = conn.prepareStatement(updateTokens)) {
			statement.setInt(1, tokensUsed);
			statement.setInt(2, memberID);

			int rowsUpdated = statement.executeUpdate();
			if (rowsUpdated > 0)
				System.out.println("\t- TotalTokens updated successfully for MemberID: " + memberID);

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Method to check if they player hit a highscore from the game they played. If
	 * they did, update from the existing less score they had. If they have never
	 * played the game before, insert the most recent score they hit to highscore.
	 * 
	 * @param recentScore
	 * @param memID
	 * @param gameID
	 * @param date
	 */
	public void checkHighestScore(int recentScore, int memID, int gameID, String date) {
		String getHighScoreQuery = "SELECT Score from ylai1.HighScore WHERE MemberID = " + memID + " AND gameID = "
				+ gameID;
		try {
			Statement stmt = conn.createStatement();
			ResultSet result = stmt.executeQuery(getHighScoreQuery);

			// if the player has had a highscore before, compare it with the recent score
			// they got.
			if (result.next()) {
				int currentScore = result.getInt(1);
				// if recentScore is greater than their current score, update.
				if (recentScore > currentScore) {
					String updateScore = "UPDATE ylai1.HighScore SET Score = ?, HDate = TO_DATE(?, 'DD-MM-YYYY') WHERE GameID = ? AND MemberID = ?";
					PreparedStatement pstmt = conn.prepareStatement(updateScore);
					pstmt.setInt(1, recentScore);
					pstmt.setString(2, date);
					pstmt.setInt(3, gameID);
					pstmt.setInt(4, memID);
					int rowsUpdated = pstmt.executeUpdate();
					if (rowsUpdated > 0)
						System.out.println("The player just hit a highscore. "
								+ "New highscore updated for the player with memberID: " + memID);
				}
				// If recentScore is less than or equal to the current score, no update needed.

				// if the player never played the game before:
			} else {
				System.out.println("The member hasn't played this game before, "
						+ "set this score to be their highscore for the game with gameID: " + gameID);
				String insertScore = "INSERT INTO ylai1.HighScore (MemberID, GameID, Score, HDate) VALUES (?, ?, ?, TO_DATE(?, 'DD-MM-YYYY'))";
				PreparedStatement insert = conn.prepareStatement(insertScore);
				insert.setInt(1, memID);
				insert.setInt(2, gameID);
				insert.setInt(3, recentScore);
				insert.setString(4, date);
				insert.executeUpdate();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Method to check if the member has a membership.
	 * 
	 * @param memID
	 * @return
	 */
	public boolean memberExist(int memID) {
		String getPlayer = "SELECT MemberID FROM ylai1.MemberInfo WHERE MemberID = " + memID;
		boolean check = false;
		try {
			Statement stmt = conn.createStatement();
			ResultSet result = stmt.executeQuery(getPlayer);
			if (result.next()) {
				check = true;
			}
			// If the member doesn't have a membership
			else {
				System.out.println("The member doesn't exist, try give the correct memberID or create membership.");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return check;
	}

	/**
	 * Method to check if the game exist. If the game existed, get the max score
	 * from the game to generate a score the player can hit.
	 * 
	 * @param gameID
	 * @return
	 */
	public int checkGameExistAndGetMaxScore(int gameID) {
		String getMaxScore = "SELECT MaxScore FROM ylai1.Game WHERE GameID = " + gameID;
		int maxScore = 0;
		try {
			Statement stmt = conn.createStatement();
			ResultSet result = stmt.executeQuery(getMaxScore);
			if (result.next()) {
				maxScore = result.getInt(1);
			}
			// If the game doesn't exist:
			else {
				System.out.println("The game id given was incorrect, or maybe the game doesn't exist?");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return maxScore;
	}

	// -----------------------------------------------------------------------------------------------
	// PRIZE FUNCTIONS START HERE
	/**
	 * Method to add a prize to the database. When adding a prize ask the user for
	 * the name, ticekt cost and initial stock quantity of the prize.
	 * 
	 * @param kb
	 */
	public void addPrize(Scanner kb) {
		// Implement the method to add a prize
		System.out.println("Provide name of the prize: ");
		String name = kb.nextLine();
		System.out.println("Provide the ticket cost of the prize: ");
		int ticketCost = kb.nextInt();
		System.out.println("Provide the quantity of the prize in stock: ");
		int quantity = kb.nextInt();

		int nextPrizeID = idGenerator.getNextPrizeID(); // Use idGenerator to get the next PrizeID

		String insertPrize = "INSERT INTO ylai1.Prize (PrizeID, PrizeName, TicketCost, Quantity) VALUES (?, ?, ?, ?)";
		try {
			PreparedStatement statement = conn.prepareStatement(insertPrize);
			statement.setInt(1, nextPrizeID);
			statement.setString(2, name);
			statement.setInt(3, ticketCost);
			statement.setInt(4, quantity);

			int rowsInserted = statement.executeUpdate();
			if (rowsInserted > 0)
				System.out.println("New prize registered successfully with ID: " + nextPrizeID);
			// Consume the newline left-over
			kb.nextLine();
		} catch (SQLException e) {
			System.out.println("ERROR: adding prize was unsuccessful");
			e.printStackTrace();
		} // end try catch
	}

	/**
	 * Method to restock a prize. When restocking a prize this asks users the give
	 * the prizeID and the the new total quantity of that prize in stock. This will
	 * update that tuple in the DB.
	 * 
	 * @param kb
	 */
	public void restockPrize(Scanner kb) {
		// Implement the method to restock a prize
		try {
			System.out.println("Provide the ID of the prize to restock: ");
			int ID = kb.nextInt();
			System.out.println("Provide the new total quantity of the prize in stock: ");
			int quantity = kb.nextInt();

			String updatePrize = "UPDATE ylai1.Prize SET Quantity = ? WHERE PrizeID = ?";
			PreparedStatement statement = conn.prepareStatement(updatePrize);
			statement.setInt(1, quantity);
			statement.setInt(2, ID);
			int rowsUpdated = statement.executeUpdate();

			if (rowsUpdated > 0) {
				System.out.println("Successfully restocked the prize with ID " + ID);
			} else {
				System.out.println("No prize found with the provided ID or the update operation failed.");
			}

			// Consume the newline left-over
			kb.nextLine();
		} catch (InputMismatchException e) {
			System.out.println("Invalid input format. Please enter numeric values for ID and quantity.");
			kb.nextLine(); // Consume the invalid input
		} catch (SQLException e) {
			System.out.println("ERROR: Updating prize quantity was unsuccessful");
			e.printStackTrace();
		}
	}

	/**
	 * Method to redeem a prize. Pre-asked the user for the PrizeID and the MemberID
	 * from the main menu. When a prize is redeem its quantity is reduced by one, if
	 * there are any available, and the transaction is recorded.
	 * 
	 * @param PrizeID
	 * @param MemID
	 */
	public void redeemPrize(int PrizeID, int MemID) {
		String quantityQuery = "SELECT Quantity FROM ylai1.Prize WHERE PrizeID = ?";
		String updateQuery = "UPDATE ylai1.Prize SET Quantity = ? WHERE PrizeID = ?";
		String insertTrans = "INSERT INTO ylai1.PrizeTransaction (PTransID, MemberID, PrizeID, PDate) VALUES (?, ?, ?, CURRENT_DATE)";
		String updateMemberTickets = "UPDATE ylai1.MemberRecord SET TotalTickets = TotalTickets - ? WHERE MemberID = ?";
		String checkMemberTickets = "SELECT TotalTickets FROM ylai1.MemberRecord WHERE MemberID = ?";
		String getTicketCostQuery = "SELECT TicketCost FROM ylai1.Prize WHERE PrizeID = ?";

		try {
			// Get current quantity of the prize
			PreparedStatement stmt = conn.prepareStatement(quantityQuery);
			stmt.setInt(1, PrizeID);
			ResultSet rSet = stmt.executeQuery();

			if (rSet.next()) {
				int quantity = rSet.getInt("Quantity");
				if (quantity > 0) {
					// Check if the member has enough tickets
					PreparedStatement checkStmt = conn.prepareStatement(checkMemberTickets);
					checkStmt.setInt(1, MemID);
					ResultSet checkResult = checkStmt.executeQuery();

					if (checkResult.next()) {
						int totalTickets = checkResult.getInt("TotalTickets");

						// Get the TicketCost for the prize
						PreparedStatement ticketCostStmt = conn.prepareStatement(getTicketCostQuery);
						ticketCostStmt.setInt(1, PrizeID);
						ResultSet ticketCostResult = ticketCostStmt.executeQuery();

						if (ticketCostResult.next()) {
							int ticketCost = ticketCostResult.getInt("TicketCost");

							if (totalTickets >= ticketCost) {
								// Reduce quantity by 1
								quantity--;
								// Update quantity in the Prize table
								PreparedStatement updatestmt = conn.prepareStatement(updateQuery);
								updatestmt.setInt(1, quantity);
								updatestmt.setInt(2, PrizeID);
								int rowsUpdated = updatestmt.executeUpdate();

								if (rowsUpdated > 0) {
									// Insert transaction into PrizeTransaction table
									PreparedStatement insertstmt = conn.prepareStatement(insertTrans);
									insertstmt.setInt(1, idGenerator.getNextPTransID());
									insertstmt.setInt(2, MemID);
									insertstmt.setInt(3, PrizeID);
									int rowsInserted = insertstmt.executeUpdate();

									if (rowsInserted > 0) {
										// Update member's total tickets
										PreparedStatement updateMemberStmt = conn.prepareStatement(updateMemberTickets);
										updateMemberStmt.setInt(1, ticketCost); // Deduct the TicketCost
										updateMemberStmt.setInt(2, MemID);
										updateMemberStmt.executeUpdate();

										System.out.println("Prize redeemed successfully.");
									} else {
										System.out.println("Failed to insert transaction record.");
									}
								} else {
									System.out.println("Failed to update prize quantity.");
								}
							} else {
								System.out.println("Insufficient tickets to redeem this prize.");
							}
						}
					}

				} else {
					System.out.println("Insufficient quantity of this prize.");
				}
			} else {
				System.out.println("Prize not found.");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Method to delete a prize. Pre-asked the PrizeID from the main menu. When
	 * deleting a prize all transaction records for that prize are deleted in the
	 * prize transaction table and then the prize itself is deletd from the prize
	 * table.
	 * 
	 * @param PrizeID
	 */
	public void deletePrize(int prizeID) {
		// Implement the method to delete a prize along with associated transactions
		String deletePTransQuery = "DELETE FROM ylai1.PrizeTransaction WHERE PrizeID = ?";
		String deletePrizeQuery = "DELETE FROM ylai1.Prize WHERE PrizeID = ?";

		try {
			// Delete associated PrizeTransaction records first
			PreparedStatement deletePTransStmt = conn.prepareStatement(deletePTransQuery);
			deletePTransStmt.setInt(1, prizeID);
			int rowsDeletedPT = deletePTransStmt.executeUpdate();

			// Then delete the Prize record
			PreparedStatement deletePrizeStmt = conn.prepareStatement(deletePrizeQuery);
			deletePrizeStmt.setInt(1, prizeID);
			int rowsDeletedP = deletePrizeStmt.executeUpdate();

			if (rowsDeletedPT > 0) {
				System.out.println("Associated PrizeTransaction records deleted successfully for Prize ID: " + prizeID);
			} else {
				System.out.println("No associated PrizeTransaction records found for Prize ID: " + prizeID);
			}

			if (rowsDeletedP > 0) {
				System.out.println("Successfully deleted the prize with ID: " + prizeID);
			} else {
				System.out.println("Unsuccessful. Maybe the given ID is wrong, or the prize doesn't exist?");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
