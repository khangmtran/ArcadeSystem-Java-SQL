
/**
 * @author Khang Tran, Yen Lai, Alex Ponce Ayala
 * Main class for the client’s user interface. In order to run the program, the user must include their <Oracle username> 
 * and <Oracle password>. Program is a text-based application that consist of a menu contains different options which
 * the user can use to add, update, and delete a member, or add and delete a game, or add and delete a prize. 
 * 
 * Addtionally, the application also support queries that the application can answer such as: 
 * 1. List all games in the arcade and the names of the members who have the current high scores.
 * 2. Give the names and membership information of all members who have spent at least $100 on tokens in the past month.
 * 3. For a given member, list all arcade rewards that they can purchase with their tickets
 * 4. Retrieve information about a member's most played game. The query calculates the game that the member has played the 
 * most based on the GameHistory records. It then displays the member's name, the most played game, and the number of times 
 * the game was played. 
 */
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

public class Main {

	public static void main(String[] args) {

		// Create instances of the other classes
		RegisterBuyTokenClass registerBuyTokenClass;
		UpdateClass updateClass;

		final String oracleURL = // Magic lectura -> aloe access spell
				"jdbc:oracle:thin:@aloe.cs.arizona.edu:1521:oracle";

		String username = null, // Oracle DBMS username
				password = null; // Oracle DBMS password

		if (args.length == 2) { // get username/password from cmd line args
			username = args[0];
			password = args[1];
		} else {
			System.out.println("\nUsage:  java JDBC <username> <password>\n"
					+ "    where <username> is your Oracle DBMS" + " username,\n    and <password> is your Oracle"
					+ " password (not your system password).\n");
			System.exit(-1);
		}

		try {
			Class.forName("oracle.jdbc.OracleDriver");

		} catch (ClassNotFoundException e) {
			System.err.println("*** ClassNotFoundException:  " + "Error loading Oracle JDBC driver.  \n"
					+ "\tPerhaps the driver is not on the Classpath?");
			System.exit(-1);
		}

		Connection dbconn = null;

		try {
			dbconn = DriverManager.getConnection(oracleURL, username, password);

		} catch (SQLException e) {

			System.err.println("*** SQLException:  " + "Could not open JDBC connection.");
			System.err.println("\tMessage:   " + e.getMessage());
			System.err.println("\tSQLState:  " + e.getSQLState());
			System.err.println("\tErrorCode: " + e.getErrorCode());
			System.exit(-1);

		}

		// REGISTER MEMBER OR EXISTING MEMBER START HERE!
		registerBuyTokenClass = new RegisterBuyTokenClass(dbconn);
		updateClass = new UpdateClass(dbconn);

		Scanner kb = new Scanner(System.in);
		String input = "";
		System.out.println("Welcome to Arcade Game!");
		System.out.println("------------");
		System.out.println("Please choose an option below:");
		System.out.println("If you would like to exit, enter 'e' for EXIT");
		System.out.println("1. Create a membership");
		System.out.println("2. Delete membership");
		System.out.println("3. Update membership");
		System.out.println("4. Add a game");
		System.out.println("5. Add different tickets a player can earn from different scores in a game.");
		System.out.println("6. Delete a score threshold that player can get ticket from");
		System.out.println("7. Delete a game");
		System.out.println("8. Add a prize");
		System.out.println("9. Delete a prize");
		System.out.println("10. Restock prize");
		System.out.println("11. Existing member");
		System.out.println("12. Redeem Coupon");
		System.out.println("13. Play a game");
		System.out.println("14. Redeem prize");

		if (kb.hasNextLine()) {
			input = kb.nextLine().toLowerCase();
		}

		while (!input.equalsIgnoreCase("e")) {

			// ADD NEW MEMBER HERE
			if (input.equals("1")) {
				registerBuyTokenClass.addMember(kb);
			}

			// EXISTING MEMBER HERE
			else if (input.equals("11")) {
				System.out.println("Enter your Member ID: ");
				if (kb.hasNextLine()) {
					String memID = kb.nextLine();
					registerBuyTokenClass.incrementVisitCount(memID, false); // update visit +1
					registerBuyTokenClass.welcomeMemberToBuyTokens(kb, memID);
				} else {
					System.out.println("No Member ID provided");
					break;
				}
			}

			// DELETE MEMBER
			else if (input.equals("2")) {
				System.out.println("Enter your Member ID that you want to delete: ");
				if (kb.hasNextLine()) {
					String memID = kb.nextLine();
					registerBuyTokenClass.deleteMember(memID, kb);
				} else {
					System.out.println("No Member ID provided");
					break;
				}
			}

			// UPDATE MEMBER INFORMATION
			else if (input.equals("3")) {
				System.out.println("Enter your Member ID that you want to update: ");
				if (kb.hasNextLine()) {
					String memID = kb.nextLine();
					registerBuyTokenClass.updateMember(kb, memID);
				} else {
					System.out.println("No Member ID provided");
					break;
				}

			}

			// ADD GAME
			else if (input.equals("4")) { // add game
				updateClass.addGame(kb);
			}

			// SET GAME
			else if (input.equals("5")) { // set game
				System.out.println("Provide ID of the game you want to add info to: ");
				if (kb.hasNextLine()) {
					int id = kb.nextInt();
					updateClass.setMoreGameInfo(id, kb);
				} else {
					System.out.println("No input provided");
					break;
				}
			}

			// DELETE GAME INFO
			else if (input.equals("6")) {
				System.out.println("Provide ID of the game you want to delete info from: ");
				if (kb.hasNextLine()) {
					int id = kb.nextInt();
					kb.nextLine(); // Consume the newline character
					System.out.println("Provide the score threshold of the game you want to delete");
					int score = kb.nextInt();
					// Consume the newline left-over
					kb.nextLine();
					updateClass.deleteGameInfo(id, score, kb);
				} else {
					System.out.println("No input provided");
					break;
				}
			}

			// DELETE GAME
			else if (input.equals("7")) { // delete game
				System.out.println("Provide ID of the game you want to delete: ");
				if (kb.hasNextLine()) {
					int id = kb.nextInt();
					kb.nextLine(); // Consume the newline character
					updateClass.deleteGame(id);
				} else {
					System.out.println("No input provided");
					break;
				}
			}

			// ADD PRIZE
			else if (input.equals("8")) {
				updateClass.addPrize(kb);
			}

			// DELETE PRIZE
			else if (input.equals("9")) {
				System.out.println("Provide ID of the prize you want to delete: ");
				if (kb.hasNextLine()) {
					String memID = kb.nextLine();
					updateClass.deletePrize(Integer.parseInt(memID));
				} else {
					System.out.println("No input provided");
					break;
				}
			}

			// RESTOCK PRIZE
			else if (input.equals("10")) {
				updateClass.restockPrize(kb);

			}

			else if (input.equals("12")) {
				System.out.println("Enter your Member ID: ");
				if (kb.hasNextLine()) {
					String memID = kb.nextLine();
					System.out.println("Enter Coupon ID: ");
					String couponId = kb.nextLine();
					System.out.println("Enter the item you want to redeem: ");
					String itemName = kb.nextLine();
					registerBuyTokenClass.redeemCoupon(Integer.parseInt(couponId), Integer.parseInt(memID), itemName);

				} else {
					System.out.println("No Member ID, Coupon ID provided");
					break;
				}

			}

			else if (input.equals("13")) {
				System.out.println("Provide the ID of the player/memberID: ");
				if (kb.hasNextLine()) {
					int playerID = kb.nextInt();
					System.out.println("Provide the ID of the game you would like to play: ");
					kb.nextLine(); // Consume the newline character
					int gameID = kb.nextInt();
					kb.nextLine(); // Consume the newline character
					System.out.println("Enter today's date in the form: dd/mm/yyyy: ");
					System.out.println("Enter day: ");
					String month = kb.nextLine() + "-";
					String date = "";
					date += month;
					System.out.println("Enter month: ");
					String day = kb.nextLine() + "-";
					date += day;
					System.out.println("Enter year: ");
					String year = kb.nextLine();
					date += year;

					updateClass.playGame(playerID, gameID, date);
				} else {
					System.out.println("No input provided");
					break;
				}

			}

			else if (input.equals("14")) {
				System.out.println("Enter your Member ID: ");
				if (kb.hasNextLine()) {
					int memID = Integer.parseInt(kb.nextLine());
					System.out.println("Enter the ID of the prize you want to redeem: ");
					int prizeID = Integer.parseInt(kb.nextLine());
					updateClass.redeemPrize(prizeID, memID);
				} else {
					System.out.println("No input provided");
					break;
				}
			}

			// Invalid choice handle here
			else {
				System.out.println(
						"Invalid input. Please enter a valid choice (e, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14).");
			}

			// LOOP START AGAIN!
			System.out.println("\n\nPlease choose an option below:");
			System.out.println("If you would like to exit, enter 'e' for EXIT");
			System.out.println("1. Create a membership");
			System.out.println("2. Delete membership");
			System.out.println("3. Update membership");
			System.out.println("4. Add a game");
			System.out.println("5. Add different tickets a player can earn from different scores in a game.");
			System.out.println("6. Delete a score threshold that player can get ticket from");
			System.out.println("7. Delete a game");
			System.out.println("8. Add a prize");
			System.out.println("9. Delete a prize");
			System.out.println("10. Restock prize");
			System.out.println("11. Existing member");
			System.out.println("12. Redeem Coupon");
			System.out.println("13. Play a game");
			System.out.println("14. Redeem prize");

			System.out.println("\nEnter your choice (e, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14):"); // ask user
																											// again
			if (kb.hasNextLine()) {
				input = kb.nextLine().toLowerCase();
			} else {
				System.out.println("No more input provided");
				break;
			}
		} // end while loop

		// Queries that your application is to be able to answer:
		// 1. List all games in the arcade and the names of the members who have the
		// current high scores
		System.out.println("\n");
		listGamesAndHighScores(dbconn);
		System.out.println("\n");
		listMembersWhoSpentAtLeast100(dbconn);
		System.out.println("\n");
		listArcadeRewardsForMember(dbconn, kb);
		System.out.println("\n");
		customQuery(dbconn, kb);

	}

