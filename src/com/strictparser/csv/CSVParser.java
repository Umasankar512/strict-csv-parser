package com.strictparser.csv;

import static com.strictparser.csv.CSVConstants.CR;
import static com.strictparser.csv.CSVConstants.END_OF_STREAM;
import static com.strictparser.csv.CSVConstants.LF;
import static com.strictparser.csv.CSVConstants.SP;
import static com.strictparser.csv.CSVConstants.SEPARATOR_DOUBLE_QUOTE;
import static com.strictparser.csv.CSVConstants.DELIM_COMMA;
import static com.strictparser.csv.Token.Type.EOF;
import static com.strictparser.csv.Token.Type.EOR;
import static com.strictparser.csv.Token.Type.INVALID;
import static com.strictparser.csv.Token.Type.TOKEN;

import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;


/** 
 * <pre>
 * Used to Read the CSV files by following the standard CSV rules.
 * </pre>
 * 
 * <b>
 * Example:
 * </b>
 * 
 * <pre>
 * <code>
 * {@link CSVParser CSVParser} parser = new {@link CSVParser CSVParser}(new FileReader(new File(filePath)), {@link CSVConstants CSVConstants}.DELIM_COMMA, {@link CSVConstants CSVConstants}.SEPARATOR_DOUBLE_QUOTE);
 * 
 * List&lt;{@link CSVRecord CSVRecord}&gt; records = parser.getRecords();
 * 
 * for ({@link CSVRecord CSVRecord} record : records) {
 * 	// Do something
 * }
 *
 * </code>
 * </pre>
 * 
 * @author Uma Sankar [umasankar.yedida@gmail.com; uyedida@opentext.com]
 * @version 1.0 Sep 1, 2015.  
 */
@SuppressWarnings("unused")
public class CSVParser implements Closeable, Iterable<CSVRecord> {

	private CSVReader reader;
	private List<CSVRecord> records;
	private Map<String, Integer> header;
	private Token token;
	private boolean continueParseOnException;
	private char delimiter;
	private char separator;
	private boolean isHeaderIncluded;
	private int columnLengthForEachRecord = -1;
	
	
	/**
	 * @param reader
	 * 			Contains the file object
	 * @throws IOException
	 */
	public CSVParser(Reader reader) throws IOException {
		if (reader == null)
			throw new IOException("Invalid file");
		this.reader = new CSVReader(reader);
		this.token = new Token();
		this.continueParseOnException = false;
		this.isHeaderIncluded = false;
		this.delimiter = ',';
		this.separator = '\0';
	}
	
	/**
	 * @param reader
	 * 			Contains the file object
	 * @throws IOException
	 */
	public CSVParser(Reader reader, boolean continueOnException) throws IOException {
		if (reader == null)
			throw new IOException("Invalid file");
		this.reader = new CSVReader(reader);
		this.token = new Token();
		this.continueParseOnException = continueOnException;
		this.isHeaderIncluded = false;
		this.delimiter = ',';
		this.separator = '\0';
	}
	
	/**
	 * <pre> 
	 * CSVParser constructor requires below parameters.
	 * </pre>
	 * 
	 * @param reader								
	 * 			Contains the file object
	 * @param delimiter								
	 * 			the character which used to seperate the columns.
	 * @param Separator								
	 * 			the character which used to enclose the column data
	 * @throws IOException
	 * 			if reader object is null or file not found
	 */
	public CSVParser(Reader reader, char delimiter, char separator) throws IOException {
		this(reader);
		this.delimiter = delimiter;
		this.separator = separator;
	}
	
	/**
	 * Overloaded constructor
	 * 
	 * @param reader								
	 * 			Contains the file object
	 * @param delimiter								
	 * 			the character which used to seperate the columns.
	 * @param Separator								
	 * 			the character which used to enclose the column data
	 * @param continueParseOnExceptionOccured
	 * <pre>
	 * 			true - continues the parsing process even invalid records found
	 * 			false - breaks the parsing process if any exception occurs
	 * </pre>
	 * @throws IOException
	 */
	public CSVParser(Reader reader, char delimiter, char separator, boolean continueParseOnExceptionOccured) throws IOException {
		this(reader, delimiter, separator);
		this.continueParseOnException = continueParseOnExceptionOccured;
	}
	
