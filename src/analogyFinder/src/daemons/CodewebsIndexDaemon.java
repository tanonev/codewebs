package daemons;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;

import models.Assignment;
import models.CodeBlock;
import models.Context;
import models.Equivalence;
import models.Profile;
import models.Program;
import models.Subforest;
import models.ast.Node;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.json.JSONObject;

import util.FileSystem;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;


public class CodewebsIndexDaemon implements Daemon {

	private static final String TASK_QUEUE_NAME = "codewebs_task_queue";
    private static ConnectionFactory factory = null;
    private static Connection connection = null;
    private static Channel channel = null;
    private static QueueingConsumer consumer = null;

    private Thread workerThread;
    private boolean stopped = false;
    
    // Index objects
    private static Assignment assn = null;
    private static Set<String> keywords = null;
    private static List<Equivalence> eqs = null;
    private static Map<Context, Map<Integer, Integer>> contextHistograms = null;
    private static Map<Subforest, Map<Integer, Integer>> subtreeHistograms = null;
    private static String dataPath = null;
    private static String assnStr = null;
    private static String host = "localhost";
    private static final int NUM_TO_LOAD = 2000;
    private static final String configPath = "./localconfig";

    private static boolean checkIsomorphism = false;
    
    @Override
    public void init(DaemonContext daemonContext) throws DaemonInitException, Exception{
    	String[] args = daemonContext.getArguments();
    	
    	workerThread = new Thread(){
    		
    		@Override
    		public synchronized void start() {
    			CodewebsIndexDaemon.this.stopped = false;
    			super.start();
    		}
    		
    		@Override
    		public void run() {
    			while(!stopped) {
    				try {
    					QueueingConsumer.Delivery delivery = consumer.nextDelivery();
    					
    					BasicProperties props = delivery.getProperties();
    					BasicProperties replyProps = new BasicProperties
    														.Builder()
    														.correlationId(props.getCorrelationId())
    														.build();
    					
                        String message = new String(delivery.getBody());

                        //System.out.println("\t[*] Received '" + message + "'");
                        System.out.println("\t[*] Received task '" + props.getCorrelationId() + "'");
                        String response = doWork(message);
                        //String response = "hello world!!";
                        System.out.println("\t[*] Done");
                        
                        channel.basicPublish( "", props.getReplyTo(), replyProps, response.getBytes());
                        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
    				} catch (InterruptedException e) {
    					System.err.println("Caught InterruptedException: " + e.getMessage());
    					e.printStackTrace();
    				} catch (IOException ioe) {
    					System.err.println("Caught IOException: " + ioe.getMessage());
    					ioe.printStackTrace();
    				}
    			}
    		}
    	};
    }
    
    @Override
    public void start() throws Exception{
        setupChannel();
    	loadIndex();
        
        workerThread.start();
    }
    
    private void setupChannel() throws Exception {
    	factory = new ConnectionFactory();
        factory.setHost(host);
        connection = factory.newConnection();
        channel = connection.createChannel();
        channel.queueDeclare(TASK_QUEUE_NAME, false, false, false, null);
        channel.basicQos(1);
        consumer = new QueueingConsumer(channel);
        channel.basicConsume(TASK_QUEUE_NAME, false, consumer);
        System.out.println(" [x] Awaiting requests from client.");
    }
    
    @Override
    public void stop() throws Exception {
    	stopped = true;
    	try {
    		workerThread.join(1000);
    	} catch (InterruptedException e){
    		System.err.println(e.getMessage());
    		throw e;
    	}
    }
    
    @Override
    public void destroy() {
    	workerThread = null;
    }
   
    // ***************************************************************************
    // Index related
    // ***************************************************************************
    private void loadIndex(){
    	System.out.println("loading...");
     
        FileSystem.setConfig(configPath);
    	assn = Assignment.loadFromFile(NUM_TO_LOAD);
    	keywords = FileSystem.loadKeywords();
    	eqs = loadEquivalences();
      contextHistograms = new HashMap<Context, Map<Integer, Integer>>();
      subtreeHistograms = new HashMap<Subforest, Map<Integer, Integer>>();
      for (Program p : assn.getPrograms()) addProgramToHistograms(p);
    }
    
