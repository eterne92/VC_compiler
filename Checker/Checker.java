/**
 * Checker.java   
 * Sun Apr 24 15:57:55 AEST 2016
 **/

package VC.Checker;

import VC.ASTs.*;
import VC.Scanner.SourcePosition;
import VC.ErrorReporter;
import VC.StdEnvironment;

public final class Checker implements Visitor {

    private String errMesg[] = { "*0: main function is missing", "*1: return type of main is not int",

            // defined occurrences of identifiers
            // for global, local and parameters
            "*2: identifier redeclared", "*3: identifier declared void", "*4: identifier declared void[]",

            // applied occurrences of identifiers
            "*5: identifier undeclared",

            // assignments
            "*6: incompatible type for =", "*7: invalid lvalue in assignment",

            // types for expressions
            "*8: incompatible type for return", "*9: incompatible type for this binary operator",
            "*10: incompatible type for this unary operator",

            // scalars
            "*11: attempt to use an array/function as a scalar",

            // arrays
            "*12: attempt to use a scalar/function as an array", "*13: wrong type for element in array initialiser",
            "*14: invalid initialiser: array initialiser for scalar",
            "*15: invalid initialiser: scalar initialiser for array", "*16: excess elements in array initialiser",
            "*17: array subscript is not an integer", "*18: array size missing",

            // functions
            "*19: attempt to reference a scalar/array as a function",

            // conditional expressions in if, for and while
            "*20: if conditional is not boolean", "*21: for conditional is not boolean",
            "*22: while conditional is not boolean",

            // break and continue
            "*23: break must be in a while/for", "*24: continue must be in a while/for",

            // parameters
            "*25: too many actual parameters", "*26: too few actual parameters", "*27: wrong type for actual parameter",

            // reserved for errors that I may have missed (J. Xue)
            "*28: misc 1", "*29: misc 2",

            // the following two checks are optional
            "*30: statement(s) not reached", "*31: missing return statement", };

    private SymbolTable idTable;
    private static SourcePosition dummyPos = new SourcePosition();
    private ErrorReporter reporter;
    private int nestDepth;
    private boolean funcWithReturn;

    // Checks whether the source program, represented by its AST,
    // satisfies the language's scope rules and type rules.
    // Also decorates the AST as follows:
    // (1) Each applied occurrence of an identifier is linked to
    // the corresponding declaration of that identifier.
    // (2) Each expression and variable is decorated by its type.

    public Checker(ErrorReporter reporter) {
        this.reporter = reporter;
        this.idTable = new SymbolTable();
        this.nestDepth = 0;
        this.funcWithReturn = true;
        establishStdEnvironment();
    }

    public void check(AST ast) {
        ast.visit(this, null);
    }

    // auxiliary methods

    private void declareVariable(Ident ident, Decl decl) {
        IdEntry entry = idTable.retrieveOneLevel(ident.spelling);

        if (entry == null) {
            ; // no problem
        } else
            reporter.reportError(errMesg[2] + ": %", ident.spelling, ident.position);
        idTable.insert(ident.spelling, decl);
    }

    // Programs

    public Object visitProgram(Program ast, Object o) {
        ast.FL.visit(this, null);
        Decl binding = idTable.retrieve("main");
        if (binding == null) {
            reporter.reportError(errMesg[0], "", ast.position);
        } else if (!binding.T.isIntType()) {
            reporter.reportError(errMesg[1], "", ast.position);
        }
        return null;
    }

    // Statements

    public Object visitCompoundStmt(CompoundStmt ast, Object o) {
        if (!(ast.parent instanceof FuncDecl)) {
            idTable.openScope();
        }

        // Your code goes here
        ast.DL.visit(this, o);
        ast.SL.visit(this, o);

        if (!(ast.parent instanceof FuncDecl)) {
            idTable.closeScope();
        }
        return null;
    }

    public Object visitStmtList(StmtList ast, Object o) {
        ast.S.visit(this, o);
        if (ast.S instanceof ReturnStmt && ast.SL instanceof StmtList)
            reporter.reportError(errMesg[30], "", ast.SL.position);
        ast.SL.visit(this, o);
        return null;
    }

