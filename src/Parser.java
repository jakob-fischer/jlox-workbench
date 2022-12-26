import java.util.List;

class Parser {
    private static class ParseError extends RuntimeException {}
    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    Expr parse() {
        try {
            return expression();
        } catch (ParseError error) {
            return null;
        }
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
        return equality();
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
    
        return primary();
    }

    private Expr primary() {
        if (match(TokenType.False)) return new Expr.Literal(false);
        if (match(TokenType.True)) return new Expr.Literal(true);
        if (match(TokenType.Nil)) return new Expr.Literal(null);
    
        if (match(TokenType.Number, TokenType.String)) {
            return new Expr.Literal(previous().literal);
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