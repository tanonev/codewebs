OCTAVE_INCLUDE=/usr/local/include/octave-3.6.3/octave
OCTAVE_LIB=/usr/local/lib/octave/3.6.3
SIMPLEJSON_INCLUDE=../SimpleJSON-master/src
MKOCTFILE=/usr/local/bin/mkoctfile

all: astgen matching lca

clean:
	rm *.o astgen matching lca

pt-pr-json.o: pt-pr-json.cc
	$(MKOCTFILE) -I. -I$(OCTAVE_INCLUDE) -DHAVE_CONFIG_H -c pt-pr-json.cc -o pt-pr-json.o

pt-pr-codemap.o: pt-pr-codemap.cc
	$(MKOCTFILE) -I. -I$(OCTAVE_INCLUDE) -DHAVE_CONFIG_H -c pt-pr-codemap.cc -o pt-pr-codemap.o

codemap-ostream.o: codemap-ostream.cc
	$(MKOCTFILE) -I. -I$(OCTAVE_INCLUDE) -DHAVE_CONFIG_H -c codemap-ostream.cc -o codemap-ostream.o

codemap-streambuf.o: codemap-streambuf.cc
	$(MKOCTFILE) -I. -I$(OCTAVE_INCLUDE) -DHAVE_CONFIG_H -c codemap-streambuf.cc -o codemap-streambuf.o

astgen: astgen.o pt-pr-json.o pt-pr-codemap.o codemap-ostream.o codemap-streambuf.o
	$(MKOCTFILE) --link-stand-alone -o astgen astgen.o pt-pr-json.o pt-pr-codemap.o codemap-ostream.o codemap-streambuf.o

astgen.o: astgen.cpp
	$(MKOCTFILE) -c -I$(OCTAVE_INCLUDE) -o astgen.o astgen.cpp

basic-treenode-distance.o: basic-treenode-distance.cc
	$(MKOCTFILE) -I. -I$(SIMPLEJSON_INCLUDE) -DHAVE_CONFIG_H -c basic-treenode-distance.cc -o basic-treenode-distance.o

matching.o: matching.cpp
	$(MKOCTFILE) -I. -I$(SIMPLEJSON_INCLUDE) -DHAVE_CONFIG_H -c matching.cpp -o matching.o

matching: matching.o basic-treenode-distance.o
	$(MKOCTFILE) --link-stand-alone -o matching matching.o basic-treenode-distance.o ../SimpleJSON-master/obj/JSON.o ../SimpleJSON-master/obj/JSONValue.o

lca.o: lca.cpp
	$(MKOCTFILE) -I. -I$(SIMPLEJSON_INCLUDE) -DHAVE_CONFIG_H -c lca.cpp -o lca.o

lca: lca.o
	$(MKOCTFILE) --link-stand-alone -o lca lca.o ../SimpleJSON-master/obj/JSON.o ../SimpleJSON-master/obj/JSONValue.o