    private static String doWork(String task) throws InterruptedException {
        // HERE IS WHERE WE ISSUE QUERY
      try {
        JSONObject taskJSON = new JSONObject(task);
        
        int queryType = taskJSON.getInt("querytype");
        JSONObject astobj = taskJSON.getJSONObject("ast");
        JSONObject root = astobj.getJSONObject("root");
        ArrayList<String> code = loadCode(taskJSON.getString("code"));

        int[][] map = loadMap(taskJSON.getString("map"));
        Program prog = new Program(keywords, -1, code, map, root, -1, -1);
        
        String result = "";
        
        if (queryType == 1) { // recognize equivalences
          List<CodeBlockEquivalencePair> locs = reduceProgram(prog);
          JSONObject ans = new JSONObject();
          for (CodeBlockEquivalencePair e : locs) {
            ans.append("equivalences", writeEquivalence(e.block.getSubforest(), e.eq));
          }
          return ans.toString();
        } else if (queryType == 2) { // find alternates
          int startLine = taskJSON.getInt("startline");
          int endLine = taskJSON.getInt("endline");
          int startLineIndex = taskJSON.getInt("startlineindex");
          int endLineIndex = taskJSON.getInt("endlineindex");
          checkIsomorphism = taskJSON.optBoolean("checkisomorphism");
          CodeBlock c = prog.getLCA(startLine, startLineIndex, endLine, endLineIndex);
          Set<Subforest> seedForest = new HashSet<Subforest>();
          seedForest.add(c.getSubforest());
          Equivalence seed = new Equivalence(seedForest, "0_seed");
          seed.addNecessaryContext(c.getContext());
          expandSeed(seed);
          ArrayList<Subforest> subforests = new ArrayList<Subforest>();
          subforests.addAll(seed.getSubforests());
          Collections.sort(subforests, new Comparator<Subforest>() {
            public int compare(Subforest a, Subforest b) {
              return getPopularity(b) - getPopularity(a);
            }
          });
          JSONObject ans = new JSONObject();
          ans.put("selected", writeSubforest(c.getSubforest())
              .put("line", c.getSubforest().getLineNumber())
              .put("popularity", getPopularity(c.getSubforest())));
          int count = 0;
          for (Subforest s : subforests) {
            ans.append("alternatives", writeSubforest(s).put("popularity", getPopularity(s)));
            count++;
            if (count == 30) break;
          }
          ans.put("count", subforests.size());
          return ans.toString();
        } else if (queryType == 3) { // find bugs
          // assume that we know the run is buggy
          Map<CodeBlock, Profile> bugs = findBugs(prog);
          JSONObject ans = new JSONObject();
          for (Map.Entry<CodeBlock, Profile> e : bugs.entrySet()) {
            CodeBlock c = e.getKey();
            Profile p = e.getValue();
            ans.append("bugs", writeSubforest(c.getSubforest())
                .put("line", c.getSubforest().getLineNumber())
                .put("correct", p.correct)
                .put("incorrect", p.incorrect));
            if (bugs.size() == 1) {
              if (assn.codeBlocksFromContext(c.getContext()) != null) {
                Subforest bestFix = null;
                int bestPopularity = 0;
                for (CodeBlock fix : assn.codeBlocksFromContext(c.getContext())) {
                  if (fix.getProgram().isCorrect()) {
                    int popularity = getPopularity(fix.getSubforest());
                    if (bestPopularity < popularity) {
                      bestFix = fix.getSubforest();
                      bestPopularity = popularity;
                    }
                  }
                }
                ans.put("soln", writeSubforest(bestFix));
              }
            }
          }
          return ans.toString();
        }
        
        List<CodeBlock> blocks = prog.getCodeBlocks();
        for (CodeBlock cb : blocks){
        	//System.out.println(cb.getSubforest().getCodeString() + '\n');
        	if (assn.codeBlocksFromSubforest(cb.getSubforest()) != null){
        		result += "True\n";
        	} else {
        		result += "False\n";
        	}
        }
        return result;
        
        
        // learn equivalence
        // send result back
      } catch (Exception e) {
        e.printStackTrace(System.err);
        return new JSONObject().put("error", e.toString()).toString();
      }
    }
    
    private static int getPopularity(Subforest subforest) {
      int count = 0;
      for (CodeBlock c : assn.codeBlocksFromSubforest(subforest)) count += c.getProgram().getStudents();
      return count;
    }
    
    private static void expandSeed(Equivalence seed) {
      Subforest init = seed.getSubforests().iterator().next();
      setInitSubforest(init);
      while (true) {
        if (expandAllPrograms(seed) == 0) break;
        if (seed.getSubforests().size() >= 200) break;
        if (totalA == 0) break;
      }
    }
    
    private static Subforest a;
    private static Map<Context, List<CodeBlock>> contextA;
    private static int totalA;
    private static Map<Subforest, Boolean> equivalentCache;
    
    private static void setInitSubforest(Subforest forest) {
      System.err.println(forest.getCodeString());
      equivalentCache = new HashMap<Subforest, Boolean>();
      a = forest;
      Set<CodeBlock> blockA = assn.codeBlocksFromSubforest(forest);
      if (blockA == null) blockA = Collections.emptySet();
      totalA = 0;
      contextA = new HashMap<Context, List<CodeBlock>>();
      for (CodeBlock block : blockA) {
        if (checkIsomorphism && !block.getSubforest().isIsomorphic(forest)) continue;
        if (!contextA.containsKey(block.getContext()))
          contextA.put(block.getContext(), new ArrayList<CodeBlock>());
        contextA.get(block.getContext()).add(block);
        totalA += block.getProgram().getStudents();
      }
    }
    