    public Object visitIfStmt(IfStmt ast, Object o) {
        Type t1 = (Type) ast.E.visit(this, o);
        if (!t1.isBooleanType()) {
            reporter.reportError(errMesg[20] + " %", "(found: " + t1.toString() + ")", ast.E.position);
        }
        ast.S1.visit(this, o);
        ast.S2.visit(this, o);
        return null;
    }

    public Object visitWhileStmt(WhileStmt ast, Object o) {
        nestDepth++;
        Type t1 = (Type) ast.E.visit(this, o);
        if (!t1.isBooleanType()) {
            reporter.reportError(errMesg[22] + " %", "(found: " + t1.toString() + ")", ast.E.position);
        }
        ast.S.visit(this, o);
        nestDepth--;
        return null;
    }

    public Object visitForStmt(ForStmt ast, Object o) {
        nestDepth++;
        ast.E1.visit(this, o);
        if(ast.E2 instanceof EmptyExpr){
            ast.E2 = new BooleanExpr(new BooleanLiteral("true",dummyPos), dummyPos);
        }
        Type t1 = (Type) ast.E2.visit(this, o);
        if (!t1.isBooleanType()) {
            reporter.reportError(errMesg[21] + " %", "(found: " + t1.toString() + ")", ast.E2.position);
        }
        ast.E3.visit(this, o);
        ast.S.visit(this, o);
        nestDepth--;
        return null;
    }

    public Object visitBreakStmt(BreakStmt ast, Object o) {
        if (nestDepth == 0) {
            reporter.reportError(errMesg[23], "", ast.position);
        }
        return null;
    }

    public Object visitContinueStmt(ContinueStmt ast, Object o) {
        if (nestDepth == 0) {
            reporter.reportError(errMesg[24], "", ast.position);
        }
        return null;
    }

    public Object visitReturnStmt(ReturnStmt ast, Object o) {
        Type t1 = (Type) ast.E.visit(this, null);
        if (o instanceof FuncDecl) {
            FuncDecl fAST = (FuncDecl) o;
            Type t2 = fAST.T;
            if (fAST.I.spelling.equals("main")) {
                if (!t1.isIntType()) {
                    reporter.reportError(errMesg[1], "", ast.position);
                }
            } else if (!t2.assignable(t1)) {
                reporter.reportError(errMesg[8], "", ast.position);
            }
        } else {
            /* should never got here */
            reporter.reportError(errMesg[28], "", ast.position);
        }
        funcWithReturn = true;
        return null;
    }

    public Object visitEmptyCompStmt(EmptyCompStmt ast, Object o) {
        return null;
    }

    public Object visitExprStmt(ExprStmt ast, Object o) {
        ast.E.visit(this, o);
        return null;
    }

    public Object visitEmptyStmt(EmptyStmt ast, Object o) {
        return null;
    }

    public Object visitEmptyStmtList(EmptyStmtList ast, Object o) {
        return null;
    }

    // Expressions

    // Returns the Type denoting the type of the expression. Does
    // not use the given object.

    public Object visitEmptyExpr(EmptyExpr ast, Object o) {
        ast.type = StdEnvironment.errorType;
        return ast.type;
    }

    public Object visitBooleanExpr(BooleanExpr ast, Object o) {
        ast.type = StdEnvironment.booleanType;
        return ast.type;
    }

    public Object visitIntExpr(IntExpr ast, Object o) {
        ast.type = StdEnvironment.intType;
        return ast.type;
    }

    public Object visitFloatExpr(FloatExpr ast, Object o) {
        ast.type = StdEnvironment.floatType;
        return ast.type;
    }

    public Object visitStringExpr(StringExpr ast, Object o) {
        ast.type = StdEnvironment.stringType;
        return ast.type;
    }

    public Object visitVarExpr(VarExpr ast, Object o) {
        ast.type = (Type) ast.V.visit(this, ast.parent);
        return ast.type;
    }

    public Object visitSimpleVar(SimpleVar ast, Object o) {
        Decl binding = (Decl) ast.I.visit(this, null);
        if (binding == null) {
            reporter.reportError(errMesg[5] + ": %", ast.I.spelling, ast.position);
            return StdEnvironment.errorType;
        }
        if (binding instanceof FuncDecl) {
            if (!(o instanceof ExprStmt)) {
                reporter.reportError(errMesg[11] + " %", ast.I.spelling, ast.position);
                return StdEnvironment.errorType;
            }
        } else if (binding.T.isArrayType()) {

        }
        return binding.T;
    }

