import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class Interpreter implements Expr.Visitor<Object>,
                             Stmt.Visitor<Void> {  
    final Environment globals = new Environment();
    private Environment environment = globals;
    private final Map<Expr, Integer> locals = new HashMap<>();

    Interpreter() {
        globals.define("clock", new LoxCallable() {
            @Override
            public int arity() { return 0; }
    
            @Override
            public Object call(Interpreter interpreter,
                             List<Object> arguments) {
                return (double)System.currentTimeMillis() / 1000.0;
            }
    
            @Override
            public String toString() { return "<native fn>"; }
        });
    }

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    void interpret(Expr expression) {
        try {
            Object value = evaluate(expression);
            System.out.println(stringify(value));
          } catch (RuntimeError error) {
            Lox.runtimeError(error);
          }
    }

    void resolve(Expr expr, int depth) {
        locals.put(expr, depth);
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
      evaluate(stmt.expression);
      return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        LoxFunction function = new LoxFunction(stmt, environment, false);
        environment.define(stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        environment.define(stmt.name.lexeme, null);

        Map<String, LoxFunction> methods = new HashMap<>();
        for (Stmt.Function method : stmt.methods) {
            LoxFunction function = new LoxFunction(method, environment,
                method.name.lexeme.equals("init"));
            methods.put(method.name.lexeme, function);
        }

        LoxClass klass = new LoxClass(stmt.name.lexeme, methods);
        environment.assign(stmt.name, klass);
        return null;
    }

    @Override
    public Object visitGetExpr(Expr.Get expr) {
        Object object = evaluate(expr.object);
        if (object instanceof LoxInstance) {
            return ((LoxInstance) object).get(expr.name);
        }
  
        throw new RuntimeError(expr.name,
            "Only instances have properties.");
    }

    @Override
    public Object visitSetExpr(Expr.Set expr) {
        Object object = evaluate(expr.object);
  
        if (!(object instanceof LoxInstance)) { 
           throw new RuntimeError(expr.name,
                                 "Only instances have fields.");
        }
  
       Object value = evaluate(expr.value);
        ((LoxInstance)object).set(expr.name, value);
        return value;
    }

    @Override
    public Object visitThisExpr(Expr.This expr) {
        return lookUpVariable(expr.keyword, expr);
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) value = evaluate(stmt.value);
  
        throw new Return(value);
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = stmt.initializer == null ? null : evaluate(stmt.initializer);
        environment.define(stmt.name.lexeme, value);
               
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
      Object value = evaluate(stmt.expression);
      System.out.println(stringify(value));
      return null;
    }

    @Override 
    public Void visitBlockStmt(Stmt.Block expr) {
         executeBlock(expr.statements, new Environment(environment));
         return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }

    void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;
            for (Stmt statement : statements) {
                execute(statement);
            }
        }
        finally {
            this.environment = previous;
        }
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
       return evaluate(expr.expression);
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return lookUpVariable(expr.name, expr);
    }

    private Object lookUpVariable(Token name, Expr expr) {
        Integer distance = locals.get(expr);
        if (distance != null) {
            return environment.getAt(distance, name.lexeme);
        } else {
            return globals.get(name);
        }
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);

        Integer distance = locals.get(expr);
        if (distance != null) {
          environment.assignAt(distance, expr.name, value);
        } else {
          globals.assign(expr.name, value);
        }

        return value;
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

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
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

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);
  
        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) { 
            arguments.add(evaluate(argument));
        }
  
        if (!(callee instanceof LoxCallable)) {
            throw new RuntimeError(expr.paren,
                "Can only call functions and classes.");
        }

        LoxCallable function = (LoxCallable)callee;
        if (!(callee instanceof LoxCallable)) {
            throw new RuntimeError(expr.paren,
                "Can only call functions and classes.");
        }

        return function.call(this, arguments);
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
      Object left = evaluate(expr.left);
  
      if (expr.operator.type == TokenType.Or) {
        if (isTruthy(left)) return left;
      } else {
        if (!isTruthy(left)) return left;
      }
  
      return evaluate(expr.right);
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