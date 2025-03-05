package one.nio.gen;

import one.nio.util.JavaInternals;
import sun.misc.Unsafe;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;

public final class GenBootstraps {
	private static final MethodHandle ALLOCATE_INSTANCE;
	private static final MethodHandle PRIVATE_LOOKUP_IN;
	private static final MethodHandle DEFINE_HIDDEN_CLASS;

	static {
		MethodHandles.Lookup l = MethodHandles.lookup();
		try {
			ALLOCATE_INSTANCE = l.findVirtual(Unsafe.class, "allocateInstance", MethodType.methodType(Object.class, Class.class));
		} catch (ReflectiveOperationException ex) {
			throw new ExceptionInInitializerError(ex);
		}
		MethodHandle privateLookupIn = null;
		if (!JavaInternals.hasModules()) {
			try {
				privateLookupIn = l.findConstructor(MethodHandles.Lookup.class, MethodType.methodType(void.class, Class.class));
				privateLookupIn = MethodHandles.dropArguments(privateLookupIn, 1, MethodHandles.Lookup.class);
			} catch (NoSuchMethodException | IllegalAccessException ignored) {
			}
		}
		if (privateLookupIn == null) {
			try {
				privateLookupIn = l.findStatic(MethodHandles.class, "privateLookupIn", MethodType.methodType(MethodHandles.Lookup.class, Class.class, MethodHandles.Lookup.class));
			} catch (NoSuchMethodException ignored) {
			} catch (IllegalAccessException ex) {
				throw new ExceptionInInitializerError(ex);
			}
		}
		PRIVATE_LOOKUP_IN = privateLookupIn;
		MethodHandle defineHiddenClass;
		try {
			Class<?> option = Class.forName("java.lang.invoke.MethodHandles$Lookup$ClassOption");
			Object NESTMATE = Enum.valueOf((Class) option, "NESTMATE");
			Object options = Array.newInstance(option, 1);
			((Object[]) options)[0] = NESTMATE;
			defineHiddenClass = l.findVirtual(MethodHandles.Lookup.class, "defineHiddenClass", MethodType.methodType(MethodHandles.Lookup.class, byte[].class, boolean.class, options.getClass()));
			defineHiddenClass = MethodHandles.insertArguments(defineHiddenClass, 3, options);
		} catch (ClassNotFoundException | NoSuchMethodException ignored) {
			defineHiddenClass = null;
		} catch (IllegalAccessException ex) {
			throw new ExceptionInInitializerError(ex);
		}
		DEFINE_HIDDEN_CLASS = defineHiddenClass;
	}

	private GenBootstraps() {
	}

	public static Class<?> defineHiddenClass(MethodHandles.Lookup l, Class<?> host, byte[] code) {
		MethodHandle defineHiddenClass = DEFINE_HIDDEN_CLASS;
		if (defineHiddenClass != null) {
			try {
				return ((MethodHandles.Lookup) defineHiddenClass.invokeExact(l, code, false)).lookupClass();
			} catch (Throwable t) {
				throw new RuntimeException(t);
			}
		}
		return null;
	}

	public static MethodHandles.Lookup lookupFor(Class<?> c) {
		MethodHandles.Lookup l = MethodHandles.lookup();
		MethodHandles.Lookup newLookup = lookupIn(l, c);
		if (newLookup != l) {
			return newLookup;
		}
		throw new IllegalStateException("Cannot create lookup for " + c);
	}

	public static MethodHandles.Lookup lookupIn(MethodHandles.Lookup caller, Class<?> c) {
		MethodHandle privateLookupIn = PRIVATE_LOOKUP_IN;
		if (privateLookupIn != null) {
			try {
				caller = (MethodHandles.Lookup) privateLookupIn.invokeExact(c, caller);
			} catch (Throwable t) {
				throw new RuntimeException(t);
			}
		}
		return caller;
	}

	public static CallSite new_(MethodHandles.Lookup caller, String name, MethodType type) throws ReflectiveOperationException {
		Unsafe unsafe = JavaInternals.getUnsafe();
		Class<?> instanceType = type.returnType();
		MethodHandle mh = MethodHandles.insertArguments(
				ALLOCATE_INSTANCE,
				0,
				unsafe,
				instanceType
		);
		mh = mh.asType(MethodType.methodType(instanceType));
		return new ConstantCallSite(mh);
	}

	public static CallSite getfield(MethodHandles.Lookup caller, String name, MethodType type) throws ReflectiveOperationException {
		Class<?> declaringClass = type.parameterType(0);
		Class<?> fieldType = type.returnType();
		MethodHandles.Lookup l = lookupIn(caller, declaringClass);
		return new ConstantCallSite(l.findGetter(declaringClass, name, fieldType));
	}

	public static CallSite getstatic(MethodHandles.Lookup caller, String name, MethodType type, Class<?> declaringClass) throws ReflectiveOperationException {
		Class<?> fieldType = type.returnType();
		MethodHandles.Lookup l = lookupIn(caller, declaringClass);
		return new ConstantCallSite(l.findStaticGetter(declaringClass, name, fieldType));
	}

	public static CallSite putfield(MethodHandles.Lookup caller, String name, MethodType type) throws ReflectiveOperationException {
		Class<?> declaringClass = type.parameterType(0);
		Class<?> fieldType = type.parameterType(1);
		MethodHandles.Lookup l = lookupIn(caller, declaringClass);
		return new ConstantCallSite(l.findSetter(declaringClass, name, fieldType));
	}

	public static CallSite putstatic(MethodHandles.Lookup caller, String name, MethodType type, Class<?> declaringClass) throws ReflectiveOperationException {
		Class<?> fieldType = type.parameterType(0);
		MethodHandles.Lookup l = lookupIn(caller, declaringClass);
		return new ConstantCallSite(l.findStaticSetter(declaringClass, name, fieldType));
	}
}
