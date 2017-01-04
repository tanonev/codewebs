package minions;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import util.IdCounter;

import models.Program;
import models.Subforest;
import models.ast.AST;
import models.ast.Forest;
import models.ast.Node;

/**
 * Class Jasonizer
 * The logic behind taking a forest and returning the corresponding json tree.
 * I image this will be used to save both subforests and contexts.
 */
public class Jasonizer {

	public static JSONObject jsonify(Subforest f) {
		return new Jasonizer().run(f);
	}
	
	public static JSONObject jsonify(AST t) {
		return new Jasonizer().runOnProgram(t);
	}

	private JSONObject runOnProgram(AST t) {
		Node rootNode = t.getARoot();
		JSONObject astJson = rootNode.getJson();
		JSONObject jsonRoot = new JSONObject();
		jsonRoot.put("root", astJson);
		return jsonRoot;
	}

	private JSONObject run(Subforest f) {
		List<Node> roots = f.getRoots();
		//IdCounter idCounter = new IdCounter();
		if(roots.size() == 1) {
			Node root = roots.get(0);
			return root.getJson();
		} else {
			
			JSONArray rootJsons = new JSONArray();
			for(Node root : roots) {
				JSONObject rootJson = root.getJson();
				rootJsons.put(rootJson);
			}
			JSONObject json = new JSONObject();
			json.put("statements", rootJsons);
			return json;
		}
	}
	
}
