package plc.project;

import java.io.PrintWriter;
import java.util.List;
import java.math.BigDecimal;
import java.math.BigInteger;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        // create a "class Main {"
        //     declare fields
        //     declare "public static void main(String[] args) {
        //          System.exit(new Main().main());
        //      }
        //      declare each of our methods
        //      one of our methods is called main()!
        // print "}" to close the class Main
        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        print(ast.getVariable().getType().getJvmName(), " ", ast.getVariable().getJvmName());

        if(ast.getValue().isPresent())
            print(" = ", ast.getValue().get());

        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {

        //     return type JVM name                        space       function name              open parenthesis
        print(ast.getFunction().getReturnType().getJvmName(), " ", ast.getFunction().getJvmName(), " (");

        List<String> paramList = ast.getParameters();
        List<Environment.Type> paramTypeList = ast.getFunction().getParameterTypes();

        for(int i = 0; i < paramTypeList.size(); i++)
        {
            if(i != paramTypeList.size() - 1) //Not the final parameter case
                print(paramTypeList.get(i).getJvmName(), " ", paramList.get(i), ", ");
            else //final parameter case
                print(paramTypeList.get(i).getJvmName(), " ", paramList.get(i));
        }
        print(") {"); //close your parameter parenthesis, then open your bracket

        List<Ast.Stmt> stmtList = ast.getStatements();

        if(stmtList.size() == 0)
            print("}"); //No statements, so no indents or newlines
        else
        {
            indent++; //create new indent for new scope
            for(int i = 0; i < stmtList.size(); i++)
            {
                newline(indent); //indented above
                print(stmtList.get(i)); //visit ast.stmt
            }
            newline(--indent); //return to original indent
            print("}");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        print(ast.getExpression(), ";");

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {
        //"LET x: Integer = 3;"
        //write: TYPE variable_name

        print(ast.getVariable().getType().getJvmName(), " ", ast.getVariable().getJvmName());

        //is there an assigned value?
        //if so, write: an = and then the value

        if(ast.getValue().isPresent())
        {
            print(" = ", ast.getValue().get());
        }

        //write: ;

        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {
        throw new UnsupportedOperationException(); //TODO
        //return null;
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {
        throw new UnsupportedOperationException(); //TODO
        //return null;
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
        print("for (int ", ast.getName(), " : ", ast.getValue(), " {");


        return null;
    }

    @Override
    public Void visit(Ast.Stmt.While ast) {
        print("while (", ast.getCondition(), ") {");

        if(!ast.getStatements().isEmpty())
        {
            newline(++indent);

            for (int i = 0; i < ast.getStatements().size(); i++)
            {
                if(i != 0)
                {
                    newline(indent);
                }
                
                print(ast.getStatements().get(i));
            }
            newline(--indent);
        }

        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Return ast) {
       print("return ", ast.getValue(), ";");
       return null;
    }

    @Override
    public Void visit(Ast.Expr.Literal ast) {
        Environment.Type type = ast.getType();

        if (type.equals(Environment.Type.STRING))
        {
            print("\"", (String)ast.getLiteral(), "\"");
        }
        else if (type.equals(Environment.Type.CHARACTER))
        {
            print("\'", (Character)ast.getLiteral(), "\'");
        }
        else if (type.equals(Environment.Type.BOOLEAN))
        {
            Boolean object = (Boolean)ast.getLiteral();
            if(object)
                print("true");
            else
                print("false");
        }
        else if (type.equals(Environment.Type.DECIMAL))
        {
            BigDecimal object = (BigDecimal)ast.getLiteral();
            print(object.toString());
        }
        else if (type.equals(Environment.Type.INTEGER))
        {
            BigInteger object = (BigInteger)ast.getLiteral();
            print(object.toString());
        }
        else if (type.equals(Environment.Type.NIL))
        {
            print("null");
        }
        else
        {
            print(ast.getLiteral());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        print("(", ast.getExpression(), ")");
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) {
        String op = ast.getOperator();
        Ast.Expr lhs = ast.getLeft();
        Ast.Expr rhs = ast.getRight();

        if(op.equals("AND"))
        {
            print(lhs, " && ", rhs);
        }
        else if (op.equals("OR"))
        {
            print(lhs, " || " , rhs);
        }
        else
        {
            print(lhs, " ", op, " ", rhs);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        String var = ast.getVariable().getJvmName();

        if(ast.getReceiver().isPresent())
        {
            print(ast.getReceiver().get(), ".", var);
        }
        else
            print(var);
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        if(ast.getReceiver().isPresent())
        {
            print(ast.getReceiver().get()
                    , "."
                    , ast.getFunction().getJvmName()
                    , "(");
        }
        else
        {
            print(ast.getFunction().getJvmName(), "(");
        }

        List<Ast.Expr> arguments = ast.getArguments();

        for(int i = 0; i < arguments.size(); i++)
        {
            if(i != arguments.size() - 1) //not last case
                print(arguments.get(i), ", ");
            else
                print(arguments.get(i));
        }

        print(")");

        return null;
    }

}
