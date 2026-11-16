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
    public enum Precedence {
        /*    */         COMMA,
        /*    */       SEMICOLON,
        /*    */             ARR_RIGHT,
        /* <- */ MOVE_RIGHT, MOVE_LEFT,
        /* -> */ FISH_LEFT,  FISH_RIGHT,
        /* -> */ OR2_LEFT,   OR2_RIGHT,
        /* -> */ AND2_LEFT,  AND2_RIGHT,
        /* -> */ OR1_LEFT,   OR1_RIGHT,
        /* -> */ AND1_LEFT,  AND1_RIGHT,
        /*    */        COMPARE,
        /* -> */ ADD_LEFT,   ADD_RIGHT,
        /* -> */ MULT_LEFT,  MULT_RIGHT,
        /* <- */ POW_RIGHT,  POW_LEFT,
        /*    */ ARR_LEFT,
        /* <- */ UN_RIGHT,   UN_LEFT,
        /*    */         INDEX,
        /* -> */ DOT_LEFT,   DOT_RIGHT,
    }

    public record Op(Precedence leftPrecedence, Precedence rightPrecedence, @Nullable Object group) {
        public static final Op RESET = new Op(Precedence.COMMA,    Precedence.COMMA, null);
        public static final Op INDEX = new Op(Precedence.INDEX,    Precedence.INDEX);
        public static final Op CALL  = new Op(Precedence.DOT_LEFT, Precedence.DOT_RIGHT);

        public static final Object COMPARE_GROUP = new Object();
        public static final Object ASSIGN_GROUP = new Object();
        public static final Object BIND_GROUP = new Object();

        public Op(Precedence leftPrecedence, Precedence rightPrecedence) {
            this(leftPrecedence, rightPrecedence, new Object());
        }
    }

    public static final Map<String, Op> OPS = Map.ofEntries(
        Map.entry("+", new Op(Precedence.ADD_LEFT, Precedence.ADD_RIGHT)),
        Map.entry("-", new Op(Precedence.ADD_LEFT, Precedence.ADD_RIGHT)),
        Map.entry("*", new Op(Precedence.MULT_LEFT, Precedence.MULT_RIGHT)),
        Map.entry("/", new Op(Precedence.MULT_LEFT, Precedence.MULT_RIGHT)),
        Map.entry("%", new Op(Precedence.MULT_LEFT, Precedence.MULT_RIGHT)),
        Map.entry("^", new Op(Precedence.POW_LEFT, Precedence.POW_RIGHT)),

        Map.entry("&", new Op(Precedence.AND1_LEFT, Precedence.AND1_RIGHT)),
        Map.entry("|", new Op(Precedence.OR1_LEFT, Precedence.OR1_RIGHT)),
        Map.entry("?", new Op(Precedence.OR1_LEFT, Precedence.OR1_RIGHT)),
        Map.entry("&&", new Op(Precedence.AND2_LEFT, Precedence.AND2_RIGHT)),
        Map.entry("||", new Op(Precedence.OR2_LEFT, Precedence.OR2_RIGHT)),
        Map.entry("??", new Op(Precedence.OR2_LEFT, Precedence.OR2_RIGHT)),

        Map.entry("==", new Op(Precedence.COMPARE, Precedence.COMPARE, Op.COMPARE_GROUP)),
        Map.entry("<", new Op(Precedence.COMPARE, Precedence.COMPARE, Op.COMPARE_GROUP)),
        Map.entry("<=", new Op(Precedence.COMPARE, Precedence.COMPARE, Op.COMPARE_GROUP)),
        Map.entry(">", new Op(Precedence.COMPARE, Precedence.COMPARE, Op.COMPARE_GROUP)),
        Map.entry(">=", new Op(Precedence.COMPARE, Precedence.COMPARE, Op.COMPARE_GROUP)),
        Map.entry("!=", new Op(Precedence.COMPARE, Precedence.COMPARE)),

        Map.entry("=", new Op(Precedence.MOVE_LEFT, Precedence.MOVE_RIGHT, Op.ASSIGN_GROUP)),
        Map.entry("+=", new Op(Precedence.MOVE_LEFT, Precedence.MOVE_RIGHT, Op.ASSIGN_GROUP)),
        Map.entry("-=", new Op(Precedence.MOVE_LEFT, Precedence.MOVE_RIGHT, Op.ASSIGN_GROUP)),
        Map.entry("*=", new Op(Precedence.MOVE_LEFT, Precedence.MOVE_RIGHT, Op.ASSIGN_GROUP)),
        Map.entry("/=", new Op(Precedence.MOVE_LEFT, Precedence.MOVE_RIGHT, Op.ASSIGN_GROUP)),
        Map.entry("%=", new Op(Precedence.MOVE_LEFT, Precedence.MOVE_RIGHT, Op.ASSIGN_GROUP)),
        Map.entry("^=", new Op(Precedence.MOVE_LEFT, Precedence.MOVE_RIGHT, Op.ASSIGN_GROUP)),
        Map.entry("&=", new Op(Precedence.MOVE_LEFT, Precedence.MOVE_RIGHT, Op.ASSIGN_GROUP)),
        Map.entry("|=", new Op(Precedence.MOVE_LEFT, Precedence.MOVE_RIGHT, Op.ASSIGN_GROUP)),
        Map.entry("?=", new Op(Precedence.MOVE_LEFT, Precedence.MOVE_RIGHT, Op.ASSIGN_GROUP)),
        // Map.entry("&&=", new Op(Precedence.MOVE_LEFT, Precedence.MOVE_RIGHT, Op.ASSIGN_GROUP)),
        // Map.entry("||=", new Op(Precedence.MOVE_LEFT, Precedence.MOVE_RIGHT, Op.ASSIGN_GROUP)),
        // Map.entry("??=", new Op(Precedence.MOVE_LEFT, Precedence.MOVE_RIGHT, Op.ASSIGN_GROUP)),
        // TODO: .= or similar

        Map.entry("<><", new Op(Precedence.FISH_LEFT, Precedence.FISH_RIGHT)),
        Map.entry(":=", new Op(Precedence.MOVE_LEFT, Precedence.MOVE_RIGHT, Op.BIND_GROUP)),
        Map.entry("::", new Op(Precedence.MOVE_LEFT, Precedence.MOVE_RIGHT, Op.BIND_GROUP)),
        Map.entry("->", new Op(Precedence.ARR_LEFT, Precedence.ARR_RIGHT)),
        Map.entry(".", new Op(Precedence.DOT_LEFT, Precedence.DOT_RIGHT)),
        Map.entry(";", new Op(Precedence.SEMICOLON, Precedence.SEMICOLON))
    );

    public static final Map<String, Op> UOPS = Map.ofEntries(
        Map.entry("!", new Op(Precedence.UN_LEFT, Precedence.UN_RIGHT)),
        Map.entry("-", new Op(Precedence.UN_LEFT, Precedence.UN_RIGHT)),
        Map.entry("+", new Op(Precedence.UN_LEFT, Precedence.UN_RIGHT)),
        Map.entry("...", new Op(Precedence.UN_LEFT, Precedence.UN_RIGHT)),
        Map.entry("?", new Op(Precedence.UN_LEFT, Precedence.UN_RIGHT)),
        Map.entry(".", new Op(Precedence.UN_LEFT, Precedence.UN_RIGHT))
    );

    public record Node(Token token, @Nullable List<Node> children) {
        public Node opLhs() {
            return children.size() == 3
                ? children.getFirst()
                : new Node(token, children.subList(0, children.size() - 2));
        }

        public Node opRhs() {
            return children.size() == 3
                ? children.getLast()
                : new Node(children.get(3).token(), children.subList(2, children.size()));
        }
    }

    public record ASTResult(@Nullable Node node, int commaIndex) {
        public ASTResult {
            if (node == null && commaIndex >= 0)
                throw new IllegalArgumentException("Empty expression before a comma");
        }
    }

    public final Expression expression;
    public final Context context;
    public final LanitiumConfig.Structured config;

    public Parser(Lexer lexer) {
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
            if (topOp.rightPrecedence.compareTo(op.leftPrecedence) <= 0)
                return topOp.rightPrecedence.compareTo(op.leftPrecedence) < 0;

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
        return switch (TokenInterface.type(token).name()) {
            case TokenTypeName.OPERATOR,
                 TokenTypeName.UNARY_OPERATOR,
                 TokenTypeName.COMMA,
                 TokenTypeName.MARKER -> false;
            default -> true;
        };
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

        loop: for (;i < trees.size(); i++) switch (trees.get(i)) {
            case TokenTree.Single(Token token) -> { switch (TokenInterface.type(token).name()) {
                case TokenTypeName.COMMA -> { break loop; }
                case TokenTypeName.VARIABLE, TokenTypeName.LITERAL -> nodes.push(new Node(token, null));
                case TokenTypeName.STRINGPARAM -> {
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
                }
                case TokenTypeName.MARKER -> {
                    if (!afterSemicolon(trees, i)) {
                        commitOperators(nodes, ops, OPS.get(token.surface));
                        ops.push(token);
                    }
                }
                case TokenTypeName.OPERATOR -> {
                    commitOperators(nodes, ops, OPS.get(token.surface));
                    ops.push(token);
                }
                case TokenTypeName.UNARY_OPERATOR -> {
                    commitOperators(nodes, ops, UOPS.get(token.surface));
                    ops.push(token);
                }
                case String tokenType -> throw new IllegalArgumentException("Unexpected token type: " + tokenType);
            } }
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

        if (i > 0
         && trees.get(i - 1) instanceof TokenTree.Single(Token token)
         && TokenInterface.type(token) == TokenTypeInterface.MARKER
         && !ops.isEmpty()
         && TokenInterface.type(ops.peek()) == TokenTypeInterface.MARKER
        ) nodes.push(new Node(TokenInterface.morphedInto(token, TokenTypeInterface.CONSTANT, "null"), null));

        commitOperators(nodes, ops, Op.RESET);
        if (i >= trees.size()) i = -1;
        assert nodes.size() <= 1 && ops.isEmpty();
        return new ASTResult(nodes.peek(), i);
    }

    public Node parseFileToAST(TokenTree.Brackets br) {
        return noCommas(parseToAST(br.contents(), 0), br);
    }
}
