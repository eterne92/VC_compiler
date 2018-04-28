/*
 * Parser.java            
 *
 * This parser for a subset of the VC language is intended to 
 *  demonstrate how to create the AST nodes, including (among others): 
 *  [1] a list (of statements)
 *  [2] a function
 *  [3] a statement (which is an expression statement), 
 *  [4] a unary expression
 *  [5] a binary expression
 *  [6] terminals (identifiers, integer literals and operators)
 *
 * In addition, it also demonstrates how to use the two methods start 
 * and finish to determine the position information for the start and 
 * end of a construct (known as a phrase) corresponding an AST node.
 *
 * NOTE THAT THE POSITION INFORMATION WILL NOT BE MARKED. HOWEVER, IT CAN BE
 * USEFUL TO DEBUG YOUR IMPLEMENTATION.
 *
 * (09-|-April-|-2016)


program       -> func-decl
func-decl     -> type identifier "(" ")" compound-stmt
type          -> void
identifier    -> ID
// statements
compound-stmt -> "{" stmt* "}" 
stmt          -> expr-stmt
expr-stmt     -> expr? ";"
// expressions 
expr                -> additive-expr
additive-expr       -> multiplicative-expr
                    |  additive-expr "+" multiplicative-expr
                    |  additive-expr "-" multiplicative-expr
multiplicative-expr -> unary-expr
	            |  multiplicative-expr "*" unary-expr
	            |  multiplicative-expr "/" unary-expr
unary-expr          -> "-" unary-expr
		    |  primary-expr

primary-expr        -> identifier
 		    |  INTLITERAL
		    | "(" expr ")"
 */

package VC.Parser;

import VC.Scanner.Scanner;
import VC.Scanner.SourcePosition;
import VC.Scanner.Token;
import VC.ErrorReporter;
import VC.ASTs.*;

public class Parser {

    private Scanner scanner;
    private ErrorReporter errorReporter;
    private Token currentToken;
    private SourcePosition previousTokenPosition;
    private SourcePosition dummyPos = new SourcePosition();

    public Parser(Scanner lexer, ErrorReporter reporter) {
        scanner = lexer;
        errorReporter = reporter;

        previousTokenPosition = new SourcePosition();

        currentToken = scanner.getToken();
    }

    // match checks to see f the current token matches tokenExpected.
    // If so, fetches the next token.
    // If not, reports a syntactic error.

    void match(int tokenExpected) throws SyntaxError {
        if (currentToken.kind == tokenExpected) {
            previousTokenPosition = currentToken.position;
            currentToken = scanner.getToken();
        } else {
            syntacticError("\"%\" expected here", Token.spell(tokenExpected));
        }
    }

    void accept() {
        previousTokenPosition = currentToken.position;
        currentToken = scanner.getToken();
    }

    void syntacticError(String messageTemplate, String tokenQuoted) throws SyntaxError {
        SourcePosition pos = currentToken.position;
        errorReporter.reportError(messageTemplate, tokenQuoted, pos);
        throw (new SyntaxError());
    }

    // start records the position of the start of a phrase.
    // This is defined to be the position of the first
    // character of the first token of the phrase.

    void start(SourcePosition position) {
        position.lineStart = currentToken.position.lineStart;
        position.charStart = currentToken.position.charStart;
    }

    // finish records the position of the end of a phrase.
    // This is defined to be the position of the last
    // character of the last token of the phrase.

    void finish(SourcePosition position) {
        position.lineFinish = previousTokenPosition.lineFinish;
        position.charFinish = previousTokenPosition.charFinish;
    }

    void copyStart(SourcePosition from, SourcePosition to) {
        to.lineStart = from.lineStart;
        to.charStart = from.charStart;
    }

    // ========================== PROGRAMS ========================

    public Program parseProgram() {

        Program programAST = null;

        SourcePosition programPos = new SourcePosition();
        start(programPos);
        List dlAST = new EmptyDeclList(dummyPos);
        try {
            if (currentToken.kind != Token.EOF) {
                Type tAST = parseType();
                Ident iAST = parseIdent();
                dlAST = parseDeclList(tAST, iAST);
            }
            match(Token.EOF);
            finish(programPos);
            programAST = new Program(dlAST, programPos);
        } catch (SyntaxError s) {
            return null;
        }
        return programAST;
    }

