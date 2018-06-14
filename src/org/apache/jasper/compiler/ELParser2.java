/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jasper.compiler;


/**
 * This class implements a parser for EL expressions.
 * 
 * It takes strings of the form xxx${..}yyy${..}zzz etc, and turn it into a
 * ELNode.Nodes.
 * 
 * Currently, it only handles text outside ${..} and functions in ${ ..}.
 * 
 * @author Kin-man Chung
 */

public class ELParser2 {

    private ELNodeToken curToken;  // current token
    private ELNodeToken prevToken; // previous token
    private String whiteSpace = "";

    private final ELNodeNodes expr;

    private ELNodeNodes ELexpr;

    private int index; // Current index of the expression

    private final String expression; // The EL expression
    
    private char type;

    private final boolean isDeferredSyntaxAllowedAsLiteral;

    private static final String reservedWords[] = { "and", "div", "empty",
            "eq", "false", "ge", "gt", "instanceof", "le", "lt", "mod", "ne",
            "not", "null", "or", "true" };

    public ELParser2(String expression, boolean isDeferredSyntaxAllowedAsLiteral) {
        setIndexData(0);
        this.expression = expression;
        this.isDeferredSyntaxAllowedAsLiteral = isDeferredSyntaxAllowedAsLiteral;
        expr = new ELNodeNodes();
    }

    /**
     * Parse an EL expression
     * 
     * @param expression
     *            The input expression string of the form Char* ('${' Char*
     *            '}')* Char*
     * @param isDeferredSyntaxAllowedAsLiteral
     *                      Are deferred expressions treated as literals?
     * @return Parsed EL expression in ELNode.Nodes
     */
    public static ELNodeNodes parse(String expression,
            boolean isDeferredSyntaxAllowedAsLiteral) {
        ELParser2 parser = new ELParser2(expression,
                isDeferredSyntaxAllowedAsLiteral);
        while (parser.hasNextChar()) {
            String text = parser.skipUntilEL();
            if (text.length() > 0) {
                parser.getExprData().add(new ELNodeText(text));
            }
            ELNodeNodes elexpr = parser.parseEL();
            if (!elexpr.isEmpty()) {
                parser.getExprData().add(new ELNodeRoot(elexpr, parser.getTypeData()));
            }
        }
        return parser.getExprData();
    }

    /**
     * Parse an EL expression string '${...}'. Currently only separates the EL
     * into functions and everything else.
     * 
     * @return An ELNode.Nodes representing the EL expression
     * 
     * Note: This can not be refactored to use the standard EL implementation as
     *       the EL API does not provide the level of access required to the
     *       parsed expression.
     */
    private ELNodeNodes parseEL() {

        StringBuilder buf = new StringBuilder();
        setELexprData(new ELNodeNodes());
        setCurTokenData(null);
        setPrevTokenData(null);
        while (hasNext()) {
            setCurTokenData(nextToken());
            if (getCurTokenData() instanceof ELNodeChar) {
                if (getCurTokenData().toChar() == '}') {
                    break;
                }
                buf.append(getCurTokenData().toString());
            } else {
                // Output whatever is in buffer
                if (buf.length() > 0) {
                    getELexprData().add(new ELNodeELText(buf.toString()));
                    buf.setLength(0);
                }
                if (!parseFunction()) {
                    getELexprData().add(new ELNodeELText(getCurTokenData().toString()));
                }
            }
        }
        if (getCurTokenData() != null) {
            buf.append(getCurTokenData().getWhiteSpace());
        }
        if (buf.length() > 0) {
            getELexprData().add(new ELNodeELText(buf.toString()));
        }

        return getELexprData();
    }

    /**
     * Parse for a function FunctionInvokation ::= (identifier ':')? identifier
     * '(' (Expression (,Expression)*)? ')' Note: currently we don't parse
     * arguments
     */
    private boolean parseFunction() {
        if (!(getCurTokenData() instanceof ELNodeId) || isELReserved(getCurTokenData().toTrimmedString()) ||
                getPrevTokenData() instanceof ELNodeChar && getPrevTokenData().toChar() == '.') {
            return false;
        }
        String s1 = null; // Function prefix
        String s2 = getCurTokenData().toTrimmedString(); // Function name
        int start = getIndexData() - getCurTokenData().toString().length();
        ELNodeToken original = getCurTokenData();
        if (hasNext()) {
            int mark = getIndex() - getWhiteSpaceData().length();
            setCurTokenData(nextToken());
            if (getCurTokenData().toChar() == ':') {
                if (hasNext()) {
                	ELNodeToken t2 = nextToken();
                    if (t2 instanceof ELNodeId) {
                        s1 = s2;
                        s2 = t2.toTrimmedString();
                        if (hasNext()) {
                            setCurTokenData(nextToken());
                        }
                    }
                }
            }
            if (getCurTokenData().toChar() == '(') {
                getELexprData().add(new ELNodeFunction(s1, s2, getExpressionData().substring(start, getIndexData() - 1)));
                return true;
            }
            setCurTokenData(original);
            setIndex(mark);
        }
        return false;
    }