	/**
	 * Overloaded constructor
	 * 
	 * @param reader								
	 * 			Contains the file object
	 * @param delimiter								
	 * 			the character which used to seperate the columns.
	 * @param Separator								
	 * 			the character which used to enclose the column data
	 * @param continueParseOnExceptionOccured					
	 * 			true - continues the parsing process even invalid records found
	 * 			false - breaks the parsing process if any exception occurs
	 * @param isHeaderIncluded
	 * 			true - header will be included in the response
	 * 			false - header will be excluded in the response
	 * @throws IOException
	 */
	public CSVParser(Reader reader, char delimiter, char separator, boolean continueParseOnExceptionOccured, boolean isHeaderIncluded) throws IOException {
		this(reader, delimiter, separator, continueParseOnExceptionOccured);
		this.isHeaderIncluded = isHeaderIncluded;
	}
	
	/** 
	 * <pre>
	 * Validate the CSV File and returns true, if it is valid and false, otherwise.
	 * </pre>
	 * 
	 * @return
	 * 			true - if file is in valid format
	 * 			false - if file is in wrong format
	 */
	public boolean isValid(){
		return true;
	}
	
	/** 
	 * <pre>
	 * Closes the InputStream opened when CSVParser object initialized
	 * </pre>
	 *  
	 *  @throws IOException
	 *  			on parse error or input-read failed
	 */
	@Override
	public void close() throws IOException {
		if (this.reader != null)
			this.reader.close();
	}
	
	/**
	 * @param length
	 * 		specify unique length for records
	 */
	public void setColumnLengthForRecord(int length) {
		this.columnLengthForEachRecord = length;
	}
	
	/**
	 * Get the Column length for records
	 */
	public int getColumnLengthForRecord() {
		return this.columnLengthForEachRecord;
	}
	
	/** 
	 * <pre>
	 * Returns the list of CSVRecord objects if the file is in proper CSV Format
	 * throws CSVFormatException if it is invalid.
	 * </pre>
	 * 
	 * @return	
	 * 		list of {@link CSVRecord CSVRecords} 
	 * @throws CSVFormatException
	 * 		if provided CSV file contains wrong format.
	 * @throws IOException
	 * 		on parse error or input-read failed.
	 */
	public List<CSVRecord> getRecords() throws CSVFormatException, IOException {
		if (this.records == null) {
			this.records = new ArrayList<CSVRecord>();
		}
		
		if (this.isHeaderIncluded)
			this.header = initializeHeader();
		
		CSVRecord record;
		while ((record = this.getNextRecord()) != null) {
			this.records.add(record);
		}
		return this.records;
	}
	
	private Map<String, Integer> initializeHeader() throws CSVFormatException, IOException {
		if (this.header == null) {
			this.header = new HashMap<String, Integer>();
			header.clear();
		}
		CSVRecord headerRecord = getNextRecord();
		if (headerRecord != null && headerRecord.isValidRecord()) {
			// Map column name to index
			//List<String> columns = headerRecord.getColumns();
			//System.out.println(columns.size());
			// TODO
		}
		return this.header;
	}
	
	private CSVRecord getNextRecord() throws CSVFormatException, IOException {
		ArrayList<String> record = new ArrayList<>();
		String exceptionMessage = "";
		int columnLength = Integer.MAX_VALUE;
		
		if (this.columnLengthForEachRecord != -1)
			columnLength = this.columnLengthForEachRecord;
		do {
			
			this.token.clear();
			this.token = this.getNextColumn(this.token);
			switch (this.token.type) {
			case TOKEN:
				record.add(this.token.content.toString());
				if (this.reader.getLastChar() != this.delimiter) {
					this.reader.read();
				}
				break;
			case EOR:
				record.add(this.token.content.toString());
				if (this.columnLengthForEachRecord != -1 && record.size() > 0) {
					if (columnLength <= 0) {
						throw new CSVFormatException("The record " + record + " at line: " + reader.getCurrentLineNumber() + " contains more than " + this.columnLengthForEachRecord + " columns. All Records must have " + this.columnLengthForEachRecord + " columns only");
					} else if (columnLength > 1) {
						throw new CSVFormatException("The record " + record + " at line: " + reader.getCurrentLineNumber() + " contains less than " + this.columnLengthForEachRecord + " columns. All Records must have " + this.columnLengthForEachRecord + " columns only");
					}
				}
				break;
			case EOF:
				if (this.token.isReady || token.content.length() != 0)
					record.add(this.token.content.toString());
				if (this.columnLengthForEachRecord != -1 && record.size() > 0) {
					if (columnLength <= 0) {
						throw new CSVFormatException("The record " + record + " at line: " + reader.getCurrentLineNumber() + " contains more than " + this.columnLengthForEachRecord + " columns. All Records must have " + this.columnLengthForEachRecord + " columns only");
					} else if (columnLength > 1) {
						throw new CSVFormatException("The record " + record + " at line: " + reader.getCurrentLineNumber() + " contains less than " + this.columnLengthForEachRecord + " columns. All Records must have " + this.columnLengthForEachRecord + " columns only");
					}
				}
				break;
			case INVALID:
				if (!this.continueParseOnException)
					throw new CSVFormatException(this.token.errorMessage + " for the record: " + record);
				else {
					exceptionMessage = this.token.errorMessage + " for the record: " + record;
					reader.readLine();
					//token.type = TOKEN;
					break;
				}
			default:
				throw new TokenTypeException("Unexpected token type: " + this.token.type);
			}
			columnLength -= 1;
		} while (this.token.type == TOKEN);
		
		if (!record.isEmpty() || !exceptionMessage.equals("")) {
			CSVRecord csvRecord;
			if (!exceptionMessage.equals("")) {
				csvRecord = new CSVRecord(record, reader.getCurrentLineNumber(), false, exceptionMessage);
			} else {
				csvRecord = new CSVRecord(record, reader.getCurrentLineNumber(), true, null);
			}
			if (token.type == EOF)
				csvRecord.setLineNumber(csvRecord.getLineNumber() + 1);
			return csvRecord;
		}
		return null;
	}

