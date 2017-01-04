#if !defined (ast_h)
#define ast_h 1

#include "JSONValue.h"

enum typeT {ANON_FCN_HANDLE, ARGUMENT_LIST, BINARY_EXP, BREAK, COLON, CONTINUE, GLOBAL, STATIC, DECL_ELT, DECL_INIT_LIST, SIMPLE_FOR, COMPLEX_FOR, USER_SCRIPT, USER_FCN, FCN_DEF, IDENT, IF_CLAUSE, IF_COMMAND, IF_COMMAND_LIST, INDEX_EXP, MATRIX, CELL, MULTI_ASSIGN, NO_OP, CONST, FCN_HANDLE, PARAM_LIST, POSTFIX, PREFIX, RETURN, RETURN_LIST, ASSIGN, STATEMENT, STATEMENT_LIST, SWITCH, SWITCH_CASE, SWITCH_CASE_LIST, TRY_CATCH, UNWIND_PROTECT, WHILE, DO_UNTIL, FCN_HANDLE_BODY, VARARGOUT, VARARGIN};

struct nodeT {
  int id;
  typeT type;
  wstring name;
  int leaf;
  bool key;
};

class ast {
public:
  ast(const JSONValue* root, const set<wstring>& important);
  const vector<nodeT>& data() {return postorder;}
private:
  vector<nodeT> postorder;
}

#endif
