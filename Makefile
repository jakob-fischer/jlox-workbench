buildall: src/Lox.java src/Scanner.java src/Token.java src/TokenType.java
	javac -d bin -cp bin src/Lox.java src/Scanner.java src/Token.java src/TokenType.java

run: buildall
	java -classpath bin Lox $(file)

clean:
	rm -f bin/*.class