    /**
     * Test if an id is a reserved word in EL
     */
    private boolean isELReserved(String id) {
        int i = 0;
        int j = reservedWords.length;
        while (i < j) {
            int k = (i + j) / 2;
            int result = reservedWords[k].compareTo(id);
            if (result == 0) {
                return true;
            }
            if (result < 0) {
                i = k + 1;
            } else {
                j = k;
            }
        }
        return false;
    }

    /**
     * Skip until an EL expression ('${' || '#{') is reached, allowing escape
     * sequences '\\' and '\$' and '\#'.
     * 
     * @return The text string up to the EL expression
     */
    private String skipUntilEL() {
        char prev = 0;
        StringBuilder buf = new StringBuilder();
        while (hasNextChar()) {
            char ch = nextChar();
            if (prev == '\\') {
                if (ch == '$' || (!isDeferredSyntaxAllowedAsLiteralData() && ch == '#')) {
                    prev = 0;
                    buf.append(ch);
                    continue;
                } else if (ch == '\\') {
                    // Not an escape (this time).
                    // Optimisation - no need to set prev as it is unchanged
                    buf.append('\\');
                    continue;
                } else {
                    // Not an escape
                    prev = 0;
                    buf.append('\\');
                    buf.append(ch);
                    continue;
                }
            } else if (prev == '$'
                    || (!isDeferredSyntaxAllowedAsLiteralData() && prev == '#')) {
                if (ch == '{') {
                    this.setTypeData(prev);
                    prev = 0;
                    break;
                }
                buf.append(prev);
                prev = 0;
            }
            if (ch == '\\' || ch == '$'
                    || (!isDeferredSyntaxAllowedAsLiteralData() && ch == '#')) {
                prev = ch;
            } else {
                buf.append(ch);
            }
        }
        if (prev != 0) {
            buf.append(prev);
        }
        return buf.toString();
    }


    /**
     * Escape '\\', '$' and '#', inverting the unescaping performed in
     * {@link #skipUntilEL()}.
     *
     * @param input Non-EL input to be escaped
     * @param isDeferredSyntaxAllowedAsLiteral
     *
     * @return The escaped version of the input
     */
    public static String escapeLiteralExpression(String input,
            boolean isDeferredSyntaxAllowedAsLiteral) {
        int len = input.length();
        int lastAppend = 0;
        StringBuilder output = null;
        for (int i = 0; i < len; i++) {
            char ch = input.charAt(i);
            if (ch =='$' || (!isDeferredSyntaxAllowedAsLiteral && ch == '#')) {
                if (output == null) {
                    output = new StringBuilder(len + 20);
                }
                output.append(input.substring(lastAppend, i));
                lastAppend = i + 1;
                output.append('\\');
                output.append(ch);
            }
        }
        if (output == null) {
            return input;
        } else {
            output.append(input.substring(lastAppend, len));
            return output.toString();
        }
    }


    /**
     * Escape '\\', '\'' and '\"', inverting the unescaping performed in
     * {@link #skipUntilEL()}.
     *
     * @param input Non-EL input to be escaped
     * @param isDeferredSyntaxAllowedAsLiteral
     *
     * @return The escaped version of the input
     */
    public static String escapeELText(String input) {
        int len = input.length();
        char quote = 0;
        int lastAppend = 0;
        int start = 0;
        int end = len;

        // Look to see if the value is quoted
        String trimmed = input.trim();
        int trimmedLen = trimmed.length();
        if (trimmedLen > 1) {
            // Might be quoted
            quote = trimmed.charAt(0);
            if (quote == '\'' || quote == '\"') {
                if (trimmed.charAt(trimmedLen - 1) != quote) {
                    throw new IllegalArgumentException(Localizer.getMessage(
                            "org.apache.jasper.compiler.ELParser.invalidQuotesForStringLiteral",
                            input));
                }
                start = input.indexOf(quote) + 1;
                end = start + trimmedLen - 2;
            } else {
                quote = 0;
            }
        }

        StringBuilder output = null;
        for (int i = start; i < end; i++) {
            char ch = input.charAt(i);
            if (ch == '\\' || ch == quote) {
                if (output == null) {
                    output = new StringBuilder(len + 20);
                }
                output.append(input.substring(lastAppend, i));
                lastAppend = i + 1;
                output.append('\\');
                output.append(ch);
            }
        }
        if (output == null) {
            return input;
        } else {
            output.append(input.substring(lastAppend, len));
            return output.toString();
        }
    }


