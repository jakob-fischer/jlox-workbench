buildall: src/Lox.java src/Scanner.java src/Token.java src/TokenType.java src/Expr.java src/Stmt.java src/Parser.java src/AstPrinter.java src/Interpreter.java src/RuntimeError.java src/Environment.java src/LoxCallable.java src/LoxFunction.java src/Return.java
	javac -d bin -cp bin src/Lox.java src/Scanner.java src/Token.java src/TokenType.java src/Expr.java src/Stmt.java src/Parser.java src/AstPrinter.java src/Interpreter.java src/RuntimeError.java src/Environment.java src/LoxCallable.java src/LoxFunction.java src/Return.java

src/Expr.java: src/generator/GenerateAst.java
	javac -d build -cp build src/generator/GenerateAst.java
	java -cp build GenerateAst src

run: buildall
	java -classpath bin Lox $(file)

clean:
	rm -f bin/*.class
	rm -f build/*.class
	rm -f src/Expr.java
	rm -f src/Stmt.java