
/**
 * @author Khang Tran, Yen Lai, Alex Ponce Ayala
 */
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * This class handles the registration of members and token purchases for the
 * Arcade Game. It provides functionalities for registering new members, buying
 * tokens, redeeming coupons, giving coupons, and updating member details.
 */
public class RegisterBuyTokenClass {
	private Connection conn;
	private IDGenerator idGenerator;

	public RegisterBuyTokenClass(Connection dbconn) {
		this.conn = dbconn;
		this.idGenerator = new IDGenerator(conn);
	}

	// INSERT MemberID and TierID
	/**
	 * This method is used to add a new member to the MemberInfo table. It prompts
	 * the user to enter their name, telephone number, and address. It then
	 * generates the next available MemberID and TierID. The new member is inserted
	 * into the MemberInfo table with the next available ID. If the member is
	 * registered successfully, initial values are inserted into the MemTier for the
	 * new TierID, a new record is created in the MemberRecord for the new MemberID,
	 * and the member is welcomed to buy tokens.
	 *
	 * @param scanner The Scanner object used for user input.
	 * @throws SQLException If an SQL error occurs.
	 */
	public void addMember(Scanner scanner) {
		// method to add a member
		try {
			System.out.println("Enter your name:");
			String name = scanner.nextLine();
			System.out.println("Enter telephone:");
			String telephone = scanner.nextLine();
			System.out.println("Enter address:");
			String address = scanner.nextLine();

			// Get the next available MemberID and TierID
			int nextMemberID = idGenerator.getNextMemberID();
			int nextTierID = idGenerator.getNextTierID();

			// Insert new member into the MemberInfo table with the next available ID
			String insertQuery = "INSERT INTO ylai1.MemberInfo (MemberID, TierID, Name, Telephone, Address) VALUES (?, ?, ?, ?, ?)";
			PreparedStatement statement = conn.prepareStatement(insertQuery);
			statement.setInt(1, nextMemberID);
			statement.setInt(2, nextTierID);
			statement.setString(3, name);
			statement.setString(4, telephone);
			statement.setString(5, address);

			int rowsInserted = statement.executeUpdate();
			if (rowsInserted > 0) {
				System.out.println("\t- New member registered successfully with ID: " + nextMemberID);
				System.out.println("\t- Please don't lose your ID, in case of stolen or lost come to register.");

				// Automatically insert initial values into MemTier for the new TierID
				String insertTierQuery = "INSERT INTO ylai1.MemTier (TierID, Discount, FreeTickets, LevelStatus) VALUES (?, ?, ?, ?)";
				try (PreparedStatement tierStatement = conn.prepareStatement(insertTierQuery)) {
					tierStatement.setInt(1, nextTierID);
					tierStatement.setInt(2, 0); // Initial discount value
					tierStatement.setInt(3, 0); // Initial free tickets value
					tierStatement.setString(4, "Regular"); // Initial level status
					tierStatement.executeUpdate();
					System.out.println("\t- Initial values inserted into MemTier for TierID: " + nextTierID);
					// Automatically create a new record in MemberRecord for the new MemberID
					createMemberRecord(nextMemberID);
					welcomeMemberToBuyTokens(scanner, String.valueOf(nextMemberID));

				} catch (SQLException tierException) {
					tierException.printStackTrace();
				}
			} else {
				System.out.println("***Failed to register new member.***");
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method is used to create a new record for a new member in the
	 * MemberRecord table. After a new member has been added to the MemberInfo
	 * table, we insert the member id to MemberRecord table. The new record is
	 * initialized with 0 total spent, 0 total tickets, 0 total tokens, and 1 visit.
	 *
	 * @param memberID The ID of the member for whom the record is to be created.
	 */
	private void createMemberRecord(int memberID) {
		String insertQuery = "INSERT INTO ylai1.MemberRecord (MemberID, TotalSpent, TotalTickets, TotalTokens, NumberOfVisits) VALUES (?, 0, 0, 0, 1)";
		try (PreparedStatement statement = conn.prepareStatement(insertQuery)) {
			statement.setInt(1, memberID);
			int rowsInserted = statement.executeUpdate();
			if (rowsInserted > 0) {
				System.out.println("\t- MemberRecord created for MemberID: " + memberID);
			} else {
				System.out.println("Failed to create MemberRecord for MemberID: " + memberID);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method is used to increment the visit count of a member in the
	 * MemberRecord table. If the member is new, the NumberOfVisits field is set to
	 * its current value. Otherwise, the NumberOfVisits field is incremented by 1.
	 *
	 * @param memID       The ID of the member.
	 * @param isNewMember A boolean indicating whether the member is new.
	 * @throws SQLException If an SQL error occurs.
	 */
	public void incrementVisitCount(String memID, boolean isNewMember) {
		String updateQuery = isNewMember
				? "UPDATE ylai1.MemberRecord SET NumberOfVisits = NumberOfVisits WHERE MemberID = ?"
				: "UPDATE ylai1.MemberRecord SET NumberOfVisits = NumberOfVisits + 1 WHERE MemberID = ?";
		try (PreparedStatement statement = conn.prepareStatement(updateQuery)) {
			int memberId = Integer.parseInt(memID);
			statement.setInt(1, memberId);
			int rowsInserted = statement.executeUpdate();
			if (rowsInserted > 0) {
				System.out.println("\t- Member Record - Number of visits updated for MemberID: " + memberId);
			} else {
				System.out.println("Failed to update MemberRecord for MemberID: " + memberId);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method is used to delete a member from various tables. Before a member
	 * can delete their account, they are asked to redeem any remaining prizes and
	 * coupons. If the member has more than 10 tickets or any coupons, they are
	 * prompted to redeem a prize or coupons. After all redemptions, the member is
	 * deleted from various tables including MemTier, MemberRecord,
	 * TokenTransaction, PrizeTransaction, Coupon, CouponTransaction, HighScore,
	 * GameHistory, and MemberInfo.
	 *
	 * @param memID The ID of the member.
	 * @param kb    The Scanner object used for user input.
	 * @throws SQLException If an SQL error occurs.
	 */
	public void deleteMember(String memID, Scanner kb) {

		// BEFORE MEMBERS CAN DELETE THEIR ACCOUNT, ASK THEM REDEEM PRIZE AND COUPONS
		try {
			if (idExists(1, memID)) {
				int[] getTickets = getTotalSpentAndTokens(Integer.parseInt(memID));
				// Get the TotalTickets of the member from MemberRecord
				int totalTickets = getTickets[2];

				// Get the number of coupons of the member from Coupon
				int totalCoupons = getTotalCoupons(memID);

				// If the member has more than 10 tickets or any coupons, prompt them to redeem
				// a prize or coupons
				if (totalTickets > 10 || totalCoupons > 0) {
					System.out.println("\t- You have " + totalTickets + " tickets and " + totalCoupons
							+ " coupons. Would you like to redeem a prize or coupons before deleting your account? (yes/no)");
					String response = kb.nextLine();
					if (response.equalsIgnoreCase("yes")) {
						// Show all the prizes that they can redeem
						showPrizes(memID, totalTickets, kb);

						// redeem prize, don't need to update PrizeXact or update MemberRecord anymore

						// Handle coupon redemption
						while (totalCoupons > 0) {
							System.out.println("\nEnter Coupon ID to redeem:");
							String couponID = kb.nextLine();
							System.out.println("Enter the item you want to redeem:");
							String itemName = kb.nextLine();
							// Call redeemCoupon for each coupon the member has
							redeemCoupon(Integer.parseInt(couponID), Integer.parseInt(memID), itemName);
							totalCoupons--;
						}
					}
				}

				// Get the TierID of the member from MemberInfo
				int tierID = idGenerator.getTierID(conn, memID);

				// Delete from MemTier using the TierID
				deleteFromTable(conn, "ylai1.MemTier", "TierID", Integer.toString(tierID));

				// Delete memberID from FK
				deleteFromTable(conn, "ylai1.MemberRecord", "MemberID", memID);

				deleteFromTable(conn, "ylai1.TokenTransaction", "MemberID", memID);

				deleteFromTable(conn, "ylai1.PrizeTransaction", "MemberID", memID);

				deleteFromTable(conn, "ylai1.Coupon", "MemberID", memID);

				deleteFromTable(conn, "ylai1.CouponTransaction", "MemberID", memID);

				deleteFromTable(conn, "ylai1.HighScore", "MemberID", memID);

				deleteFromTable(conn, "ylai1.GameHistory", "MemberID", memID);

				deleteFromTable(conn, "ylai1.MemberInfo", "MemberID", memID);

				// Delete from MemberInfo (MAIN PK so it should be delete last)
				System.out.println("Member with ID " + memID + " deleted successfully.");
			}
		} catch (NumberFormatException e) {
			System.out.println("Invalid member ID format. Please enter a valid numeric ID.");
		} catch (SQLException e) {
			System.out.println("Error deleting member: " + e.getMessage());
		}

	}

	/**
	 * This method is used to delete a record from a specified table. It constructs
	 * a SQL DELETE statement using the provided table name and attribute, and
	 * executes the statement using the provided connection and member ID.
	 *
	 * @param conn      The Connection object.
	 * @param table     The name of the table.
	 * @param attribute The attribute on which the deletion is based.
	 * @param memID     The ID of the member.
	 * @throws SQLException If an SQL error occurs.
	 */
	private void deleteFromTable(Connection conn, String table, String attribute, String memID) throws SQLException {
		String sql = "DELETE FROM " + table + " WHERE " + attribute + " = ?";
		// Set the parameter for the prepared statement
		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, memID);
			// Execute the statement and get the number of affected rows
			int rowsDeleted = pstmt.executeUpdate();
			System.out.println(rowsDeleted + " row(s) deleted from " + table);
		} catch (SQLException e) {
			System.out.println("Error deleting from " + table + ": " + e.getMessage());
		}
	}

	/**
	 * This method is used to display the prizes that a member can redeem before
	 * deleting their account. It first retrieves the prizes that the member can
	 * afford with their total tickets. Then it prompts the member to choose a prize
	 * to redeem. If the member chooses to redeem a prize, the cost of the prize is
	 * deducted from their total tickets. The list of prizes is updated to remove
	 * the prizes that the member can no longer afford. This process continues until
	 * the member chooses to exit or they have redeemed all the prizes they could.
	 *
	 * @param memID        The ID of the member.
	 * @param totalTickets The total number of tickets that the member has.
	 * @param kb           The Scanner object used for user input.
	 * @throws SQLException If an SQL error occurs.
	 */
	public void showPrizes(String memID, int totalTickets, Scanner kb) {
		try {
			String sql = "SELECT ylai1.Prize.PrizeName, ylai1.Prize.TicketCost FROM ylai1.Prize WHERE ylai1.Prize.TicketCost <= ?";
			PreparedStatement stmt = conn.prepareStatement(sql);
			stmt.setInt(1, totalTickets);
			ResultSet rs = stmt.executeQuery();

			List<String> prizes = new ArrayList<>();
			while (rs.next()) {
				prizes.add(rs.getString("PrizeName") + " - " + rs.getInt("TicketCost") + " tickets");
			}

			while (true) {
				for (int i = 0; i < prizes.size(); i++) {
					System.out.println((i + 1) + ". " + prizes.get(i));
				}

				System.out.println("\nEnter the number of the prize you want to redeem (or type 'exit' to finish):");
				String input = kb.nextLine();

				if (input.equalsIgnoreCase("exit")) {
					break;
				}

				int chosenIndex = Integer.parseInt(input) - 1; // Subtract 1 because list indices start at 0

				if (chosenIndex >= 0 && chosenIndex < prizes.size()) {
					String chosenPrize = prizes.get(chosenIndex);
					int ticketCost = Integer.parseInt(chosenPrize.split(" - ")[1].split(" ")[0]);

					if (ticketCost <= totalTickets) {
						System.out.println("You redeemed: " + chosenPrize);
						totalTickets -= ticketCost;
					} else {
						System.out.println("Not enough tickets.");
					}
				} else {
					System.out.println("Invalid selection.");
				}

				// Remove prizes that the user can no longer afford
				for (int i = prizes.size() - 1; i >= 0; i--) {
					int ticketCost = Integer.parseInt(prizes.get(i).split(" - ")[1].split(" ")[0]);
					if (ticketCost > totalTickets) {
						prizes.remove(i);
					}
				}

				if (prizes.isEmpty()) {
					System.out.println(
							"\n🎉 Congratulations! You've redeemed all the prizes you could with your tickets. Thank you for playing!");
					break;
				}
			}
		} catch (SQLException e) {
			System.out.println("Error: " + e.getMessage());
		}
	}

	/**
	 * This method is used to get the total number of coupons a member has. It
	 * constructs a SQL SELECT COUNT statement and executes it using the provided
	 * member ID. The count of coupons is returned.
	 *
	 * @param memID The ID of the member.
	 * @return The total number of coupons the member has.
	 * @throws SQLException If an SQL error occurs.
	 */
	public int getTotalCoupons(String memID) {
		// Initialize the count
		int count = 0;

		// Create a SQL query
		String query = "SELECT COUNT(*) FROM ylai1.Coupon WHERE MemberID = ?";

		try {
			// Create a PreparedStatement
			PreparedStatement pstmt = conn.prepareStatement(query);

			// Set the parameter
			pstmt.setString(1, memID);

			// Execute the query
			ResultSet rs = pstmt.executeQuery();

			// If a result is returned, set the count
			if (rs.next()) {
				count = rs.getInt(1);
			}

			// Close the ResultSet and PreparedStatement
			rs.close();
			pstmt.close();
		} catch (SQLException e) {
			System.out.println("Error getting total coupons: " + e.getMessage());
		}

		// Return the count
		return count;
	}

	/**
	 * This method is used to update the information of a member. It prompts the
	 * member to enter their new name, address, and telephone number. It then
	 * constructs a SQL UPDATE statement using the provided member ID and the new
	 * information, and executes the statement.
	 *
	 * @param kb    The Scanner object used for user input.
	 * @param memID The ID of the member.
	 * @throws SQLException If an SQL error occurs.
	 */
	public void updateMember(Scanner kb, String memID) {
		try {
			// Check if the member ID exists
			if (idExists(1, memID)) {
				System.out.println("Enter new name :");
				String newName = kb.nextLine();

				System.out.println("Enter new address :");
				String newAddress = kb.nextLine();

				System.out.println("Enter new telephone number :");
				String newTelephone = kb.nextLine();

				// Construct the update query based on user input
				StringBuilder updateQuery = new StringBuilder("UPDATE ylai1.MemberInfo SET ");

				// This is used to determine whether a comma is needed before appending the next
				// set clause
				boolean needsComma = false;

				// If the user entered a new name, append it to the update query
				if (!newName.isEmpty()) {
					updateQuery.append("Name = '").append(newName).append("'");
					needsComma = true;
				}
				if (!newAddress.isEmpty()) {
					if (needsComma) {
						updateQuery.append(", ");
					}
					updateQuery.append("Address = '").append(newAddress).append("'");
					needsComma = true;
				}
				if (!newTelephone.isEmpty()) {
					if (needsComma) {
						updateQuery.append(", ");
					}
					updateQuery.append("Telephone = '").append(newTelephone).append("'");
				}
				// Append the WHERE clause to the update query
				updateQuery.append(" WHERE MemberID = ").append(memID);

				// Execute the update
				Statement statement = conn.createStatement();
				int rowsUpdated = statement.executeUpdate(updateQuery.toString());
				if (rowsUpdated > 0) {
					System.out.println("Member information updated successfully.");
				} else {
					System.out.println("Failed to update member information.");
				}

			} else {
				System.out.println("Member with ID " + memID + " not found.");
			}
		} catch (SQLException e) {
			System.out.println("Error updating member information: " + e.getMessage());
		}
	}

	/**
	 * Checks if a member exists in the database based on their ID.
	 *
	 * @param memID the ID of the member to check
	 * @return true if the member exists, false otherwise
	 * @throws SQLException if a database error occurs
	 */
	private boolean idExists(int idType, String id) throws SQLException {
		String query;
		if (idType == 1) {
			query = "SELECT * FROM ylai1.MemberInfo WHERE MemberID = ?";
		} else if (idType == 2) {
			query = "SELECT * FROM ylai1.Coupon WHERE CouponID = ?";
		} else {
			throw new IllegalArgumentException("Invalid ID type provided.");
		}

		try (PreparedStatement pstmt = conn.prepareStatement(query)) {
			pstmt.setString(1, id);
			try (ResultSet rs = pstmt.executeQuery()) {
				return rs.next(); // Returns true if a record is found
			}
		}
	}

	// --------------------------------------------------------------------------------
	// Buy Token HERE!
	/**
	 * This method is used to welcome a member to buy tokens. It first checks if the
	 * member ID exists in the database. Then it fetches the discount for the member
	 * and calculates the total cost after applying the discount. It displays a
	 * welcome message and prompts the member to buy tokens. After the member
	 * chooses the number of tokens to buy, it updates the token transaction and the
	 * member record and tier.
	 *
	 * @param kb    The Scanner object used for user input.
	 * @param memID The ID of the member.
	 * @throws SQLException If an SQL error occurs.
	 */
	public void welcomeMemberToBuyTokens(Scanner kb, String memID) {
		// Check if the member ID is in our table yet
		try {
			idExists(1, memID);
		} catch (SQLException e) {
			System.out.println("The member ID you provided does not exist.");
			e.printStackTrace();
		}

		int tierId = idGenerator.getTierID(conn, memID);
		// Fetch the discount for the member
		int discount = getDiscountForMember(String.valueOf(tierId));

		// Welcome message for buying tokens
		System.out.println("\n\n--------------------------------------------------");
		System.out.println("                  ARCADE TOKEN SHOP                ");
		System.out.println("--------------------------------------------------");
		System.out.println("Welcome, Member " + memID + "! Ready to buy some tokens and have fun?");
		System.out.println("1 token = $1. How many tokens would you like to buy today?");
		System.out.println("--------------------------------------------------\n");

		int tokenCount = kb.nextInt();

		// Calculate the total cost after applying the discount
		float discountRate = discount / 100.0f;
		float spentMoney = tokenCount * (1.0f - discountRate);

		System.out.println("\n--------------------------------------------------");
		System.out.println("Great! You've chosen to buy " + tokenCount + " tokens.");
		System.out.println("With your " + discount + "% discount, that will be $" + String.format("%.1f", spentMoney)
				+ ". Enjoy your games!");
		System.out.println("--------------------------------------------------\n");

		tokenXactUpdate(memID, tokenCount); // UPDATE TOKEN-XACT
		updateMemberRecordAndTier(Integer.parseInt(memID), tokenCount, spentMoney);

		kb.nextLine(); // Consume the newline left-over
	}

	/**
	 * This method is used to get the discount for a member. It constructs a SQL
	 * SELECT statement and executes it using the provided tier ID. The discount is
	 * returned.
	 *
	 * @param tierID The ID of the tier.
	 * @return The discount for the member.
	 * @throws SQLException If an SQL error occurs.
	 */
	public int getDiscountForMember(String tierID) {
		// Initialize the discount
		int discount = 0;

		// Create a SQL query
		String query = "SELECT Discount FROM ylai1.MemTier WHERE TierID = ?";

		try {
			PreparedStatement pstmt = conn.prepareStatement(query);
			pstmt.setString(1, tierID);

			// Execute the query
			ResultSet rs = pstmt.executeQuery();

			// If a result is returned, set the discount
			if (rs.next()) {
				discount = rs.getInt("Discount");
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Return the discount
		return discount;
	}

	/**
	 * This method is used to update the token transaction. It gets the current date
	 * and the next transaction ID, and inserts a new transaction into the
	 * TokenTransaction table.
	 *
	 * @param memID           The ID of the member.
	 * @param tokensPurchased The number of tokens purchased by the member.
	 * @throws SQLException If an SQL error occurs.
	 */
	public void tokenXactUpdate(String memID, int tokensPurchased) {
		// Get the current date
		java.sql.Date date = new java.sql.Date(System.currentTimeMillis());
		int memberId = Integer.parseInt(memID);
		// Get the next transaction ID
		int nextTTransID = idGenerator.getNextTTransID();

		// Insert the new transaction
		String query = "INSERT INTO ylai1.TokenTransaction (TTransID, MemberID, TokensPurchased, TDate) VALUES (?, ?, ?, ?)";
		try {
			PreparedStatement statement = conn.prepareStatement(query);
			statement.setInt(1, nextTTransID);
			statement.setInt(2, memberId);
			statement.setInt(3, tokensPurchased);
			statement.setDate(4, date);

			int rowsInserted = statement.executeUpdate();
			if (rowsInserted > 0)
				System.out.println("\t- New token transaction registered successfully with ID: " + nextTTransID);
			else
				System.out.println("Something went wrong while inserting token transaction");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	// 1. This is used only after buying tokens, then it will proceed to give coupon
	/**
	 * This method is used to update the member record and tier after a member buys
	 * tokens. It first updates the TotalSpent and TotalTokens in the MemberRecord.
	 * Then it gets the TierID from the MemberInfo and updates the Membership Tier
	 * with discount, free ticket, and level status. Finally, it checks if the
	 * member has visited the arcade game at least 3 times; if yes, it gives them a
	 * coupon.
	 *
	 * @param memID           The ID of the member.
	 * @param tokensPurchased The number of tokens purchased by the member.
	 * @param spentMoney      The amount of money spent by the member.
	 */
	public void updateMemberRecordAndTier(int memID, int tokensPurchased, float spentMoney) {

		// Update TotalSpent and TotalTokens in MemberRecord
		updateMemberRecord2(memID, tokensPurchased, spentMoney);

		// Get TierID from MemberInfo
		int tierID = idGenerator.getTierID(conn, String.valueOf(memID));

		// Now update Membership Tier with discount, free ticket and level status
		updateMemTier(tierID, memID);

		// Check if the member has visited the arcade game at least 3 times; if yes,
		// give them a coupon
		giveCoupon(memID);
	}

	// 2(a). First, update the spending and tokens for each member.
	/**
	 * This method is used to update the spending and tokens for each member. It
	 * gets the current TotalSpent and TotalTokens for the member, and adds the
	 * newly spent money and purchased tokens. Then it updates the TotalSpent and
	 * TotalTokens in the MemberRecord.
	 *
	 * @param memID           The ID of the member.
	 * @param tokensPurchased The number of tokens purchased by the member.
	 * @param spentMoney      The amount of money spent by the member.
	 */
	private void updateMemberRecord2(int memID, int tokensPurchased, float spentMoney) {
		// Get the current TotalSpent and TotalTokens for the member
		int[] spentAndTokens = getTotalSpentAndTokens(memID);
		float currentTotalSpent = spentAndTokens[0] + spentMoney;
		int currentTotalTokens = spentAndTokens[1] + tokensPurchased;

		// Update TotalSpent and TotalTokens in MemberRecord
		String updateMemberRecord = "UPDATE ylai1.MemberRecord SET TotalSpent = ?, TotalTokens = ? WHERE MemberID = ?";
		try {
			PreparedStatement statement = conn.prepareStatement(updateMemberRecord);
			statement.setFloat(1, currentTotalSpent);
			statement.setInt(2, currentTotalTokens);
			statement.setInt(3, memID);

			int rowsUpdated = statement.executeUpdate();
			if (rowsUpdated > 0)
				System.out.println(
						"\t- MemberRecord Total Spent and Total Token updated successfully for MemberID: " + memID);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	// 2(b,c). Before we can update the Member Record, we need to get their current
	// total spent
	/**
	 * This method is used to get the current total spent and total tokens for a
	 * member. It constructs a SQL SELECT statement and executes it using the
	 * provided member ID. The total spent, total tokens, and total tickets are
	 * returned in an array.
	 *
	 * @param memID The ID of the member.
	 * @return An array containing the total spent, total tokens, and total tickets
	 *         for the member.
	 */
	public int[] getTotalSpentAndTokens(int memID) {
		int[] result = new int[3]; // Index 0 for TotalSpent, Index 1 for TotalTokens
		try {
			String query = "SELECT TotalSpent, TotalTokens, TotalTickets FROM ylai1.MemberRecord WHERE MemberID = ?";
			PreparedStatement statement = conn.prepareStatement(query);
			statement.setInt(1, memID);
			ResultSet resultSet = statement.executeQuery();
			if (resultSet.next()) {
				result[0] = resultSet.getInt("TotalSpent");
				result[1] = resultSet.getInt("TotalTokens");
				result[2] = resultSet.getInt("TotalTickets");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;
	}

	// 3(a). This will check the current status/level of a member
	/**
	 * This method is used to update the membership tier based on the total spent
	 * and current tier status. It gets the current total spent for the member and
	 * the current tier information from the MemTier. Then it updates the tier based
	 * on the total spent and current tier status.
	 *
	 * @param tierID The ID of the tier.
	 * @param memID  The ID of the member.
	 */
	private void updateMemTier(int tierID, int memID) {
		// Get the current total spent for the member
		int[] spentAndTokens = getTotalSpentAndTokens(memID);
		int totalSpent = spentAndTokens[0];

		// Get the current tier information from MemTier
		String getMemTierQuery = "SELECT Discount, FreeTickets, LevelStatus FROM ylai1.MemTier WHERE TierID = ?";
		try (PreparedStatement stmt = conn.prepareStatement(getMemTierQuery)) {
			stmt.setInt(1, tierID);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				int currentDiscount = rs.getInt("Discount");
				int currentFreeTickets = rs.getInt("FreeTickets");
				String currentLevelStatus = rs.getString("LevelStatus");

				// Update the tier based on total spent and current tier status
				if (currentLevelStatus.equals("Regular") && totalSpent >= 250 && totalSpent < 500) {
					helperUpdateTier(tierID, "Gold", currentDiscount + 10, currentFreeTickets + 5000, memID);
				} else if (currentLevelStatus.equals("Regular") && totalSpent >= 500) {
					helperUpdateTier(tierID, "Diamond", 20, currentFreeTickets + 15000, memID);
				} else if (currentLevelStatus.equals("Gold") && totalSpent >= 500) {
					helperUpdateTier(tierID, "Diamond", currentDiscount + 10, currentFreeTickets + 10000, memID);
				} else {
					System.out.println("\t- Status not updated.");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	// 3(b). UPDATE THE MEMTIER TO SET VALUE SUCH AS DISCOUNT, FREE-TICKET AND
	// STATUS(GOLD,DIAMOND OR REGULAR)
	/**
	 * This helper method is used to update the membership tier for a member. It
	 * constructs a SQL UPDATE statement and executes it using the provided tier ID,
	 * new level status, new discount, new free tickets, and member ID. If the
	 * update is successful, it prints a success message and the new tier details.
	 * It also updates the MemberRecord with the new total tickets.
	 *
	 * @param tierID         The ID of the tier.
	 * @param newLevelStatus The new level status.
	 * @param newDiscount    The new discount.
	 * @param newFreeTickets The new number of free tickets.
	 * @param memID          The ID of the member.
	 */
	private void helperUpdateTier(int tierID, String newLevelStatus, int newDiscount, int newFreeTickets, int memID) {
		String updateMemTierQuery = "UPDATE ylai1.MemTier SET Discount = ?, FreeTickets = ?, LevelStatus = ? WHERE TierID = ?";
		try (PreparedStatement stmt = conn.prepareStatement(updateMemTierQuery)) {
			stmt.setInt(1, newDiscount);
			stmt.setInt(2, newFreeTickets);
			stmt.setString(3, newLevelStatus);
			stmt.setInt(4, tierID);

			int rowsUpdated = stmt.executeUpdate();
			if (rowsUpdated > 0) {
				System.out.println("\t- MemTier updated successfully for MemberID: " + memID);
				System.out.println("\t- New Level Status: " + newLevelStatus);
				System.out.println("\t- New Discount: " + newDiscount + "%");
				System.out.println("\t- New Free Tickets: " + newFreeTickets);

				// Update MemberRecord with new total tickets
				String updateTickets = "UPDATE ylai1.MemberRecord SET TotalTickets = TotalTickets + ? WHERE MemberID = ?";
				try (PreparedStatement memberRecordStmt = conn.prepareStatement(updateTickets)) {
					memberRecordStmt.setInt(1, newFreeTickets); // Add the new free tickets
					memberRecordStmt.setInt(2, memID);
					int memberRecordRowsUpdated = memberRecordStmt.executeUpdate();
					if (memberRecordRowsUpdated > 0) {
						System.out.println("\t- Successfully updated total tickets for MemberID: " + memID);
					} else {
						System.out.println("Failed to update MemberRecord for MemberID: " + memID);
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}

			} else {
				System.out.println("Failed to update MemTier for MemberID: " + memID);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	// GIVE MEMBER COUPON IF THEY VISIT ACARDE GAME >= 3 TIMES
	/**
	 * This method is used to give a coupon to a member if they have visited the
	 * arcade game at least 3 times. It prepares a statement to check the number of
	 * visits for the member and executes the query. If the member has visited at
	 * least 3 times, it gets the next coupon ID and inserts a new coupon for the
	 * member.
	 *
	 * @param memID The ID of the member.
	 */
	private void giveCoupon(int memID) {
		try {
			// Prepare a statement to check the number of visits for the member
			PreparedStatement stmt = conn
					.prepareStatement("SELECT NumberOfVisits FROM ylai1.MemberRecord WHERE MemberID = ?");
			stmt.setInt(1, memID);

			// Execute the query
			ResultSet rs = stmt.executeQuery();

			if (rs.next()) {
				int numberOfVisits = rs.getInt("NumberOfVisits");

				if (numberOfVisits >= 3) {
					// Get the next coupon ID
					int nextCouponID = idGenerator.getNextCouponID();

					// Prepare a statement to insert a new coupon for the member
					PreparedStatement insertStmt = conn.prepareStatement(
							"INSERT INTO ylai1.Coupon (CouponID, MemberID, IssueDate) VALUES (?, ?, CURRENT_DATE)");
					insertStmt.setInt(1, nextCouponID);
					insertStmt.setInt(2, memID);

					// Execute the update
					int rowsInserted = insertStmt.executeUpdate();

					if (rowsInserted > 0) {
						System.out.println(
								"\t- Success! Coupon ID " + nextCouponID + " was given to Member ID " + memID + ".");
					}
				} else {
					System.out.println("\t- No coupon was given on this visit. Keep visiting to earn free coupons!");
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	// REDEEM COUPON OVER HERE!!!
	/**
	 * This method is used to redeem a coupon for a member. It first checks if the
	 * member ID and coupon ID exist in the database. Then it gets the next
	 * transaction ID.
	 *
	 * @param couponID The ID of the coupon.
	 * @param memID    The ID of the member.
	 * @param itemName The name of the item to redeem.
	 */
	public void redeemCoupon(int couponID, int memID, String itemName) {
		// Check if the member ID and couponID is in our table yet
		try {
			idExists(1, String.valueOf(memID));
			idExists(2, String.valueOf(couponID));
			// Get the next transaction ID
			int nextTransactionID = idGenerator.getNextXactCouponID();

			// Prepare a statement to insert a new transaction for the redeemed coupon
			PreparedStatement insertStmt = conn.prepareStatement(
					"INSERT INTO ylai1.CouponTransaction (CTransID, MemberID, ItemRedeem, CDate) VALUES (?, ?, ?, CURRENT_DATE)");
			insertStmt.setInt(1, nextTransactionID);
			insertStmt.setInt(2, memID);
			insertStmt.setString(3, itemName);

			// Execute the update
			int rowsInserted = insertStmt.executeUpdate();

			if (rowsInserted > 0) {
				System.out.println("\t- Coupon Transaction ID " + nextTransactionID
						+ " successfully created for Member ID " + memID + " redeeming coupon ID " + couponID + ".\n");
				// Delete the redeemed coupon from the Coupon table
				deleteFromTable(conn, "Coupon", "CouponID", String.valueOf(couponID));
			}
		} catch (SQLException e) {
			System.out.println("The member ID OR coupon ID you provided does not exist.");
			e.printStackTrace();
		}
	}

}
