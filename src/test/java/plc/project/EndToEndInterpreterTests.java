package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

final class EndToEndInterpreterTests {

    @ParameterizedTest
    @MethodSource
    void testSource(String test, String input, Object expected) {
        test(input, expected, new Scope(null), Parser::parseSource);
    }

    private static Stream<Arguments> testSource() {
        return Stream.of(
                Arguments.of("Main",
                        "DEF main() DO\n    RETURN 0;\nEND",
                        BigInteger.ZERO
                ),
                Arguments.of("Fields & No Return",
                        "LET x: Integer = 1;\nLET y: Integer = 10;\nDEF main() DO\n    x + y;\nEND",
                        Environment.NIL.getValue()
                )
        );
    }

    @ParameterizedTest
    @MethodSource
//  void testField(String test, Ast.Field ast, Object expected) {
    void testField(String test, String input, Object expected, String variableName) {
//      Scope scope = test(ast, Environment.NIL.getValue(), new Scope(null));
        Scope scope = test(input, Environment.NIL.getValue(), new Scope(null), Parser::parseField);
        Assertions.assertEquals(expected, scope.lookupVariable(variableName).getValue().getValue());
    }

    private static Stream<Arguments> testField() {
        return Stream.of(
                Arguments.of("Declaration", "LET name: Integer;", Environment.NIL.getValue(), "name"),
                Arguments.of("Initialization", "LET name: Integer = 1;", BigInteger.ONE, "name")
        );
    }

    @ParameterizedTest
    @MethodSource
    void testMethod(String test, String input, List<Environment.PlcObject> args, Object expected, String functionName) {
        Scope scope = test(input, Environment.NIL.getValue(), new Scope(null), Parser::parseMethod);
        Assertions.assertEquals(expected, scope.lookupFunction(functionName, args.size()).invoke(args).getValue());
    }

    private static Stream<Arguments> testMethod() {
        return Stream.of(
                Arguments.of("Main",
                        "DEF main(): Integer DO\n    RETURN 0;\nEND",
                        Arrays.asList(),
                        BigInteger.ZERO,
                        "main"
                ),
                Arguments.of("Arguments",
                        "DEF square(x: Integer): Integer DO\n    RETURN x * x;\nEND",
                        Arrays.asList(Environment.create(BigInteger.TEN)),
                        BigInteger.valueOf(100),
                        "square"
                )
        );
    }

    @Test
    void testExpressionStatement() {
        PrintStream sysout = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        try {
            test("print(\"Hello, World!\");", Environment.NIL.getValue(), new Scope(null), Parser::parseStatement);
            Assertions.assertEquals("Hello, World!" + System.lineSeparator(), out.toString());
        } finally {
            System.setOut(sysout);
        }
    }

    @ParameterizedTest
    @MethodSource
    void testDeclarationStatement(String test, String input, Object expected, String variableName) {
        Scope scope = test(input, Environment.NIL.getValue(), new Scope(null), Parser::parseStatement);
        Assertions.assertEquals(expected, scope.lookupVariable(variableName).getValue().getValue());
    }

    private static Stream<Arguments> testDeclarationStatement() {
        return Stream.of(
                Arguments.of("Declaration",
                        "LET name;",
                        Environment.NIL.getValue(),
                        "name"
                ),
                Arguments.of("Initialization",
                        "LET name = 1;",
                        BigInteger.ONE,
                        "name"
                )
        );
    }

    @Test
    void testVariableAssignmentStatement() {
        Scope scope = new Scope(null);
        scope.defineVariable("variable", Environment.create("variable"));
        test("variable = 1;", Environment.NIL.getValue(), scope, Parser::parseStatement);
        Assertions.assertEquals(BigInteger.ONE, scope.lookupVariable("variable").getValue().getValue());
    }

    @Test
    void testFieldAssignmentStatement() {
        Scope scope = new Scope(null);
        Scope object = new Scope(null);
        object.defineVariable("field", Environment.create("object.field"));
        scope.defineVariable("object", new Environment.PlcObject(object, "object"));
        test("object.field = 1;", Environment.NIL.getValue(), scope, Parser::parseStatement);
        Assertions.assertEquals(BigInteger.ONE, object.lookupVariable("field").getValue().getValue());
    }

    @ParameterizedTest
    @MethodSource
    void testIfStatement(String test, String input, Object expected) {
        Scope scope = new Scope(null);
        scope.defineVariable("num", Environment.NIL);
        test(input, Environment.NIL.getValue(), scope, Parser::parseStatement);
        Assertions.assertEquals(expected, scope.lookupVariable("num").getValue().getValue());
    }

    private static Stream<Arguments> testIfStatement() {
        return Stream.of(
                Arguments.of("True Condition",
                        "IF TRUE DO\n    num = 1;\nEND",
                        BigInteger.ONE
                ),
                Arguments.of("False Condition",
                        "IF FALSE DO\nELSE\n    num = 10;\nEND",
                        BigInteger.TEN
                )
        );
    }

