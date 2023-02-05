package plc.project;

import java.util.List;
import java.util.ArrayList;

/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the character which is
 * invalid or missing.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are
 * helpers you need to use, they will make the implementation a lot easier.
 */
public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /** Helper method start **/
    public boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    public boolean isOkForIdentifier(char c)
    {
        return isAlpha(c) || isNumeric(c) || c == '-';
    }

    public boolean isNumeric(char c) {
        return c >= '0' && c <= '9';
    }

    public boolean isWhiteSpace(char current){
        if(current == ' ' || current == '\b' || current == '\n' || current == '\r' || current == '\t')
            return true;
        return false;
    }
    /** Helper Method End **/

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    //Test for git
    public List<Token> lex() {

        List<Token> tokenList = new ArrayList<Token>();

        while(chars.has(0)) //loop through input
        {
            char current = chars.get(0);
            if(isWhiteSpace(current))
            {
                lexEscape();
            }
            else
            {
                tokenList.add(lexToken());
            }
        }
        return tokenList;
    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {

        Token token = null;
        char curToken = chars.get(0);

        if(isAlpha(curToken)) { //Identifier case

           token = lexIdentifier();

        }
        else if(isNumeric(curToken) || curToken == '+' || curToken == '-') { //Number case

            token = lexNumber();

        }
        else if(curToken == '\'') { //Character case

            token = lexCharacter();

        }
        else if(curToken == '\"') { //String case

            token = lexString();

        }
        else { //Operator/Everything else case

            token = lexOperator();

        }

        return token;
    }

    public Token lexIdentifier() {
        List<String> strList = new ArrayList<String>();
        int i = 0;
        while(chars.has(i) && isOkForIdentifier(chars.get(i))) { //checks to make sure char is ok, i is incremented checking char at a time
            String temp = ""+chars.get(i);
            strList.add(temp); //adds to string list
            i++; //increments count
        }

        String[] identifierList = strList.toArray(new String[0]);

        if(match(identifierList))
            return chars.emit(Token.Type.IDENTIFIER);
        else
            throw new ParseException("Error parsing identifier", chars.index);
    }

    public Token lexNumber() {

        //First case, covers +/- and any numeric characters.
        //First char will always be either +, -, or 0-9 because of LexToken.

        List<String> numList = new ArrayList<String>();
        int i = 0;
        String first = "" + chars.get(i);
        numList.add(first);
        i++;


        /*
        Loop through. Everything is ok if it's a number. IF there is a '.', peek to make sure the next value is
        numeric, then turn off the flag for '.' so that if another is lexed it is passed as an operator.
         */

        boolean decimal = false;
        while(chars.has(i) && (isNumeric(chars.get(i)) || chars.get(i) == '.'))
        {
            char curChar = chars.get(i);
            if(decimal && curChar == '.')
            {
                break; //you have reached the end of this decimal number, now . is an operator
            }
            else if(!decimal && curChar == '.')
            {

                if(chars.has(i + 1) && chars.has(i - 1)) //must have following and trailing digits
                {
                    if(isNumeric(chars.get(i+1)) && isNumeric(chars.get(i-1))) //Must be numbers
                    {
                        decimal = true; //It is a decimal
                    }
                    else //Somehow non-numerics showed up
                    {
                        throw new ParseException("Non numeric characters following decimal", chars.index);
                    }
                }
                else //stream does not have a numeric character following a decimal.
                {
                    throw new ParseException("Decimal does not have leading digits", chars.index);
                }
            }

            String toAdd = "" + curChar;
            numList.add(toAdd); //add to buffer
            i++;
        }

        String[] numberStr = numList.toArray(new String[0]);
        boolean matches = match(numberStr);

        if(matches && decimal)
        {
            return chars.emit(Token.Type.DECIMAL);
        }
        else if(matches && !decimal)
        {
            return chars.emit(Token.Type.INTEGER);
        }
        else
        {
            throw new ParseException("Error parsing numbers/deicmals", chars.index);
        }
    }

    public Token lexCharacter() {

        List<String> charList = new ArrayList<String>();
        char curChar = chars.get(0);
        String quote1 = "" + curChar;
        charList.add(quote1); //Add \'


        curChar = chars.get(1);
        if(curChar == '\\') //Escape case
        {
            String slash = "\\" + curChar; //Escape characters are weird man
            charList.add(slash);
            if(chars.get(3) == '\''
                && (chars.get(2) == 'b'
                    || chars.get(2) == 'n'
                    || chars.get(2) == 'r'
                    || chars.get(2) == 't'
                    || chars.get(2) == '\\'
                    || chars.get(2) == '\''
                    || chars.get(2) == '\"')) {

                String val = "" + chars.get(2);
                charList.add(val); //Add Escape char
                String quote2 = "" + chars.get(3); //Add ending \'
                charList.add(quote2);
            }
            else
                throw new ParseException("Error parsing character", chars.index + 1);
        }
        else if(curChar != '\"' && curChar != '\'' && curChar != '\n' && curChar != '\r')
        { //Add non-escaped character
            String val = "" + curChar;
            charList.add(val);
            if(chars.get(2) == '\'')
            {
                String quote2 = "" + chars.get(2);
                charList.add(quote2);
            }
            else
            {
                throw new ParseException("Error parsing character, missing closing \'", chars.index + 2);
            }

        }
        else
        {
            throw new ParseException("Error parsing character", chars.index);
        }

        String[] charStr = charList.toArray(new String[0]);

        boolean matches = match(charStr);
        if(matches)
        {
            return chars.emit(Token.Type.CHARACTER);
        }
        else
        {
            throw new ParseException("Error matching character string", chars.index);
        }

    }

    public Token lexString() {
        List<String> strList = new ArrayList<String>();
        char curChar = chars.get(0);
        String dQuote1 = "" + curChar;
        strList.add(dQuote1); //Add \"

        int i = 1;
        boolean terminated = false;
        boolean properlyEscaped = false;
        while(chars.has(i) && !terminated)
        {
            String empty = "";
            curChar = chars.get(i);

            if(curChar == '\"')
                terminated = true;

            if(curChar == '\\' && !properlyEscaped)
            {
                if(chars.has(i+1) && (chars.get(i+1) == 'b' || chars.get(i+1) == 'n' || chars.get(i+1) == 'r' || chars.get(i+1) == 't'
                        || chars.get(i+1) == '\"'|| chars.get(i+1) == '\''|| chars.get(i+1) == '\\'))  //Doesn't have another character
                {
                    empty = "\\";
                    properlyEscaped = true;
                } //else if the next character (which must exist) is bnrt " ' or \\
                else
                {
                    throw new ParseException("Invalid escape sequence", chars.index + i);
                }

            }
            else if(curChar == '\\' && properlyEscaped)
            {
                empty = "\\";
                properlyEscaped = false;
            }

            String value = empty + curChar;
            strList.add(value);
            i++;
        }

        String[] strStr = strList.toArray(new String[0]);
        boolean matches = match(strStr);
        if(matches && terminated)
        {
            return chars.emit(Token.Type.STRING);
        }
        else if(matches && !terminated)
        {
            throw new ParseException("Error: Unterminated string", chars.index);
        }
        else
        {
            throw new ParseException("Error matching string", chars.index);
        }
    }

    public void lexEscape() {
        while(chars.has(0)) //always 0 since advance and skip below change the starting point every time
        {
            char tempChar = chars.get(0);
            if(isWhiteSpace(tempChar))
            {
                chars.advance();
                chars.skip();
            }
            else
                break;
        }
    }

    public Token lexOperator() {
        List<String> opList = new ArrayList<String>();
        char curChar = chars.get(0);
        String temp = "\\" + curChar; //Look here if it breaks, this feels like a hack
        opList.add(temp);             //Kept getting an error with Java's match() that is called in peek()
                                      //Do "(" chars need to be escaped? If they're in a string I've never seen
                                      //That issue before. Adding the \\ seems to work though

        if(chars.has(1)) //Allows for an additional equals
        {
            if(chars.get(1) == '=')
            {
                temp = "=";
                opList.add(temp);
            }
        }

        String[] opStr = opList.toArray(new String[0]);
        boolean matches = match(opStr);
        if(matches)
        {
            return chars.emit(Token.Type.OPERATOR);
        }
        else
        {
            throw new ParseException("Unexpected character in Operator", chars.index);
        }
    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {
        for( int i = 0; i < patterns.length; i++) {
            if( !chars.has(i) ||
                !String.valueOf(chars.get(i)).matches(patterns[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {
        boolean peek = peek(patterns);

        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                chars.advance();
            }
        }
        return peek;
    }


    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {

            this.input = input;
        }

        public boolean has(int offset) {

            return index + offset < input.length();
        }

        public char get(int offset) {

            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {

            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }

}