    public Object visitUnaryExpr(UnaryExpr ast, Object o) {
        Type t1 = (Type) ast.E.visit(this, o);

        if (t1.isErrorType()) {
            ast.type = StdEnvironment.errorType;
            return ast.type;
        }

        switch (ast.O.spelling) {
        case "!":
            if (t1.isBooleanType()) {
                ast.type = StdEnvironment.booleanType;
                ast.O.spelling = "i" + ast.O.spelling;
            } else {
                ast.type = StdEnvironment.errorType;
                reporter.reportError(errMesg[10] + ": %", ast.O.spelling, ast.position);
            }
            break;

        default:
            if (t1.isIntType()) {
                ast.type = StdEnvironment.intType;
                ast.O.spelling = "i" + ast.O.spelling;
            } else if (t1.isFloatType()) {
                ast.type = StdEnvironment.floatType;
                ast.O.spelling = "f" + ast.O.spelling;
            } else {
                ast.type = StdEnvironment.errorType;
                reporter.reportError(errMesg[10] + ": %", ast.O.spelling, ast.position);
            }
        }

        return ast.type;
    }

    public Object visitBinaryExpr(BinaryExpr ast, Object o) {
        Type t1 = (Type) ast.E1.visit(this, o);
        Type t2 = (Type) ast.E2.visit(this, o);
        if (t1.isErrorType() || t2.isErrorType()) {
            ast.type = StdEnvironment.errorType;
            return ast.type;
        }
        if (t1.isArrayType() || t2.isArrayType()) {
            ast.type = StdEnvironment.errorType;
            reporter.reportError(errMesg[11], "", ast.position);
        }
        switch (ast.O.spelling) {
        case "+":
        case "-":
        case "*":
        case "/":
            if (t1.isIntType() && t2.isIntType()) {
                ast.type = StdEnvironment.intType;
                ast.O.spelling = "i" + ast.O.spelling;
            } else if (t1.isFloatType() && t2.isFloatType()) {
                ast.type = StdEnvironment.floatType;
                ast.O.spelling = "f" + ast.O.spelling;
            } else if (t1.isIntType() && t2.isFloatType()) {
                ast.E1 = i2f(ast.E1);
                ast.type = StdEnvironment.floatType;
                ast.O.spelling = "f" + ast.O.spelling;
            } else if (t1.isFloatType() && t2.isIntType()) {
                ast.E2 = i2f(ast.E2);
                ast.type = StdEnvironment.floatType;
                ast.O.spelling = "f" + ast.O.spelling;
            } else {
                ast.type = StdEnvironment.errorType;
                reporter.reportError(errMesg[9] + ": %", ast.O.spelling, ast.position);
            }
            break;

        case "<":
        case "<=":
        case ">":
        case ">=":
            if (t1.isIntType() && t2.isIntType()) {
                ast.type = StdEnvironment.booleanType;
                ast.O.spelling = "i" + ast.O.spelling;
            } else if (t1.isFloatType() && t2.isFloatType()) {
                ast.type = StdEnvironment.booleanType;
                ast.O.spelling = "f" + ast.O.spelling;
            } else if (t1.isIntType() && t2.isFloatType()) {
                ast.E1 = i2f(ast.E1);
                ast.type = StdEnvironment.floatType;
                ast.O.spelling = "f" + ast.O.spelling;
            } else if (t1.isFloatType() && t2.isIntType()) {
                ast.E2 = i2f(ast.E2);
                ast.type = StdEnvironment.floatType;
                ast.O.spelling = "f" + ast.O.spelling;
            } else {
                ast.type = StdEnvironment.errorType;
                reporter.reportError(errMesg[9] + ": %", ast.O.spelling, ast.position);
            }
            break;

        case "==":
        case "!=":
            if (t1.isIntType() && t2.isIntType()) {
                ast.type = StdEnvironment.booleanType;
                ast.O.spelling = "i" + ast.O.spelling;
            } else if (t1.isFloatType() && t2.isFloatType()) {
                ast.type = StdEnvironment.booleanType;
                ast.O.spelling = "f" + ast.O.spelling;
            } else if (t1.isIntType() && t2.isFloatType()) {
                ast.E1 = i2f(ast.E1);
                ast.type = StdEnvironment.floatType;
                ast.O.spelling = "f" + ast.O.spelling;
            } else if (t1.isFloatType() && t2.isIntType()) {
                ast.E2 = i2f(ast.E2);
                ast.type = StdEnvironment.floatType;
                ast.O.spelling = "f" + ast.O.spelling;
            } else if (t1.isBooleanType() && t2.isBooleanType()) {
                ast.type = StdEnvironment.booleanType;
                ast.O.spelling = "i" + ast.O.spelling;
            } else {
                ast.type = StdEnvironment.errorType;
                reporter.reportError(errMesg[9] + ": %", ast.O.spelling, ast.position);
            }
            break;

        default:
            if (t1.isBooleanType() && t2.isBooleanType()) {
                ast.type = StdEnvironment.booleanType;
                ast.O.spelling = "i" + ast.O.spelling;
            } else {
                ast.type = StdEnvironment.errorType;
                reporter.reportError(errMesg[9] + ": %", ast.O.spelling, ast.position);
            }

        }
        return ast.type;
    }

