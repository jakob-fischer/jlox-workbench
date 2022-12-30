import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

class Parser {
    private static class ParseError extends RuntimeException {}
    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
    
        return statements; 
    }

    Expr parseExpression() {
        try {
            return expression();
        } catch (ParseError error) {
            return null;
        }
    }

    private Stmt declaration() {
        try {
            if (match(TokenType.Fun)) return function("function");
            if (match(TokenType.Var)) return varDeclaration();
    
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt.Function function(String kind) {
        Token name = consume(TokenType.Identifier, "Expect " + kind + " name.");

        consume(TokenType.LeftParen, "Expect '(' after " + kind + " name.");
        List<Token> parameters = new ArrayList<>();
        if (!check(TokenType.RightParen)) {
          do {
            if (parameters.size() >= 255) {
              error(peek(), "Can't have more than 255 parameters.");
            }
    
            parameters.add(
                consume(TokenType.Identifier, "Expect parameter name."));
          } while (match(TokenType.Comma));
        }
        consume(TokenType.RightParen, "Expect ')' after parameters.");

        consume(TokenType.LeftBrace, "Expect '{' before " + kind + " body.");
        List<Stmt> body = block();
        return new Stmt.Function(name, parameters, body);
    }

    private Stmt varDeclaration() {
        Token name = consume(TokenType.Identifier, "Expect variable name.");
    
        Expr initializer = null;
        if (match(TokenType.Equal)) {
            initializer = expression();
        }
    
        consume(TokenType.Semicolon, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    private Stmt whileStatement() {
        consume(TokenType.LeftParen, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(TokenType.RightParen, "Expect ')' after condition.");
        Stmt body = statement();
    
        return new Stmt.While(condition, body);
      }

    private Stmt statement() {
        if (match(TokenType.For)) return forStatement();
        if (match(TokenType.If)) return ifStatement();
        if (match(TokenType.Print)) return printStatement();
        if (match(TokenType.Return)) return returnStatement();
        if (match(TokenType.While)) return whileStatement();
        if (match(TokenType.LeftBrace)) return new Stmt.Block(block()); 

        return expressionStatement();
    }

    private Stmt forStatement() {
        consume(TokenType.LeftParen, "Expect '(' after 'for'.");
    
        Stmt initializer;
        if (match(TokenType.Semicolon)) {
            initializer = null;
        } else if (match(TokenType.Var)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        } 
        
        
        Expr condition = null;
        if (!check(TokenType.Semicolon)) {
            condition = expression();
        }
        consume(TokenType.Semicolon, "Expect ';' after loop condition.");

        Expr increment = null;
        if (!check(TokenType.RightParen)) {
            increment = expression();
        }
        consume(TokenType.RightParen, "Expect ')' after for clauses.");

        Stmt body = statement();

        if (increment != null) {
            body = new Stmt.Block(
                Arrays.asList(
                    body,
                    new Stmt.Expression(increment)));
        }

        if (condition == null) condition = new Expr.Literal(true);
            body = new Stmt.While(condition, body);

        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        return body;
    }

    private Stmt ifStatement() {
        consume(TokenType.LeftParen, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(TokenType.RightParen, "Expect ')' after if condition."); 
    
        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(TokenType.Else)) {
          elseBranch = statement();
        }
    
        return new Stmt.If(condition, thenBranch, elseBranch);
      }
    
    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(TokenType.RightBrace) && !isAtEnd()) {
            statements.add(declaration());
          }

        consume(TokenType.RightBrace, "Expect '}' after block.");
        return statements;
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(TokenType.Semicolon, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    private Stmt returnStatement() {
          Token keyword = previous();
          Expr value = null;
          if (!check(TokenType.Semicolon)) {
              value = expression();
          }

          consume(TokenType.Semicolon, "Expect ';' after return value.");
          return new Stmt.Return(keyword, value);
      }

    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(TokenType.Semicolon, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
      }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
    
        return false;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.Eof;
    }
    
    private Token peek() {
        return tokens.get(current);
    }
    
    private Token previous() {
        return tokens.get(current - 1);
    }

    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        Expr expr = or();
    
        if (match(TokenType.Equal)) {
            Token equals = previous();
            Expr value = assignment();
    
            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }
    
            error(equals, "Invalid assignment target."); 
        }
    
        return expr;
    }

    private Expr or() {
        Expr expr = and();
    
        while (match(TokenType.Or)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }
    
        return expr;
    }

    private Expr and() {
        Expr expr = equality();

        while (match(TokenType.And)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();
    
        while (match(TokenType.BangEqual, TokenType.EqualEqual)) {
          Token operator = previous();
          Expr right = comparison();
          expr = new Expr.Binary(expr, operator, right);
        }
    
        return expr;
    }

    private Expr comparison() {
        Expr expr = term();
    
        while (match(TokenType.Greater, TokenType.GreaterEqual, TokenType.Less, TokenType.LessEqual)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
    
        return expr;
    }

    private Expr term() {
        Expr expr = factor();
    
        while (match(TokenType.Minus, TokenType.Plus)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
    
        return expr;
    }

    private Expr factor() {
        Expr expr = unary();
    
        while (match(TokenType.Slash, TokenType.Star)) {
          Token operator = previous();
          Expr right = unary();
          expr = new Expr.Binary(expr, operator, right);
        }
    
        return expr;
    }

    private Expr unary() {
        if (match(TokenType.Plus, TokenType.Minus)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
    
        return call();
    }

    private Expr call() {
        Expr expr = primary();

        while (true) {
            if (match(TokenType.LeftParen)) {
                expr = finishCall(expr);
            } else {
                break;
            }
        }

        return expr;
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(TokenType.RightParen)) {
            do {
                if (arguments.size() >= 255) {
                    error(peek(), "Can't have more than 255 arguments.");
                }

                arguments.add(expression());
            } while (match(TokenType.Comma));
        }
    
        Token paren = consume(TokenType.RightParen,
                              "Expect ')' after arguments.");
    
        return new Expr.Call(callee, paren, arguments);
    }

    private Expr primary() {
        if (match(TokenType.False)) return new Expr.Literal(false);
        if (match(TokenType.True)) return new Expr.Literal(true);
        if (match(TokenType.Nil)) return new Expr.Literal(null);
    
        if (match(TokenType.Number, TokenType.String)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(TokenType.Identifier)) {
            return new Expr.Variable(previous());
          }
    
        if (match(TokenType.LeftParen)) {
            Expr expr = expression();
            consume(TokenType.RightParen, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression.");
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
    
        throw error(peek(), message);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();
    
        while (!isAtEnd()) {
            if (previous().type == TokenType.Semicolon) return;
    
            switch (peek().type) {
                case Class:
                case Fun:
                case Var:
                case For:
                case If:
                case While:
                case Print:
                case Return:
              return;
            }
    
            advance();
        }
    }
}