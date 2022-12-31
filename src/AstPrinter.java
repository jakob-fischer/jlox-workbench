import java.util.List;

class AstPrinter implements Expr.Visitor<String> {
    String print(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return parenthesize(expr.operator.lexeme,
                          expr.left, expr.right);
    }
  
    @Override
    public String visitLogicalExpr(Expr.Logical expr) {
        return parenthesize(expr.operator.lexeme,
                          expr.left, expr.right);
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return parenthesize("group", expr.expression);
    }
  
    @Override
    public String visitCallExpr(Expr.Call expr) {
        return parenthesize("call", expr.callee, expr.arguments);
    }

    @Override
    public String visitGetExpr(Expr.Get expr) {
        return parenthesize("." + expr.name.lexeme, expr.object);
    }

    @Override
    public String visitSetExpr(Expr.Set expr) {
        return parenthesize("." + expr.name.lexeme, expr.object);
    }

    @Override
    public String visitThisExpr(Expr.This expr) {
        return "this";
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null) return "nil";
            return expr.value.toString();
    }
  
    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }

    @Override
    public String visitVariableExpr(Expr.Variable variable) {
        return variable.name.lexeme;
    }

    @Override
    public String visitAssignExpr(Expr.Assign assign) {
        return parenthesize("=", new Expr.Variable(assign.name), assign.value);
    }

    private String parenthesize(String name, Expr expr, List<Expr> exprs) {
        StringBuilder builder = new StringBuilder();
    
        builder.append("(").append(name).append(" ").append(expr.accept(this));
        for (Expr e : exprs) {
            builder.append(" ");
            builder.append(e.accept(this));
        }
        builder.append(")");
    
        return builder.toString();
    }

    private String parenthesize(String name, Expr... exprs) {
        StringBuilder builder = new StringBuilder();
    
        builder.append("(").append(name);
        for (Expr expr : exprs) {
            builder.append(" ");
            builder.append(expr.accept(this));
        }
        builder.append(")");
    
        return builder.toString();
    }

    public static void main(String[] args) {
        Expr expression = new Expr.Binary(
            new Expr.Unary(
                new Token(TokenType.Minus, "-", null, 1),
                new Expr.Literal(123)),
            new Token(TokenType.Star, "*", null, 1),
            new Expr.Grouping(
                new Expr.Literal(45.67)));
    
        System.out.println(new AstPrinter().print(expression));
      }
}