    public Object visitInitExpr(InitExpr ast, Object o) {
        ast.IL.visit(this, o);
        ExprList il = (ExprList) ast.IL;
        ArrayType t;
        if (o instanceof GlobalVarDecl) {
            t = (ArrayType) ((GlobalVarDecl) o).T;
        } else {
            t = (ArrayType) ((LocalVarDecl) o).T;
        }
        if (t.E instanceof IntExpr) {
            IntLiteral value = ((IntExpr) t.E).IL;
            if (il.index > Integer.valueOf(value.spelling)) {
                reporter.reportError(errMesg[16], "", ast.position);
                return null;
            }
        } else if (t.E instanceof EmptyExpr) {
            IntLiteral i = new IntLiteral(String.valueOf(il.index), dummyPos);
            t.E = new IntExpr(i, dummyPos);
        }
        return null;
    }

    public Object visitExprList(ExprList ast, Object o) {
        Type t = (Type) ast.E.visit(this, o);

        if (o instanceof GlobalVarDecl) {
            GlobalVarDecl vAST = (GlobalVarDecl) o;
            ArrayType t2 = (ArrayType) vAST.T;
            if (!t2.T.assignable(t)) {
                reporter.reportError(errMesg[13], "", ast.position);
                return ast;
            } else if (t2.T.isFloatType()) {
                ast.E = i2f(ast.E);
            }
        } else if (o instanceof LocalVarDecl) {
            LocalVarDecl vAST = (LocalVarDecl) o;
            ArrayType t2 = (ArrayType) vAST.T;
            if (!t2.T.assignable(t)) {
                reporter.reportError(errMesg[13], "", ast.position);
            } else if (t2.isFloatType()) {
                ast.E = i2f(ast.E);
            }
        }

        if (ast.EL instanceof ExprList) {
            ExprList el = (ExprList) ast.EL.visit(this, o);
            ast.index = el.index + 1;
        } else {
            ast.EL.visit(this, o);
            ast.index = 1;
        }
        return ast;
    }

    public Object visitArrayExpr(ArrayExpr ast, Object o) {
        Type t1 = (Type) ast.V.visit(this, ast.parent);
        Type t2 = (Type) ast.E.visit(this, null);
        if (!t1.isArrayType()) {
            reporter.reportError(errMesg[12], "", ast.position);
            return StdEnvironment.errorType;
        }
        if (!t2.isIntType()) {
            reporter.reportError(errMesg[17], "", ast.position);
            return StdEnvironment.errorType;
        }
        return ((ArrayType) t1).T;
    }

    public Object visitCallExpr(CallExpr ast, Object o) {
        Decl binding = (Decl) ast.I.visit(this, null);
        if (ast.I.spelling.equals("main")) {
            reporter.reportError(errMesg[29] + "main called", "", ast.position);
            return StdEnvironment.errorType;
        }
        if (binding == null) {
            reporter.reportError(errMesg[5] + ": %", ast.I.spelling, ast.I.position);
            return StdEnvironment.errorType;
        } else if (!(binding instanceof FuncDecl)) {
            reporter.reportError(errMesg[19], "", ast.position);
            return StdEnvironment.errorType;
        }
        ast.AL.visit(this, ((FuncDecl) binding).PL);
        return binding.T;
    }

