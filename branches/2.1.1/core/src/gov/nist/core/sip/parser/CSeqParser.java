/*******************************************************************************
 * Conditions Of Use
 * 
 * This software was developed by employees of the National Institute of
 * Standards and Technology (NIST), an agency of the Federal Government.
 * Pursuant to title 15 Untied States Code Section 105, works of NIST
 * employees are not subject to copyright protection in the United States
 * and are considered to be in the public domain.  As a result, a formal
 * license is not needed to use the software.
 * 
 * This software is provided by NIST as a service and is expressly
 * provided "AS IS."  NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED
 * OR STATUTORY, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT
 * AND DATA ACCURACY.  NIST does not warrant or make any representations
 * regarding the use of the software or the results thereof, including but
 * not limited to the correctness, accuracy, reliability or usefulness of
 * the software.
 * 
 * Permission to use this software is contingent upon your acceptance
 * of the terms of this agreement
 ******************************************************************************/
package gov.nist.core.sip.parser;

import gov.nist.core.ParseException;
import gov.nist.core.sip.header.CSeqHeader;
import gov.nist.core.sip.header.Header;



/**
 * Parser for CSeq headers.
 * 
 * @version JAIN-SIP-1.1
 * 
 * @author M. Ranganathan <mranga@nist.gov> <br/>
 * @author Olivier Deruelle <deruelle@nist.gov> <br/>
 * 
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 * 
 */
public class CSeqParser extends HeaderParser {
	CSeqParser() {
	}

	public CSeqParser(String cseq) {
		super(cseq);
	}

	protected CSeqParser(Lexer lexer) {
		super(lexer);
	}

	public Header parse() throws ParseException {
		try {
			CSeqHeader c = new CSeqHeader();

			this.lexer.match(TokenTypes.CSEQ);
			this.lexer.SPorHT();
			this.lexer.match(':');
			this.lexer.SPorHT();
			String number = this.lexer.number();
			c.setSequenceNumber(Integer.parseInt(number));
			this.lexer.SPorHT();
			String m = method();
			c.setMethod(m);
			this.lexer.SPorHT();
			this.lexer.match('\n');
			return c;
		} catch (NumberFormatException ex) {

			throw createParseException("Number format exception");
		}
	}
}
