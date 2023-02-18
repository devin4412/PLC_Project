package plc.project;

import java.util.List;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Optional;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have its own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a field, aka {@code LET}.
     */
    public Ast.Field parseField() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Method parseMethod() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Stmt parseStatement() throws ParseException
    {
        if(peek("LET"))
        {
            return parseDeclarationStatement();
        }
        else if(peek("IF"))
        {
            return parseIfStatement();
        }
        else if(peek("FOR"))
        {
            return parseForStatement();
        }
        else if(peek("WHILE"))
        {
            return parseWhileStatement();
        }
        else if(peek("RETURN"))
        {
            return parseReturnStatement();
        }
        else //Check if Assignment expression or if just normal expression
        {
            Ast.Expr returnedExpr1 = parseExpression(); //This should advance the tokens

            if(peek("=")) //Assignment case
            {
                //peek, then match the "="
                if(match("=") && tokens.has(0) ) //Check to make sure there is RHS to assignment
                {
                    //If there is, parse that expression and try to match the semicolon
                    Ast.Expr returnedExpr2 = parseExpression(); //Should advance tokens
                    if(tokens.has(0) && match(";")) //Check for final semi
                    {
                        Ast.Expr.Access lhs, rhs;

                        if(returnedExpr1 instanceof Ast.Expr.Access) //LHS typecasting
                        {
                            lhs = (Ast.Expr.Access)returnedExpr1; //TODO look closely at this, is simple cast allowed here
                        }
                        else if(returnedExpr1 instanceof Ast.Expr.Literal)
                        {
                            lhs = new Ast.Expr.Access(null, (String) ((Ast.Expr.Literal) returnedExpr1).getLiteral());
                        }
                        else
                        {
                            throw new ParseException("Something is wrong in Assignment LHS. Unexpected token?", tokens.index);
                        }


                        if(returnedExpr2 instanceof Ast.Expr.Access) //RHS typecasting
                        {
                            rhs = (Ast.Expr.Access)returnedExpr2; //TODO look closely at this, is simple cast allowed here, does it correctly return an Ast.Expr.Access?
                        }
                        else if(returnedExpr2 instanceof Ast.Expr.Literal)
                        {
                            rhs = new Ast.Expr.Access(null, (String) ((Ast.Expr.Literal) returnedExpr2).getLiteral());
                        }
                        else
                        {
                            throw new ParseException("Something is wrong in Assignment RHS. Unexpected token?", tokens.index);
                        }

                        return new Ast.Stmt.Assignment(lhs, rhs); //Return newly created assignment statement.
                    }
                    else
                    {
                        throw new ParseException("Missing semicolon", tokens.index);
                    }
                }
                else //Has no RHS for Assignment
                {
                    throw new ParseException("Missing value (Right side) to Assignment Expression", tokens.index);
                }
            }
            else //Non-assignment expression case
            {
                boolean semi = peek(";");
                if(semi && match(";"))
                    return new Ast.Stmt.Expression(returnedExpr1);
                else
                    throw new ParseException("Error: Missing semicolon in Expression", tokens.index);
            }
        }
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Stmt.Declaration parseDeclarationStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Stmt.If parseIfStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a for statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a for statement, aka
     * {@code FOR}.
     */
    public Ast.Stmt.For parseForStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Stmt.While parseWhileStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Stmt.Return parseReturnStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expr parseExpression() throws ParseException {

        return parsePrimaryExpression();
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expr parseLogicalExpression() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expr parseEqualityExpression() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expr parseAdditiveExpression() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expr parseMultiplicativeExpression() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code secondary-expression} rule.
     */
    public Ast.Expr parseSecondaryExpression() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expr parsePrimaryExpression() throws ParseException
    {
        //Starting with the literals "TRUE", "FALSE", "NIL"/Null, integers, decimals, chars, and strings
        if(match("TRUE"))
        {
            return new Ast.Expr.Literal(Boolean.TRUE);
        }
        else if(match("FALSE"))
        {
            return new Ast.Expr.Literal(Boolean.FALSE);
        }
        else if(match("NIL"))
        {
            return new Ast.Expr.Literal(null);
        }
        else if(peek(Token.Type.INTEGER))
        {
            Ast.Expr.Literal integerLiteral = new Ast.Expr.Literal(new BigInteger(tokens.get(0).getLiteral()));
            match(Token.Type.INTEGER);
            return integerLiteral;
        }
        else if(peek(Token.Type.DECIMAL))
        {
            Ast.Expr.Literal decimalLiteral = new Ast.Expr.Literal(new BigDecimal(tokens.get(0).getLiteral()));
            match(Token.Type.DECIMAL);
            return decimalLiteral;
        }
        else if(peek(Token.Type.CHARACTER))
        {
            String replacement = tokens.get(0).getLiteral(); //Find and replace escape chars
            replacement = replacement.replaceAll("\'", "");
            replacement = replacement.replaceAll("\\\\b", "\b");
            replacement = replacement.replaceAll("\\\\n", "\n");
            replacement = replacement.replaceAll("\\\\r", "\r");
            replacement = replacement.replaceAll("\\\\t", "\t");
            replacement = replacement.replaceAll("\\\\\\\\", "\\\\");

            char toPass = replacement.charAt(0); //Java String to char cast
            Ast.Expr.Literal charLiteral = new Ast.Expr.Literal(new Character(toPass));

            match(Token.Type.CHARACTER);
            return charLiteral;
        }
        else if(peek(Token.Type.STRING))
        {
            String replacement = tokens.get(0).getLiteral();
            replacement = replacement.replaceAll("\"", "");
            replacement = replacement.replaceAll("\\\\b", "\b");
            replacement = replacement.replaceAll("\\\\n", "\n");
            replacement = replacement.replaceAll("\\\\r", "\r");
            replacement = replacement.replaceAll("\\\\t", "\t");
            replacement = replacement.replaceAll("\\\\\\\\", "\\\\");

            Ast.Expr.Literal stringLiteral = new Ast.Expr.Literal(new String(replacement));

            match(Token.Type.STRING);
            return stringLiteral;
        }
        else if(match("(")) //Grouped expression
        {
            //Should remove ( from the stack, leaving only the center expression to be parsed
            Ast.Expr centralExpression = parseExpression();

            if(match(")"))
            {
                 return new Ast.Expr.Group(centralExpression);
            }
            else
            {
                throw new ParseException("Unclosed group () at ", tokens.index);
            }

        }
        else if(peek(Token.Type.IDENTIFIER)) //Either identifier/variable or function case
        {
            //  identifier ('(' (expression (',' expression)*)? ')')?
            String identifierName = tokens.get(0).getLiteral();

            if(match(Token.Type.IDENTIFIER, "(")) //Function
            {
                List<Ast.Expr> paramsList = new ArrayList<Ast.Expr>();
                boolean firstpass = true;
                boolean hasPreComma = true;

                while(!match(")"))
                {
                    if(firstpass)
                        firstpass = false;
                    else
                        hasPreComma = match(",");

                    if(hasPreComma)
                    {
                        paramsList.add(parseExpression());
                        hasPreComma = false;
                    }
                    else
                        throw new ParseException("Invalid function call", tokens.index);

                }

                return new Ast.Expr.Function(Optional.empty(), identifierName, paramsList);
            }
            else //No open parenthesis, therefore, return identifier as access
            {
                match(Token.Type.IDENTIFIER);
                return new Ast.Expr.Access(Optional.empty(), identifierName);
            }
        }

        throw new ParseException("Unrecognized character/end of parsePrimary reached", tokens.index);
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++)
        {
            if (!tokens.has(i))
            {
                return false;
            }
            else if (patterns[i] instanceof Token.Type)
            {
                if(patterns[i] != tokens.get(i).getType())
                {
                    return false;
                }
            }
            else if (patterns[i] instanceof String)
            {
                if(!patterns[i].equals(tokens.get(i).getLiteral()))
                {
                    return false;
                }
            }
            else
            {
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns)
    {
        boolean peek = peek(patterns);

        if(peek)
        {
            for (int i = 0; i < patterns.length; i++)
            {
                tokens.advance();
            }
        }
        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}
