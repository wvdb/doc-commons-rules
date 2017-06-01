package be.telenet.commons_rules;

import be.telenet.commons_rules.RuleEngine.Action;
import be.telenet.commons_rules.RuleEngine.Condition;
import be.telenet.commons_rules.RuleEngine.Context;
import be.telenet.commons_rules.RuleEngine.Selector;

import javax.script.*;
import java.util.Map;

public class Script implements Condition,Action,Selector {

	private static ScriptEngineManager manager = new ScriptEngineManager();
	private ScriptEngine engine = null;
	private String script = null;
	private CompiledScript compiledScript = null;
	
	public Script (String script, String syntax) {
        
		this.script = script;
		
		if (syntax == null)
			syntax = RuleEngine.DEFAULT_EXPRESSION_SYNTAX;
		
        engine = manager.getEngineByName(syntax);
        
        if (engine instanceof Compilable) {
        	
        	try {
        	
        		compiledScript = ((Compilable) engine).compile(script);
        		
        		//System.out.println(String.format("script compiled (engine=%s)", engine.getClass().getName()));
        		
        	} catch (Exception e) {
        		
        		throw new RuntimeException("Script compilation failed", e);
        	}
        }
	}

	@Override
	public boolean evaluate(Context context) {

        Object result = select(context);
        
        return result != null ? Boolean.valueOf(result.toString()) : false;
	}

	@Override
	public void execute(Context context) {

		evaluate (context);
	}

	@Override
	public Object select(Context context) {

		Map<String,Object> model = context.getModel();
		
        Bindings bindings = new SimpleBindings(model);

        Object result = null;
        
        try {
        	
        	result = compiledScript != null ? compiledScript.eval(bindings) : engine.eval(script, bindings);
        	
        	// Dirty hack to read back variables set in Nashorn scripts...
        	
        	if (model.containsKey(NASHORN_GLOBAL) && model.get(NASHORN_GLOBAL) instanceof Map) {
        		
            	@SuppressWarnings("unchecked")
				Map<String,Object> global = (Map<String,Object>) model.get(NASHORN_GLOBAL);
            	
            	model.putAll(global);
        	}
        	
        } catch (Exception e) {
        	
        	throw new RuntimeException("Script exception", e);
        }
        
		return result;
	}

	private static final String NASHORN_GLOBAL = "nashorn.global";
	
}