    private static boolean isEquivalent(Subforest b) {
      if (!equivalentCache.containsKey(b)) equivalentCache.put(b, computeIsEquivalent(b)); 
      return equivalentCache.get(b);
    }
    
    private static boolean computeIsEquivalent(Subforest b) {
      if (totalA == 0) return true;
      if (a.equals(b)) return true;
      Set<CodeBlock> blockB = assn.codeBlocksFromSubforest(b);
      int totalB = 0;
      int matchA = 0, matchB = 0;
      for (CodeBlock block : blockB) {
        if (checkIsomorphism && !block.getSubforest().isIsomorphic(b)) continue;
        totalB += block.getProgram().getStudents();
        if (contextA.containsKey(block.getContext())) {
          List<CodeBlock> bas = contextA.get(block.getContext());
          for (CodeBlock ba : bas) {
            if (checkIsomorphism && !ba.getContext().isIsomorphic(block.getContext())) {
              System.out.println("skipped a context due to non-isomorphism");
              continue;
            }
            if (ba.getProgram().getOutput() != block.getProgram().getOutput()) return false;
            matchA += ba.getProgram().getStudents();
          }
          matchB += block.getProgram().getStudents();
        }
      }
      if (matchA < 5 || matchB < 5) return false;
      if (matchA / (double) totalA > 0.01) return true;
      if (matchB / (double) totalB > 0.01) return true;
      
      return false;
    }
    
    private static int expandAllPrograms(Equivalence seed) {
      int count = 0;
      for (Subforest s : seed.getSubforests()) {
        Set<CodeBlock> blocks = assn.codeBlocksFromSubforest(s);
        if (blocks == null) continue;
        for (CodeBlock c : blocks) {
          if (c.getProgram().isCorrect()) seed.addNecessaryContext(c.getContext());
        }
      }
      for (CodeBlock block : assn.codeBlocksFromContext(seed.getContexts())) {
        if (checkForContextMatch(block, seed)) count++;
      }
      return count;
    }
    
    private static boolean checkForContextMatch(CodeBlock block, Equivalence eq){
      Subforest subforest = block.getSubforest();
      Context context = block.getContext();
      
      // Then, you check if this fits our idea of where the eq exists.
      if(eq.contextRequiresInstance(context)) {
        if(block.getProgram().isCorrect()) {
          if (isEquivalent(subforest)) {
            if(eq.addSubforest(subforest)) {
              return true;
            }
          }
        } 
      }
      return false;
    }
    
    private static Map<CodeBlock, Profile> findBugs(Program current) {
      boolean[] mark = new boolean[current.getRoot().getSize()];
      int count = 0;
      int worstIndex = -1;
      double worstConfidence = 1.0;
    
      for (Context c : current.getLocalContexts()) {
        Profile p = new Profile(contextHistograms.get(c), 0);
        if (worstConfidence > p.getConfidence()) {
          worstConfidence = p.getConfidence();
          worstIndex = c.getRoot().getPostorderIndex();
        }
        if (p.isBugSpike()) {
          count += mark[c.getRoot().getPostorderIndex()] ? 0 : 1;
          mark[c.getRoot().getPostorderIndex()] = true;
        }
      }
      
      for (int i = 0; i < current.getRoot().getSize(); i++) {
        if (mark[i]) {
          Node cur = current.getTree().getNode(i);
          while (cur.getParent() != null) {
            cur = cur.getParent();
            count -= mark[cur.getPostorderIndex()] ? 1 : 0;
            mark[cur.getPostorderIndex()] = false;
          }
        }
      }
      
      Map<CodeBlock, Profile> ans = new HashMap<CodeBlock, Profile>();
      
      for (int i = 0; i < current.getRoot().getSize(); i++) {
        if (mark[i] || (count == 0 && i == worstIndex)) {
          int cur = i;
          CodeBlock c = current.getTree().makeCodeBlocks(current, cur).get(0);
          while (new Profile(contextHistograms.get(c.getContext()), 0).correct < 10 && cur < current.getRoot().getSize() - 1) {
            cur = c.getSubforest().getRoots().get(0).getParent().getPostorderIndex();
            c = current.getTree().makeCodeBlocks(current, cur).get(0);
          }
          ans.put(c, new Profile(subtreeHistograms.get(c.getSubforest()), 0));
        }
      }
//      
//      for (CodeBlock c : current.getCodeBlocks()) {
//        if (c.getSubforest().getRoots().size() == 1) {
//          if (c.getSubforest().getRoots().get(0) == Node.NULL) continue;
//          int idx = c.getSubforest().getRoots().get(0).getPostorderIndex(); 
//          if (mark[idx] || (count == 0 && idx == worstIndex)) ans.put(c, new Profile(subtreeHistograms.get(c.getSubforest()), 0));
//        }
//      }
      
      return ans;
    }
    
