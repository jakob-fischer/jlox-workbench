src/Expr.java: src/generator/GenerateAst.java
	javac -d build -cp build src/generator/GenerateAst.java
	java -cp build GenerateAst src

buildall: src/Lox.java src/Scanner.java src/Token.java src/TokenType.java src/Expr.java
	javac -d bin -cp bin src/Lox.java src/Scanner.java src/Token.java src/TokenType.java src/Expr.java

run: buildall
	java -classpath bin Lox $(file)

clean:
	rm -f bin/*.class
	rm -f build/*.class
	rm -f src/Expr.java