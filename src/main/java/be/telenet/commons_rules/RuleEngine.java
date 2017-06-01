package be.telenet.commons_rules;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class RuleEngine {
	
	public static final String DEFAULT_EXPRESSION_SYNTAX = "js";
	
	private Map<String,Rule> rules = new HashMap<String,Rule>();
	private List<Listener> listeners = new LinkedList<Listener>();
	
	public Rule getRule (String name) {
		
		return rules.get(name);
	}
	
	public Collection<Rule> getRules () {
		
		return rules.values();
	}
	
	public boolean fireAll (Context context) {
		
		boolean result = true;
		
		for (Rule rule : getRules())
			if (!rule.fire(context))
				result = false;
		
		return result;
	}
	
	public Rule createRule (String name) {
		
		Rule rule = new Rule(name);
		
		rules.put(rule.getName(), rule);

		return rule;
	}
	
	public Rule createRule () {
		
		return createRule(null);
	}
	
	public Context createContext () {
		
		return new Context();
	}
	
	public RuleEngine add (Listener...listeners) {
		
		for (Listener listener : listeners)
			this.listeners.add(listener);
		
		return this;
	}
	
	class Iteration {
	
		String name = "item";
		Selector selector = null;
		boolean andResults = true;
		
		Iteration(String name, Selector selector, boolean andResults) {
			
			if (name != null)
				this.name = name;
			
			this.selector = selector;
			this.andResults = andResults;
		}
	}
	
	public class Rule {

		private Condition condition = null;
		private Action actionTrue = null;
		private Action actionFalse = null;
		private Action actionBefore = null;
		private Action actionAfter = null;
		private String name = null;
		private Iteration iteration = null;
		
		private Rule (String name) {
			
			this.name = name != null ? name : UUID.randomUUID().toString();
		}
		
		public String getName () {
			
			return name;
		}
		
		public Action getThenAction () {
			
			return actionTrue;
		}
		
		public Action getOtherwiseAction () {
			
			return actionFalse;
		}
		
		public Action getBeforeAction () {
			
			return actionBefore;
		}
		
		public Action getAfterAction () {
			
			return actionAfter;
		}
		
		public Condition getCondition () {
			
			return condition;
		}

		public Rule when (Condition condition) {
			
			this.condition = condition;
			
			return this;
		}
		
		public Rule then (Action action) {
			
			this.actionTrue = action;
			
			return this;
		}
		
		public Rule otherwise (Action action) {
			
			this.actionFalse = action;
			
			return this;
		}
		
		public Rule before (Action action) {
			
			this.actionBefore = action;
			
			return this;
		}
		
		public Rule after (Action action) {
			
			this.actionAfter = action;
			
			return this;
		}
		
		public Rule iterate (String itemName, Selector selector, boolean andResults) {
			
			iteration = new Iteration(itemName, selector, andResults);
			
			return this;
		}
		
		public Rule iterate (Selector selector, boolean andResults) {
			
			return iterate(null, selector, andResults);
		}
		
		public Rule iterate (Selector selector) {
			
			return iterate(selector, true);
		}
		
		public boolean fire (Context context) {
			
			return context.execute(this);
		}
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		private boolean execute (Context context) {

			try {
					
				context.changeState(State.AT_START);
				
				if (actionBefore != null) {

					context.changeState(State.IN_BEFORE);
					
					actionBefore.execute(context);
				}
				
				if (iteration == null)
					return evaluate(context);
	
				Object selection = iteration.selector.select(context);
				
				if (selection != null && selection.getClass().isArray()) {
				
					int size = Array.getLength(selection);
					
					Collection col = new ArrayList(size);
					
					for (int i = 0; i < size; i++)
						col.add(Array.get(selection, i));
					
					selection = col;
					
				} else if (selection instanceof Map)
					selection = ((Map) selection).entrySet();
				
				boolean result = iteration.andResults;
	
				if (selection instanceof Collection) {
						
					Collection<?> col = (Collection<?>) selection;
					
					for (Object value : col) {
						
						context.add(iteration.name, value);
						
						boolean r = evaluate(context);
						
						if (iteration.andResults && !r)
							result = false;
						if (!iteration.andResults || r)
							result = true;
					}
	
					context.remove(iteration.name);
				}
				
				return result;
			
			} finally {
				
				if (actionAfter != null) {
					
					context.changeState(State.IN_AFTER);
					
					actionAfter.execute(context);
				}
				
				context.changeState(State.AT_END);
			}

		}
		
		private boolean evaluate (Context context) {
			
			if (condition == null)
				throw new RuntimeException("Rule condition is null");

			context.changeState(State.BEFORE_WHEN);
			
			context.changeState(State.IN_WHEN);
			
			boolean result = condition.evaluate(context);
			
			context.results.put(name, result);
			
			context.changeState(State.AFTER_WHEN);
			
			if (result) {
				
				if (actionTrue != null) {
					
					context.changeState(State.IN_THEN);
					
					actionTrue.execute(context);
				
					context.changeState(State.AFTER_THEN);
				}

			} else {
				
				if (actionFalse != null) {
					
					context.changeState(State.IN_OTHERWISE);
					
					actionFalse.execute(context);
					
					context.changeState(State.AFTER_OTHERWISE);
				}
				
			}
			
			return result;
		}
		
		public Boolean result (Context context) {
			
			return context.results.get(name);
		}
		
		@Override
		public String toString () {
			
			return String.format("Rule{name=%s}", name);
		}
	}

	public enum State {
		AT_START(true),
		IN_BEFORE(false),
		BEFORE_WHEN(true),
		IN_WHEN(false),
		AFTER_WHEN(true),
		IN_THEN(false),
		AFTER_THEN(true),
		IN_OTHERWISE(false),
		AFTER_OTHERWISE(true),
		IN_AFTER(false),
		AT_END(true);
		
		private boolean trigger = true;
		
		private State (boolean trigger) { this.trigger = trigger; }		
	}

	public class Context {

		private Map<String,Object> model = new HashMap<String,Object>();
		private List<String> log = new LinkedList<String>();
		private Set<String> executing = new HashSet<String>();
		private Rule currentRule = null;
		private Map<String,Boolean> results = new HashMap<String,Boolean>();
		private State state = null;

		private Context () {		
		}
		
		private void changeState (State state) {
			
			this.state = state;
			
			if (state.trigger)
				for (Listener listener : listeners)
					listener.handle(this);
		}
		
		public State getState () {
			
			return state;
		}

		public Rule getCurrentRule () {
			
			return currentRule;
		}
		
		public RuleEngine getRuleEngine () {
			
			return RuleEngine.this;
		}

		public Map<String, Object> getModel() {
			
			return model;
		}
		
		public Context add (String name, Object value) {
			
			model.put(name, value);
			
			return this;
		}
		
		public Context remove (String...names) {
			
			for (String name : names)
				model.remove(name);
			
			return this;
		}

		public Object get (String name) {
			
			return model.get(name);
		}
		
		public void log (String s) {
			
			log.add(s);
		}
		
		public List<String> getLog () {
			
			return log;
		}
		
		private boolean execute (Rule rule) {
			
			Rule previousRule = currentRule;
			
			currentRule = rule;
			
			String ruleName = rule.getName();
			
			if (executing.contains(ruleName))
				throw new RuntimeException("Execution loop detected for Rule: " + ruleName);
			
			executing.add(ruleName);
			
			try {
			
				return rule.execute(this);
				
			} finally {
			
				executing.remove(ruleName);
				
				currentRule = previousRule;
			}
		}
		
		public boolean fireAll () {
			
			return RuleEngine.this.fireAll(this);
		}
		
		@Override
		public String toString () {
			
			return String.format(
				"Context{state=%s,model=%s,current=%s,executing=%s,results=%s}", 
				state, 
				model, 
				currentRule.getName(), 
				executing, 
				results
			);
		}
	}
	
	public static abstract class Helper {
		
		public static CompositeCondition all (Condition... conditions) {
			
			return new All().add(conditions);
		}
		
		public static CompositeCondition any (Condition... conditions) {
			
			return new Any().add(conditions);
		}
		
		public static CompositeCondition not (Condition condition) {
			
			return new Not().add(condition);
		}
		
		public static Condition expression (String expression, String syntax) {
			
			return new Script(expression, syntax);
		}

		public static Condition expression (String expression) {
			
			return expression(expression, null);
		}
		
		public static Condition test (String path, Object value) {
			
			return new TestOrSetOrSelect(path, value);
		}

		public static Condition test (String path, Object value, String op) {
			
			return new TestOrSetOrSelect(path, value, op);
		}

		public static Condition test (String path, Object value, String op, String type) {
			
			return new TestOrSetOrSelect(path, value, type, op);
		}
		
		public static Condition test (String path, Object value, String op, String type, boolean ignoreCase) {
			
			return new TestOrSetOrSelect(path, value, type, op, ignoreCase);
		}
		
		public static Condition ref (String ruleName) {
			
			return new RuleRef(ruleName);
		}
		
		public static Condition ref (Rule rule) {
			
			return new RuleRef(rule);
		}

		public static Action sequence (Action...actions) {
			
			return new CompositeAction().add(actions);
		}

		public static Action script (String expression, String syntax) {
			
			return new Script(expression, syntax);
		}

		public static Action script (String expression) {
			
			return script(expression, null);
		}

		public static Action rule (String ruleName) {
			
			return new RuleRef(ruleName);
		}

		public static Action rule (Rule rule) {
			
			return new RuleRef(rule);
		}

		public static Action set (String path, Object value, String type) {
			
			return new TestOrSetOrSelect(path, value, type, null);
		}
		
		public static Action set (String path, Object value) {
			
			return new TestOrSetOrSelect(path, value);
		}

		public static Selector get (String path) {
			
			return new TestOrSetOrSelect(path, null);
		}
		
		public static Selector get (String path, String type) {
			
			return new TestOrSetOrSelect(path, null, type);
		}
		
		public static Selector select (String expression) {
			
			return select(expression, null);
		}

		public static Selector select (String expression, String syntax) {
			
			return new Script(expression, syntax);
		}

	}

	public interface Action {

		public void execute(Context context);
		
	}

	public interface Condition {

		public boolean evaluate(Context context);
		
	}

	public interface Selector {
		
		public Object select(Context context);
		
	}
	
	public interface Listener {
		
		public void handle(Context context);
		
	}
}