    public Object visitAssignExpr(AssignExpr ast, Object o) {
        if (!(ast.E1 instanceof VarExpr || ast.E1 instanceof ArrayExpr)) {
            reporter.reportError(errMesg[7], "", ast.position);
            return StdEnvironment.errorType;
        }
        Type t1 = (Type) ast.E1.visit(this, null);
        Type t2 = (Type) ast.E2.visit(this, null);
        if (t1.isArrayType()) {
            reporter.reportError(errMesg[7], "", ast.position);
            return StdEnvironment.errorType;
        }
        if (t2.isArrayType()) {
            reporter.reportError(errMesg[11], "", ast.position);
            return StdEnvironment.errorType;
        }
        if (!t1.assignable(t2)) {
            reporter.reportError(errMesg[6], "", ast.position);
            return StdEnvironment.errorType;
        }
        if (t1.isFloatType() && t2.isIntType()) {
            ast.E2 = i2f(ast.E2);
        }
        return t1;
    }

    public Object visitEmptyExprList(EmptyExprList ast, Object o) {
        return null;
    }
    // Declarations

    // Always returns null. Does not use the given object.

    public Object visitFuncDecl(FuncDecl ast, Object o) {
        // idTable.insert (ast.I.spelling, ast);
        declareVariable(ast.I, ast);

        // Your code goes here

        // HINT
        // Pass ast as the 2nd argument (as done below) so that the
        // formal parameters of the function an be extracted from ast when the
        // function body is later visited

        idTable.openScope();
        if (!ast.T.isVoidType()) {
            funcWithReturn = false;
        } else {
            funcWithReturn = true;
        }
        ast.PL.visit(this, ast);
        ast.S.visit(this, ast);
        if (funcWithReturn == false) {
            reporter.reportError(errMesg[31], "", ast.position);
        }
        idTable.closeScope();

        return null;
    }

    public Object visitDeclList(DeclList ast, Object o) {
        ast.D.visit(this, null);
        ast.DL.visit(this, null);
        return null;
    }

    public Object visitEmptyDeclList(EmptyDeclList ast, Object o) {
        return null;
    }

    public Object visitGlobalVarDecl(GlobalVarDecl ast, Object o) {
        declareVariable(ast.I, ast);

        // declare void id
        if (ast.T.isVoidType()) {
            reporter.reportError(errMesg[3] + ": %", ast.I.spelling, ast.I.position);
        }
        /* no init */
        if (ast.E instanceof EmptyExpr) {
            if (ast.T.isArrayType()) {
                if (((ArrayType) ast.T).E instanceof EmptyExpr) {
                    reporter.reportError(errMesg[18], "", ast.position);
                }
            }
            return ast.T;
        }

        // array
        if (ast.T.isArrayType()) {
            // void array
            if (((ArrayType) ast.T).T.isVoidType()) {
                reporter.reportError(errMesg[4] + ": %", ast.I.spelling, ast.I.position);
            }

            if (ast.E instanceof InitExpr) {
                ast.E.visit(this, ast);
            } else {
                // init with scalar
                reporter.reportError(errMesg[15], "", ast.position);
            }
        }
        // not array
        else {
            // scaler init using array
            if (ast.E instanceof InitExpr) {
                reporter.reportError(errMesg[14], "", ast.position);
            }
            // check assignable
            Type t = (Type) ast.E.visit(this, ast);
            if (!ast.T.assignable(t)) {
                reporter.reportError(errMesg[6], "", ast.position);
            } else {
                if (ast.T.isFloatType() && t.isIntType()) {
                    ast.E = i2f(ast.E);
                }
            }
        }

        return null;
    }

