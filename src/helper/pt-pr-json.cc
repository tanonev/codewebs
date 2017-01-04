#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <cctype>

#include <iostream>
#include <sstream>
#include <algorithm>

#include "comment-list.h"
#include "error.h"
#include "ov-usr-fcn.h"
#include "pr-output.h"
#include "pt-all.h"
#include "pt-pr-json.h"

void tree_print_json::visit_anon_fcn_handle (tree_anon_fcn_handle& afh) {
  print_astnode_start("ANON_FCN_HANDLE", "");

  tree_parameter_list *param_list = afh.parameter_list ();
  if (param_list) param_list->accept (*this);
  tree_statement_list *b = afh.body ();
  if (b) b->accept (*this);

  print_astnode_end();
}

void tree_print_json::visit_argument_list (tree_argument_list& lst) {
  print_astnode_start("ARGUMENT_LIST", "");

  tree_argument_list::iterator p = lst.begin ();
  while (p != lst.end ()) {
    tree_expression *elt = *p++;
    if (elt) elt->accept (*this);
  }
  
  print_astnode_end();
}

void tree_print_json::visit_binary_expression (tree_binary_expression& expr) {
  print_astnode_start("BINARY_EXP", expr.oper());
  
  tree_expression *op1 = expr.lhs ();
  if (op1) op1->accept (*this);
  tree_expression *op2 = expr.rhs ();
  if (op2) op2->accept (*this);

  print_astnode_end();
}

void tree_print_json::visit_break_command (tree_break_command& cmd) {
  print_astnode_start("BREAK", "");
  print_astnode_end();
}

void tree_print_json::visit_colon_expression (tree_colon_expression& expr) {
  print_astnode_start("COLON", "");
  
  tree_expression *op1 = expr.base ();
  if (op1) op1->accept (*this);
  tree_expression *op3 = expr.increment ();
  if (op3) op3->accept (*this);
  tree_expression *op2 = expr.limit ();
  if (op2) op2->accept (*this);

  print_astnode_end();
}

void tree_print_json::visit_continue_command (tree_continue_command& cmd) {
  print_astnode_start("CONTINUE", "");
  print_astnode_end();
}

void tree_print_json::do_decl_command (tree_decl_command& cmd) {
  tree_decl_init_list *init_list = cmd.initializer_list ();
  if (init_list) init_list->accept (*this);
}

void tree_print_json::visit_global_command (tree_global_command& cmd) {
  print_astnode_start("GLOBAL", cmd.name());
  
  do_decl_command (cmd);
  
  print_astnode_end();
}

void tree_print_json::visit_static_command (tree_static_command& cmd) {
  print_astnode_start("STATIC", cmd.name());
  
  do_decl_command (cmd);
  
  print_astnode_end();
}

void tree_print_json::visit_decl_elt (tree_decl_elt& cmd) {
  print_astnode_start("DECL_ELT", "");
  
  tree_identifier *id = cmd.ident ();
  if (id) id->accept (*this);
  tree_expression *expr = cmd.expression ();
  if (expr) expr->accept (*this);
  
  print_astnode_end();
}

void tree_print_json::visit_decl_init_list (tree_decl_init_list& lst) {
  print_astnode_start("DECL_INIT_LIST", "");
  
  tree_decl_init_list::iterator p = lst.begin ();
  while (p != lst.end ()) {
    tree_decl_elt *elt = *p++;

    if (elt) elt->accept (*this);
  }
  
  print_astnode_end();
}

void tree_print_json::visit_simple_for_command (tree_simple_for_command& cmd) {
  print_astnode_start("SIMPLE_FOR", cmd.in_parallel () ? "parfor" : "for");
  
  tree_expression *lhs = cmd.left_hand_side ();
  if (lhs) lhs->accept (*this);
  tree_expression *expr = cmd.control_expr ();
  if (expr) expr->accept (*this);
  tree_expression *maxproc = cmd.maxproc_expr ();
  if (maxproc) maxproc->accept (*this);
  tree_statement_list *list = cmd.body ();
  if (list) list->accept (*this);
  
  print_astnode_end();
}