    // ========================== DECLARATIONS ========================

    List parseDeclList(Type tAST, Ident iAST) throws SyntaxError {
        List dlAST = null;
        Decl dAST = null;

        int flag = 0;
        SourcePosition dlPos = new SourcePosition();
        start(dlPos);

        if(currentToken.kind == Token.LPAREN){
            dAST = parseFuncDecl(tAST, iAST);
        }
        else {
            dAST = parseGlobalVarDecl(tAST, iAST);
            /* make sure it ends with ; or , */
            if(currentToken.kind == Token.SEMICOLON){
                match(Token.SEMICOLON);
            }
            else{
                match(Token.COMMA);
                flag = 1;
            }
        }
      
        if (currentToken.kind == Token.VOID || currentToken.kind == Token.BOOLEAN
        || currentToken.kind == Token.INT || currentToken.kind == Token.FLOAT) {
            tAST = parseType();
            iAST = parseIdent();
            dlAST = parseDeclList(tAST, iAST);
            finish(dlPos);
            dlAST = new DeclList(dAST, dlAST, dlPos);
        }
        else if(currentToken.kind == Token.ID && flag == 1) {
            iAST = parseIdent();
            if(currentToken.kind == Token.LPAREN){
                match(Token.SEMICOLON);
            }
            dlAST = parseDeclList(tAST, iAST);
            finish(dlPos);
            dlAST = new DeclList(dAST, dlAST, dlPos);
        }
        else if (dAST != null) {
            finish(dlPos);
            dlAST = new DeclList(dAST, new EmptyDeclList(dummyPos), dlPos);
        }
        if (dlAST == null)
            dlAST = new EmptyDeclList(dummyPos);

        return dlAST;
    }

    Decl parseFuncDecl(Type tAST, Ident iAST) throws SyntaxError {

        Decl fAST = null;

        SourcePosition funcPos = new SourcePosition();
        start(funcPos);

        /* not sure whether they would be null, depend on where you call them */
        /* pretty sure you wont set them null because of ambiguies */
        if(tAST == null){
            tAST = parseType();
        }
        if(iAST == null){
            iAST = parseIdent();
        }
        match(Token.LPAREN);
        List fplAST = parseParaList();
        match(Token.RPAREN);
        Stmt cAST = parseCompoundStmt();
        finish(funcPos);
        fAST = new FuncDecl(tAST, iAST, fplAST, cAST, funcPos);
        return fAST;
    }

    Decl parseGlobalVarDecl(Type tAST, Ident iAST) throws SyntaxError {
        Decl vAST = null;
        /* incase the array type */
        Expr eAST = null;
        SourcePosition vPos = new SourcePosition();
        start(vPos);

        if(tAST == null){
            tAST = parseType();
        }
        if(iAST == null){
            iAST = parseIdent();
        }
        Type realtAST = tAST;
        if(currentToken.kind == Token.LBRACKET){
            realtAST = parseArrayType(tAST);
        }
        /* with initializer */
        if(currentToken.kind == Token.EQ) {
            match(Token.EQ);
            if(currentToken.kind == Token.LCURLY){
                /* init expr */
                eAST = parseInitExpr();
            }
            else {
                eAST = parseExpr();
            }
        }
        else {
            eAST = new EmptyExpr(dummyPos);
        }
        finish(vPos);
        vAST = new GlobalVarDecl(realtAST, iAST, eAST, vPos);
        return vAST;
    }

    /* varDecl always happens in compoundStmt */
    List parseVarDeclList(Type tAST) throws SyntaxError {
        List vlAST = null;

        SourcePosition vlPos = new SourcePosition();
        start(vlPos);
        
        int flag = 0;
        Decl vAST = parseLocalVarDecl(tAST, null);
        if(currentToken.kind == Token.SEMICOLON){
            match(Token.SEMICOLON);
        }
        else if(currentToken.kind == Token.COMMA){
            match(Token.COMMA);
            flag = 1;
        }

        if(currentToken.kind == Token.VOID || currentToken.kind == Token.BOOLEAN
        || currentToken.kind == Token.INT || currentToken.kind == Token.FLOAT) {
            tAST = parseType();
            vlAST = parseVarDeclList(tAST);
            finish(vlPos);
            vlAST = new DeclList(vAST, vlAST, vlPos);
        }
        else if(currentToken.kind == Token.ID && flag == 1){
            vlAST = parseVarDeclList(tAST);
            finish(vlPos);
            vlAST = new DeclList(vAST, vlAST, vlPos);
        }
        else if(vAST == null) {
            vlAST = new EmptyDeclList(dummyPos);
        }
        else if(vAST != null) {
            finish(vlPos);
            vlAST = new DeclList(vAST, new EmptyDeclList(dummyPos), vlPos);
        }

        return vlAST;
    }


