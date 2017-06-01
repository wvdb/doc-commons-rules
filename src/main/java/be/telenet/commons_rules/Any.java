package be.telenet.commons_rules;

import be.telenet.commons_rules.RuleEngine.Condition;
import be.telenet.commons_rules.RuleEngine.Context;

public class Any extends CompositeCondition {

	public boolean evaluate(Context context) {

		for (Condition c : conditions)
			if (c.evaluate(context))
				return true;
		
		return false;
	}

}