void tree_print_json::visit_complex_for_command (tree_complex_for_command& cmd) {
  print_astnode_start("COMPLEX_FOR", "");
  
  tree_argument_list *lhs = cmd.left_hand_side ();
  if (lhs) lhs->accept (*this);
  tree_expression *expr = cmd.control_expr ();
  if (expr) expr->accept (*this);
  tree_statement_list *list = cmd.body ();
  if (list) list->accept (*this);

  print_astnode_end();
}

void tree_print_json::visit_octave_user_script (octave_user_script& fcn) {
  reset ();

  print_astnode_start("USER_SCRIPT", "");
  
  tree_statement_list *cmd_list = fcn.body ();
  if (cmd_list) cmd_list->accept (*this);
  
  print_astnode_end();
}

void tree_print_json::visit_octave_user_function (octave_user_function& fcn) {
  print_astnode_start("USER_FCN", fcn.name());
  
  tree_parameter_list *ret_list = fcn.return_list ();
  if (ret_list) ret_list->accept (*this);
  if (fcn.takes_var_return()) {
    print_astnode_start("VARARGOUT", "");
    print_astnode_end();
  }
  tree_parameter_list *param_list = fcn.parameter_list ();
  if (param_list) param_list->accept (*this);
  if (fcn.takes_varargs()) {
    print_astnode_start("VARARGIN", "");
    print_astnode_end();
  }
  tree_statement_list *cmd_list = fcn.body ();
  if (cmd_list) cmd_list->accept (*this);

  print_astnode_end();
}

void tree_print_json::visit_function_def (tree_function_def& fdef) {
  print_astnode_start("FCN_DEF", "");

  octave_value fcn = fdef.function ();
  octave_function *f = fcn.function_value ();
  if (f) f->accept (*this);
  
  print_astnode_end();
}

void tree_print_json::visit_identifier (tree_identifier& id) {
  print_astnode_start("IDENT", id.name());
  print_astnode_end();
}

void tree_print_json::visit_if_clause (tree_if_clause& cmd) {
  print_astnode_start("IF_CLAUSE", cmd.is_else_clause() ? "else" : "if");
  
  tree_expression *expr = cmd.condition ();
  if (expr) expr->accept (*this);
  tree_statement_list *list = cmd.commands ();
  if (list) list->accept (*this);
  
  print_astnode_end();
}

void tree_print_json::visit_if_command (tree_if_command& cmd) {
  print_astnode_start("IF_COMMAND", "");

  tree_if_command_list *list = cmd.cmd_list ();
  if (list) list->accept (*this);

  print_astnode_end();
}

void tree_print_json::visit_if_command_list (tree_if_command_list& lst) {
  print_astnode_start("IF_COMMAND_LIST", "");
  
  tree_if_command_list::iterator p = lst.begin ();
  while (p != lst.end ()) {
    tree_if_clause *elt = *p++;
    if (elt) elt->accept (*this);
  }
  
  print_astnode_end();
}

void tree_print_json::visit_index_expression (tree_index_expression& expr) {
  print_astnode_start("INDEX_EXP", "");
  
  tree_expression *e = expr.expression ();
  if (e) e->accept (*this);

  std::list<tree_argument_list *> arg_lists = expr.arg_lists ();
  std::string type_tags = expr.type_tags ();
  std::list<string_vector> arg_names = expr.arg_names ();

  int n = type_tags.length ();

  std::list<tree_argument_list *>::iterator p_arg_lists = arg_lists.begin ();
  std::list<string_vector>::iterator p_arg_names = arg_names.begin ();

  for (int i = 0; i < n; i++) {
    switch (type_tags[i]) {
      case '(':
      case '{':
        {
          tree_argument_list *l = *p_arg_lists;
          if (l) l->accept (*this);
        }
        break;

      case '.':
        {
          string_vector nm = *p_arg_names;
          assert (nm.length () == 1);
          
          print_astnode_start("CONST", nm(0));
          print_astnode_end();
        }
        break;

      default:
        panic_impossible ();
    }

    p_arg_lists++;
    p_arg_names++;
  }

  print_astnode_end();
}