    Decl parseLocalVarDecl(Type tAST, Ident iAST) throws SyntaxError {
        Decl vAST = null;
        /* incase the array type */
        Expr eAST = null;
        SourcePosition vPos = new SourcePosition();
        start(vPos);

        if(tAST == null){
            tAST = parseType();
        }
        if(iAST == null){
            iAST = parseIdent();
        }
        Type realtAST = tAST;
        if(currentToken.kind == Token.LBRACKET){
            realtAST = parseArrayType(tAST);
        }
        /* with initializer */
        if(currentToken.kind == Token.EQ) {
            match(Token.EQ);
            if(currentToken.kind == Token.LCURLY){
                /* init expr */
                eAST = parseInitExpr();
            }
            else {
                eAST = parseExpr();
            }
        }
        else {
            eAST = new EmptyExpr(dummyPos);
        }
        finish(vPos);
        vAST = new LocalVarDecl(realtAST, iAST, eAST, vPos);
        return vAST;
    }


    //  ======================== TYPES ==========================

    Type parseArrayType(Type tAST) throws SyntaxError {
        Expr eAST = null;
        SourcePosition aPos = tAST.getPosition();
        SourcePosition ePos = new SourcePosition();
        match(Token.LBRACKET);
        start(ePos);
        if(currentToken.kind == Token.RBRACKET){
            eAST = new EmptyExpr(ePos);
        }
        else {
            IntLiteral iAST = parseIntLiteral();
            eAST = new IntExpr(iAST, ePos);
        }
        match(Token.RBRACKET);
        finish(ePos);
        Type realtAST = new ArrayType(tAST, eAST, aPos);
        return realtAST;
    }

    Type parseType() throws SyntaxError {
        Type tAST = null;
        SourcePosition typePos = new SourcePosition();
        start(typePos);
        switch (currentToken.kind) {

        case Token.VOID:
            accept();
            finish(typePos);
            tAST = new VoidType(typePos);
            break;
        case Token.BOOLEAN:
            accept();
            finish(typePos);
            tAST = new BooleanType(typePos);
            break;
        case Token.INT:
            accept();
            finish(typePos);
            tAST = new IntType(typePos);
            break;
        default:
            accept();
            finish(typePos);
            tAST = new FloatType(typePos);
        }
        return tAST;
    }

    // ======================= STATEMENTS ==============================

    Stmt parseCompoundStmt() throws SyntaxError {
        Stmt cAST = null;

        SourcePosition stmtPos = new SourcePosition();
        start(stmtPos);

        match(Token.LCURLY);

        List dlAST = new EmptyDeclList(dummyPos);
        // Insert code here to build a DeclList node for variable declarations
        if(currentToken.kind == Token.VOID || currentToken.kind == Token.BOOLEAN
        || currentToken.kind == Token.INT || currentToken.kind == Token.FLOAT) {
            Type tAST = parseType();
            dlAST = parseVarDeclList(tAST);
        }
        List slAST = parseStmtList();
        match(Token.RCURLY);
        finish(stmtPos);

        /* In the subset of the VC grammar, no variable declarations are
         * allowed. Therefore, a block is empty iff it has no statements.
         */
        if (slAST instanceof EmptyStmtList && dlAST instanceof EmptyDeclList)
            cAST = new EmptyCompStmt(stmtPos);
        else
            cAST = new CompoundStmt(dlAST, slAST, stmtPos);
        return cAST;
    }

