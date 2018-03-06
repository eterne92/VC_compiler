/**
 **	Scanner.java                        
 **/

package VC.Scanner;

import VC.ErrorReporter;
import VC.vc;

public final class Scanner {

  private SourceFile sourceFile;
  private boolean debug;

  private ErrorReporter errorReporter;
  private StringBuffer currentSpelling;
  private char currentChar;
  private SourcePosition sourcePos;
  private int charLine;
  private int charCol;

  // =========================================================

  public Scanner(SourceFile source, ErrorReporter reporter) {
    sourceFile = source;
    errorReporter = reporter;
    currentChar = sourceFile.getNextChar();
    debug = false;

    // you may initialise your counters for line and column numbers here
    charLine = 1;
    charCol = 1;
  }

  public void enableDebugging() {
    debug = true;
  }

  // accept gets the next character from the source program.

  private void accept() {
    currentSpelling.append(currentChar);
    if(currentChar == '\n'){
      charLine ++;
      charCol = 1;
    }
    else if(currentChar == '\t'){
      int spaces = (((charCol - 1) / 8) + 1) * 8 - charCol + 1;
      for(int i= 0;i<spaces;i++){
        currentSpelling.append(' ');
      }
      charCol += spaces;
    }
    else{
      charCol ++;
    }
    currentChar = sourceFile.getNextChar();

    // you may save the lexeme of the current token incrementally here
    // you may also increment your line and column counters here
  }

  // inspectChar returns the n-th character after currentChar
  // in the input stream. 
  //
  // If there are fewer than nthChar characters between currentChar 
  // and the end of file marker, SourceFile.eof is returned.
  // 
  // Both currentChar and the current position in the input stream
  // are *not* changed. Therefore, a subsequent call to accept()
  // will always return the next char after currentChar.

  private char inspectChar(int nthChar) {
    return sourceFile.inspectChar(nthChar);
  }

