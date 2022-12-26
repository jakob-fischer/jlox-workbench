class Interpreter implements Expr.Visitor<Object> {
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
                return -(double)right;
            case Bang:
                return !isTruthy(right);
        }
  
        // Unreachable.
        return null;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right); 
  
        switch (expr.operator.type) {
            case BangEqual: return !isEqual(left, right);
            case EqualEqual: return isEqual(left, right);
            case Greater:
                return (double)left > (double)right;
            case GreaterEqual:
                return (double)left >= (double)right;
            case Less:
                return (double)left < (double)right;
            case LessEqual:
                return (double)left <= (double)right;
            case Minus:
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
                return (double)left / (double)right;
            case Star:
                return (double)left * (double)right;

            
        }
  
        // Unreachable.
        return null;
    }

    boolean isTruthy(Object obj) {
        if (obj == null)
            return false;


        if (obj instanceof Boolean) 
            return (boolean)obj;

        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
    
        return a.equals(b);
    }
}