    List parseStmtList() throws SyntaxError {
        List slAST = null;

        SourcePosition stmtPos = new SourcePosition();
        start(stmtPos);

        if (currentToken.kind != Token.RCURLY) {
            Stmt sAST = parseStmt();
            {
                if (currentToken.kind != Token.RCURLY) {
                    slAST = parseStmtList();
                    finish(stmtPos);
                    slAST = new StmtList(sAST, slAST, stmtPos);
                } else {
                    finish(stmtPos);
                    slAST = new StmtList(sAST, new EmptyStmtList(dummyPos), stmtPos);
                }
            }
        } else
            slAST = new EmptyStmtList(dummyPos);

        return slAST;
    }

    Stmt parseStmt() throws SyntaxError {
        Stmt sAST = null;

        SourcePosition sPos = new SourcePosition();
        start(sPos);

        switch(currentToken.kind) {

            case Token.LCURLY:
            sAST = parseCompoundStmt();
            break;

            case Token.IF:
            sAST = parseIfStmt();
            break;

            case Token.FOR:
            sAST = parseForStmt();
            break;

            case Token.WHILE:
            sAST = parseWhileStmt();
            break;

            case Token.CONTINUE:
            accept();
            finish(sPos);
            sAST = new ContinueStmt(sPos);
            match(Token.SEMICOLON);
            break;

            case Token.BREAK:
            accept();
            finish(sPos);
            sAST = new BreakStmt(sPos);
            match(Token.SEMICOLON);
            break;

            case Token.RETURN:
            accept();
            Expr eAST = parseExpr();
            finish(sPos);
            match(Token.SEMICOLON);
            sAST = new ReturnStmt(eAST, sPos);
            break;

            default:
            sAST = parseExprStmt();
        }

        return sAST;
    }

    Stmt parseIfStmt() throws SyntaxError {
        SourcePosition sPos = new SourcePosition();
        start(sPos);
        Stmt sAST = null;
        match(Token.IF);
        match(Token.LPAREN);
        Expr eAST = parseExpr();
        match(Token.RPAREN);
        Stmt s1AST = parseStmt();
        if(currentToken.kind == Token.ELSE){
            match(Token.ELSE);
            Stmt s2AST = parseStmt();
            finish(sPos);
            sAST = new IfStmt(eAST, s1AST, s2AST, sPos);
        }
        else {
            sAST = new IfStmt(eAST, s1AST, sPos);
        }

        return sAST;
    }

    Stmt parseForStmt() throws SyntaxError {
        Stmt sAST = null;

        SourcePosition sPos = new SourcePosition();
        start(sPos);

        Expr e1AST = new EmptyExpr(dummyPos);
        Expr e2AST = new EmptyExpr(dummyPos);
        Expr e3AST = new EmptyExpr(dummyPos);
        match(Token.FOR);
        match(Token.LPAREN);
        if(currentToken.kind != Token.SEMICOLON) {
            e1AST = parseExpr();
        }
        match(Token.SEMICOLON);
        if(currentToken.kind != Token.SEMICOLON) {
            e2AST = parseExpr();
        }
        match(Token.SEMICOLON);
        if(currentToken.kind != Token.RPAREN) {
            e3AST = parseExpr();
        }
        match(Token.RPAREN);
        Stmt s2AST = parseStmt();

        finish(sPos);
        sAST = new ForStmt(e1AST, e2AST, e3AST, s2AST, sPos);

        return sAST;
    }

    Stmt parseWhileStmt() throws SyntaxError {
        SourcePosition sPos = new SourcePosition();
        start(sPos);
        match(Token.WHILE);
        match(Token.LPAREN);
        Expr eAST = parseExpr();
        match(Token.RPAREN);
        Stmt sAST = parseStmt();
        finish(sPos);
        return new WhileStmt(eAST, sAST, sPos);
    }

    Stmt parseExprStmt() throws SyntaxError {
        Stmt sAST = null;

        SourcePosition stmtPos = new SourcePosition();
        start(stmtPos);

        if(currentToken.kind != Token.SEMICOLON){
            Expr eAST = parseExpr();
            match(Token.SEMICOLON);
            finish(stmtPos);
            sAST = new ExprStmt(eAST, stmtPos);
        } else {
            match(Token.SEMICOLON);
            finish(stmtPos);
            sAST = new ExprStmt(new EmptyExpr(dummyPos), stmtPos);
        }
        return sAST;
    }

