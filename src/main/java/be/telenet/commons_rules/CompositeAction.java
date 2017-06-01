package be.telenet.commons_rules;

import be.telenet.commons_rules.RuleEngine.Action;
import be.telenet.commons_rules.RuleEngine.Context;

import java.util.LinkedList;
import java.util.List;

public class CompositeAction implements Action {
	
	protected List<Action> actions = new LinkedList<Action>();
	protected int max = -1;
	
	public CompositeAction add (Action...actions) {
		
		for (Action action : actions)
			this.actions.add(action);
		
		return this;
	}

	@Override
	public void execute (Context context) {

		for (Action action : actions)
			action.execute(context);
		
	}
	
}
