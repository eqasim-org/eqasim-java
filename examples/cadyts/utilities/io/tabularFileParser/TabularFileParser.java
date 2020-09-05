/*
 * Cadyts - Calibration of dynamic traffic simulations
 *
 * Copyright 2009, 2010 Gunnar Flötteröd
 * 
 *
 * This file is part of Cadyts.
 *
 * Cadyts is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Cadyts is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Cadyts.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.floetteroed@epfl.ch
 *
 */ 
package cadyts.utilities.io.tabularFileParser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser for plain text files that are structured in columns.
 * 
 * @author gunnar
 * 
 */
public class TabularFileParser {

	// -------------------- CLASS VARIABLES --------------------

	private static final String ANYTHING = ".*";

	private static final String ANY_SPACE = "\\s*";

	// -------------------- INSTANCE VARIABLES --------------------

	private String startRegex = null;

	private String endRegex = null;

	private String commentRegex = null;

	private String delimiterRegex = null;

	private int minRowLength = 0;

	// -------------------- CONSTRUCTION --------------------

	/**
	 * Empty default constructor.
	 */
	public TabularFileParser() {
	}

	// -------------------- GENERAL CONFIGURATION --------------------

	public void setMinRowLength(final int minRowLength) {
		this.minRowLength = minRowLength;
	}

	public int getMinRowLength() {
		return this.minRowLength;
	}

	// --------------- CONFIGURATION VIA REGULAR EXPRESSIONS ---------------

	/**
	 * Sets the regular expression that identifies the the first line of the
	 * file section to be parsed.
	 * 
	 * @param regex
	 *            the regular expression that identifies the the first line of
	 *            the file section to be parsed
	 */
	public void setStartRegex(String regex) {
		this.startRegex = regex;
	}

	public String getStartRegex() {
		return startRegex;
	}

	/**
	 * Sets the regular expression that identifies the first line <em>after</em>
	 * the file section to be parsed.
	 * 
	 * @param regex
	 *            the regular expression that identifies the first line
	 *            <em>after</em> the file section to be parsed
	 */
	public void setEndRegex(String regex) {
		this.endRegex = regex;
	}

	public String getEndRegex() {
		return endRegex;
	}

	/**
	 * Sets the regular expression that identifies lines to be ignored during
	 * parsing.
	 * 
	 * @param regex
	 *            the regular expression that identifies lines to be ignored
	 *            during parsing
	 */
	public void setCommentRegex(String regex) {
		this.commentRegex = regex;
	}

	public String getCommentRegex() {
		return commentRegex;
	}

	/**
	 * Sets the regular expression that identifies splitting locations in a
	 * parsed line.
	 * 
	 * @param regex
	 *            the regular expression that identifies splitting locations in
	 *            a parsed line
	 */
	public void setDelimiterRegex(String regex) {
		this.delimiterRegex = regex;
	}

	public String getDelimiterRegex() {
		return delimiterRegex;
	}

	// -------------------- CONFIGURATION VIA TAGS --------------------

	private String alternativeExpr(String[] alternatives) {
		StringBuffer result = new StringBuffer();

		if (alternatives != null)
			for (int i = 0; i < alternatives.length; i++) {
				result.append(quote(alternatives[i]));
				if (i < alternatives.length - 1)
					result.append("|");
			}
		return result.toString();
	}

	private String quote(String expr) {
		return "\\Q" + expr + "\\E";
	}

	/**
	 * Parsing starts with the first occurrence of <code>tag</code> at the
	 * beginning of a line.
	 * 
	 * @param tag
	 *            the first line to be parsed begins with this
	 *            <code>String</code>
	 */
	public void setStartTag(String tag) {
		if (tag != null)
			startRegex = quote(tag) + ANYTHING;
	}

	/**
	 * Parsing ends with the first occurence of <code>tag</code> at the
	 * beginning of a line.
	 * 
	 * @param tag
	 *            the line before which parsing stops begins with this
	 *            <code>String</code>
	 */
	public void setEndTag(String tag) {
		if (tag != null)
			endRegex = quote(tag) + ANYTHING;
	}

	/**
	 * All lines that begin with an element in the <code>tags</code> array are
	 * ignored
	 * 
	 * @param tags
	 *            an array of line beginnings that are to be ignored
	 */
	public void setCommentTags(String[] tags) {
		commentRegex = alternativeExpr(tags) + ANYTHING;
	}

	/**
	 * A line is split into separate rows wherever the parser encounters an
	 * element of the <code>tags</code> array, which is be preceded and
	 * succeeded by zero or more whitespaces.
	 * 
	 * @param tags
	 *            an array of <code>String</code>s denoting column delimiters
	 */
	public void setDelimiterTags(String[] tags) {
		if (tags == null || tags.length == 0)
			delimiterRegex = null;
		else
			delimiterRegex = ANY_SPACE + alternativeExpr(tags) + ANY_SPACE;
	}

	// -------------------- PARSING --------------------

	private boolean isStart(String line) {
		final String regex = this.startRegex;
		if (regex == null)
			return true;
		else
			return line.matches(regex);
	}

	private boolean isEnd(String line) {
		final String regex = this.endRegex;
		if (regex == null)
			return false;
		else
			return line.matches(regex);
	}

	private boolean isComment(String line) {
		final String regex = this.commentRegex;
		if (regex == null)
			return false;
		else
			return line.matches(regex);
	}

	private String[] split(String line) {
		final String regex = this.delimiterRegex;
		if (regex == null)
			return new String[] { line };
		else {
			final String[] naiveSplit = line.split(regex);
			final List<String> properSplit = new ArrayList<String>(
					naiveSplit.length);
			for (String c : naiveSplit) {
				if (c != null && c.length() > 0) {
					properSplit.add(c);
				}
			}
			return properSplit.toArray(new String[] {});
		}
	}

	/**
	 * Parses the file. All relevant rows of the file are split into columns and
	 * then are passed to the <code>handler</code>.
	 * 
	 * @param file
	 *            name of the file to be parsed
	 * @param handler
	 *            defines in what way the parsed file is to be treated
	 *            <em>logically</em>
	 * 
	 * @throws IllegalArgumentException
	 *             if <code>handler</code> is <code>null</code>
	 * @throws IOException
	 */
	public void parse(final String file, TabularFileHandler handler)
			throws IOException {

		if (handler == null) {
			throw new IllegalArgumentException("handler is null");
		}

		boolean started = (this.startRegex == null);
		boolean ended = false;

		handler.startDocument();
		
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line;
		while ((line = reader.readLine()) != null && !ended) {
			if (started) {
				line = line.trim();
				ended = isEnd(line);
				if (!ended && !isComment(line)) {
					final String[] split = this.split(line);
					if (split.length >= this.minRowLength) {
						handler.startRow(split);
					}
				}
			} else {
				started = isStart(line);
			}
		}
		reader.close();
		
		handler.endDocument();
	}
}