    // ======================= PARAMETERS =======================

    List parseParaList() throws SyntaxError {
        List formalsAST = null;

        SourcePosition formalsPos = new SourcePosition();
        start(formalsPos);

        if(currentToken.kind != Token.RPAREN){
            ParaDecl pAST = parseParaDecl();
            if(currentToken.kind == Token.COMMA){
                match(Token.COMMA);
                List plAST = parseParaList();
                finish(formalsPos);
                formalsAST = new ParaList(pAST , plAST, formalsPos);
            }
            else {
                finish(formalsPos);
                formalsAST = new ParaList(pAST, new EmptyParaList(dummyPos), formalsPos);
            }
       }
        else {
            finish(formalsPos);
            formalsAST = new EmptyParaList(formalsPos);
        }

        return formalsAST;
    }

    ParaDecl parseParaDecl() throws SyntaxError {
        SourcePosition pPos = new SourcePosition();
        start(pPos);
        Type realtAST = null;
        Type tAST = parseType();
        realtAST = tAST;
        Ident iAST = parseIdent();
        if(currentToken.kind == Token.LBRACKET) {
            realtAST = parseArrayType(tAST);
        }
        finish(pPos);
        return new ParaDecl(realtAST, iAST, pPos);
    }

    List parseArgList() throws SyntaxError {
        SourcePosition aPos = new SourcePosition();
        start(aPos);

        List arlAST = new EmptyArgList(dummyPos);
        if(currentToken.kind == Token.RPAREN) {
            return arlAST;
        }
        else {
            Expr eAST = parseExpr();
            finish(aPos);
            Arg aAST = new Arg(eAST, aPos);
            if(currentToken.kind == Token.COMMA) {
                match(Token.COMMA);
                List arlList = parseArgList();
                finish(aPos);
                arlAST = new ArgList(aAST, arlList, aPos);
            }
            else{
                finish(aPos);
                arlAST = new ArgList(aAST, new EmptyArgList(dummyPos), aPos);
            }
        }

        return arlAST;
    }


    // ======================= EXPRESSIONS ======================
    /* special Exprs */
    Expr parseInitExpr() throws SyntaxError {
        SourcePosition ePos = new SourcePosition();
        start(ePos);
        match(Token.LCURLY);
        List elAST = parseExprList();
        finish(ePos);
        Expr eAST = new InitExpr(elAST, ePos);
        return eAST;
    }
    /* only used for Init Expr */
    List parseExprList() throws SyntaxError {
        SourcePosition elPos = new SourcePosition();
        start(elPos);
        List elAST = null;
        if(currentToken.kind == Token.RCURLY){
            return new EmptyExprList(dummyPos);
        }
        Expr eAST = parseExpr();
        if(currentToken.kind == Token.RCURLY){
            /* end of list */
            match(Token.RCURLY);
            finish(elPos);
            elAST = new ExprList(eAST, new EmptyExprList(dummyPos), elPos);
        }
        else {
            match(Token.COMMA);
            List nextElList = parseExprList();
            finish(elPos);
            elAST = new ExprList(eAST, nextElList, elPos);
        }
        return elAST;
    }

    Expr parseExpr() throws SyntaxError {
        Expr exprAST = null;
        exprAST = parseAssignExpr();
        return exprAST;
    }

    Expr parseAssignExpr() throws SyntaxError {
        Expr exprAST = null;

        SourcePosition assStartPos = new SourcePosition();
        start(assStartPos);

        exprAST = parseCondOrExpr();
        while(currentToken.kind == Token.EQ) {
            Operator opAST = acceptOperator();
            Expr e2AST = parseAssignExpr();

            SourcePosition assPos = new SourcePosition();
            copyStart(assStartPos, assPos);
            finish(assPos);
            exprAST = new AssignExpr(exprAST, e2AST, assPos);
        }


        return exprAST;
    }

