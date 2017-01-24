# To run make, you need to create a file named Makefile.local
# under the same directory that tells the Makefile where your
# octave lib/include/bin directories are.  An example is as follows:
#
# OCTAVE_BASE=/home/jhuang11/work/code/octave-3.6.3
# OCTAVE_INCLUDE=$(OCTAVE_BASE)/include/octave-3.6.3/octave
# OCTAVE_LIB=$(OCTAVE_BASE)/lib/octave/3.6.3
# MKOCTFILE=$(OCTAVE_BASE)/bin/mkoctfile



include Makefile.local

SIMPLEJSON_INCLUDE=ext/SimpleJSON-master/src

OCT_OBJS=obj/astgen.o obj/pt-pr-json.o obj/pt-pr-codemap.o obj/codemap-ostream.o obj/codemap-streambuf.o
JSON_EXT_OBJS=ext/SimpleJSON-master/obj/JSON.o ext/SimpleJSON-master/obj/JSONValue.o


all: astgen matching lca batch-matching identcount compose naive-propagate db-matching

clean:
	rm -f obj/*.o bin/astgen bin/matching bin/lca bin/batch-matching bin/identcount bin/compose bin/naive-propagate
	cd ext/SimpleJSON-master; make clean

obj/pt-pr-json.o: src/matching/pt-pr-json.cc
	$(MKOCTFILE) -I. -I$(OCTAVE_INCLUDE) -DHAVE_CONFIG_H -c $< -o $@

obj/pt-pr-codemap.o: src/matching/pt-pr-codemap.cc
	$(MKOCTFILE) -I. -I$(OCTAVE_INCLUDE) -DHAVE_CONFIG_H -c $< -o $@

obj/codemap-ostream.o: src/matching/codemap-ostream.cc
	$(MKOCTFILE) -I. -I$(OCTAVE_INCLUDE) -DHAVE_CONFIG_H -c $< -o $@

obj/codemap-streambuf.o: src/matching/codemap-streambuf.cc
	$(MKOCTFILE) -I. -I$(OCTAVE_INCLUDE) -DHAVE_CONFIG_H -c $< -o $@

obj/astgen.o: src/matching/astgen.cpp
	$(MKOCTFILE) -I. -I$(OCTAVE_INCLUDE) -DHAVE_CONFIG_H -c $< -o $@

astgen: $(OCT_OBJS)
	$(MKOCTFILE) --link-stand-alone -o bin/astgen $(OCT_OBJS) -lpthread

obj/basic-treenode-distance.o: src/matching/basic-treenode-distance.cc
	g++ -O2 -I. -I$(SIMPLEJSON_INCLUDE) -c $< -o $@

obj/matching.o: src/matching/matching.cpp
	g++ -O2 -I. -I$(SIMPLEJSON_INCLUDE) -c $< -o $@

matching: obj/matching.o obj/basic-treenode-distance.o json
	g++ -O2 -o bin/matching obj/matching.o obj/basic-treenode-distance.o $(JSON_EXT_OBJS)

obj/batch-matching.o: src/matching/batch-matching.cpp
	g++ -O2 -I. -I$(SIMPLEJSON_INCLUDE) -c $< -o $@

batch-matching: obj/batch-matching.o json
	g++ -O2 -o bin/batch-matching obj/batch-matching.o  $(JSON_EXT_OBJS)

obj/db-matching.o: src/matching/db-matching.cpp
	g++ -O2 -I. -I$(SIMPLEJSON_INCLUDE) -I/usr/include/mysql -c $< -o $@

db-matching: obj/db-matching.o json
	g++ -O2 -o bin/db-matching obj/db-matching.o  $(JSON_EXT_OBJS) `mysql_config --cflags --libs`

obj/identcount.o: src/matching/identcount.cpp
	g++ -O2 -I. -I$(SIMPLEJSON_INCLUDE) -c $< -o $@

identcount: obj/identcount.o json
	g++ -O2 -o bin/identcount obj/identcount.o  $(JSON_EXT_OBJS)

obj/lca.o: src/matching/lca.cpp
	g++ -O2 -I. -I$(SIMPLEJSON_INCLUDE) -c $< -o $@

lca: obj/lca.o json
	g++ -O2 -o bin/lca obj/lca.o $(JSON_EXT_OBJS)

obj/naive-propagate.o: src/matching/naive-propagate.cpp
	g++ -O2 -I. -I$(SIMPLEJSON_INCLUDE) -c $< -o $@

naive-propagate: obj/naive-propagate.o json
	g++ -O2 -o bin/naive-propagate obj/naive-propagate.o $(JSON_EXT_OBJS)

obj/compose.o: src/matching/compose.cpp
	g++ -O2 -I. -c $< -o $@

compose: obj/compose.o
	g++ -O2 -o bin/compose obj/compose.o

json:
	cd ext/SimpleJSON-master; make

