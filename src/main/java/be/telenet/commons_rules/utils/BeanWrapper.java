package be.telenet.commons_rules.utils;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

// TODO: Support [] style indexes in the paths & index values that contain dots!

public class BeanWrapper {

	private Object bean = null;
	private Class<?> clazz = null;
	private Method[] methods = null;
	
	public BeanWrapper (Object bean) {
		
		this.bean = bean;
		this.clazz = bean.getClass();
		
		methods = clazz.getMethods();
	}
	
	public Object getBean () {
		
		return bean;
	}
	
	@SuppressWarnings("rawtypes")
	public Object getPropertyValue (String name) throws Exception {
		
		String[] parts = name.split("\\.", 2);
		String property = parts[0];
		
		Object value = null;
		
		if (bean instanceof Map) {
			
			value = ((Map) bean).get(property);
			
		} else {
		
			for (Method method : methods) {
				
				if (method.getParameterCount() > 0)
					continue;
				
				String methodName = method.getName().toLowerCase();
				
				if (methodName.equals("get" + property) || methodName.equals("is" + property)) {
					
					value = method.invoke(bean);
					
					//System.out.println(String.format("method=%s,value=%s", methodName, value));

					break;
				}
				
			}
		}

		if (parts.length > 1)
			return new BeanWrapper(value).getPropertyValue(parts[1]);
		
		return value;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void setPropertyValue (String name, Object value) throws Exception {
		
		int dotPos = name.lastIndexOf(".");

		String property = dotPos == -1 ? name : name.substring(dotPos + 1);
		
		if (dotPos != -1) {
			
			String path = name.substring(0, dotPos);
			
			new BeanWrapper(getPropertyValue(path)).setPropertyValue(property, value);
			
		} else if (bean instanceof Map) {
			
			((Map) bean).put(property, value);
		
		} else if (bean instanceof Collection) {
			
			((Collection) bean).add(value);
			
		} else {
		
			for (Method method : methods) {
				
				if (method.getParameterCount() != 1)
					continue;
				
				String methodName = method.getName().toLowerCase();
				
				if (methodName.equals("set" + property)) {
					
					method.invoke(bean, new Object[] { value });
					
					//System.out.println(String.format("method=%s,value=%s", methodName, value));

					break;
				}
				
			}
		}
	}
	
	/*
	private static String join (String delimiter, String[] array, int from, int to) {
		
		StringBuffer sb = new StringBuffer();
		
		for (int i = from; i < to; i++) {
			
			sb.append(array[i]);
			
			if (i < to - 1)
				sb.append(delimiter);
		}
		
		return sb.toString();
	}
	*/
}