    Expr parseCondOrExpr() throws SyntaxError {
        Expr exprAST = null;

        SourcePosition orStartPos = new SourcePosition();
        start(orStartPos);

        exprAST = parseCondAndExpr();
        while(currentToken.kind == Token.OROR) {
            Operator opAST = acceptOperator();
            Expr e2AST = parseCondAndExpr();

            SourcePosition orPos = new SourcePosition();
            copyStart(orStartPos, orPos);
            finish(orPos);
            exprAST = new BinaryExpr(exprAST, opAST, e2AST, orPos);
        }

        return exprAST;

    }

    Expr parseCondAndExpr() throws SyntaxError {
        Expr exprAST = null;

        SourcePosition andStartPos = new SourcePosition();
        start(andStartPos);

        exprAST = parseEqExpr();
        while(currentToken.kind == Token.ANDAND) {
            Operator opAST = acceptOperator();
            Expr e2AST = parseEqExpr();

            SourcePosition andPos = new SourcePosition();
            copyStart(andStartPos, andPos);
            finish(andPos);
            exprAST = new BinaryExpr(exprAST, opAST, e2AST, andPos);
        }

        return exprAST;
    }

    Expr parseEqExpr() throws SyntaxError {
        Expr exprAST = null;

        SourcePosition eqStartPos = new SourcePosition();
        start(eqStartPos);

        exprAST = parseRelExpr();
        while(currentToken.kind == Token.EQEQ || currentToken.kind == Token.NOTEQ) {
            Operator opAST = acceptOperator();
            Expr e2AST = parseRelExpr();

            SourcePosition eqPos = new SourcePosition();
            copyStart(eqStartPos, eqPos);
            finish(eqPos);
            exprAST = new BinaryExpr(exprAST, opAST, e2AST, eqPos);
        }

        return exprAST;
    }

    Expr parseRelExpr() throws SyntaxError {
        Expr exprAST = null;

        SourcePosition relStartPos = new SourcePosition();
        start(relStartPos);

        exprAST = parseAdditiveExpr();
        while(currentToken.kind == Token.LT || currentToken.kind == Token.GT
        || currentToken.kind == Token.LTEQ || currentToken.kind == Token.GTEQ) {
            Operator opAST = acceptOperator();
            Expr e2AST = parseAdditiveExpr();

            SourcePosition relPos = new SourcePosition();
            copyStart(relStartPos, relPos);
            finish(relPos);
            exprAST = new BinaryExpr(exprAST, opAST, e2AST, relPos);
        }

        return exprAST;
    }

    Expr parseAdditiveExpr() throws SyntaxError {
        Expr exprAST = null;

        SourcePosition addStartPos = new SourcePosition();
        start(addStartPos);

        exprAST = parseMultiplicativeExpr();
        while (currentToken.kind == Token.PLUS || currentToken.kind == Token.MINUS) {
            Operator opAST = acceptOperator();
            Expr e2AST = parseMultiplicativeExpr();

            SourcePosition addPos = new SourcePosition();
            copyStart(addStartPos, addPos);
            finish(addPos);
            exprAST = new BinaryExpr(exprAST, opAST, e2AST, addPos);
        }
        return exprAST;
    }

    Expr parseMultiplicativeExpr() throws SyntaxError {

        Expr exprAST = null;

        SourcePosition multStartPos = new SourcePosition();
        start(multStartPos);

        exprAST = parseUnaryExpr();
        while (currentToken.kind == Token.MULT || currentToken.kind == Token.DIV) {
            Operator opAST = acceptOperator();
            Expr e2AST = parseUnaryExpr();
            SourcePosition multPos = new SourcePosition();
            copyStart(multStartPos, multPos);
            finish(multPos);
            exprAST = new BinaryExpr(exprAST, opAST, e2AST, multPos);
        }
        return exprAST;
    }

    Expr parseUnaryExpr() throws SyntaxError {

        Expr exprAST = null;

        SourcePosition unaryPos = new SourcePosition();
        start(unaryPos);

        switch (currentToken.kind) {
        case Token.MINUS: 
        case Token.PLUS:
        case Token.NOT:
        {
            Operator opAST = acceptOperator();
            Expr e2AST = parseUnaryExpr();
            finish(unaryPos);
            exprAST = new UnaryExpr(opAST, e2AST, unaryPos);
        }
            break;

        default:
            exprAST = parsePrimaryExpr();
            break;

        }
        return exprAST;
    }