	private Token getNextColumn(Token token) throws CSVFormatException, IOException {
		
		int lastChar = this.reader.getLastChar();
		
		int c = this.reader.read();
		long currentLineNumber = reader.getCurrentLineNumber();
		
		//boolean endOfLine = isEndOfLine(lastChar);
		
		if (isEndOfFile(lastChar) || (!isDelimiter(lastChar) && isEndOfFile(c))) {
			this.token.type = EOF;
			return this.token;
		} /*else if (endOfLine) {
			this.token.type = EOR;
			return this.token;
		} */
		/*if (lastChar == this.delimiter && (c == CR || c == LF)) {
			return appendErrorMessageToToken("Unexpected character encountered at line: " + currentLineNumber + " position: " + reader.getCurrPosition());
		}*/
		
		if (this.separator == '\0') {
			while (token.type == INVALID) {
				if (c != '\0') {
					/*if (c == ',' && isEndOfLine(reader.lookAhead())) {
						return appendErrorMessageToToken("Unexpected character encountered at line: " + currentLineNumber + " position: " + reader.getCurrPosition());
					}*/
					try {
						this.token = getColumnData(this.token);
					} catch (CSVFormatException csvFormatException) {
						return appendErrorMessageToToken(csvFormatException.getMessage());
					}
				} else if (!isValidEndOfLine(c, lastChar)) {
					if (c != SP)
						return appendErrorMessageToToken("Unexpected delimiter" + this.delimiter + " encountered at line: " + currentLineNumber);
					else if (c == SP || !isDelimiter(c) || !isSeparator(c) || !isEndOfFile(c) || !isEndOfLine(c)) {
						return appendErrorMessageToToken("Unexpected character encountered at line: " + currentLineNumber + " position: " + reader.getCurrPosition());
					}
				} else if (isEndOfLine(c)) {
					this.token.type = EOR;
				} else if (isEndOfFile(c)) {
					this.token.type = EOF;
				} else {
					return appendErrorMessageToToken("Unexpected character encountered at line: " + currentLineNumber + " position: " + reader.getCurrPosition());
				}
				lastChar = c;
			}
			return this.token;
		}
		while (token.type == INVALID) {
			if (isSeparator(c)) {
				try {
					this.token = getColumnDataBetweenSeparator(this.token);
				} catch(CSVFormatException csvFormatException) {
					return appendErrorMessageToToken(csvFormatException.getMessage());
				}
			} else if(!isValidEndOfLine(c, lastChar)) {
				if (c != SP)
					return appendErrorMessageToToken("Unexpected delimiter" + this.delimiter + " encountered at line: " + currentLineNumber);
				else if (c == SP || !isDelimiter(c) || !isSeparator(c) || !isEndOfFile(c) || !isEndOfLine(c)) {
					return appendErrorMessageToToken("Unexpected character encountered at line: " + currentLineNumber + " position: " + reader.getCurrPosition());
				}
			} else if (isEndOfLine(c)) {
				this.token.type = EOR;
			} else if (isEndOfFile(c)) {
				this.token.type = EOF;
			} else {
				return appendErrorMessageToToken("Column information should starts with Separator [" + this.separator + "] at line: "+ currentLineNumber);
			}
		}
	
		return this.token;
	}
	
