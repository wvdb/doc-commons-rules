package be.telenet.commons_rules;

import be.telenet.commons_rules.RuleEngine.Condition;
import java.util.LinkedList;
import java.util.List;

public abstract class CompositeCondition implements Condition {
	
	protected List<Condition> conditions = new LinkedList<Condition>();
	protected int max = -1;
	
	public CompositeCondition add (Condition...conditions) {
		
		for (Condition condition : conditions)
			if (max <= 0 || this.conditions.size() < max)
				this.conditions.add(condition);
			else
				throw new RuntimeException("Trying to add too many Conditions");
		
		return this;
	}
	
	protected Condition first () {
		
		return conditions.get(0);
	}
}
