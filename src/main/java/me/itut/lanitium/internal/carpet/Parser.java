package me.itut.lanitium.internal.carpet;

import carpet.script.*;
import carpet.script.exception.ExpressionException;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Parser {
    public record Op(int leftPrecedence, int rightPrecedence, @Nullable Object group) {
        public static final int SEMICOLON                   =   5;
        public static final int                  ARR_RIGHT  =  10;
        public static final int MOVE_LEFT =  25, MOVE_RIGHT =  20; // <-
        public static final int OR_LEFT   =  30, OR_RIGHT   =  35; // ->
        public static final int AND_LEFT  =  40, AND_RIGHT  =  45; // ->
        public static final int COMPARE                     =  50;
        public static final int ADD_LEFT  =  60, ADD_RIGHT  =  65; // ->
        public static final int MULT_LEFT =  70, MULT_RIGHT =  75; // ->
        public static final int POW_LEFT  =  85, POW_RIGHT  =  80; // <-
        public static final int ARR_LEFT  =  90                  ;
        public static final int UN_LEFT   = 105, UN_RIGHT   = 100; // <-
        public static final int DOT_LEFT  = 120, DOT_RIGHT  = 125; // ->

        public static final Op RESET     = new Op(  0,   0, null);
        public static final Op INDEX     = new Op(110, 110);
        public static final Op CALL      = new Op(DOT_LEFT, DOT_RIGHT);

        public static final Object COMPARE_GROUP = new Object();

        public Op(int leftPrecedence, int rightPrecedence) {
            this(leftPrecedence, rightPrecedence, new Object());
        }
    }

    public static final Map<String, Op> OPS = new HashMap<>() {{
        put("+", new Op(Op.ADD_LEFT, Op.ADD_RIGHT));
        put("-", new Op(Op.ADD_LEFT, Op.ADD_RIGHT));
        put("*", new Op(Op.MULT_LEFT, Op.MULT_RIGHT));
        put("/", new Op(Op.MULT_LEFT, Op.MULT_RIGHT));
        put("%", new Op(Op.MULT_LEFT, Op.MULT_RIGHT));
        put("^", new Op(Op.POW_LEFT, Op.POW_RIGHT));
        put("&&", new Op(Op.AND_LEFT, Op.AND_RIGHT));
        put("||", new Op(Op.OR_LEFT, Op.OR_RIGHT));
        put("??", new Op(Op.OR_LEFT, Op.OR_RIGHT));
        put("==", new Op(Op.COMPARE, Op.COMPARE, Op.COMPARE_GROUP));
        put("<", new Op(Op.COMPARE, Op.COMPARE, Op.COMPARE_GROUP));
        put("<=", new Op(Op.COMPARE, Op.COMPARE, Op.COMPARE_GROUP));
        put(">", new Op(Op.COMPARE, Op.COMPARE, Op.COMPARE_GROUP));
        put(">=", new Op(Op.COMPARE, Op.COMPARE, Op.COMPARE_GROUP));
        put("!=", new Op(Op.COMPARE, Op.COMPARE));
        put("=", new Op(Op.MOVE_LEFT, Op.MOVE_RIGHT));
        put("+=", new Op(Op.MOVE_LEFT, Op.MOVE_RIGHT));
        put(":=", new Op(Op.MOVE_LEFT, Op.MOVE_RIGHT));
        put("::", new Op(Op.MOVE_LEFT, Op.MOVE_RIGHT));
        put("->", new Op(Op.ARR_LEFT, Op.ARR_RIGHT));
        put(".", new Op(Op.DOT_LEFT, Op.DOT_RIGHT));
        put(";", new Op(Op.SEMICOLON, Op.SEMICOLON));
    }};

    public static final Map<String, Op> UOPS = new HashMap<>() {{
        put("!", new Op(Op.UN_LEFT, Op.UN_RIGHT));
        put("-", new Op(Op.UN_LEFT, Op.UN_RIGHT));
        put("+", new Op(Op.UN_LEFT, Op.UN_RIGHT));
        put("...", new Op(Op.UN_LEFT, Op.UN_RIGHT));
        put(".", new Op(Op.UN_LEFT, Op.UN_RIGHT));
    }};

    public record Node(Token token, @Nullable List<Node> children) {}

    public record ASTResult(@Nullable Node node, int commaIndex) {
        public ASTResult {
            if (node == null && commaIndex >= 0)
                throw new IllegalArgumentException("Empty expression before a comma");
        }
    }

    public final String input;
    public final Expression expression;
    public final Context context;
    public final LanitiumConfig.Structured config;

    public Parser(Lexer lexer) {
        input = lexer.input;
        expression = lexer.expression;
        context = lexer.context;
        config = Objects.requireNonNull(lexer.config);
    }

    private static boolean commitOperators(ArrayDeque<Node> nodes, ArrayDeque<Token> ops, Op op) {
        for (;;) {
            Token top = ops.peek();
            if (top == null) return true;
            boolean unary = TokenInterface.type(top) == TokenTypeInterface.UNARY_OPERATOR;
            Op topOp = (unary ? UOPS : OPS).get(top.surface);

            if (op.group == topOp.group) return true;
            if (topOp.rightPrecedence <= op.leftPrecedence)
                return topOp.rightPrecedence < op.leftPrecedence;

            if (unary) {
                nodes.push(new Node(ops.pop(), List.of(nodes.pop())));
                continue;
            }

            List<Node> operands = new ArrayList<>();
            do {
                operands.add(nodes.pop());
                operands.add(new Node(ops.pop(), null));
            } while (!ops.isEmpty()
                && TokenInterface.type(ops.peek()) != TokenTypeInterface.UNARY_OPERATOR
                && topOp.group == OPS.get(ops.peek().surface).group
            );
            operands.add(nodes.pop());
            nodes.push(new Node(top, List.copyOf(operands.reversed())));
        }
    }

    private static boolean prevValue(List<TokenTree> trees, int i) {
        if (i == 0) return false;
        if (!(trees.get(i - 1) instanceof TokenTree.Single(Token token))) return true;
        TokenTypeInterface tokenType = TokenInterface.type(token);
        if (tokenType == TokenTypeInterface.OPERATOR
         || tokenType == TokenTypeInterface.UNARY_OPERATOR
         || tokenType == TokenTypeInterface.COMMA
         || tokenType == TokenTypeInterface.MARKER) return false;
        return true;
    }

    private static boolean afterSemicolon(List<TokenTree> trees, int i) {
        return i == 0
            || trees.get(i - 1) instanceof TokenTree.Single(Token token)
            && TokenInterface.type(token) == TokenTypeInterface.MARKER;
    }

    private Node noCommas(ASTResult result, TokenTree.Brackets br) {
        if (result.commaIndex >= 0)
            throw new ExpressionException(context, expression, ((TokenTree.Single)br.contents().get(result.commaIndex)).token(), "Commas are not allowed here");
        if (result.node == null)
            return new Node(TokenInterface.morphedInto(br.open(), TokenTypeInterface.CONSTANT, "null"), null);
        return result.node;
    }

    private List<Node> trailingComma(List<Node> nodes, TokenTree.Brackets br) {
        for (int i = 0;;) {
            ASTResult result = parseToAST(br.contents(), i);
            if (result.node != null) nodes.add(result.node);
            if (result.commaIndex < 0) break;
            i = result.commaIndex + 1;
        }
        return List.copyOf(nodes);
    }

    public ASTResult parseToAST(List<TokenTree> trees, int i) {
        ArrayDeque<Node> nodes = new ArrayDeque<>();
        ArrayDeque<Token> ops = new ArrayDeque<>();

        for (;i < trees.size(); i++) switch (trees.get(i)) {
            case TokenTree.Single(Token token) -> {
                TokenTypeInterface tokenType = TokenInterface.type(token);
                if (tokenType == TokenTypeInterface.COMMA) break;

                if (tokenType == TokenTypeInterface.MARKER) {
                    if (afterSemicolon(trees, i)) continue;
                    commitOperators(nodes, ops, OPS.get(token.surface));
                    ops.push(token);
                    continue;
                }

                if (tokenType == TokenTypeInterface.VARIABLE
                 || tokenType == TokenTypeInterface.LITERAL) {
                    nodes.push(new Node(token, null));
                    continue;
                }

                if (tokenType == TokenTypeInterface.STRINGPARAM) {
                    List<Node> children = new ArrayList<>();
                    boolean call = prevValue(trees, i);
                    if (call) {
                        children.add(nodes.pop());
                        children.add(new Node(token, null));
                    }

                    while (i + 1 < trees.size()
                        && trees.get(i + 1) instanceof TokenTree.Brackets br
                        && br.type() == TokenTree.BracketType.INTERPOLATION
                    ) {
                        children.add(noCommas(parseToAST(br.contents(), 0), br));
                        children.add(new Node(((TokenTree.Single)trees.get(i += 2)).token(), null));
                    }

                    if (call) {
                        nodes.push(new Node(
                            TokenInterface.morphedInto(token, TokenTypeInterface.OPEN_PAREN, "("),
                            List.copyOf(children)
                        ));
                        commitOperators(nodes, ops, Op.CALL);
                    } else nodes.push(new Node(token, List.copyOf(children)));
                    continue;
                }

                if (tokenType == TokenTypeInterface.OPERATOR) {
                    commitOperators(nodes, ops, OPS.get(token.surface));
                    ops.push(token);
                    continue;
                }

                if (tokenType == TokenTypeInterface.UNARY_OPERATOR) {
                    commitOperators(nodes, ops, UOPS.get(token.surface));
                    ops.push(token);
                    continue;
                }

                throw new IllegalArgumentException("Unexpected token type: " + tokenType);
            }
            case TokenTree.Brackets br -> { switch (br.type()) {
                case TokenTree.BracketType.ROUND -> {
                    if (prevValue(trees, i)) {
                        List<Node> children = new ArrayList<>();
                        children.add(nodes.pop());
                        nodes.push(new Node(br.open(), trailingComma(children, br)));
                        commitOperators(nodes, ops, Op.CALL);
                    } else nodes.push(noCommas(parseToAST(br.contents(), 0), br));
                }
                case TokenTree.BracketType.SQUARE -> {
                    List<Node> children = new ArrayList<>();
                    boolean index = prevValue(trees, i);
                    if (index) {
                        commitOperators(nodes, ops, Op.INDEX);
                        children.add(nodes.pop());
                    } else children.add(new Node(br.open(), null));

                    nodes.push(new Node(index
                        ? br.open()
                        : TokenInterface.morphedInto(br.open(), TokenTypeInterface.OPEN_PAREN, "("),
                        trailingComma(children, br)
                    ));
                }
                case TokenTree.BracketType.CURLY -> {
                    List<Node> children = new ArrayList<>();
                    children.add(new Node(br.open(), null));
                    Node node = new Node(
                        TokenInterface.morphedInto(br.open(), TokenTypeInterface.OPEN_PAREN, "("),
                        trailingComma(children, br)
                    );

                    if (prevValue(trees, i)) {
                        nodes.push(new Node(node.token, List.of(nodes.pop(), node)));
                        commitOperators(nodes, ops, Op.CALL);
                    } else nodes.push(node);
                }
                case TokenTree.BracketType.INTERPOLATION -> throw new IllegalStateException("You can't type this");
            } }
        }

        commitOperators(nodes, ops, Op.RESET);
        if (i >= trees.size()) i = -1;
        assert nodes.size() <= 1 && ops.isEmpty();
        return new ASTResult(nodes.peek(), i);
    }

    public Node parseFileToAST(TokenTree.Brackets br) {
        return noCommas(parseToAST(br.contents(), 0), br);
    }
}