	private Token getColumnData(Token token) throws IOException {
		// TODO Auto-generated method stub
		long currentLineNumber = reader.getCurrentLineNumber();
		
		int c = reader.getLastChar();
		//this.token.content.append((char) reader.getLastChar());
		
		while (true) {			
			if (c == this.delimiter) {
				token.type = TOKEN;
				return token;
			} else if (isEndOfLine(c)) {
				token.type = EOR;
				return token;
			} else if (isEndOfFile(c)) {
				token.type = EOF;
				return token;
			} else if (c == '"') {
				if (token.content.length() == 0) {
					token = getColumnDataWithInDoubleQuotes(token);
					// Truncating the " from the column value
					/*if (token.content.length() > 1) {
						String columnValue = token.content.substring(0, token.content.length());
						token.content.setLength(0);
						token.content.append(columnValue);
					}*/
					if (token.type == INVALID)
						return token;
					else if (token.type == TOKEN) 
						return token;
					else if (token.type == EOR)
						return token;
				} else {
					if (reader.lookAhead() == '"') {
						c = reader.read();
						token.content.append((char) c);
					} else {
						throw new CSVFormatException("Unexpected character '\"' encountered at line: " + currentLineNumber + " position: " + reader.getCurrPosition());
					}
				}
			} else {
				token.content.append((char) c);
			}
			c = reader.read();
		}
	}

	private Token getColumnDataWithInDoubleQuotes(Token token) throws IOException {
		int c = reader.getLastChar();
		int doubleQuoteCount = 0;
		while (true) {
			if (c == '"') {
				doubleQuoteCount += 1;
				if (reader.lookAhead() == '"') {
					doubleQuoteCount += 1;
					c = reader.read();
					if (reader.lookAhead() == this.delimiter && (doubleQuoteCount % 2 == 0)) {
						token.type = TOKEN;
						return token;
					}
					token.content.append((char) c);
					if (reader.lookAhead() == this.delimiter) {
						c = reader.read();
						token.content.append((char) c);
					}
				} else if (isEndOfLine(c)) {
					token.type = EOR;
					return this.token;
				} else if (isEndOfFile(c)) {
					token.type = EOF;
					return this.token;
				} else if (c == this.delimiter && (doubleQuoteCount % 2 == 0)) {
					token.type = TOKEN;
					return token;
				} else if (c != this.separator) {
					if (c == '"') {
						c = reader.read();
						continue;
					}
					token.content.append((char) c);
				} else {
					return appendErrorMessageToToken("Unexpected character encountered at line: " + reader.getCurrentLineNumber() + " position: " + reader.getCurrPosition());
				}
			} else  {
				if (isEndOfLine(c)) {
					token.type = EOR;
					return this.token;
				} else if (isEndOfFile(c)) {
					token.type = EOF;
					return this.token;
				} else if (c == this.delimiter && (doubleQuoteCount % 2 == 0)) {
					token.type = TOKEN;
					return token;
				} else if (c != this.separator) {
					token.content.append((char) c);
				} else {
					return appendErrorMessageToToken("Unexpected character encountered at line: " + reader.getCurrentLineNumber() + " position: " + reader.getCurrPosition());
				}
			}
			c = reader.read();
		}
	}

	private Token appendErrorMessageToToken(String message) {
		token.clear();
		token.errorMessage.append(message);
		return this.token;
	}
	
	private Token getColumnDataBetweenSeparator(Token token) throws IOException, CSVFormatException {
		
		long currentLineNumber = reader.getCurrentLineNumber();
		
		int c;
		
		while (true) {
			
			c = reader.read();
			if (isSeparator(c)) {
				if (isSeparator(reader.lookAhead())) { // Checking for the existence of another separator 
					c = reader.read();
					this.token.content.append((char) c);
				} else { // Reading until reach of delimiter, end of line or end of file. Throwing exception if any other characted occurs
					c = reader.read();
					if (isDelimiter(c)) {
						token.type = TOKEN;
						return token;
					} else if (isEndOfFile(c)) {
						token.type = EOF;
						token.isReady = true;
						return token;
					} else if (isEndOfLine(c)) {
						token.type = EOR;
						return token;
					} else {
						throw new CSVFormatException("Separator [" + this.separator +"] is missing at line: " + reader.getCurrentLineNumber() + " position: " + reader.getCurrPosition());
					}
				}
			} else if (isEndOfLine(c)) {
				throw new CSVFormatException("Separator [" + this.separator + "] expected at end of line: " + currentLineNumber);
			} else { // Simply appending the character to content
				token.content.append((char) c);
			}
			
		}
	}
	