  private int fraction(int n){
     //with digit
      while(Character.isDigit(currentChar)){
        accept();
      }
      //e or E
      //without e or E, it's a float
      if(currentChar != 'e' && currentChar != 'E'){
        return Token.FLOATLITERAL;
      }
      //with e, we need to look forward
      else{
        //if next one is + or -
        if(inspectChar(1) == '-' || inspectChar(1) == '+'){
          //if there is a digit after + or -, it's a float
          if(Character.isDigit(inspectChar(2))){
            accept();//accept Ee
            accept();//accept +-
            while(Character.isDigit(currentChar)){
              accept();
            }
            return Token.FLOATLITERAL;
          }
          //if there isn't, float ends before e
          else{
            return Token.FLOATLITERAL;
          }
        }
        //if next one is digit, it's a float
        else if(Character.isDigit(inspectChar(1))){
          accept();
          while(Character.isDigit(currentChar)){
            accept();
          }
          return Token.FLOATLITERAL;
        }
      }
    return n;
  }
  private int nextToken() {
    // Tokens: separators, operators, literals, identifiers and keyworods

    switch (currentChar) {
    // separators 
    case '(':
      accept();
      return Token.LPAREN;
    case ')':
      accept();
      return Token.RPAREN;
    case '{':
      accept();
      return Token.LCURLY;
    case '}':
      accept();
      return Token.RCURLY;
    case '[':
      accept();;
      return Token.LBRACKET;
    case ']':
      accept();
      return Token.RBRACKET;
    case ';':
      accept();
      return Token.SEMICOLON;
    case ',':
      accept();
      return Token.COMMA;

    // floatliteral start with .
    case '.':
      //digit part first
      //without digit it's an error(only .)
      if(!Character.isDigit(inspectChar(1))){
        accept();
        return Token.ERROR;
      }
      accept();
      return fraction(Token.FLOATLITERAL);

    //operators
    case '|':
      accept();
      if (currentChar == '|') {
        accept();
        return Token.OROR;
      } else {
        return Token.ERROR;
      }

    case '+':
      accept();
      return Token.PLUS;

    case '-':
      accept();
      return Token.MINUS;

    case '*':
      accept();
      return Token.MULT;

    case '/':
      accept();
      return Token.DIV;

    case '=':
      accept();
      if (currentChar == '='){
        accept();
        return Token.EQEQ;
      }
      else{
        return Token.EQ;
      }

    case '!':
      accept();
      if(currentChar == '='){
        accept();
        return Token.NOTEQ;
      }
      else{
        return Token.NOT;
      }

    case '&':
      accept();
      if(currentChar == '&'){
        accept();
        return Token.ANDAND;
      }
      else{
        return Token.ERROR;
      }

    case '<':
      accept();
      if(currentChar == '='){
        accept();
        return Token.LTEQ;
      }
      else{
        return Token.LT;
      }
    
    case '>':
      accept();
      if(currentChar == '='){
        accept();
        return Token.GTEQ;
      }
      else{
        return Token.GT;
      }

    //stringliteral
    case '"':
      accept();
      currentSpelling.deleteCharAt(currentSpelling.length()-1);
      while(currentChar != '"'){
        if(currentChar == '\\'){
          accept();
          if(currentChar != 'b' && currentChar != 'f' && currentChar != 'n' &&
              currentChar != 'r' && currentChar != 't' && currentChar != '\'' &&
                currentChar != '"' && currentChar != '\\'){
                  SourcePosition currentPos = new SourcePosition();
                  currentPos.lineStart = sourcePos.lineStart;
                  currentPos.charStart = sourcePos.charStart;
                  currentPos.lineFinish = charLine;
                  currentPos.charFinish = charCol - 1;
                  errorReporter.reportError("%: illegal escape character",
                   "\\" + currentChar, currentPos);
            accept();
          }
          else{
            currentSpelling.deleteCharAt(currentSpelling.length() - 1);
            switch(currentChar){
              case 'b':
                currentSpelling.append('\b');
                break;
              case 'r':
                currentSpelling.append('\r');
                break;
              case 'n':
                currentSpelling.append('\n');
                break;
              case 'f':
                currentSpelling.append('\f');
                break;
              case 't':
                currentSpelling.append('\t');
                break;
              case '\'':
                currentSpelling.append('\'');
                break;
              case '"':
                currentSpelling.append('"');
                break;
              case '\\':
                currentSpelling.append('\\');
                break;
            }
            accept();
            currentSpelling.deleteCharAt(currentSpelling.length() - 1);
          }
        }
        else if(currentChar == '\n'){
          // SourcePosition currentPos = new SourcePosition();
          // currentPos.lineStart = sourcePos.lineStart;
          // currentPos.charStart = sourcePos.charStart;
          // currentPos.lineFinish = charLine;
          // currentPos.charFinish = charCol;
          errorReporter.reportError("%: unterminated string", 
           currentSpelling.toString(), sourcePos);
          return Token.STRINGLITERAL;
        }
        else{
          accept();
        }
      }
      accept();
      currentSpelling.deleteCharAt(currentSpelling.length()-1);
      return Token.STRINGLITERAL;
    case SourceFile.eof:
      currentSpelling.append(Token.spell(Token.EOF));
      return Token.EOF;
    default:
      if(Character.isDigit(currentChar)){
        while(Character.isDigit(currentChar)){
          accept();
        }
        if(currentChar == '.'){
          accept();
          return fraction(Token.INTLITERAL);
        }
        else if(currentChar == 'E' || currentChar == 'e'){
          return fraction(Token.INTLITERAL);
        }
        else{
          return Token.INTLITERAL;
        }
      }
      else if(letter(currentChar)){
        accept();
        while(letter(currentChar) || Character.isDigit(currentChar)){
          accept();
        }
        switch(currentSpelling.toString()){
          case "boolean":
            return Token.BOOLEAN;
          case "break":
            return Token.BREAK;
          case "continue":
            return Token.CONTINUE;
          case "else":
            return Token.ELSE;
          case "float":
            return Token.FLOAT;
          case "for":
            return Token.FOR;
          case "if":
            return Token.IF;
          case "int":
            return Token.INT;
          case "return":
            return Token.RETURN;
          case "void":
            return Token.VOID;
          case "while":
            return Token.WHILE;
          case "true":
            return Token.BOOLEANLITERAL;
          case "false":
            return Token.BOOLEANLITERAL;
        }
        return Token.ID;
      }
      break;
    }

    accept();
    return Token.ERROR;
  }
  boolean letter(char c){
    if(c >= 'a' && c <= 'z'){
      return true;
    }
    else if(c >= 'A' && c <= 'Z'){
      return true;
    }
    else if(c == '_'){
      return true;
    }
    else return false;
  }
  boolean vcBlank(char c){
    if(c == ' ' || c == '\f'|| c == '\t' || c == '\r' || c == '\n'){
      return true;
    }
    else{
      return false;
    }
  }

  void skipSpaceAndComments() {
    if(vcBlank(currentChar)){
      accept();
      while(vcBlank(currentChar)){
        accept();
      }
    }
    //deal with comments
    if(currentChar == '/'){
      if(inspectChar(1) == '/'){
        accept();
        while(currentChar != '\r' && currentChar != '\n'){
          accept();
        }
        accept();
          skipSpaceAndComments();
      }
      else if(inspectChar(1) == '*'){
        accept();
        SourcePosition sp = new SourcePosition(charLine,charCol - 1, charCol - 1);
        while(!(currentChar == '*' && inspectChar(1) == '/')){
          if(currentChar == SourceFile.eof){
            errorReporter.reportError(": unterminated comment"," ",sp);
            return;
          }
          accept();
        }
        accept();
        accept();
          skipSpaceAndComments();
      }
    }
  }

  public Token getToken() {
    Token tok;
    int kind;

    // skip white space and comments

    currentSpelling = new StringBuffer("");
    skipSpaceAndComments();

    currentSpelling = new StringBuffer("");

    sourcePos = new SourcePosition();
    sourcePos.lineStart = charLine;
    sourcePos.charStart = charCol;
    sourcePos.lineFinish = charLine;
    sourcePos.charFinish = charCol;
    // You must record the position of the current token somehow

    kind = nextToken();
    sourcePos.lineFinish = charLine;
    sourcePos.charFinish = charCol;
    if(kind != Token.EOF){
      sourcePos.charFinish --;
    }

    tok = new Token(kind, currentSpelling.toString(), sourcePos);

    // * do not remove these three lines
    if (debug)
      System.out.println(tok);
    return tok;
  }

}
