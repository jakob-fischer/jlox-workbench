all: Lox.class

bin/Lox.class: src/Lox.java
	javac -d bin -cp bin src/Lox.java


run: bin/Lox.class
	java -classpath bin Lox $(file)

clean:
	rm -f bin/*.class