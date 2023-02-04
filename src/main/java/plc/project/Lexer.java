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

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {
        List<Token> tokenList = new ArrayList<Token>();
        while(chars.has(0)) { //loop through input
            char current = chars.get(0);
            if(isWhiteSpace(current)) {
                lexEscape();
            }
            else {

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

        String numberStr = "";
        int i = 0;
        char curChar = chars.get(i);
        numberStr += curChar;
        i++;


        /*
        Loop through. Everything is ok if it's a number. IF there is a '.', peek to make sure the next value is
        numeric, then turn off the flag for '.' so that if another is lexed it is passed as an operator.
         */

        boolean decimal = false;
        while(chars.has(i) && (isNumeric(chars.get(i)) || chars.get(i) == '.'))
        {
            curChar = chars.get(i);
            if(decimal && curChar == '.')
            {
                break; //you have reached the end of this decimal number, now . is an operator
            }
            else if(!decimal && curChar == '.')
            {

                if(chars.has(1) || isNumeric(chars.get(1)))
                {
                    decimal = true;
                }
                else //stream does not have a numeric character following a decimal.
                {
                    decimal = false;
                    break; //therefore, exit the loop since you have finished your number
                }
            }

            numberStr += curChar; //add to buffer
            i++;
        }

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

        String charStr = "";
        char curChar = chars.get(0);
        charStr += curChar;

        //charstr == '

        curChar = chars.get(1);
        if(curChar == '\\') //Escape case
        {
            charStr += curChar;
            if(chars.get(3) == '\''
                    &&
                    (chars.get(2) == 'b' || chars.get(2) == 'n' || chars.get(2) == 'r' || chars.get(2) == 't' || chars.get(2) == '\\' || chars.get(2) == '\'' || chars.get(2) == '\"'))
            {
                charStr += chars.get(2) + chars.get(3);
            }
            else
                throw new ParseException("Error parsing character", chars.index + 1);
        }
        else if(curChar != '\"' && curChar != '\'' && curChar != '\n' && curChar != '\r')
        { //Non-escape character
            charStr += curChar;
            if(chars.get(2) == '\'')
            {
                charStr +=  chars.get(2);
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
        throw new UnsupportedOperationException(); //TODO
    }

    public void lexEscape() {
        throw new UnsupportedOperationException(); //TODO
    }

    public Token lexOperator() {
        throw new UnsupportedOperationException(); //TODO
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