    private static JSONObject writeSubforest(Subforest forest) {
      JSONObject ans = new JSONObject();
      ans.put("code", forest.getCodeString());
      return ans;
    }
    
    private static JSONObject writeEquivalence(Subforest forest, Equivalence eq) {
      JSONObject ans = new JSONObject();
      ans.put("code", forest.getCodeString());
      ans.put("line", forest.getLineNumber());
      ans.put("name", eq.getName());
      return ans;
    }
    
    static class CodeBlockEquivalencePair {
      final CodeBlock block;
      final Equivalence eq;
      public CodeBlockEquivalencePair(CodeBlock block, Equivalence eq) {
        this.block = block;
        this.eq = eq;
      }
    }

    private static List<CodeBlockEquivalencePair> reduceProgram(Program current) {
      List<CodeBlockEquivalencePair> locs = new ArrayList<CodeBlockEquivalencePair>();
      for(Equivalence eq : eqs) {
        for(CodeBlock block : current.getCodeBlocks()){
          Subforest forest = block.getSubforest();
          
          if(eq.containsSubforest(forest)) {
            forest.markEquivalence(eq);
            locs.add(new CodeBlockEquivalencePair(block, eq));
          }
        }
        current = current.reduce();
      }
      
      return locs;
    }


    private static ArrayList<String> loadCode(String codeStr){
    	ArrayList<String> code = new ArrayList<String>();
    	Scanner codeIn = new Scanner(codeStr);
    	while (codeIn.hasNextLine())
    		code.add(codeIn.nextLine());
    	codeIn.close();    	
    	return code;
    }
    
    private static int[][] loadMap(String mapStr){
    	Scanner mapIn = new Scanner(mapStr);
    	int mapN = mapIn.nextInt();
    	int[][] map = new int[mapN][];
    	for(int j = 0; j < mapN; j++) {
    		int mapC = mapIn.nextInt();
    		map[j] = new int[mapC];
    		for (int k = 0; k < mapC; k++)
    			map[j][k] = mapIn.nextInt();
    	}
    	mapIn.close();
    	return map;
    }
   
    private List<Equivalence> loadEquivalences() {
      System.out.println("loading equivalences...");
      ArrayList<Equivalence> eqs = new ArrayList<Equivalence>();
      String allEquivalences = FileSystem.getExpandedDir();
      File folder = new File(allEquivalences);
      File[] listOfFiles = folder.listFiles();
      for(File eqDir : listOfFiles){
        String eqName = eqDir.getName();
        if(eqName.startsWith(".")) continue;
        Equivalence eq = Equivalence.loadFromFile(eqName, keywords, FileSystem.getExpandedDir());
        eqs.add(eq);
      }
      sortEquivalences(eqs);
      return eqs;
    }

    private void sortEquivalences(ArrayList<Equivalence> eqs) {
      Collections.sort(eqs, new Comparator<Equivalence>() {
        @Override
        public int compare(Equivalence a, Equivalence b) {
          return a.getPriority() - b.getPriority();
        }
      });
    }
    
    private static void addProgramToHistograms(Program program) {
      for (Context c : program.getLocalContexts()) {
        if (!contextHistograms.containsKey(c)) contextHistograms.put(c, new TreeMap<Integer, Integer>());
        Map<Integer, Integer> histogram = contextHistograms.get(c);
        if (!histogram.containsKey(program.getOutput())) histogram.put(program.getOutput(), 0);
        histogram.put(program.getOutput(), histogram.get(program.getOutput()) + program.getStudents());
      }
      for (CodeBlock c : program.getCodeBlocks()) {
        Subforest s = c.getSubforest();
        if (!subtreeHistograms.containsKey(s)) subtreeHistograms.put(s, new TreeMap<Integer, Integer>());
        Map<Integer, Integer> histogram = subtreeHistograms.get(s);
        if (!histogram.containsKey(program.getOutput())) histogram.put(program.getOutput(), 0);
        histogram.put(program.getOutput(), histogram.get(program.getOutput()) + program.getStudents());
        
        Context cxt = c.getContext();
        if (!contextHistograms.containsKey(cxt)) contextHistograms.put(cxt, new TreeMap<Integer, Integer>());
        histogram = contextHistograms.get(cxt);
        if (!histogram.containsKey(program.getOutput())) histogram.put(program.getOutput(), 0);
        histogram.put(program.getOutput(), histogram.get(program.getOutput()) + program.getStudents());
      }
    }
}