void tree_print_json::visit_matrix (tree_matrix& lst) {
  print_astnode_start("MATRIX", "");

  tree_matrix::iterator p = lst.begin ();
  while (p != lst.end ()) {
    tree_argument_list *elt = *p++;
    if (elt) elt->accept (*this);
  }

  print_astnode_end();
}

void tree_print_json::visit_cell (tree_cell& lst) {
  print_astnode_start("CELL", "");
  
  tree_cell::iterator p = lst.begin ();
  while (p != lst.end ()) {
    tree_argument_list *elt = *p++;
    if (elt) elt->accept (*this);
  }

  print_astnode_end();
}

void tree_print_json::visit_multi_assignment (tree_multi_assignment& expr) {
  print_astnode_start("MULTI_ASSIGN", expr.oper());

  tree_argument_list *lhs = expr.left_hand_side ();
  if (lhs) lhs->accept (*this);
  tree_expression *rhs = expr.right_hand_side ();
  if (rhs) rhs->accept (*this);

  print_astnode_end();
}

void tree_print_json::visit_no_op_command (tree_no_op_command& cmd) {
  print_astnode_start("NO_OP", cmd.original_command());
  print_astnode_end();
}

void replaceAll(std::string& str, const std::string& from, const std::string& to) {
  size_t start_pos = 0;
  while((start_pos = str.find(from, start_pos)) != std::string::npos) {
    str.replace(start_pos, from.length(), to);
    start_pos += to.length(); // In case 'to' contains 'from', like replacing 'x' with 'yx'
  }
}

void tree_print_json::visit_constant (tree_constant& val) {
  std::stringstream ss;
  val.print_raw (ss, false, print_original_text);
  print_astnode_start("CONST", ss.str());
  print_astnode_end();
}

void tree_print_json::visit_fcn_handle (tree_fcn_handle& fh) {
  std::stringstream ss;
  fh.print_raw (ss, true, print_original_text);
  print_astnode_start("FCN_HANDLE", ss.str());
  print_astnode_end();
}

void tree_print_json::visit_parameter_list (tree_parameter_list& lst) {
  print_astnode_start("PARAM_LIST", "");

  tree_parameter_list::iterator p = lst.begin ();
  while (p != lst.end ()) {
    tree_decl_elt *elt = *p++;
    if (elt) elt->accept (*this);
  }
  
  print_astnode_end();
}

void tree_print_json::visit_postfix_expression (tree_postfix_expression& expr) {
  print_astnode_start("POSTFIX", expr.oper());

  tree_expression *e = expr.operand ();
  if (e) e->accept (*this);

  print_astnode_end();
}

void tree_print_json::visit_prefix_expression (tree_prefix_expression& expr) {
  print_astnode_start("PREFIX", expr.oper());

  tree_expression *e = expr.operand ();
  if (e) e->accept (*this);

  print_astnode_end();
}

void tree_print_json::visit_return_command (tree_return_command& cmd) {
  print_astnode_start("RETURN", "");
  print_astnode_end();
}

void tree_print_json::visit_return_list (tree_return_list& lst) {
  print_astnode_start("RETURN_LIST", "");
  
  tree_return_list::iterator p = lst.begin ();
  while (p != lst.end ()) {
    tree_index_expression *elt = *p++;
    if (elt) elt->accept (*this);
  }
  
  print_astnode_end();
}

void tree_print_json::visit_simple_assignment (tree_simple_assignment& expr) {
  print_astnode_start("ASSIGN", expr.oper());

  tree_expression *lhs = expr.left_hand_side ();
  if (lhs) lhs->accept (*this);
  tree_expression *rhs = expr.right_hand_side ();
  if (rhs) rhs->accept (*this);

  print_astnode_end();
}

void tree_print_json::visit_statement (tree_statement& stmt) {
  print_astnode_start("STATEMENT", "");

  tree_command *cmd = stmt.command ();
  if (cmd) cmd->accept (*this);
  else {
    tree_expression *expr = stmt.expression ();
    if (expr) expr->accept (*this);
  }
  
  print_astnode_end();
}

