package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Field ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Method ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Expression ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Declaration ast)
    {
        if(ast.getValue().isPresent()) //Has initial value
        {
            scope.defineVariable(ast.getName(), visit(ast.getValue().get()));
        }
        else //isPresent() == false, NIL initial value
        {
            scope.defineVariable(ast.getName(), Environment.NIL);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Assignment ast) {
       // requireType(Ast.Expr.Access.class, visit(ast.getReceiver()))
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.If ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.For ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.While ast)
    {
        while(requireType(Boolean.class, visit(ast.getCondition())))
        {
            try {
                scope = new Scope(scope); //New scope created for while loop
                for (Ast.Stmt stmt : ast.getStatements())
                {
                    visit(stmt);
                }
            } finally {
                scope = scope.getParent(); //Escaping back up to outside of while scope
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Return ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Literal ast) {
        if(ast.getLiteral() == null) {
            return Environment.NIL;
        }
        else {
            return Environment.create(ast.getLiteral());
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Group ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Binary ast)
    {
        String operator = ast.getOperator();

        if(operator.equals("AND"))
        {
            boolean lhs = requireType(Boolean.class, visit(ast.getLeft()));

            if(!lhs) //Short circuit rules say if the left is false then AND must return false
                return Environment.create(false);
            //Otherwise, use the logical and on two booleans and return it. requireType throws the error in both cases for invalid args
            return Environment.create(lhs && requireType(Boolean.class, visit(ast.getRight())));
        }
        else if(operator.equals("OR")) //OR is the same but Truthy instead of falsey
        {
            boolean lhs = requireType(Boolean.class, visit(ast.getLeft()));
            if(lhs)
                return Environment.create(true);
            return Environment.create(lhs || requireType(Boolean.class, visit(ast.getRight())));
        }
        else if(operator.equals("<"))
        {
            Comparable lhs = requireType(Comparable.class, visit(ast.getLeft()));
            Comparable rhs = requireType(Comparable.class, visit(ast.getRight()));

            if(lhs.compareTo(rhs) < 0)
                return Environment.create(true);
            return Environment.create(false);
        }
        else if(operator.equals("<="))
        {
            Comparable lhs = requireType(Comparable.class, visit(ast.getLeft()));
            Comparable rhs = requireType(Comparable.class, visit(ast.getRight()));

            if(lhs.compareTo(rhs) <= 0)
                return Environment.create(true);
            return Environment.create(false);
        }
        else if(operator.equals(">"))
        {
            Comparable lhs = requireType(Comparable.class, visit(ast.getLeft()));
            Comparable rhs = requireType(Comparable.class, visit(ast.getRight()));

            if(lhs.compareTo(rhs) > 0)
                return Environment.create(true);
            return Environment.create(false);
        }
        else if(operator.equals(">="))
        {
            Comparable lhs = requireType(Comparable.class, visit(ast.getLeft()));
            Comparable rhs = requireType(Comparable.class, visit(ast.getRight()));

            if(lhs.compareTo(rhs) >= 0)
                return Environment.create(true);
            return Environment.create(false);
        }
        else if(operator.equals("=="))
        {
            Object lhs = requireType(Object.class, visit(ast.getLeft()));
            Object rhs = requireType(Object.class, visit(ast.getRight()));

            if(lhs.equals(rhs))
                return Environment.create(true);
            return Environment.create(false);
        }
        else if(operator.equals("!="))
        {
            Object lhs = requireType(Object.class, visit(ast.getLeft()));
            Object rhs = requireType(Object.class, visit(ast.getRight()));

            if(lhs.equals(rhs))
                return Environment.create(false);
            return Environment.create(true);
        }
        else if(operator.equals("+"))
        {
           Object lhs = requireType(Object.class, visit(ast.getLeft()));
           Object rhs = requireType(Object.class, visit(ast.getRight()));

            if(lhs instanceof String || rhs instanceof String)
            {
                String Left = (String) lhs;
                String Right = (String) rhs;

                return Environment.create(Left.concat(Right));
            }
            else if(lhs instanceof BigInteger)
            {
                if(!(rhs instanceof BigInteger))
                {
                    throw new RuntimeException("Expected BigInteger type on rhs of addition operation, instead received: " + rhs.toString()); //TODO this might not work how you think
                }
                else
                {
                    BigInteger Left = (BigInteger)lhs;
                    BigInteger Right = (BigInteger)rhs;
                    return Environment.create(Left.add(Right));
                }
            }
            else if(lhs instanceof BigDecimal)
            {
                if(!(rhs instanceof BigDecimal))
                {
                    throw new RuntimeException("Expected BigDecimal type on rhs of addition operation, instead received: " + rhs.toString()); //TODO this might not work how you think
                }
                else
                {
                    BigDecimal Left = (BigDecimal)lhs;
                    BigDecimal Right = (BigDecimal)rhs;
                    return Environment.create(Left.add(Right));
                }
            }
            else
            {
                throw new RuntimeException("Add: Expected String, or matching BigDecimal/BigInteger types, instead received: " +
                        "\nLHS: " + lhs.getClass().toString()
                    +   "\nRHS: " + rhs.getClass().toString());
            }
        }
        else if(operator.equals("-"))
        {
            Object lhs = requireType(Object.class, visit(ast.getLeft()));
            Object rhs = requireType(Object.class, visit(ast.getRight()));

            if(lhs instanceof BigInteger)
            {
                if(!(rhs instanceof BigInteger))
                {
                    throw new RuntimeException("Expected BigInteger type on rhs of subtraction operation, instead received: " + rhs.toString()); //TODO this might not work how you think
                }
                else
                {
                    BigInteger Left = (BigInteger)lhs;
                    BigInteger Right = (BigInteger)rhs;
                    return Environment.create(Left.subtract(Right));
                }
            }
            else if(lhs instanceof BigDecimal)
            {
                if(!(rhs instanceof BigDecimal))
                {
                    throw new RuntimeException("Expected BigDecimal type on rhs of subtraction operation, instead received: " + rhs.toString()); //TODO this might not work how you think
                }
                else
                {
                    BigDecimal Left = (BigDecimal)lhs;
                    BigDecimal Right = (BigDecimal)rhs;
                    return Environment.create(Left.subtract(Right));
                }
            }
            else
            {
                throw new RuntimeException("Subtract: Expected matching BigDecimal/BigInteger types, instead received: " +
                        "\nLHS: " + lhs.getClass().toString()
                        +   "\nRHS: " + rhs.getClass().toString());
            }
        }
        else if(operator.equals("*"))
        {
            Object lhs = requireType(Object.class, visit(ast.getLeft()));
            Object rhs = requireType(Object.class, visit(ast.getRight()));

            if(lhs instanceof BigInteger)
            {
                if(!(rhs instanceof BigInteger))
                {
                    throw new RuntimeException("Expected BigInteger type on rhs of multiplication operation, instead received: " + rhs.toString()); //TODO this might not work how you think
                }
                else
                {
                    BigInteger Left = (BigInteger)lhs;
                    BigInteger Right = (BigInteger)rhs;
                    return Environment.create(Left.multiply(Right));
                }
            }
            else if(lhs instanceof BigDecimal)
            {
                if(!(rhs instanceof BigDecimal))
                {
                    throw new RuntimeException("Expected BigDecimal type on rhs of multiplication operation, instead received: " + rhs.toString()); //TODO this might not work how you think
                }
                else
                {
                    BigDecimal Left = (BigDecimal)lhs;
                    BigDecimal Right = (BigDecimal)rhs;
                    return Environment.create(Left.multiply(Right));
                }
            }
            else
            {
                throw new RuntimeException("Multiply: Expected matching BigDecimal/BigInteger types, instead received: " +
                        "\nLHS: " + lhs.getClass().toString()
                        +   "\nRHS: " + rhs.getClass().toString());
            }
        }
        else if(operator.equals("/"))
        {
            //.setScale(RoundingMode.HALF_EVEN)
            Object lhs = requireType(Object.class, visit(ast.getLeft()));
            Object rhs = requireType(Object.class, visit(ast.getRight()));

            if(lhs instanceof BigInteger)
            {
                if(!(rhs instanceof BigInteger))
                {
                    throw new RuntimeException("Expected BigInteger type on rhs of division operation, instead received: " + rhs.toString()); //TODO this might not work how you think
                }
                else
                {
                    BigInteger Left = (BigInteger)lhs;
                    BigInteger Right = (BigInteger)rhs;

                    if(Right.equals(BigDecimal.ZERO))
                        throw new RuntimeException("Divide by zero");

                    return Environment.create(Left.divide(Right));
                }
            }
            else if(lhs instanceof BigDecimal)
            {
                if(!(rhs instanceof BigDecimal))
                {
                    throw new RuntimeException("Expected BigDecimal type on rhs of division operation, instead received: " + rhs.toString()); //TODO this might not work how you think
                }
                else
                {
                    BigDecimal Left = (BigDecimal)lhs;
                    BigDecimal Right = (BigDecimal)rhs;

                    if(Right.equals(BigDecimal.ZERO))
                        throw new RuntimeException("Divide by zero");

                    return Environment.create(Left.divide(Right, RoundingMode.HALF_EVEN));
                }
            }
            else
            {
                throw new RuntimeException("Divide: Expected matching BigDecimal/BigInteger types, instead received: " +
                        "\nLHS: " + lhs.getClass().toString()
                        +   "\nRHS: " + rhs.getClass().toString());
            }
        }

        //Unreachable
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Access ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Function ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