    @Test
    void testForStatement() {
        Scope scope = new Scope(null);
        scope.defineVariable("sum", Environment.create(BigInteger.ZERO));
        scope.defineVariable("list", Environment.create(IntStream.range(0, 5)
                .mapToObj(i -> Environment.create(BigInteger.valueOf(i)))
                .collect(Collectors.toList())));
        test("FOR num IN list DO\n    sum = sum + num;\nEND", Environment.NIL.getValue(), scope, Parser::parseStatement);
        Assertions.assertEquals(BigInteger.TEN, scope.lookupVariable("sum").getValue().getValue());
    }

    @Test
    void testWhileStatement() {
        Scope scope = new Scope(null);
        scope.defineVariable("num", Environment.create(BigInteger.ZERO));
        test("WHILE num < 10 DO\n    num = num + 1;\nEND",Environment.NIL.getValue(), scope, Parser::parseStatement);
        Assertions.assertEquals(BigInteger.TEN, scope.lookupVariable("num").getValue().getValue());
    }

    @ParameterizedTest
    @MethodSource
    void testLiteralExpression(String test, String input, Object expected) {
        test(input, expected, new Scope(null), Parser::parseExpression);
    }

    private static Stream<Arguments> testLiteralExpression() {
        return Stream.of(
                Arguments.of("Nil", "NIL", Environment.NIL.getValue()), //remember, special case
                Arguments.of("Boolean", "TRUE", true),
                Arguments.of("Integer", "1", BigInteger.ONE),
                Arguments.of("Decimal", "1.0", new BigDecimal("1.0")),
                Arguments.of("Character", "'c'", 'c'),
                Arguments.of("String", "\"string\"", "string")
        );
    }

    @ParameterizedTest
    @MethodSource
    void testGroupExpression(String test, String input, Object expected) {
        test(input, expected, new Scope(null), Parser::parseExpression);
    }

    private static Stream<Arguments> testGroupExpression() {
        return Stream.of(
                Arguments.of("Literal", "(1)", BigInteger.ONE),
                Arguments.of("Binary",
                        "(1 + 10)",
                        BigInteger.valueOf(11)
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testBinaryExpression(String test, String input, Object expected) {
        test(input, expected, new Scope(null), Parser::parseExpression);
    }

    private static Stream<Arguments> testBinaryExpression() {
        return Stream.of(
                Arguments.of("And",
                        "TRUE AND FALSE",
                        false
                ),
                Arguments.of("Or (Short Circuit)",
                        "TRUE OR undefined",
                        true
                ),
                Arguments.of("Less Than",
                        "1 < 10",
                        true
                ),
                Arguments.of("Greater Than or Equal",
                        "1 >= 10",
                        false
                ),
                Arguments.of("Equal",
                        "1 == 10",
                        false
                ),
                Arguments.of("Concatenation",
                        "\"a\" + \"b\"",
                        "ab"
                ),
                Arguments.of("Addition",
                        "1 + 10",
                        BigInteger.valueOf(11)
                ),
                Arguments.of("Division",
                        "1.2 / 3.4",
                        new BigDecimal("0.4")
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAccessExpression(String test, String input, Object expected) {
        Scope scope = new Scope(null);
        scope.defineVariable("variable", Environment.create("variable"));
        Scope object = new Scope(null);
        object.defineVariable("field", Environment.create("object.field"));
        scope.defineVariable("object", new Environment.PlcObject(object, "object"));
        test(input, expected, scope, Parser::parseExpression);
    }

    private static Stream<Arguments> testAccessExpression() {
        return Stream.of(
                Arguments.of("Variable",
                        "variable",
                        "variable"
                ),
                Arguments.of("Field",
                        "object.field",
                        "object.field"
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testFunctionExpression(String test, String input, Object expected) {
        Scope scope = new Scope(null);
        scope.defineFunction("function", 0, args -> Environment.create("function"));
        Scope object = new Scope(null);
        object.defineFunction("method", 1, args -> Environment.create("object.method"));
        scope.defineVariable("object", new Environment.PlcObject(object, "object"));
        test(input, expected, scope, Parser::parseExpression);
    }

    private static Stream<Arguments> testFunctionExpression() {
        return Stream.of(
                Arguments.of("Function",
                        "function()",
                        "function"
                ),
                Arguments.of("Method",
                        "object.method()",
                        "object.method"
                ),
                Arguments.of("Print",
                        "print(\"Hello, World!\")",
                        Environment.NIL.getValue()
                )
        );
    }

    private static <T extends Ast> Scope test(String input, Object expected, Scope scope, Function<Parser, T> function) {
        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer.lex());

        Ast ast = function.apply(parser);

        Interpreter interpreter = new Interpreter(scope);
        if (expected != null) {
            Assertions.assertEquals(expected, interpreter.visit(ast).getValue());
        } else {
            Assertions.assertThrows(RuntimeException.class, () -> interpreter.visit(ast));
        }
        return interpreter.getScope();
    }

}
