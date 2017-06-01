package be.telenet.commons_rules;

import be.telenet.commons_rules.RuleEngine.Context;

public class Not extends CompositeCondition {

	public Not () {
		
		this.max = 1;
	}
	
	public boolean evaluate(Context context) {

		return !first().evaluate(context);
	}

}
