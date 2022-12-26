class Interpreter implements Expr.Visitor<Object> {
    void interpret(Expr expression) { 
        try {
            Object value = evaluate(expression);
            System.out.println(stringify(value));
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
       return evaluate(expr.expression);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);
  
        switch (expr.operator.type) {
            case Minus:
                checkNumberOperand(expr.operator, right);
                return -(double)right;
            case Bang:
                return !isTruthy(right);
        }
  
        // Unreachable.
        return null;
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
            throw new RuntimeError(operator, "Operand must be a number.");
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right); 
  
        switch (expr.operator.type) {
            case BangEqual: return !isEqual(left, right);
            case EqualEqual: return isEqual(left, right);
            case Greater:
                checkNumberOperands(expr.operator, left, right);
                return (double)left > (double)right;
            case GreaterEqual:
                checkNumberOperands(expr.operator, left, right);
                return (double)left >= (double)right;
            case Less:
                checkNumberOperands(expr.operator, left, right);
                return (double)left < (double)right;
            case LessEqual:
                checkNumberOperands(expr.operator, left, right);
                return (double)left <= (double)right;
            case Minus:
                checkNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;
            case Plus:
                if (left instanceof Double && right instanceof Double) {
                  return (double)left + (double)right;
                } 
                if (left instanceof String && right instanceof String) {
                  return (String)left + (String)right;
                }
                break;
            case Slash:
                checkNumberOperands(expr.operator, left, right);
                return (double)left / (double)right;
            case Star:
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;
        }

        throw new RuntimeError(expr.operator,
            "Operands must be two numbers or two strings.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) 
            return;

         throw new RuntimeError(operator, "Operands must be numbers.");
    }

    boolean isTruthy(Object obj) {
        if (obj == null) return false;
        if (obj instanceof Boolean) return (boolean)obj;

        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
    
        return a.equals(b);
    }

    private String stringify(Object object) {
        if (object == null) return "nil";
    
        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }
    
        return object.toString();
    }
}