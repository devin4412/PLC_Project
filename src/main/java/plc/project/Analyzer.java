package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Method method;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Field ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Method ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {

        if(!ast.getTypeName().isPresent() && !ast.getValue().isPresent())
            throw new RuntimeException("Declaration must have type or value to infer type.");

        Environment.Type type = null;

        if (ast.getTypeName().isPresent())
            type = Environment.getType(ast.getTypeName().get());

        if(ast.getValue().isPresent())
        {
            visit(ast.getValue().get());

            if(type == null)
                type = ast.getValue().get().getType();

            requireAssignable(type, ast.getValue().get().getType());
        }

        ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), type, Environment.NIL));

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {

        visit(ast.getCondition());

        requireAssignable(ast.getCondition().getType(), Environment.Type.BOOLEAN);

        List<Ast.Stmt> thenStatements = ast.getThenStatements();
        List<Ast.Stmt> elseStatements = ast.getElseStatements();

        if(thenStatements.isEmpty())
            throw new RuntimeException("IF statement is missing body after DO (No then statements)");

        Scope thenScope = new Scope(scope); //Create new scope for then
        for(Ast.Stmt statement : thenStatements) //Do then
            visit(statement);

        scope = thenScope.getParent(); //Escape thenscope

        if(!elseStatements.isEmpty()) //Else exists
        {
            Scope elseScope = new Scope(scope); //Else scope entry
            for(Ast.Stmt statement : elseStatements) //Do else
                visit(statement);
            scope = elseScope.getParent(); //Else scope exit
        }

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Stmt.While ast)
    {
        // Condition may very well be larger than just TRUE, i.e. X AND Y evaluates to a boolean. Which would be valid.
        // Therefore, I need to evaluate the condition first by visiting it.

        visit(ast.getCondition());

        //By visiting this condition, it will evaluate it to its simplest form and assign it a type.
        //If I didn't visit this condition, then While would get stuck at the start, since an expression is
        //Untyped until visited.

        requireAssignable(ast.getCondition().getType(), Environment.Type.BOOLEAN);

        //I can still call ast.getCond.getType()
        //Because it has now FOR SURE been visited.

        List<Ast.Stmt> whileBody = ast.getStatements();

        Scope whileScope = new Scope(scope);

        for(Ast.Stmt statement : whileBody)
        {
            visit(statement);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Return ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expr.Literal ast) {
        Object literal = ast.getLiteral();

        if (literal instanceof Boolean)
        {
            ast.setType(Environment.Type.BOOLEAN);
        }
        else if(literal instanceof Character)
        {
            ast.setType(Environment.Type.CHARACTER);
        }
        else if(literal instanceof String)
        {
            ast.setType(Environment.Type.STRING);
        }
        else if(literal instanceof BigInteger)
        {
            BigInteger literalInt = (BigInteger)literal;
            if(literalInt.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0
                    || literalInt.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0)

            {
                throw new RuntimeException("Integer value exceeds given bounds");
            }
            //else
            ast.setType(Environment.Type.INTEGER);
        }
        else if(literal instanceof BigDecimal)
        {
            BigDecimal literalDec = (BigDecimal)literal;

            Double literalDub = literalDec.doubleValue();

            Double negInf = Double.NEGATIVE_INFINITY;
            Double posInf = Double.POSITIVE_INFINITY;

            if(literalDub.equals(negInf) || literalDub.equals(posInf))
            {
                throw new RuntimeException("Double value exceeds given bounds");
            }

            ast.setType(Environment.Type.DECIMAL);
        }
        else //literal.equals(null)
        {
            ast.setType(Environment.Type.NIL);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) {

        String op = ast.getOperator();

        if (op.equals("AND") || op.equals("OR"))
        {
            //Both of the operands and the result must be boolean
        }
        else if (op.equals("<") ||
                op.equals("<=") ||
                op.equals(">") ||
                op.equals(">=") ||
                op.equals("==") ||
                op.equals("!=")) {

            //Both comparable and SAME TYPE
            //Result is boolean

        }
        else if (op.equals("+"))
        {
            //If either side is string, concatenate
            //Otherwise, left must be Int/Dec, whatever it is the right side and result must be the same
        }
        else if (op.equals("-") ||
                op.equals("*") ||
                op.equals("/"))
        {
            //Same as above without concatenation
        }
        else
        {
            throw new RuntimeException("Unrecognized operator in Ast.Expr.Binary");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {

        if(!(target.getName().equals(type.getName()))) //If target does not equal type
        {
            if(!(target.getName().equals("Any"))) //Not Any (Object) type
            {
                if(target.getName().equals("Comparable")) //Is it Comparable? if not throw exception
                {
                    if(!(type.getName().equals("Integer"))
                            && !(type.getName().equals("Decimal"))
                            && !(type.getName().equals("Character"))
                            && !(type.getName().equals("String"))) //If it's not any of these Comparable types.
                    {
                        throw new RuntimeException("Target is comparable, yet given type is not included in Comparable types");
                    }
                }
                else
                    throw new RuntimeException("Target type cannot be matched to given type in requireAssignable()");

            }
        }
    }

}
