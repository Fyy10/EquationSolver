import java.util.*;

public class Main {
    public static void main(String[] args) {
        String stmt = new String();
        // test cases
        String tests = """
            -x + 3 = 6
            -(4 + x) * 2 + 5x = 2x + 10
            x + 3 = x + 6
            3x + 2 * 3 + 2 * (4x + 5) = 5x - 2
            x + 8 = -x - 6
            -(2x + 5*(3 + 4x +(4*-x + 1)) + 3) = 3 * (2x + 8)
            3 * -2 = -2x / -1
            2 * (4x - 2) - 3 * 4 = 4x
            exit
            """;

        // use builtin test cases or custom inputs
        Scanner sc = new Scanner(tests);
        // Scanner sc = new Scanner(System.in);

        while (sc.hasNextLine()) {
            stmt = sc.nextLine();
            if (stmt.equals("exit")) {
                break;
            }

            System.out.println("Equ: " + stmt);
            EquationSolver solver = new EquationSolver();
            String result = solver.solve(stmt);
            System.out.println("Sol: " + result);
        }
        sc.close();
    }
}

class EquationSolver {
    // operator -> priority
    Map<Character, Integer> prior;
    // numbers
    Stack<Number> nums;
    // operators
    Stack<Character> ops;

    public EquationSolver() {
        prior = new HashMap<>();
        nums = new Stack<>();
        ops = new Stack<>();
        prior.put('+', 1);
        prior.put('-', 1);
        prior.put('*', 2);
        prior.put('/', 2);
        prior.put('~', 3); // negate
        prior.put('(', 4);
        prior.put(')', 4);
    }

    public String solve(String stmt) {
        String[] exprs = stmt.split("=");
        if (exprs.length != 2) {
            return "Not a valid equation";
        }

        Number ans_left = eval(exprs[0]);
        Number ans_right = eval(exprs[1]);

        // ax = b
        int a = ans_left.a - ans_right.a;
        int b = ans_right.b - ans_left.b;
        if (a == 0) {
            if (b == 0) {
                return "Infinite solutions";
            } else {
                return "No solution";
            }
        } else {
            if ((b % a) == 0) {
                return String.format("x = %d", b / a);
            } else {
                double res = (double)b / a;
                return String.format("x = %f", res);
            }
        }
    }

    private Number eval(String expr) {
        nums.clear();
        ops.clear();
        Number ans = new Number("");
        expr = preprocess(expr);
        if (expr.length() == 0) return ans;

        char[] carr = expr.toCharArray();
        int n = expr.length();

        for (int i = 0; i < n; i++) {
            char c = carr[i];
            if (!Number.isDigitOrX(c)) {
                // c is +-*/~()
                if (hasPrevNum(i, expr)) {
                    nums.push(getPrevNum(i, expr));
                }

                if (c == '(') {
                    ops.push(c);
                } else if (c == ')') {
                    // compute all until '('
                    compute(0);
                    // ops.peek() should be '('
                    ops.pop();
                } else {
                    // c is +-*/~
                    compute(prior.get(c));
                    ops.push(c);
                }
            }
        }

        // last number
        if (hasPrevNum(n, expr)) {
            nums.push(getPrevNum(n, expr));
        }

        // compute all
        compute(0);

        assert nums.size() == 1;
        assert ops.size() == 0;
        ans = nums.pop();
        // System.out.printf("Eval ax+b: a = %d, b = %d\n", ans.a, ans.b);
        return ans;
    }

    private static String preprocess(String expr) {
        // remove spaces
        expr = expr.replace(" ", "");
        if (expr.length() == 0) return expr;

        // handle the '+' in the beginning
        if (expr.startsWith("+")) {
            expr = "0" + expr;
        }
        // handle the '-' in the beginning
        if (expr.startsWith("-")) {
            expr = "0" + expr;
        }
        // handle (+
        expr = expr.replace("(+", "(0+");
        // handle (-
        expr = expr.replace("(-", "(0-");
        // handle +-
        expr = expr.replace("+-", "+0-");
        // handle --
        expr = expr.replace("--", "-0-");
        // handle negate
        char[] carr = expr.toCharArray();
        for (int i = 1; i < carr.length; i++) {
            if (carr[i] == '-' && "+-*/(".contains(String.valueOf(carr[i-1]))) {
                carr[i] = '~';
            }
        }
        expr = String.valueOf(carr);

        // System.out.printf("preprocessed: %s\n", expr);
        return expr;
    }

    private static boolean hasPrevNum(int idx, String expr) {
        // if the previous character is digit or x, the there is a previous number
        if (idx == 0) return false;
        if (idx > expr.length()) return false;
        return Number.isDigitOrX(expr.charAt(idx-1));
    }

    private static Number getPrevNum(int i, String expr) {
        assert hasPrevNum(i, expr);
        int first = i-1;
        while (first >= 0) {
            if (!Number.isDigitOrX(expr.charAt(first))) break;
            first--;
        }
        first++;
        return new Number(expr.substring(first, i));
    }

    private void compute(int p) {
        // process all higher or equal priority ops in the stack
        // until the nearest '('
        while (!ops.empty()) {
            if (prior.get(ops.peek()) < p) break;
            if (ops.peek() == '(') break;
            char op = ops.pop();
            Number res = new Number("");
            if (op == '~') {
                // negate
                Number num = nums.pop();
                res = num.neg();
            } else {
                Number num2 = nums.pop();
                Number num1 = nums.pop();
                res = num1.calc(num2, op);
            }
            nums.push(res);
        }
    }
}

// ax + b
class Number {
    int a;
    int b;
    public Number(String num_s) {
        a = 0;
        b = 0;
        if (num_s.length() == 0) return;

        // parse num_s
        if (isnum(num_s)) {
            b = Integer.valueOf(num_s);
        } else {
            if (num_s.length() == 1) {
                // a single x
                a = 1;
            } else {
                a = Integer.valueOf(num_s.substring(0, num_s.length()-1));
            }
        }
    }

    public Number calc(Number num, char op) {
        // op is +-*/
        Number res = new Number("");
        switch (op) {
            case '+': res = add(num); break;
            case '-': res = sub(num); break;
            case '*': res = mul(num); break;
            case '/': res = div(num); break;
            // case '~': res = neg(); break;

            default:
                System.err.println("Unsupported operator: " + op);
                break;
        }
        return res;
    }

    // +
    public Number add(Number num) {
        Number ans = new Number("");
        ans.a = a + num.a;
        ans.b = b + num.b;
        return ans;
    }
    // -
    public Number sub(Number num) {
        Number ans = new Number("");
        ans.a = a - num.a;
        ans.b = b - num.b;
        return ans;
    }
    // *
    public Number mul(Number num) {
        Number ans = new Number("");
        ans.a = a * num.b + b * num.a;
        ans.b = b * num.b;
        return ans;
    }
    // /
    public Number div(Number num) {
        Number ans = new Number("");
        ans.a = a / num.b;
        ans.b = b / num.b;
        return ans;
    }
    // negate
    public Number neg() {
        Number ans = new Number("");
        ans.a = -a;
        ans.b = -b;
        return ans;
    }

    public static boolean isnum(String num_s) {
        assert num_s.length() > 0;
        return num_s.charAt(num_s.length()-1) != 'x';
    }

    public static boolean isDigitOrX(char c) {
        return Character.isDigit(c) || c == 'x';
    }
}
