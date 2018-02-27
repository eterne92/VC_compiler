/**
 **	Scanner.java                        
 **/

package VC.Scanner;

import VC.ErrorReporter;

public final class Scanner {

  private SourceFile sourceFile;
  private boolean debug;

  private ErrorReporter errorReporter;
  private StringBuffer currentSpelling;
  private char currentChar;
  private SourcePosition sourcePos;

  // =========================================================

  public Scanner(SourceFile source, ErrorReporter reporter) {
    sourceFile = source;
    errorReporter = reporter;
    currentChar = sourceFile.getNextChar();
    debug = false;

    // you may initialise your counters for line and column numbers here
  }

  public void enableDebugging() {
    debug = true;
  }

  // accept gets the next character from the source program.

  private void accept() {

    currentSpelling.append(currentChar);
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
    case '.':
      accept();
      //digit part first
      //without digit it's an error(only .)
      if(!Character.isDigit(currentChar)){
        return Token.ERROR;
      }
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

      //  attempting to recognise a float

    case '|':
      accept();
      if (currentChar == '|') {
        accept();
        return Token.OROR;
      } else {
        return Token.ERROR;
      }

      // ....
    case SourceFile.eof:
      currentSpelling.append(Token.spell(Token.EOF));
      return Token.EOF;
    default:
      break;
    }

    accept();
    return Token.ERROR;
  }

  void skipSpaceAndComments() {
  }

  public Token getToken() {
    Token tok;
    int kind;

    // skip white space and comments

    skipSpaceAndComments();

    currentSpelling = new StringBuffer("");

    sourcePos = new SourcePosition();

    // You must record the position of the current token somehow

    kind = nextToken();

    tok = new Token(kind, currentSpelling.toString(), sourcePos);

    // * do not remove these three lines
    if (debug)
      System.out.println(tok);
    return tok;
  }

}