void tree_print_json::visit_statement_list (tree_statement_list& lst) {
  print_astnode_start("STATEMENT_LIST", "");
  for (tree_statement_list::iterator p = lst.begin (); p != lst.end (); p++) {
    tree_statement *elt = *p;
    if (elt) elt->accept (*this);
  }
  print_astnode_end();
}

void tree_print_json::visit_switch_case (tree_switch_case& cs) {
  print_astnode_start("SWITCH_CASE", cs.is_default_case() ? "otherwise" : "case");

  tree_expression *label = cs.case_label ();
  if (label) label->accept (*this);

  tree_statement_list *list = cs.commands ();
  if (list) list->accept (*this);
  
  print_astnode_end();
}

void tree_print_json::visit_switch_case_list (tree_switch_case_list& lst) {
  print_astnode_start("SWITCH_CASE_LIST", "");
  
  tree_switch_case_list::iterator p = lst.begin ();
  while (p != lst.end ()) {
    tree_switch_case *elt = *p++;
    if (elt) elt->accept (*this);
  }
  
  print_astnode_end();
}

void tree_print_json::visit_switch_command (tree_switch_command& cmd) {
  print_astnode_start("SWITCH", "");

  tree_expression *expr = cmd.switch_value ();
  if (expr) expr->accept (*this);
  tree_switch_case_list *list = cmd.case_list ();
  if (list) list->accept (*this);
  
  print_astnode_end();
}

void tree_print_json::visit_try_catch_command (tree_try_catch_command& cmd) {
  print_astnode_start("TRY_CATCH", "");
  
  tree_statement_list *try_code = cmd.body ();
  if (try_code) try_code->accept (*this);
  tree_statement_list *catch_code = cmd.cleanup ();
  if (catch_code) catch_code->accept (*this);

  print_astnode_end();
}

void tree_print_json::visit_unwind_protect_command (tree_unwind_protect_command& cmd) {
  print_astnode_start("UNWIND_PROTECT", "");
  
  tree_statement_list *unwind_protect_code = cmd.body ();
  if (unwind_protect_code) unwind_protect_code->accept (*this);
  tree_statement_list *cleanup_code = cmd.cleanup ();
  if (cleanup_code) cleanup_code->accept (*this);

  print_astnode_end();
}

void tree_print_json::visit_while_command (tree_while_command& cmd) {
  print_astnode_start("WHILE", "");
  
  tree_expression *expr = cmd.condition ();
  if (expr) expr->accept (*this);
  tree_statement_list *list = cmd.body ();
  if (list) list->accept (*this);

  print_astnode_end();
}

void tree_print_json::visit_do_until_command (tree_do_until_command& cmd) {
  print_astnode_start("DO_UNTIL", "");

  tree_statement_list *list = cmd.body ();
  if (list) list->accept (*this);
  tree_expression *expr = cmd.condition ();
  if (expr) expr->accept (*this);

  print_astnode_end();
}

void tree_print_json::indent (void) {
  assert (curr_print_indent_level >= 0);
  for (int i = 0; i < curr_print_indent_level; i++)
    os << " ";
}

// For resetting print_code state.

void tree_print_json::reset (void) {
  curr_print_indent_level = 0;
  needs_comma = false;
}

void tree_print_json::print_astnode_start (const char *type, const std::string name) {
  if (needs_comma) os << ",";
  needs_comma = false;
  os << " {\n";
  increment_indent_level();
  indent(); os << "\"id\": " << id++ << ",\n";
  indent(); os << "\"type\": \"" << type << "\",\n";
  if (name.length() > 0) {
    std::string str = name;
    std::replace(str.begin(), str.end(), '\n', ';');
    std::replace(str.begin(), str.end(), '\t', ' ');
    replaceAll(str, "\\", "\\\\");
    replaceAll(str, "\"", "\\\"");
    indent(); os << "\"name\": \"" << str << "\",\n";
  }
  indent(); os << "\"children\": [";
  increment_indent_level();
}

void tree_print_json::print_astnode_end () {
  needs_comma = true;
  os << "],\n";
  decrement_indent_level();
  indent(); os << "\"annotations\": []\n";
  decrement_indent_level();
  indent(); os << "}";
}