	private boolean isEndOfFile(int ch) {
		return ch == END_OF_STREAM;
	}
	
	private boolean isEndOfLine(int ch) throws IOException {
		// For \r\n 
		if (ch == CR && reader.lookAhead() == LF) {
			ch = reader.read();
		}
		return ch == CR || ch == LF;
	}
	
	private boolean isWhiteSpace(int ch) {
		return ch == SP;
	}
	
	private boolean isDelimiter(int ch) {
		return ch == this.delimiter;
	}
	
	private boolean isSeparator(int ch) {
		return ch == this.separator;
	}
	
	private boolean isValidEndOfLine(int ch, int lastChar) throws IOException {
		if (ch == CR && !isDelimiter(lastChar)) {
			return isEndOfLine(ch);
		}
		if (ch == LF)
			return true;
		return false;
	}
	/**
	 * <pre>
	 * Returns the {@link CSVRecord CSVRecord} iterator object
	 * </pre>
	 * 
	 * @return {@link CSVRecord CSVRecord} iterator object
	 */
	@Override
	public Iterator<CSVRecord> iterator() {
		return new RecordIterator<CSVRecord>(this.records);
	}
	
	@SuppressWarnings("hiding")
	private class RecordIterator<CSVRecord> implements Iterator<CSVRecord> {
		
		private List<CSVRecord> records;
		private long size;
		private long counter;
		
		public RecordIterator(List<CSVRecord> records) {
			if (records == null) {
				throw new NullPointerException();
			}
			this.records = records;
			this.size = records.size();
			this.counter = 0L;
		}
		
		@Override
		public boolean hasNext() {
			return counter <= size;
		}

		@Override
		public CSVRecord next() {
			if (!hasNext())
				throw new NoSuchElementException("No more elements in Iterator");
			CSVRecord record = records.get((int)counter);
			counter++;
			return record;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("Operation not supported");
		}
		
	}

	public static void main(String[] args) throws IOException, CSVFormatException, TokenTypeException {
		//CSVParser parser = new CSVParser(new FileReader(new File("C:\\Users\\uyedida\\Desktop\\Panasonic Project\\Vendors_OCC\\FTPRoot\\iORA\\SmallPA\\vendormasterdata\\VM_SMALL PA.csv")), DELIM_COMMA, SEPARATOR_DOUBLE_QUOTE, false, false);
		CSVParser parser;
		{
			parser = new CSVParser(new FileReader(new File("C:\\Users\\uyedida\\Desktop\\CoE-Projects\\MSIG\\MasterData Setup\\Latest Master Data\\05-10-2016\\BMS\\HO - Policy Processing\\User4.csv")));
			// Setting column length for records
			parser.setColumnLengthForRecord(7);
		}
		long startTime = System.currentTimeMillis();
		
		List<CSVRecord> records = parser.getRecords();
		
		long endTime = System.currentTimeMillis();
		float result = (endTime - startTime);
		
		System.out.println("Parsed " + records.size() + " records in " + result + " milliseconds");
		System.out.println(records.size());
		
		Iterator<CSVRecord> recordIterator = records.iterator();
		
		startTime = System.currentTimeMillis();
		
		while(recordIterator.hasNext()) {
			CSVRecord record = recordIterator.next();
			System.out.println("Line Number: " + record.getLineNumber() + ", Column count: " + record.size() + ", isValid: " + record.isValidRecord() + ", Record: " + record.getColumns());
		}
		
		/*endTime = System.currentTimeMillis();
		
		result = endTime - startTime;
		System.out.println(result);
		startTime = System.currentTimeMillis();
		for (CSVRecord record: records) {
			System.out.println("Line Number: " + record.getLineNumber() + ", Column count: " + record.size() + ", isValid: " + record.isValidRecord());
			for (String column : record.getColumns()) {
				System.out.println(column);
			}
		}
		endTime = System.currentTimeMillis();
		
		result = endTime - startTime;
		System.out.println(result);*/
		
		parser.close();
	}
}