    public Object visitLocalVarDecl(LocalVarDecl ast, Object o) {
        declareVariable(ast.I, ast);

        // declare void id
        if (ast.T.isVoidType()) {
            reporter.reportError(errMesg[3] + ": %", ast.I.spelling, ast.I.position);
            return null;
        }
        /* no init */
        if (ast.E instanceof EmptyExpr) {
            if (ast.T.isArrayType()) {
                if (((ArrayType) ast.T).E instanceof EmptyExpr) {
                    reporter.reportError(errMesg[18], "", ast.position);
                }
            }
            return ast.T;
        }

        // array
        if (ast.T.isArrayType()) {
            // void array
            if (((ArrayType) ast.T).T.isVoidType()) {
                reporter.reportError(errMesg[4] + ": %", ast.I.spelling, ast.I.position);
                return null;
            }

            if (ast.E instanceof InitExpr) {
                ast.E.visit(this, ast);
            } else {
                // init with scalar
                reporter.reportError(errMesg[15], "", ast.position);
                return null;
            }
        }
        // not array
        else {
            // scaler init using array
            if (ast.E instanceof InitExpr) {
                reporter.reportError(errMesg[14], "", ast.position);
                return null;
            }
            // check assignable
            Type t = (Type) ast.E.visit(this, ast);
            if (!ast.T.assignable(t)) {
                reporter.reportError(errMesg[6], "", ast.position);
                return null;
            } else {
                if (ast.T.isFloatType() && t.isIntType()) {
                    ast.E = i2f(ast.E);
                }
            }
        }

        return null;
    }

    // Parameters

    // Always returns null. Does not use the given object.

    public Object visitParaList(ParaList ast, Object o) {
        ast.P.visit(this, null);
        ast.PL.visit(this, null);
        return null;
    }

    public Object visitParaDecl(ParaDecl ast, Object o) {
        declareVariable(ast.I, ast);

        if (ast.T.isVoidType()) {
            reporter.reportError(errMesg[3] + ": %", ast.I.spelling, ast.I.position);
        } else if (ast.T.isArrayType()) {
            if (((ArrayType) ast.T).T.isVoidType())
                reporter.reportError(errMesg[4] + ": %", ast.I.spelling, ast.I.position);
        }
        return null;
    }

    public Object visitEmptyParaList(EmptyParaList ast, Object o) {
        return null;
    }

    // Arguments

    // Your visitor methods for arguments go here
    public Object visitArgList(ArgList ast, Object o) {
        if (o instanceof EmptyParaList) {
            reporter.reportError(errMesg[25], "", ast.position);
            return null;
        }
        ast.A.visit(this, ((ParaList) o).P);
        ast.AL.visit(this, ((ParaList) o).PL);
        return null;
    }

    public Object visitArg(Arg ast, Object o) {
        Type t1 = ((ParaDecl) o).T;
        Type t2 = (Type) ast.E.visit(this, null);
        if (t1.isArrayType() && t2.isArrayType()) {
            if (((ArrayType) t1).T.assignable(((ArrayType) t2).T)) {
            } else {
                reporter.reportError(errMesg[27], "", ast.E.position);
                return StdEnvironment.errorType;
            }
        } else if (!t1.assignable(t2)) {
            reporter.reportError(errMesg[27], "", ast.E.position);
            return StdEnvironment.errorType;
        } else {
            if (t1.isFloatType() && t2.isIntType()) {
                ast.E = i2f(ast.E);
            }
        }
        return t1;
    }

    public Object visitEmptyArgList(EmptyArgList ast, Object o) {
        if (o instanceof EmptyParaList) {
        } else {
            reporter.reportError(errMesg[26], "", ast.parent.position);
        }
        return null;
    }

    // Types

    // Returns the type predefined in the standard environment.

    public Object visitErrorType(ErrorType ast, Object o) {
        return StdEnvironment.errorType;
    }

    public Object visitBooleanType(BooleanType ast, Object o) {
        return StdEnvironment.booleanType;
    }

    public Object visitIntType(IntType ast, Object o) {
        return StdEnvironment.intType;
    }

    public Object visitFloatType(FloatType ast, Object o) {
        return StdEnvironment.floatType;
    }

    public Object visitStringType(StringType ast, Object o) {
        return StdEnvironment.stringType;
    }

    public Object visitVoidType(VoidType ast, Object o) {
        return StdEnvironment.voidType;
    }

    public Object visitArrayType(ArrayType ast, Object o) {

        return null;
    }

    // Literals, Identifiers and Operators

    public Object visitIdent(Ident I, Object o) {
        Decl binding = idTable.retrieve(I.spelling);
        if (binding != null)
            I.decl = binding;
        return binding;
    }

    public Object visitBooleanLiteral(BooleanLiteral SL, Object o) {
        return StdEnvironment.booleanType;
    }

    public Object visitIntLiteral(IntLiteral IL, Object o) {
        return StdEnvironment.intType;
    }

