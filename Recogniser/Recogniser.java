/***
 * *
 * * Recogniser.java            
 * *
 ***/

/* At this stage, this parser accepts a subset of VC defined	by
 * the following grammar. 
 *
 * You need to modify the supplied parsing methods (if necessary) and 
 * add the missing ones to obtain a parser for the VC language.
 *
 * (17---March---2017)

program       -> func-decl

// declaration

func-decl     -> void identifier "(" ")" compound-stmt

identifier    -> ID

// statements 
compound-stmt -> "{" stmt* "}" 
stmt          -> continue-stmt
    	      |  expr-stmt
continue-stmt -> continue ";"
expr-stmt     -> expr? ";"

// expressions 
expr                -> assignment-expr
assignment-expr     -> additive-expr
additive-expr       -> multiplicative-expr
                    |  additive-expr "+" multiplicative-expr
multiplicative-expr -> unary-expr
	            |  multiplicative-expr "*" unary-expr
unary-expr          -> "-" unary-expr
		    |  primary-expr

primary-expr        -> identifier
 		    |  INTLITERAL
		    | "(" expr ")"
*/

package VC.Recogniser;

import VC.Scanner.Scanner;
import VC.Scanner.SourcePosition;
import VC.Scanner.Token;

import java.sql.SQLSyntaxErrorException;

import VC.ErrorReporter;

public class Recogniser {

    private Scanner scanner;
    private ErrorReporter errorReporter;
    private Token currentToken;

    public Recogniser(Scanner lexer, ErrorReporter reporter) {
        scanner = lexer;
        errorReporter = reporter;

        currentToken = scanner.getToken();
    }

    // match checks to see f the current token matches tokenExpected.
    // If so, fetches the next token.
    // If not, reports a syntactic error.

    void match(int tokenExpected) throws SyntaxError {
        if (currentToken.kind == tokenExpected) {
            currentToken = scanner.getToken();
        } else {
            syntacticError("\"%\" expected here", Token.spell(tokenExpected));
        }
    }

    // accepts the current token and fetches the next
    void accept() {
        currentToken = scanner.getToken();
    }

    void syntacticError(String messageTemplate, String tokenQuoted) throws SyntaxError {
        SourcePosition pos = currentToken.position;
        errorReporter.reportError(messageTemplate, tokenQuoted, pos);
        throw (new SyntaxError());
    }

    // ========================== SELF DEFINE FUNCS==================

    // ========================== PROGRAMS ========================

    public void parseProgram() {

        try {
            while (currentToken.kind != Token.EOF) {
                parseType();
                parseIdent();
                switch(currentToken.kind){
                    case Token.LPAREN:
                        parseFuncDecl();
                        break;
                    default:
                        parseVarDecl();
                }
                parseFuncDecl();
            }
        } catch (SyntaxError s) {
        }
    }

    // ========================== DECLARATIONS ========================

    void parseFuncDecl() throws SyntaxError {
        parseParaList();
        parseCompoundStmt();
    }


    void parseVarDecl() throws SyntaxError {
        parseInitDecList();
        match(Token.SEMICOLON);
    }

    void parseInitDecList() throws SyntaxError {
        parseInitDeclarator();
        while(currentToken.kind == Token.COMMA){
            parseInitDeclarator();
        }
    }

    void parseInitDeclarator() throws SyntaxError {
        parseDeclarator();
        if(currentToken.kind == Token.EQ){
            parseInitialiser();
        }
    }
    // ID already parsed so no more ID been parse in Declarator
    void parseDeclarator() throws SyntaxError {
        if(currentToken.kind == Token.LBRACKET){
            match(Token.LBRACKET);
            if(currentToken.kind == Token.INTLITERAL){
                match(Token.INTLITERAL);
            }
            match(Token.RBRACKET);
        }
    }

    void parseInitialiser() throws SyntaxError {
        switch(currentToken.kind){
            case Token.LCURLY:
                match(Token.LCURLY);
                parseExpr();
                while(currentToken.kind == Token.COMMA){
                    match(Token.COMMA);
                    parseExpr();
                }
                match(Token.RCURLY);
                break;
            default:
                parseExpr();
        }

    }
    // ======================= PRIMITIVE TYPES =========================
    void parseType() throws SyntaxError {
        switch(currentToken.kind){
            case Token.VOID:
                match(Token.VOID);
                break;
            case Token.BOOLEAN:
                match(Token.BOOLEAN);
                break;
            case Token.INT:
                match(Token.INT);
                break;
            default:
                match(Token.FLOAT);
        }
    }

    // ======================= STATEMENTS ==============================

    void parseCompoundStmt() throws SyntaxError {

        match(Token.LCURLY);
        parseVarDeclList();
        parseStmtList();
        match(Token.RCURLY);
    }
    void parseVarDeclList() throws SyntaxError {
        while(currentToken.kind == Token.INT || currentToken.kind == Token.BOOLEAN 
        || currentToken.kind == Token.FLOAT || currentToken.kind == Token.VOID){
            parseType();
            parseIdent();
            parseVarDecl();
        }
    }

    // Here, a new nontermial has been introduced to define { stmt } *
    void parseStmtList() throws SyntaxError {

        while (currentToken.kind != Token.RCURLY)
            parseStmt();
    }

