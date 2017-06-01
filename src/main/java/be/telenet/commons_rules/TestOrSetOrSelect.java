package be.telenet.commons_rules;

import be.telenet.commons_rules.RuleEngine.Action;
import be.telenet.commons_rules.RuleEngine.Condition;
import be.telenet.commons_rules.RuleEngine.Context;
import be.telenet.commons_rules.RuleEngine.Selector;
import be.telenet.commons_rules.utils.BeanWrapper;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

public class TestOrSetOrSelect implements Condition,Action,Selector {

	public enum Operator {
		STREQ,
		REGEX,
		EQ,
		LT,
		LE,
		GT,
		GE,
		IN,
		CONTAINS
	}
	
	private String path = null;
	private Object value = null;
	private Operator operator = Operator.EQ;
	private String type = null;
	private boolean ignoreCase = false;
	
	public TestOrSetOrSelect (String path, Object value, String type, String operator, boolean ignoreCase) {
		
		this.path = path;
		this.value = value;
		this.type = type;
		this.ignoreCase = ignoreCase;	
		
		if (operator != null)
			this.operator = Operator.valueOf(operator.toUpperCase());
		
		if (type != null) {
			
			try {
			
				if (!(value instanceof Map) && !(value instanceof Collection) && !value.getClass().isArray())
					this.value = convert(value, type);
				
			} catch (Exception e) {
				
				throw new RuntimeException("Type conversion failed", e);
			}
		}
	}
		
	public TestOrSetOrSelect (String path, Object value, String type, String operator) {
		
		this(path, value, type, operator, false);	
	}

	public TestOrSetOrSelect (String path, Object value, String operator) {
		
		this(path, value, null, operator, false);
	}
		
	public TestOrSetOrSelect (String path, Object value) {
		
		this(path, value, null);
	}

	@Override
	public boolean evaluate(Context context) {

		BeanWrapper wrapper = new BeanWrapper(context.getModel());
		
		try {

			Object propertyValue = wrapper.getPropertyValue(path);
			
			if (propertyValue == null)
				return false;
			
			Object targetValue = value;
			
			if (type != null)
				propertyValue = convert(propertyValue, type);
					
			switch (operator) {
				case STREQ: return ignoreCase ? 
					propertyValue.toString().equalsIgnoreCase(targetValue.toString()) : 
					propertyValue.toString().equals(targetValue.toString());
				case REGEX: return ignoreCase ? 
					propertyValue.toString().toLowerCase().matches(targetValue.toString()) :
					propertyValue.toString().matches(targetValue.toString().toLowerCase());
				case EQ: return equals(propertyValue, targetValue);
				case IN: return in(propertyValue, targetValue);
				case CONTAINS: return in(targetValue, propertyValue);
				case GT: return compare(propertyValue, targetValue) > 0;
				case GE: return compare(propertyValue, targetValue) >= 0;
				case LT: return compare(propertyValue, targetValue) < 0;
				case LE: return compare(propertyValue, targetValue) <= 0;
				default: return false;
			}

		} catch (Exception e) {
			
			throw new RuntimeException("BeanWrapper exception", e);
		}
		
	}

	@Override
	public void execute(Context context) {

		BeanWrapper wrapper = new BeanWrapper(context.getModel());
		
		try {
			
			wrapper.setPropertyValue(path, value);

		} catch (Exception e) {
			
			throw new RuntimeException("BeanWrapper exception", e);			
		}
		
	}

	@Override
	public Object select(Context context) {

		try {
			
			return new BeanWrapper(context.getModel()).getPropertyValue(path);

		} catch (Exception e) {
			
			throw new RuntimeException("BeanWrapper exception", e);			
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static int compare (Object propertyValue, Object targetValue) {
		
		if (!(propertyValue instanceof Comparable))
			throw new RuntimeException("Value is not a Comparable: " + propertyValue);
			
		if (!(targetValue instanceof Comparable))
			throw new RuntimeException("Value is not a Comparable: " + targetValue);
				
		return ((Comparable) propertyValue).compareTo(targetValue);	
	}
	
	private static boolean equals (Object propertyValue, Object targetValue) {
		
		return propertyValue.equals(targetValue);
	}
	
	@SuppressWarnings("rawtypes")
	private static boolean in (Object propertyValue, Object targetValue) {
		
		if (targetValue instanceof Collection) {
			
			return ((Collection) targetValue).contains(propertyValue);
			
		} else if (targetValue.getClass().isArray()) {
			
		    int length = Array.getLength(targetValue);
		    
		    for (int i = 0; i < length; i ++) {
		    	
		        Object item = Array.get(targetValue, i);
		        
		        if (item != null && item.equals(propertyValue))
		        	return true;
		    }

		}
		
		return false;
	}
	
	public static Object convert (Object value, String className) throws Exception {
		
		if (className == null)
			return value;
		
		Class<?> clazz = forName(className);
		
		if (clazz == value.getClass())
			return value;
		
		Method valueOf = clazz.getMethod("valueOf", String.class);
		
		return valueOf.invoke(null, value.toString());
	}
	
	private static Class<?> forName (String className) throws Exception {

		if (className.equals("int"))
			return Integer.class;
		
		if (className.equals("long"))
			return Long.class;
		
		if (className.equals("float"))
			return Float.class;
		
		if (className.equals("double"))
			return Double.class;
		
		if (className.equals("boolean"))
			return Boolean.class;
		
		if (className.indexOf(".") == -1)
			className = "java.lang." + className;
		
		return Class.forName(className);
	}

}
