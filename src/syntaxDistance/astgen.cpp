#include <oct.h>
#include <ops.h>
#include <parse.h>
#include <string>
#include <iostream>
#include <fstream>
#include "pt-pr-json.h"
#include "pt-pr-codemap.h"
#include "codemap-ostream.h"
using namespace std;

int main(int argc, char *argv[])
{
	// these next two lines are important!
	install_types();
	install_ops();

	octave_function* f = load_fcn_from_file(argv[1],"./","","",true);
	if (!f) {
	  cerr << "The provided script does not parse as valid Octave code." << endl;
	  return 1;
	}
	
	ofstream jsonout(argv[2]);
	ofstream codeout(argv[3]);
	ofstream mapout(argv[4]);
	
	jsonout << "{\"root\":";

	tree_print_json T2(jsonout, false);
	f->accept(T2);
	
  jsonout << "}" << endl;

	codemap_ostream os(codeout);
	tree_print_codemap T(os);
	f->accept(T);
	os.print_codemap(mapout);

  jsonout.close();
  codeout.close();
  mapout.close();
  
	return 0;
}