    void parseStmt() throws SyntaxError {

        switch (currentToken.kind) {
            case Token.LCURLY:
                parseCompoundStmt();
                break;
            
            case Token.IF:
                parseIfStmt();
                break;
            
            case Token.FOR:
                parseForStmt();
                break;
            
            case Token.WHILE:
                parseWhileStmt();
                break;

            case Token.BREAK:
                parseBreakStmt();
                break;

            case Token.CONTINUE:
                parseContinueStmt();
                break;
            
            case Token.RETURN:
                parseReturnStmt();
                break;

            default:
                parseExprStmt();
                break;

        }
    }
    void parseIfStmt() throws SyntaxError {
        match(Token.IF);
        match(Token.LPAREN);
        parseExpr();
        match(Token.RPAREN);
        parseStmt();
        if(currentToken.kind == Token.ELSE){
            match(Token.ELSE);
            parseStmt();
        }
    }

    void parseForStmt() throws SyntaxError {
        match(Token.FOR);
        match(Token.LPAREN);
        if(currentToken.kind != Token.SEMICOLON){
            parseExpr();
        }
        match(Token.SEMICOLON);
        if(currentToken.kind != Token.SEMICOLON){
            parseExpr();
        }
        match(Token.SEMICOLON);
        if(currentToken.kind != Token.SEMICOLON){
            parseExpr();
        }
        match(Token.SEMICOLON);
        match(Token.RPAREN);
        parseStmt();
    }

    void parseWhileStmt() throws SyntaxError {
        match(Token.LPAREN);
        parseExpr();
        match(Token.RPAREN);
        parseStmt();
    }

    void parseBreakStmt() throws SyntaxError {
        match(Token.BREAK);
        match(Token.SEMICOLON);
    }

    void parseReturnStmt() throws SyntaxError {
        match(Token.RETURN);
        match(Token.SEMICOLON);
    }
    void parseContinueStmt() throws SyntaxError {

        match(Token.CONTINUE);
        match(Token.SEMICOLON);

    }

    void parseExprStmt() throws SyntaxError {

        if(currentToken.kind != Token.SEMICOLON){
            parseExpr();
            match(Token.SEMICOLON);
        } else {
            match(Token.SEMICOLON);
        }
    }

    // ======================= IDENTIFIERS ======================

    // Call parseIdent rather than match(Token.ID). 
    // In Assignment 3, an Identifier node will be constructed in here.

    void parseIdent() throws SyntaxError {

        if (currentToken.kind == Token.ID) {
            currentToken = scanner.getToken();
        } else
            syntacticError("identifier expected here", "");
    }

    // ======================= OPERATORS ======================

    // Call acceptOperator rather than accept(). 
    // In Assignment 3, an Operator Node will be constructed in here.

    void acceptOperator() throws SyntaxError {

        currentToken = scanner.getToken();
    }

    // ======================= EXPRESSIONS ======================

    void parseExpr() throws SyntaxError {
        parseAssignExpr();
    }

    void parseAssignExpr() throws SyntaxError {

        parseAdditiveExpr();

    }

    void parseAdditiveExpr() throws SyntaxError {

        parseMultiplicativeExpr();
        while (currentToken.kind == Token.PLUS) {
            acceptOperator();
            parseMultiplicativeExpr();
        }
    }

    void parseMultiplicativeExpr() throws SyntaxError {

        parseUnaryExpr();
        while (currentToken.kind == Token.MULT) {
            acceptOperator();
            parseUnaryExpr();
        }
    }

    void parseUnaryExpr() throws SyntaxError {

        switch (currentToken.kind) {
        case Token.MINUS: {
            acceptOperator();
            parseUnaryExpr();
        }
            break;

        default:
            parsePrimaryExpr();
            break;

        }
    }

    void parsePrimaryExpr() throws SyntaxError {

        switch (currentToken.kind) {

        case Token.ID:
            parseIdent();
            break;

        case Token.LPAREN: {
            accept();
            parseExpr();
            match(Token.RPAREN);
        }
            break;

        case Token.INTLITERAL:
            parseIntLiteral();
            break;

        default:
            syntacticError("illegal parimary expression", currentToken.spelling);

        }
    }

    // ========================== LITERALS ========================

    // Call these methods rather than accept().  In Assignment 3, 
    // literal AST nodes will be constructed inside these methods. 

    void parseIntLiteral() throws SyntaxError {

        if (currentToken.kind == Token.INTLITERAL) {
            currentToken = scanner.getToken();
        } else
            syntacticError("integer literal expected here", "");
    }

    void parseFloatLiteral() throws SyntaxError {

        if (currentToken.kind == Token.FLOATLITERAL) {
            currentToken = scanner.getToken();
        } else
            syntacticError("float literal expected here", "");
    }

    void parseBooleanLiteral() throws SyntaxError {

        if (currentToken.kind == Token.BOOLEANLITERAL) {
            currentToken = scanner.getToken();
        } else
            syntacticError("boolean literal expected here", "");
    }

    //  ======================= PARAMETERS ======================
    void parseParaList() throws SyntaxError {
        match(Token.LPAREN);
        switch(currentToken.kind) {
            case Token.RPAREN:
                match(Token.RPAREN);
                break;
            default:
                parseProperParaList();
                match(Token.RPAREN);
        }
    }

    void parseProperParaList() throws SyntaxError {
        parseParaDecl();
        while(currentToken.kend == Token.COMMA){
            match(Token.COMMA);
            parseParaDecl();
        }
    }

    void parseParaDecl() throws SyntaxError {
        parseType();
        // parse ID first
        parseIdent();
        parseDeclarator();
    }

}
