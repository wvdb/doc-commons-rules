package be.telenet.commons_rules;

import be.telenet.commons_rules.RuleEngine.Action;
import be.telenet.commons_rules.RuleEngine.Condition;
import be.telenet.commons_rules.RuleEngine.Context;
import be.telenet.commons_rules.RuleEngine.Rule;

public class RuleRef implements Condition,Action {

	private String ruleName = null;
	private Rule rule = null;
	
	public RuleRef (String ruleName) {
		
		this.ruleName = ruleName;
	}
	
	public RuleRef (Rule rule) {
		
		this.rule = rule;
	}
	
	@Override
	public boolean evaluate(Context context) {

		return rule != null ? rule.fire(context) : getRule(context).fire(context);
	}

	@Override
	public void execute(Context context) {

		evaluate (context);
	}

	private Rule getRule (Context context) {
		
		Rule rule = context.getRuleEngine().getRule(ruleName);
		
		if (rule == null)
			throw new RuntimeException("Rule not found in RuleEngine: " + ruleName);
		
		return rule;
	}
}