	/**
	 * Method to list all games in the arcade and the names of the members who have
	 * the current high scores.
	 * 
	 * @param dbconn
	 */
	public static void listGamesAndHighScores(Connection dbconn) {
		try (Statement stmt = dbconn.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT " + "G.GameName Game, " + "M.Name MemberName, "
						+ "HS.Score Score " + "FROM ylai1.Game G " + "JOIN ylai1.HighScore HS ON G.GameID = HS.GameID "
						+ "JOIN ylai1.MemberInfo M ON HS.MemberID = M.MemberID " + "WHERE HS.Score = (" + "SELECT MAX(Score) "
						+ "FROM ylai1.HighScore " + "WHERE GameID = G.GameID " + ")")) {
			System.out.println("Game\t\t\tMember Name\t\tScore");
			System.out.println("---------------------------------------------------------------------");
			while (rs.next()) {
				String gameName = rs.getString("Game");
				String memberName = rs.getString("MemberName");
				int memberScore = rs.getInt("Score");
				System.out.printf("%-20s\t%-20s\t%d\n", gameName, memberName, memberScore);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Method to give the names and membership information of all members who have
	 * spent at least $100 on tokens in the past month.
	 * 
	 * @param dbconn
	 */
	public static void listMembersWhoSpentAtLeast100(Connection dbconn) {
	    String sql = "SELECT ylai1.MemberInfo.Name, ylai1.MemberInfo.Address, ylai1.MemberInfo.Telephone " 
	               + "FROM ylai1.MemberInfo "
	               + "JOIN ylai1.TokenTransaction ON ylai1.MemberInfo.MemberID = ylai1.TokenTransaction.MemberID "
	               + "WHERE ylai1.TokenTransaction.TDate >= TRUNC(SYSDATE, 'MM') - INTERVAL '1' MONTH "
	               + "GROUP BY ylai1.MemberInfo.MemberID, ylai1.MemberInfo.Name, ylai1.MemberInfo.Address, ylai1.MemberInfo.Telephone "
	               + "HAVING SUM(ylai1.TokenTransaction.TokensPurchased) >= 100";

		try (PreparedStatement stmt = dbconn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {

			System.out.println("Members who have spent at least $100 on tokens in the past month:");
			System.out.println("-----------------------------------------------------------");
			while (rs.next()) {
				String name = rs.getString("Name");
				String address = rs.getString("Address");
				String telephone = rs.getString("Telephone");

				// Print member information
				System.out.println("Name: " + name);
				System.out.println("Address: " + address);
				System.out.println("Telephone: " + telephone);
				System.out.println("-------------------------------------------");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Method to list all arcade rewards that a member can purchase with their tickets.
	 * @param dbconn
	 * @param kb
	 */
	public static void listArcadeRewardsForMember(Connection dbconn, Scanner kb) {
		try {
			System.out.println("Enter the Member ID to list arcade rewards:");
			int memberID = kb.nextInt();

			String sql = "SELECT ylai1.Prize.PrizeName " 
			           + "FROM ylai1.Prize "
			           + "JOIN ylai1.MemberRecord ON ylai1.Prize.TicketCost <= ylai1.MemberRecord.TotalTickets "
			           + "WHERE ylai1.MemberRecord.MemberID = ?";


			try (PreparedStatement stmt = dbconn.prepareStatement(sql)) {
				stmt.setInt(1, memberID); // Set the member ID parameter in the query

				try (ResultSet rs = stmt.executeQuery()) {
					System.out.println("Arcade rewards that the member can purchase:");
					System.out.println("----------------------------------------");
					while (rs.next()) {
						String prizeName = rs.getString("PrizeName");
						System.out.println(prizeName);
					}
				}
			}
			// Consume the newline left-over
			kb.nextLine();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Executes a custom SQL query to retrieve information about a member's most
	 * played game. The query calculates the game that the member has played the
	 * most based on the GameHistory records. It then displays the member's name,
	 * the most played game, and the number of times the game was played.
	 *
	 * @param dbconn The database connection to use for executing the query.
	 * @param kb     Scanner object for user input (used to enter the Member ID).
	 */
	public static void customQuery(Connection dbconn, Scanner kb) {
		try {
			System.out.println("Would you like to know how many times a member has played their favorite game?");
			System.out.println("Enter the Member ID to find out:");

			int memberID = kb.nextInt();

			// Prepare the SQL query with a placeholder for the member ID
			String sql = "SELECT MemberName, MostPlayedGame, TimesPlayed " + "FROM ( "
					+ "    SELECT m.Name AS MemberName, g.GameName AS MostPlayedGame, COUNT(*) AS TimesPlayed "
					+ "    FROM ylai1.MemberInfo m " + "    JOIN ylai1.GameHistory gh ON m.MemberID = gh.MemberID "
					+ "    JOIN ylai1.Game g ON gh.GameID = g.GameID " + "    WHERE m.MemberID = ? "
					+ "    GROUP BY m.Name, g.GameName " + "    ORDER BY TimesPlayed DESC " + ") "
					+ "WHERE ROWNUM <= 1";

			// Create a prepared statement with the SQL query
			try (PreparedStatement stmt = dbconn.prepareStatement(sql)) {
				// Set the member ID parameter in the prepared statement
				stmt.setInt(1, memberID);

				// Execute the query
				ResultSet rs = stmt.executeQuery();

				// Check if any rows are returned
				if (rs.next()) {
					// Retrieve values from the result set
					String memberName = rs.getString("MemberName");
					String mostPlayedGame = rs.getString("MostPlayedGame");
					int timesPlayed = rs.getInt("TimesPlayed");

					// Display the result
					System.out.println("Member Name: " + memberName);
					System.out.println("Most Played Game: " + mostPlayedGame);
					System.out.println("Times Played: " + timesPlayed);
				} else {
					System.out.println("No records found for the provided Member ID.");
				}
				// Consume the newline left-over
				kb.nextLine();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