    /*
     * @return true if there is something left in EL expression buffer other
     * than white spaces.
     */
    private boolean hasNext() {
        skipSpaces();
        return hasNextChar();
    }

    private String getAndResetWhiteSpace() {
        String result = getWhiteSpaceData();
        setWhiteSpaceData("");
        return result;
    }

    /*
     * Implementation note: This method assumes that it is always preceded by a
     * call to hasNext() in order for whitespace handling to be correct.
     *
     * @return The next token in the EL expression buffer.
     */
    private ELNodeToken nextToken() {
        setPrevTokenData(getCurTokenData());
        if (hasNextChar()) {
            char ch = nextChar();
            if (Character.isJavaIdentifierStart(ch)) {
                int start = getIndexData() - 1;
                while (getIndexData() < getExpressionData().length() &&
                        Character.isJavaIdentifierPart(
                                ch = getExpressionData().charAt(getIndexData()))) {
                    nextChar();
                }
                return new ELNodeId(getAndResetWhiteSpace(), getExpressionData().substring(start, getIndexData()));
            }

            if (ch == '\'' || ch == '"') {
                return parseQuotedChars(ch);
            } else {
                // For now...
                return new ELNodeChar(getAndResetWhiteSpace(), ch);
            }
        }
        return null;
    }

    /*
     * Parse a string in single or double quotes, allowing for escape sequences
     * '\\', '\"' and "\'"
     */
    private ELNodeToken parseQuotedChars(char quote) {
        StringBuilder buf = new StringBuilder();
        buf.append(quote);
        while (hasNextChar()) {
            char ch = nextChar();
            if (ch == '\\') {
                ch = nextChar();
                if (ch == '\\' || ch == '\'' || ch == '\"') {
                    buf.append(ch);
                } else {
                    throw new IllegalArgumentException(Localizer.getMessage(
                            "org.apache.jasper.compiler.ELParser.invalidQuoting",
                            getExpressionData()));
                }
            } else if (ch == quote) {
                buf.append(ch);
                break;
            } else {
                buf.append(ch);
            }
        }
        return new ELNodeQuotedString(getAndResetWhiteSpace(), buf.toString());
    }

    /*
     * A collection of low level parse methods dealing with character in the EL
     * expression buffer.
     */

    private void skipSpaces() {
        int start = getIndexData();
        while (hasNextChar()) {
            char c = getExpressionData().charAt(getIndexData());
            if (c > ' ')
                break;
            setIndexData(getIndexData() + 1);
        }
        setWhiteSpaceData(getExpressionData().substring(start, getIndexData()));
    }

    private boolean hasNextChar() {
        return getIndexData() < getExpressionData().length();
    }

    private char nextChar() {
        if (getIndexData() >= getExpressionData().length()) {
            return (char) -1;
        }
        setIndexData(getIndexData() + 1);
        return getExpressionData().charAt(getIndexData()-1);
    }

    private int getIndex() {
        return getIndexData();
    }

    private void setIndex(int i) {
        setIndexData(i);
    }

    public char getType() {
        return getTypeData();
    }

	public ELNodeNodes getExprData() {
		return expr;
	}

	public ELNodeToken getCurTokenData() {
		return curToken;
	}

	public void setCurTokenData(ELNodeToken curToken) {
		this.curToken = curToken;
	}

	public ELNodeToken getPrevTokenData() {
		return prevToken;
	}

	public void setPrevTokenData(ELNodeToken prevToken) {
		this.prevToken = prevToken;
	}

	public String getWhiteSpaceData() {
		return whiteSpace;
	}

	public void setWhiteSpaceData(String whiteSpace) {
		this.whiteSpace = whiteSpace;
	}

	public ELNodeNodes getELexprData() {
		return ELexpr;
	}

	public void setELexprData(ELNodeNodes eLexpr) {
		ELexpr = eLexpr;
	}

	public int getIndexData() {
		return index;
	}

	public void setIndexData(int index) {
		this.index = index;
	}

	public String getExpressionData() {
		return expression;
	}

	public char getTypeData() {
		return type;
	}

	public void setTypeData(char type) {
		this.type = type;
	}

	public boolean isDeferredSyntaxAllowedAsLiteralData() {
		return isDeferredSyntaxAllowedAsLiteral;
	}
}
