import java.util.HashMap;
import java.util.Map;

class Environment {
    private final Map<String, Object> values = new HashMap<>();
    final Environment parent;

    Environment() {  parent = null;  }   
    Environment(Environment parent) {  this.parent = parent;  }

    void define(String name, Object value) {
        values.put(name, value);
    }

    Object get(Token name) {
        if (!values.containsKey(name.lexeme)) {
            if (parent != null) {
                return parent.get(name);
            } else {
                throw new RuntimeError(name,
                    "Undefined variable '" + name.lexeme + "'.");
            }
        }

        return values.get(name.lexeme);
    }

    void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
        } else {
            if (parent != null) {
                parent.assign(name, value);
            } else {
                throw new RuntimeError(name,
                    "Undefined variable '" + name.lexeme + "'.");
            }
        }    
    }

    Object getAt(int distance, String name) {
        return ancestor(distance).values.get(name);
    }

    Environment ancestor(int distance) {
        Environment environment = this;
        for (int i = 0; i < distance; i++) {
            environment = environment.parent; 
        }
    
        return environment;
    }

    void assignAt(int distance, Token name, Object value) {
        ancestor(distance).values.put(name.lexeme, value);
    }
}