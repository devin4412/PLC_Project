package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
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
        Environment.Function main = null;
        try
        {
            main = scope.lookupFunction("main", 0);
        }
        catch (Exception e) {
            throw new RuntimeException("Ast.Source is missing a main/0 function");
        }

        if(main.getReturnType().getName().compareTo("Integer") != 0)
            throw new RuntimeException("Ast.Source has main, but main is mising correct return type of Integer");

        List<Ast.Field> fieldsList = ast.getFields();
        for(Ast.Field field : fieldsList)
            visit(field);

        List<Ast.Method> methodsList = ast.getMethods();
        for(Ast.Method method : methodsList)
            visit(method);

        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        String typeName = ast.getTypeName();

        if(ast.getValue().isPresent()) //Initialized to a value
        {
            visit(ast.getValue().get());

            Environment.Type type = ast.getValue().get().getType();

            requireAssignable(Environment.getType(typeName), type); //Same method as requireAssignable, the first has just been changed to a string because it's way easier
        }

        Environment.Variable var = scope.defineVariable(ast.getName(), ast.getName(), Environment.getType(typeName), Environment.NIL);
        ast.setVariable(var);

        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        String returnType = null;
        if(ast.getReturnTypeName().isPresent())
        {
            returnType = ast.getReturnTypeName().get();
        }

        List<String> typeNames =  ast.getParameterTypeNames();
        List<String> parameters = ast.getParameters();
        List<Environment.Type> paramTypes = new ArrayList<Environment.Type>();
        for(String type : typeNames)
            paramTypes.add(Environment.getType(type));

        ast.setFunction(scope.defineFunction(ast.getName(), ast.getName(), paramTypes,Environment.getType(returnType), args -> Environment.NIL));

        method = ast; //Coordinating with the return node
        scope = new Scope(scope); //New Scope

        for(int i = 0; i < parameters.size(); i++) //Defining parameters as variables
            scope.defineVariable(parameters.get(i), parameters.get(i), Environment.getType(typeNames.get(i)), Environment.NIL);

        List<Ast.Stmt> stmtList = ast.getStatements();
        for(Ast.Stmt statement : stmtList)
            visit(statement);

        scope = scope.getParent(); //exiting method scope
        method = null;
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        visit(ast.getExpression());

        Ast.Expr expression = ast.getExpression();

        if(!(expression instanceof Ast.Expr.Function))
            throw new RuntimeException("Expression is not a function");

        return null;
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
        visit(ast.getReceiver());
        visit(ast.getValue());

        if(!(ast.getReceiver() instanceof Ast.Expr.Access))
            throw new RuntimeException("Receiver is not an access expression");

        requireAssignable(ast.getReceiver().getType(), ast.getValue().getType());

        return null;
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
        visit(ast.getValue());
        Ast.Expr condition = ast.getValue();

        List<Ast.Stmt> stmtList = ast.getStatements();

        if(condition.getType().getName().compareTo("IntegerIterable") != 0)
            throw new RuntimeException("Value is not of type IntegerIterable");

        if(stmtList.isEmpty())
            throw new RuntimeException("For loop has no body");

        Scope forScope = new Scope(scope);
        forScope.defineVariable(ast.getName(), ast.getName(), Environment.Type.INTEGER, Environment.NIL);

        for(Ast.Stmt stmt : stmtList)
            visit(stmt);

        scope = forScope.getParent();

        return null;
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
        String typename = method.getReturnTypeName().get();

        visit(ast.getValue());
        Ast.Expr val = ast.getValue();

        requireAssignable(Environment.getType(typename), val.getType());

        return null;
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
        visit(ast.getExpression());

        Ast.Expr expression = ast.getExpression();

        if(!(expression instanceof Ast.Expr.Binary))
            throw new RuntimeException("Grouped expression is not Binary");

        ast.setType(ast.getExpression().getType());

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) {

        String op = ast.getOperator();

        visit(ast.getLeft());
        visit(ast.getRight());

        Ast.Expr lhs = ast.getLeft();
        Ast.Expr rhs = ast.getRight();

        Environment.Type lhsType = lhs.getType();
        Environment.Type rhsType = rhs.getType();

        if(op.equals("AND") || op.equals("OR"))
        {
            //Both of the operands and the result must be boolean
            requireAssignable(lhsType, Environment.Type.BOOLEAN);
            requireAssignable(rhsType, Environment.Type.BOOLEAN);
            //LHS and RHS are now sure to be boolean

            ast.setType(Environment.Type.BOOLEAN);
        }
        else if (op.equals("<") ||
                op.equals("<=") ||
                op.equals(">") ||
                op.equals(">=") ||
                op.equals("==") ||
                op.equals("!=")) {

            //Both comparable and SAME TYPE
            //Result is boolean

            requireAssignable(lhsType, Environment.Type.COMPARABLE);
            requireAssignable(rhsType, Environment.Type.COMPARABLE);

            if(lhsType.getName().compareTo(rhsType.getName()) != 0)
                throw new RuntimeException("Mismatched comparable types in binary expression");

            ast.setType(Environment.Type.BOOLEAN);
        }
        else if (op.equals("+"))
        {
            //If either side is string, concatenate
            //Otherwise, left must be Int/Dec, whatever it is the right side and result must be the same

            if(lhsType.getName().equals("String") || rhsType.getName().equals("String"))
            {
                ast.setType(Environment.Type.STRING);
            }
            else //Not concatenation
            {
                if(lhsType.getName().equals("Integer"))
                {
                    if(rhsType.getName().compareTo("Integer") != 0)
                        throw new RuntimeException("lhs is integer, rhs is not");

                    ast.setType(Environment.Type.INTEGER);
                }
                else if(lhsType.getName().equals("Decimal"))
                {
                    if(rhsType.getName().compareTo("Decimal") != 0)
                        throw new RuntimeException("lhs is decimal, rhs is not");

                    ast.setType(Environment.Type.DECIMAL);
                }
                else
                    throw new RuntimeException("Unexpected type in addition statement");
            }
        }
        else if (op.equals("-") ||
                op.equals("*") ||
                op.equals("/"))
        {
            //Same as above without concatenation

            if(lhsType.getName().compareTo("Integer") == 0)
            {
                if(rhsType.getName().compareTo("Integer") != 0)
                    throw new RuntimeException("lhs is integer, rhs is not");

                ast.setType(Environment.Type.INTEGER);
            }
            else if(lhsType.getName().equals("Decimal"))
            {
                if(rhsType.getName().compareTo("Decimal") != 0)
                    throw new RuntimeException("lhs is decimal, rhs is not");

                ast.setType(Environment.Type.DECIMAL);
            }
            else
                throw new RuntimeException("Unexpected type in binary statement");
        }
        else
        {
            throw new RuntimeException("Unrecognized operator in Ast.Expr.Binary");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {

        if(ast.getReceiver().isPresent()) //Receiver is present (object.**receiver**)
        {
            visit(ast.getReceiver().get()); //Visit it to evaluate. Will probably bring you back right here?
            Ast.Expr received = ast.getReceiver().get(); //Object
            //must be access
            if(!(received instanceof Ast.Expr.Access))
                throw new RuntimeException("Attempting to access unaccessible field");

            Scope objScope = received.getType().getScope(); //Found it

            ast.setVariable(objScope.lookupVariable(ast.getName()));
        }
        else //No receiver, just a field case
        {
            ast.setVariable(scope.lookupVariable(ast.getName())); //"otherwise it is a variable in the current scope"
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        List<Ast.Expr> argList = ast.getArguments();
        for(Ast.Expr expr : argList) //give arguments types
            visit(expr);

        if(ast.getReceiver().isPresent()) //Object.function()
        {
            visit(ast.getReceiver().get());
            Ast.Expr receiver = ast.getReceiver().get();

            if(!(receiver instanceof Ast.Expr.Access))
                throw new RuntimeException("Attempting to access unaccessible function");

            Ast.Expr.Access received = (Ast.Expr.Access)receiver;

            Scope objScope = received.getType().getScope();

            ast.setFunction(objScope.lookupFunction(ast.getName(), argList.size() + 1)); //Accounts for the IMPORTANT note
        }
        else //function()
        {
            ast.setFunction(scope.lookupFunction(ast.getName(), argList.size()));

            Environment.Function func = ast.getFunction();
            List<Environment.Type> parameterTypesList = func.getParameterTypes();

            for(int i = 1; i < parameterTypesList.size(); i++)
                requireAssignable(argList.get(i-1).getType(), parameterTypesList.get(i));
        }

        return null;
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
