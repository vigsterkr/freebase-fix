/**
Copyright (c) 2014, Viktor Gal
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

The views and conclusions contained in the software and documentation are those
of the authors and should not be interpreted as representing official policies,
either expressed or implied, of the FreeBSD Project.
*/

import java.util.Iterator;
import java.util.NoSuchElementException;

import java.util.ArrayList;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.text.ParseException;

class FreebaseFixit {

	private final long line = 0;

	/* patterns */
	private final String FREEBASE_ESCAPE_CHAR = Pattern.quote("$");

	private final Pattern freebaseEscape =
		Pattern.compile(FREEBASE_ESCAPE_CHAR+"([0-9A-Fa-f]{2})([0-9A-Fa-f]{2})");

	private final Pattern tripletPattern =
		Pattern.compile("^(\\S+)\\s+(\\S+)\\s+(.*)$");

	private final Pattern nullChar = Pattern.compile("\\x00");

	private final Pattern URIPattern = Pattern.compile("^<(.*)>$");

	private final Pattern lePattern = Pattern.compile("<");
	private final Pattern gePattern = Pattern.compile(">");

	private final Pattern trailingDotPattern = Pattern.compile("\\s+\\.$");

	private String fixEscaping(String input) {
		Matcher m = freebaseEscape.matcher(input);
		return m.replaceAll("%$1%$2");
	}

	private String removeNullChar(String input) {
		Matcher m = nullChar.matcher(input);
		return m.replaceAll("");
	}

	private List<String> splitToTriplet(String input) {
		Matcher m = tripletPattern.matcher(input);
		List<String> triplet = new ArrayList<String>();
		if (m.matches()) {
			triplet.add(m.group(1));
			triplet.add(m.group(2));
			triplet.add(m.group(3));
		}

		return triplet;
	}

	private boolean isLiteral(String s) {
		if (s.startsWith("\""))
			return true;
		return false;
	}

	private String fixURI(String uri) {
		Matcher m = URIPattern.matcher(uri);
		if (m.matches()) {
			String fixedURI = lePattern.matcher(m.group(1)).replaceAll("%3C");
			fixedURI = gePattern.matcher(fixedURI).replaceAll("%3E");
	 		return String.format("<%s>", fixedURI);
	 	}

	 	return uri;
	}

	public String fixSubject(String subj) {
		String fixedSubject = fixEscaping(subj);

		return fixedSubject;
	}

	public String fixPredicate(String pred) {
		String fixedPredicate = fixEscaping(pred);

		return fixedPredicate;
	}

	public String fixObject(String obj) {
		String fixedObject = obj;

		fixedObject = fixURI(fixedObject);

		if(isLiteral(fixedObject)) {
			fixedObject = removeNullChar(fixedObject);
		}

		return fixedObject;
	}

	private String cleanObject(String obj) {
		String cleanObj = obj.trim();
		cleanObj = trailingDotPattern.matcher(cleanObj).replaceAll("");

		return cleanObj;
	}

	public String fixTriplet(String tripletLine) throws Exception {
		List<String> triplet = splitToTriplet(tripletLine);
		if (triplet.isEmpty())
			throw new Exception("ERROR: Failed to parse the line");

		String subject = triplet.get(0);
		String predicate = triplet.get(1);
		String object = triplet.get(2);

		/* fix subject */
		subject = fixSubject(subject);

		/* fix predicate */
		predicate = fixPredicate(predicate);

		/* fix object */
		if (!object.endsWith("."))
			throw new Exception("ERROR: object doesn't end with dot");

		object = cleanObject(object);
		if (object.length() == 0)
			throw new Exception("ERROR: Unexpected empty object");

		object = fixObject(object);

		return String.format("%s %s %s .", subject, predicate, object);
	}

	public void fix(BufferedReader br) {
		try {
			String input;
			while((input=br.readLine())!=null){
				try {
					System.out.println(fixTriplet(input));
				} catch(Exception e) {
					System.err.println(e.getMessage());
				}
			}
		} catch(IOException io) {
			io.printStackTrace();
		}
	}

	public static void main(String[] args) {
		BufferedReader br =
			new BufferedReader(new InputStreamReader(System.in));

		FreebaseFixit fbFix = new FreebaseFixit();
		fbFix.fix(br);
	}
}
