package run.aloop.elevator;


import java.util.ArrayList;
import java.util.HashMap;

import Moka7.S7;

public class DataStruct {

    static class Variable {

        enum Type {
            INT, SHORT, FLOAT, BOOL, STRING
        }

        String name;
        Type type;
        int size;

        public Variable(String name, Type type, int size) {
            this.name = name;
            this.type = type;
            this.size = size;
        }

        public Variable(String name, Type type) throws Exception {
            this(name, type, 0);
            switch (type) {
                case STRING:
                    throw new Exception("no size is specified with string");
                case INT:
                case FLOAT:
                    size = 4;
                    break;
                case SHORT:
                    size = 2;
                    break;
                case BOOL:
                    size = 1;
                    break;
            }
        }
    }

    public int area, db, start;
    public ArrayList<Variable> variables;

    public DataStruct(int area, int db, int start) {
        this.area = area;
        this.db = db;
        this.start = start;
        this.variables = new ArrayList<>();
    }

    public DataStruct add(Variable variable) {
        this.variables.add(variable);
        return this;
    }

    public int length() {
        int len = 0;
        for (Variable v : this.variables) {
            len += v.size;
        }
        return len;
    }

    public int getOffset(String name) {
        int offset = 0;
        for (Variable v : this.variables) {
            if (v.name.equals(name)) {
                return offset;
            }
            offset += v.size;
        }
        return -1;
    }

    public Variable getVariable(String name) {
        for (Variable v : this.variables) {
            if (v.name.equals(name)) {
                return v;
            }
        }
        return null;
    }

    public HashMap<String, Object> unpack(byte[] data) {
        HashMap<String, Object> values = new HashMap<>();
        int index = 0;
        for (Variable v : this.variables) {
            switch (v.type) {
                case INT:
                    values.put(v.name, S7.GetDIntAt(data, index));
                    break;
                case BOOL:
                    values.put(v.name, S7.GetBitAt(data, index, 0));
                    break;
                case FLOAT:
                    values.put(v.name, S7.GetFloatAt(data, index));
                    break;
                case SHORT:
                    values.put(v.name, S7.GetShortAt(data, index));
                    break;
                case STRING:
                    values.put(v.name, S7.GetStringAt(data, index, v.size));
                    break;
            }
            index += v.size;
        }
        return values;
    }
}