    Expr parsePrimaryExpr() throws SyntaxError {

        Expr exprAST = null;

        SourcePosition primPos = new SourcePosition();
        start(primPos);

        switch (currentToken.kind) {

        case Token.ID:
            Ident iAST = parseIdent();
            if(currentToken.kind == Token.LPAREN) {
                /* call expr */
                match(Token.LPAREN);
                List aplAST = parseArgList();
                match(Token.RPAREN);
                finish(primPos);
                exprAST = new CallExpr(iAST, aplAST, primPos);
            }
            else if(currentToken.kind == Token.LBRACKET) {
                finish(primPos);
                accept();
                Expr indexAST = parseExpr();
                Var idAST = new SimpleVar(iAST, primPos);
                match(Token.RBRACKET);
                finish(primPos);
                exprAST = new ArrayExpr(idAST, indexAST, primPos);
            }
            else {
                finish(primPos);
                Var simVAST = new SimpleVar(iAST, primPos);
                exprAST = new VarExpr(simVAST, primPos);
            }
            break;

        case Token.LPAREN: {
            accept();
            exprAST = parseExpr();
            match(Token.RPAREN);
        }
            break;

        case Token.INTLITERAL:
            IntLiteral ilAST = parseIntLiteral();
            finish(primPos);
            exprAST = new IntExpr(ilAST, primPos);
            break;

        case Token.FLOATLITERAL:
            FloatLiteral flAST = parseFloatLiteral();
            finish(primPos);
            exprAST = new FloatExpr(flAST, primPos);
            break;

        case Token.BOOLEANLITERAL:
            BooleanLiteral blAST = parseBooleanLiteral();
            finish(primPos);
            exprAST = new BooleanExpr(blAST, primPos);
            break;

        case Token.STRINGLITERAL:
            StringLiteral slAST = parseStringLiteral();
            finish(primPos);
            exprAST = new StringExpr(slAST, primPos);
            break;

        default:
            syntacticError("illegal primary expression", currentToken.spelling);

        }
        return exprAST;
    }

    // ========================== ID, OPERATOR and LITERALS ========================

    Ident parseIdent() throws SyntaxError {

        Ident I = null;

        if (currentToken.kind == Token.ID) {
            previousTokenPosition = currentToken.position;
            String spelling = currentToken.spelling;
            I = new Ident(spelling, previousTokenPosition);
            currentToken = scanner.getToken();
        } else
            syntacticError("identifier expected here", "");
        return I;
    }

    // acceptOperator parses an operator, and constructs a leaf AST for it

    Operator acceptOperator() throws SyntaxError {
        Operator O = null;

        previousTokenPosition = currentToken.position;
        String spelling = currentToken.spelling;
        O = new Operator(spelling, previousTokenPosition);
        currentToken = scanner.getToken();
        return O;
    }

    IntLiteral parseIntLiteral() throws SyntaxError {
        IntLiteral IL = null;

        if (currentToken.kind == Token.INTLITERAL) {
            String spelling = currentToken.spelling;
            accept();
            IL = new IntLiteral(spelling, previousTokenPosition);
        } else
            syntacticError("integer literal expected here", "");
        return IL;
    }

    FloatLiteral parseFloatLiteral() throws SyntaxError {
        FloatLiteral FL = null;

        if (currentToken.kind == Token.FLOATLITERAL) {
            String spelling = currentToken.spelling;
            accept();
            FL = new FloatLiteral(spelling, previousTokenPosition);
        } else
            syntacticError("float literal expected here", "");
        return FL;
    }

    BooleanLiteral parseBooleanLiteral() throws SyntaxError {
        BooleanLiteral BL = null;

        if (currentToken.kind == Token.BOOLEANLITERAL) {
            String spelling = currentToken.spelling;
            accept();
            BL = new BooleanLiteral(spelling, previousTokenPosition);
        } else
            syntacticError("boolean literal expected here", "");
        return BL;
    }

    StringLiteral parseStringLiteral() throws SyntaxError {
        StringLiteral SL = null;

        if (currentToken.kind == Token.STRINGLITERAL) {
            String spelling = currentToken.spelling;
            accept();
            SL = new StringLiteral(spelling, previousTokenPosition);
        } else
            syntacticError("string literal expected here", "");
        return SL;
    }

}
