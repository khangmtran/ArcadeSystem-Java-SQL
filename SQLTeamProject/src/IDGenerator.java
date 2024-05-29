
/**
 * @author Khang Tran, Yen Lai, Alex Ponce Ayala
 */
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * This class is used to generate unique IDs in an Arcade Game system. It
 * provides functionalities for generating unique IDs for various entities like
 * members, coupons, prizes, etc.
 */
public class IDGenerator {

	private Connection conn;

	public IDGenerator(Connection conn) {
		this.conn = conn;
	}

	// FIND NEXT MEMBER ID
	/**
	 * This method is used to get the next available member ID. The next available
	 * ID is one more than the maximum ID. If no data is found or an error occurs,
	 * it defaults to starting ID of 1.
	 *
	 * @return The next available member ID.
	 */
	public int getNextMemberID() {
		String maxIdQuery = "SELECT MAX(MemberID) FROM ylai1.MemberInfo";
		try (Statement maxIdStatement = conn.createStatement();
				ResultSet resultSet = maxIdStatement.executeQuery(maxIdQuery)) {
			if (resultSet.next()) {
				int maxId = resultSet.getInt(1);
				// Increment the max ID to get the next available ID
				return maxId + 1;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		// Default to starting ID of 1 if no data or an error occurs
		return 1;
	}

	// GET NEXT GAMEID
	/**
	 * This method is used to get the next available game ID. The next available ID
	 * is one more than the maximum ID. If no data is found or an error occurs, it
	 * defaults to starting ID of 1.
	 *
	 * @return The next available game ID.
	 */
	public int getNextGameID() {
		String maxQuery = "SELECT MAX(GameID) FROM ylai1.Game";
		try (Statement maxStatement = conn.createStatement();
				ResultSet resultSet = maxStatement.executeQuery(maxQuery)) {
			if (resultSet.next()) {
				int maxGameID = resultSet.getInt(1);
				// Start from 1 if no games exist
				if (resultSet.wasNull()) {
					return 1;
				}
				// Increment the maximum GameID to get the next available ID
				return maxGameID + 1;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		// Default to starting ID of 1 if no data or an error occurs
		return 1;
	}

	// FIND THE NEXT TIER ID
	/**
	 * This method is used to get the next available tier ID. The next available ID
	 * is one more than the maximum ID. If no data is found or an error occurs, it
	 * defaults to starting ID of 1000.
	 *
	 * @return The next available tier ID.
	 */
	public int getNextTierID() {
		String maxQuery = "SELECT MAX(TierID) FROM ylai1.MemTier";
		try (Statement maxStatement = conn.createStatement();
				ResultSet resultSet = maxStatement.executeQuery(maxQuery)) {
			if (resultSet.next()) {
				int maxTierID = resultSet.getInt(1);
				if (resultSet.wasNull()) {
					return 1000; // Return default ID if result is null
				}
				// Increment the maximum TierID to get the next available ID
				return maxTierID + 1;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		// Default to starting ID of 1000 if no data or an error occurs
		return 1000;
	}

	// Find the TIERID that we needed using MemberID
	/**
	 * This method is used to get the TierID for a member. The TierID is returned.
	 * If no data is found or an error occurs, it returns -1.
	 *
	 * @param conn  The Connection object.
	 * @param memID The ID of the member.
	 * @return The TierID for the member.
	 */
	public int getTierID(Connection conn, String memID) {
		String sql = "SELECT TierID FROM ylai1.MemberInfo WHERE MemberID = ?";
		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, memID);
			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					return rs.getInt("TierID");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		// Return -1 if TierID not found
		return -1;
	}

	// GENERATE THE NEXT TRANSACTION ID
	/**
	 * This method is used to get the next available transaction ID. The next
	 * available ID is one more than the maximum ID. If no data is found or an error
	 * occurs, it defaults to starting ID of 9000.
	 *
	 * @return The next available transaction ID.
	 */
	public int getNextTTransID() {
		String sql = "SELECT MAX(TTransID) FROM ylai1.TokenTransaction";
		try (Statement stm = conn.createStatement()) {
			ResultSet result = stm.executeQuery(sql);
			if (result.next()) {
				int ttID = result.getInt(1);
				if (result.wasNull()) {
					return 9000; // Return default ID if result is null
				}
				return ttID + 1;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		// Return 9000 if no transaction found
		return 9000;
	}

	// GET THE NEXT COUPON ID!
	/**
	 * This method is used to get the next available coupon ID. The next available
	 * ID is one more than the maximum ID. If no data is found or an error occurs,
	 * it defaults to starting ID of 2000.
	 *
	 * @return The next available coupon ID.
	 */
	public int getNextCouponID() {
		String maxQuery = "SELECT MAX(CouponID) FROM ylai1.Coupon";
		try (Statement maxStatement = conn.createStatement();
				ResultSet resultSet = maxStatement.executeQuery(maxQuery)) {
			if (resultSet.next()) {
				int maxCouponID = resultSet.getInt(1);
				if (resultSet.wasNull()) {
					return 2000; // Return default ID if result is null
				}
				// Increment the maximum TierID to get the next available ID
				return maxCouponID + 1;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		// Default to starting ID of 2000 if no data or an error occurs
		return 2000;
	}

	// GET THE NEXT TRANSACTION ID!
	/**
	 * This method is used to get the next available transaction ID. The next
	 * available ID is one more than the maximum ID. If no data is found or an error
	 * occurs, it defaults to starting ID of 4000.
	 *
	 * @return The next available transaction ID.
	 */
	public int getNextXactCouponID() {
		String maxQuery = "SELECT MAX(CTransID) FROM ylai1.CouponTransaction";
		try (Statement maxStatement = conn.createStatement();
				ResultSet resultSet = maxStatement.executeQuery(maxQuery)) {
			if (resultSet.next()) {
				int maxTransactionID = resultSet.getInt(1);

				if (resultSet.wasNull()) {
					return 4000; // Return default ID if result is null
				}
				// Increment the maximum TransactionID to get the next available ID
				return maxTransactionID + 1;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		// Default to starting ID of 4000 if no data or an error occurs
		return 4000;
	}

	// GET THE NEXT TRANSACTION FOR PRIZE-XACT
	/**
	 * This method is used to get the next available transaction ID for
	 * PrizeTransaction. The next available ID is one more than the maximum ID. If
	 * no data is found or an error occurs, it defaults to starting ID of 6000.
	 *
	 * @return The next available transaction ID for PrizeTransaction.
	 */
	public int getNextPTransID() {
		String maxQuery = "SELECT MAX(PTransID) FROM ylai1.PrizeTransaction";
		try (Statement maxStatement = conn.createStatement();
				ResultSet resultSet = maxStatement.executeQuery(maxQuery)) {
			if (resultSet.next()) {
				int maxPTransID = resultSet.getInt(1);
				if (resultSet.wasNull()) {
					return 6000; // Return default ID if result is null
				}
				// Increment the maximum PTransID to get the next available ID
				return maxPTransID + 1;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		// Default to starting ID of 6000 if no data or an error occurs
		return 6000;
	}

	// GET THE PRIZE ID
	/**
	 * This method is used to get the next available prize ID. The next available ID
	 * is one more than the maximum ID. If no data is found or an error occurs, it
	 * defaults to starting ID of 1.
	 *
	 * @return The next available prize ID.
	 */
	public int getNextPrizeID() {
		String maxQuery = "SELECT MAX(PrizeID) FROM ylai1.Prize";
		try (Statement maxStatement = conn.createStatement();
				ResultSet resultSet = maxStatement.executeQuery(maxQuery)) {
			if (resultSet.next()) {
				int maxPrizeID = resultSet.getInt(1);
				if (resultSet.wasNull()) {
					return 1; // Start from 1 if no prizes exist
				}
				// Increment the maximum PrizeID to get the next available ID
				return maxPrizeID + 1;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		// Default to starting ID of 1 if no data or an error occurs
		return 1;
	}

	// GET THE NEXT PLAYID
	/**
	 * This method is used to get the next available play ID. The next available ID
	 * is one more than the maximum ID. If no data is found or an error occurs, it
	 * defaults to starting ID of 8000.
	 *
	 * @return The next available play ID.
	 */
	public int getNextPlayID() {
		String maxQuery = "SELECT MAX(PlayID) FROM ylai1.GameHistory";
		try (Statement maxStatement = conn.createStatement();
				ResultSet resultSet = maxStatement.executeQuery(maxQuery)) {
			if (resultSet.next()) {
				int maxPlayID = resultSet.getInt(1);
				if (resultSet.wasNull()) {
					return 8000; // Start from 8000 if no plays exist
				}
				// Increment the maximum PlayID to get the next available ID
				return maxPlayID + 1;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		// Default to starting ID of 8000 if no data or an error occurs
		return 8000;
	}
}
