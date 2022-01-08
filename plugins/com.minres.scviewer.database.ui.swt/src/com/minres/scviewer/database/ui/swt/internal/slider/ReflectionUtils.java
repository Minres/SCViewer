package com.minres.scviewer.database.ui.swt.internal.slider;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectionUtils {
	/**
	 * Call a method using introspection (so ones can call a private or protected method)
	 * @param object object on which the method will be called
	 * @param methodName method name
	 * @param args arguments of this method (can be null)
	 * @return the value returned by this method (if this method returns a value)
	 */
	public static Object callMethod(final Object object, final String methodName, final Object... args) {
		if (object == null) {
			return null;
		}
		final Class<?>[] array = new Class<?>[args == null ? 0 : args.length];
		int index = 0;
		if (args != null) {
			for (final Object o : args) {
				array[index++] = o == null ? Object.class : o.getClass();
			}
		}

		return callMethodWithClassType(object, methodName, array, args);
	}

	private static Object callMethodWithClassType(final Object object, final String methodName, final Class<?>[] array, final Object... args) {
		Class<?> currentClass = object.getClass();
		Method method = null;
		while (currentClass != null) {
			try {
				method = currentClass.getDeclaredMethod(methodName, array);
				break;
			} catch (final NoSuchMethodException nsme) {
				currentClass = currentClass.getSuperclass();
			}
		}

		try {
			method.setAccessible(true);
			return method.invoke(object, args);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
			return null;
		}
	}
}