    public Object visitFloatLiteral(FloatLiteral IL, Object o) {
        return StdEnvironment.floatType;
    }

    public Object visitStringLiteral(StringLiteral IL, Object o) {
        return StdEnvironment.stringType;
    }

    public Object visitOperator(Operator O, Object o) {
        return null;
    }

    // Creates a small AST to represent the "declaration" of each built-in
    // function, and enters it in the symbol table.

    private FuncDecl declareStdFunc(Type resultType, String id, List pl) {

        FuncDecl binding;

        binding = new FuncDecl(resultType, new Ident(id, dummyPos), pl, new EmptyStmt(dummyPos), dummyPos);
        idTable.insert(id, binding);
        return binding;
    }

    // Creates small ASTs to represent "declarations" of all
    // build-in functions.
    // Inserts these "declarations" into the symbol table.

    private final static Ident dummyI = new Ident("x", dummyPos);

    // insert i2f for a int
    private Expr i2f(Expr E) {
        Operator op = new Operator("i2f", dummyPos);
        UnaryExpr eAST = new UnaryExpr(op, E, dummyPos);
        eAST.type = StdEnvironment.floatType;
        return eAST;
    }

    private void establishStdEnvironment() {

        // Define four primitive types
        // errorType is assigned to ill-typed expressions

        StdEnvironment.booleanType = new BooleanType(dummyPos);
        StdEnvironment.intType = new IntType(dummyPos);
        StdEnvironment.floatType = new FloatType(dummyPos);
        StdEnvironment.stringType = new StringType(dummyPos);
        StdEnvironment.voidType = new VoidType(dummyPos);
        StdEnvironment.errorType = new ErrorType(dummyPos);

        // enter into the declarations for built-in functions into the table

        StdEnvironment.getIntDecl = declareStdFunc(StdEnvironment.intType, "getInt", new EmptyParaList(dummyPos));
        StdEnvironment.putIntDecl = declareStdFunc(StdEnvironment.voidType, "putInt", new ParaList(
                new ParaDecl(StdEnvironment.intType, dummyI, dummyPos), new EmptyParaList(dummyPos), dummyPos));
        StdEnvironment.putIntLnDecl = declareStdFunc(StdEnvironment.voidType, "putIntLn", new ParaList(
                new ParaDecl(StdEnvironment.intType, dummyI, dummyPos), new EmptyParaList(dummyPos), dummyPos));
        StdEnvironment.getFloatDecl = declareStdFunc(StdEnvironment.floatType, "getFloat", new EmptyParaList(dummyPos));
        StdEnvironment.putFloatDecl = declareStdFunc(StdEnvironment.voidType, "putFloat", new ParaList(
                new ParaDecl(StdEnvironment.floatType, dummyI, dummyPos), new EmptyParaList(dummyPos), dummyPos));
        StdEnvironment.putFloatLnDecl = declareStdFunc(StdEnvironment.voidType, "putFloatLn", new ParaList(
                new ParaDecl(StdEnvironment.floatType, dummyI, dummyPos), new EmptyParaList(dummyPos), dummyPos));
        StdEnvironment.putBoolDecl = declareStdFunc(StdEnvironment.voidType, "putBool", new ParaList(
                new ParaDecl(StdEnvironment.booleanType, dummyI, dummyPos), new EmptyParaList(dummyPos), dummyPos));
        StdEnvironment.putBoolLnDecl = declareStdFunc(StdEnvironment.voidType, "putBoolLn", new ParaList(
                new ParaDecl(StdEnvironment.booleanType, dummyI, dummyPos), new EmptyParaList(dummyPos), dummyPos));

        StdEnvironment.putStringLnDecl = declareStdFunc(StdEnvironment.voidType, "putStringLn", new ParaList(
                new ParaDecl(StdEnvironment.stringType, dummyI, dummyPos), new EmptyParaList(dummyPos), dummyPos));

        StdEnvironment.putStringDecl = declareStdFunc(StdEnvironment.voidType, "putString", new ParaList(
                new ParaDecl(StdEnvironment.stringType, dummyI, dummyPos), new EmptyParaList(dummyPos), dummyPos));

        StdEnvironment.putLnDecl = declareStdFunc(StdEnvironment.voidType, "putLn", new EmptyParaList(dummyPos));

    }

}
