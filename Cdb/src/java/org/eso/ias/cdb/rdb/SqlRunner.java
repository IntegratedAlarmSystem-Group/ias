package org.eso.ias.cdb.rdb;

import java.io.Reader;
import java.io.StringReader;
import java.util.Objects;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.hibernate.Query;
import org.hibernate.Session;

/**
 * <code>SqlRunner</code> runs a SQL script in the RDB server.
 * <P>
 * OJDBC drivers runs a single SQL statement so the purpose of this class
 * is to split the statements in a script into a sequence of statements 
 * to be executed one at a time.
 * <P>
 * <EM>Note</EM>: The SQL script can contain comments as they are cleaned by objects of this
 * class. The cleaning is "best effort" i.e. not perfect. For example
 * nested comments are not removed.
 *  
 * @author acaproni
 * @since v1.0
 *
 */
public class SqlRunner {
	
	/**
	 * The reader to read the SQL from
	 */
	private final Reader reader;
	
	/**
	 * The regular expression to catch multi-line comments 
	 */
	private static final Pattern MultiLineComment = Pattern.compile("(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)");
	
	/**
	 * SQL single line comments, matched by this regular expression, are removed as well
	 */
	private static final String SingleLineComment = "--.*";
	
	/**
	 * Constructor 
	 * 
	 * @param reader The reader to get the SQL script from
	 */
	public SqlRunner(Reader reader) {
		Objects.requireNonNull(reader,"The reader can't be null");
		this.reader=reader;
	}

	/**
	 * Constructor
	 * 
	 * @param sqlScript The SQL script to run
	 */
	public SqlRunner(String sqlScript) {
		this(new StringReader(sqlScript));
	}
	
	/**
	 * Remove comments from the script.
	 * <P>
	 * The string returned by this method contains all the
	 * SQL commands in the script separated by ';'. 
	 * Multi- and single- line comments are removed.
	 * 
	 * 
	 * @param source The source to read the SQL script from
	 * @return The SQL script without comments
	 */
	private String removeComments(Reader source) {
		// Clean the script from multi-line comments
		StringBuilder ret = new StringBuilder();
		Scanner scanner= new Scanner(source).useDelimiter(MultiLineComment);
		try {
			while (scanner.hasNext()) {
				String nextEntry = scanner.next().trim();
				
				// Remove single line comments i.e. those starting with --
				String[] parts = nextEntry.split("\n");
				for (String str: parts) {
					String withoutSingleLineComments = str.replaceAll(SingleLineComment, "").trim();
					
					ret.append(withoutSingleLineComments);
					ret.append(' ');
				}
			}	
		} finally {
			if (Objects.nonNull(scanner)) {
				scanner.close();
			}
		}
		
		return ret.toString();
	}
	
	/**
	 * Run the script on the RDB server.
	 * 
	 * @param session The Session
	 */
	public void runSQLScript(Session session) {
		Objects.requireNonNull(session, "The session can't be null");
		String cleanedScript = removeComments(reader);
		String[] sqlStatements = cleanedScript.split(";");
		
		session.beginTransaction();
		
		for (String sqlStatement: sqlStatements) {
			String trimmed = sqlStatement.trim();
			if (!trimmed.isEmpty()) {
				Query query = session.createSQLQuery(sqlStatement);
				query.executeUpdate();	
			}
		}
		session.getTransaction().commit();
		session.close();
	}

}
