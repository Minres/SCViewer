package org.apache.jdbm;

import org.apache.jdbm.Serialization.FastArrayList;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class stores information about serialized classes and fields.
 */
abstract class SerialClassInfo {

    static final Serializer<ArrayList<ClassInfo>> serializer = new Serializer<ArrayList<ClassInfo>>() {

        public void serialize(DataOutput out, ArrayList<ClassInfo> obj) throws IOException {
            LongPacker.packInt(out, obj.size());
            for (ClassInfo ci : obj) {
                out.writeUTF(ci.getName());
                out.writeBoolean(ci.isEnum);
                out.writeBoolean(ci.isExternalizable);
                if(ci.isExternalizable) continue; //no fields

                LongPacker.packInt(out, ci.fields.size());
                for (FieldInfo fi : ci.fields) {
                    out.writeUTF(fi.getName());
                    out.writeBoolean(fi.isPrimitive());
                    out.writeUTF(fi.getType());
                }
            }
        }

        public ArrayList<ClassInfo> deserialize(DataInput in) throws IOException, ClassNotFoundException {

            int size = LongPacker.unpackInt(in);
            ArrayList<ClassInfo> ret = new ArrayList<ClassInfo>(size);

            for (int i = 0; i < size; i++) {
                String className = in.readUTF();
                boolean isEnum = in.readBoolean();
                boolean isExternalizable = in.readBoolean();

                int fieldsNum = isExternalizable? 0 : LongPacker.unpackInt(in);
                FieldInfo[] fields = new FieldInfo[fieldsNum];
                for (int j = 0; j < fieldsNum; j++) {
                    fields[j] = new FieldInfo(in.readUTF(), in.readBoolean(), in.readUTF(), Class.forName(className));
                }
                ret.add(new ClassInfo(className, fields,isEnum,isExternalizable));
            }
            return ret;
        }
    };

    long serialClassInfoRecid;


    public SerialClassInfo(DBAbstract db, long serialClassInfoRecid, ArrayList<ClassInfo> registered){
        this.db = db;
        this.serialClassInfoRecid = serialClassInfoRecid;
        this.registered = registered;
    }

    /**
     * Stores info about single class stored in JDBM.
     * Roughly corresponds to 'java.io.ObjectStreamClass'
     */
    static class ClassInfo {

        private final String name;
        private final List<FieldInfo> fields = new ArrayList<FieldInfo>();
        private final Map<String, FieldInfo> name2fieldInfo = new HashMap<String, FieldInfo>();
        private final Map<String, Integer> name2fieldId = new HashMap<String, Integer>();
        private ObjectStreamField[] objectStreamFields;

        final boolean isEnum;

        final boolean isExternalizable;
                                  
		ClassInfo(final String name, final FieldInfo[] fields, final boolean isEnum, final boolean isExternalizable) {
            this.name = name;
            this.isEnum = isEnum;
            this.isExternalizable = isExternalizable;

            for (FieldInfo f : fields) {
                this.name2fieldId.put(f.getName(), this.fields.size());
                this.fields.add(f);
                this.name2fieldInfo.put(f.getName(), f);
            }
        }

        public String getName() {
            return name;
        }

        public FieldInfo[] getFields() {
            return (FieldInfo[]) fields.toArray();
        }

        public FieldInfo getField(String name) {
        	return name2fieldInfo.get(name);
        }

        public int getFieldId(String name) {
        	Integer fieldId = name2fieldId.get(name);
        	if(fieldId != null)
        		return fieldId;
            return -1;
        }

        public FieldInfo getField(int serialId) {
            return fields.get(serialId);
        }

		public int addFieldInfo(FieldInfo field) {
			name2fieldId.put(field.getName(), fields.size());
			name2fieldInfo.put(field.getName(), field);
			fields.add(field);
			return fields.size() - 1;
		}

        public ObjectStreamField[] getObjectStreamFields() {
			return objectStreamFields;
		}

		public void setObjectStreamFields(ObjectStreamField[] objectStreamFields) {
			this.objectStreamFields = objectStreamFields;
		}


    }

    /**
     * Stores info about single field stored in JDBM.
     * Roughly corresponds to 'java.io.ObjectFieldClass'
     */
    static class FieldInfo {    	
        private final String name;
        private final boolean primitive;
        private final String type;
        private Class typeClass;
        // Class containing this field
        private final Class clazz;
        private Object setter;
        private int    setterIndex;
        private Object getter;
        private int    getterIndex;

        public FieldInfo(String name, boolean primitive, String type, Class clazz) {
            this.name = name;
            this.primitive = primitive;
            this.type = type;
            this.clazz = clazz;
            try {
				this.typeClass = Class.forName(type);
			} catch (ClassNotFoundException e) {
				this.typeClass = null;
			}
            initSetter();
            initGetter();
        }

		private void initSetter() {
			// Set setter
			String setterName = "set" + firstCharCap(name);
			String fieldSetterName = clazz.getName() + "#" + setterName;

			Class aClazz = clazz; 
			
			// iterate over class hierarchy, until root class
			while (aClazz != Object.class) {
				// check if there is getMethod
				try {
					Method m = aClazz.getMethod(setterName, typeClass);
					if (m != null) {
						setter = m;
						return;
					}
				} catch (Exception e) {
					// e.printStackTrace();
				}

				// no get method, access field directly
				try {
					Field f = aClazz.getDeclaredField(name);
					// security manager may not be happy about this
					if (!f.isAccessible())
						f.setAccessible(true);
					setter = f;
					return;
				} catch (Exception e) {
//					e.printStackTrace();
				}
				// move to superclass
				aClazz = aClazz.getSuperclass();
			}
		}

		private void initGetter() {
			// Set setter
			String getterName = "get" + firstCharCap(name);
			String fieldSetterName = clazz.getName() + "#" + getterName;

			Class aClazz = clazz; 
			
			// iterate over class hierarchy, until root class
			while (aClazz != Object.class) {
				// check if there is getMethod
				try {
					Method m = aClazz.getMethod(getterName);
					if (m != null) {
						getter = m;
						return;
					}
				} catch (Exception e) {
					// e.printStackTrace();
				}

				// no get method, access field directly
				try {
					Field f = aClazz.getDeclaredField(name);
					// security manager may not be happy about this
					if (!f.isAccessible())
						f.setAccessible(true);
					getter = f;
					return;
				} catch (Exception e) {
//					e.printStackTrace();
				}
				// move to superclass
				aClazz = aClazz.getSuperclass();
			}
		}

		public FieldInfo(ObjectStreamField sf, Class clazz) {
            this(sf.getName(), sf.isPrimitive(), sf.getType().getName(), clazz);
        }

        public String getName() {
            return name;
        }

        public boolean isPrimitive() {
            return primitive;
        }

        public String getType() {
            return type;
        }

        private String firstCharCap(String s) {
            return Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }
    }


    ArrayList<ClassInfo> registered;
    Map<Class, Integer> class2classId = new HashMap<Class, Integer>();
    Map<Integer, Class> classId2class = new HashMap<Integer, Class>();

    final DBAbstract db;


    public void registerClass(Class clazz) throws IOException {
        if(clazz != Object.class)
        	assertClassSerializable(clazz);

        if (containsClass(clazz))
            return;

        ObjectStreamField[] streamFields = getFields(clazz);
        FieldInfo[] fields = new FieldInfo[streamFields.length];
        for (int i = 0; i < fields.length; i++) {
            ObjectStreamField sf = streamFields[i];
            fields[i] = new FieldInfo(sf, clazz);
        }

        ClassInfo i = new ClassInfo(clazz.getName(), fields,clazz.isEnum(), Externalizable.class.isAssignableFrom(clazz));
        class2classId.put(clazz, registered.size());
        classId2class.put(registered.size(), clazz);
        registered.add(i);


        if (db != null)
            db.update(serialClassInfoRecid, (Serialization) this, db.defaultSerializationSerializer);

    }

    private ObjectStreamField[] getFields(Class clazz) {
    	ObjectStreamField[] fields = null;
    	ClassInfo classInfo = null;
    	Integer classId = class2classId.get(clazz);
		if (classId != null) {
			classInfo = registered.get(classId);
			fields = classInfo.getObjectStreamFields();
		}
		if (fields == null) {
			ObjectStreamClass streamClass = ObjectStreamClass.lookup(clazz);
			FastArrayList<ObjectStreamField> fieldsList = new FastArrayList<ObjectStreamField>();
			while (streamClass != null) {
				for (ObjectStreamField f : streamClass.getFields()) {
					fieldsList.add(f);
				}
				clazz = clazz.getSuperclass();
				streamClass = ObjectStreamClass.lookup(clazz);
			}
			fields = new ObjectStreamField[fieldsList
					.size()];
			for (int i = 0; i < fields.length; i++) {
				fields[i] = fieldsList.get(i);
			}
			if(classInfo != null)
				classInfo.setObjectStreamFields(fields);
		}
        return fields;
    }

    private void assertClassSerializable(Class clazz) throws NotSerializableException, InvalidClassException {
    	if(containsClass(clazz))
    		return;
    	
        if (!Serializable.class.isAssignableFrom(clazz))
            throw new NotSerializableException(clazz.getName());
    }

	public Object getFieldValue(String fieldName, Object object) {
		try {
			registerClass(object.getClass());
		} catch (IOException e) {
			e.printStackTrace();
		}
		ClassInfo classInfo = registered.get(class2classId.get(object.getClass()));
		return getFieldValue(classInfo.getField(fieldName), object);
	}
	
	public Object getFieldValue(FieldInfo fieldInfo, Object object) {
		
		Object fieldAccessor = fieldInfo.getter;
		try {
			if (fieldAccessor instanceof Method) {
				Method m = (Method) fieldAccessor;
				return m.invoke(object);
			} else {
				Field f = (Field) fieldAccessor;
				return f.get(object);
			}
		} catch (Exception e) {

		}
		
		throw new NoSuchFieldError(object.getClass() + "." + fieldInfo.getName());
	}

	public void setFieldValue(String fieldName, Object object, Object value) {
		try {
			registerClass(object.getClass());
		} catch (IOException e) {
			e.printStackTrace();
		}
		ClassInfo classInfo = registered.get(class2classId.get(object.getClass()));
		setFieldValue(classInfo.getField(fieldName), object, value);
	}
	
	public void setFieldValue(FieldInfo fieldInfo, Object object, Object value) {
		
		Object fieldAccessor = fieldInfo.setter;
		try {
			if (fieldAccessor instanceof Method) {
				Method m = (Method) fieldAccessor;
				m.invoke(object, value);
			} else {
				Field f = (Field) fieldAccessor;
				f.set(object, value);
			}
			return;
		} catch (Throwable e) {
			e.printStackTrace();
		}
		
		throw new NoSuchFieldError(object.getClass() + "." + fieldInfo.getName());
	}
	
    public boolean containsClass(Class clazz) {
    	return (class2classId.get(clazz) != null);
    }

    public int getClassId(Class clazz) {
    	Integer classId = class2classId.get(clazz);
    	if(classId != null) {
    		return classId;
        }
        throw new Error("Class is not registered: " + clazz);
    }

    public void writeObject(DataOutput out, Object obj, FastArrayList objectStack) throws IOException {
        registerClass(obj.getClass());

        //write class header
        int classId = getClassId(obj.getClass());
        LongPacker.packInt(out, classId);
        ClassInfo classInfo = registered.get(classId);

        if(classInfo.isExternalizable){
            Externalizable o = (Externalizable) obj;
            DataInputOutput out2 = (DataInputOutput) out;
            try{
                out2.serializer = this;
                out2.objectStack = objectStack;
                o.writeExternal(out2);
            }finally {
                out2.serializer = null;
                out2.objectStack = null;
            }
            return;
        }

        
        if(classInfo.isEnum) {
        	int ordinal = ((Enum)obj).ordinal();
            LongPacker.packInt(out, ordinal);
        }

        ObjectStreamField[] fields = getFields(obj.getClass());
        LongPacker.packInt(out, fields.length);

        for (ObjectStreamField f : fields) {
            //write field ID
            int fieldId = classInfo.getFieldId(f.getName());
            if (fieldId == -1) {
                //field does not exists in class definition stored in db,
                //propably new field was added so add field descriptor
                fieldId = classInfo.addFieldInfo(new FieldInfo(f, obj.getClass()));
                db.update(serialClassInfoRecid, (Serialization) this, db.defaultSerializationSerializer);
            }
            LongPacker.packInt(out, fieldId);
            //and write value
            Object fieldValue = getFieldValue(classInfo.getField(fieldId), obj);
            serialize(out, fieldValue, objectStack);
        }
    }


    public Object readObject(DataInput in, FastArrayList objectStack) throws IOException {
        //read class header
        try {
            int classId = LongPacker.unpackInt(in);
            ClassInfo classInfo = registered.get(classId);
//            Class clazz = Class.forName(classInfo.getName());
            Class clazz = classId2class.get(classId);
            if(clazz == null)
            	clazz = Class.forName(classInfo.getName());
            assertClassSerializable(clazz);

            Object o;
            
            if(classInfo.isEnum) {
                int ordinal = LongPacker.unpackInt(in);
                o = clazz.getEnumConstants()[ordinal];
            }
            else {
            	o = createInstance(clazz, Object.class);
            }
            
            objectStack.add(o);

            if(classInfo.isExternalizable){
                Externalizable oo = (Externalizable) o;
                DataInputOutput in2 = (DataInputOutput) in;
                try{
                    in2.serializer = this;
                    in2.objectStack = objectStack;
                    oo.readExternal(in2);
                }finally {
                    in2.serializer = null;
                    in2.objectStack = null;
                }

            }else{
                int fieldCount = LongPacker.unpackInt(in);
                for (int i = 0; i < fieldCount; i++) {
                    int fieldId = LongPacker.unpackInt(in);
                    FieldInfo f = classInfo.getField(fieldId);
                    Object fieldValue = deserialize(in, objectStack);
                    setFieldValue(f, o, fieldValue);
                }
            }
            return o;
        } catch (Exception e) {
            throw new Error("Could not instanciate class", e);
        }
    }

    //TODO dependecy on nonpublic JVM API
    static private sun.reflect.ReflectionFactory rf =
            sun.reflect.ReflectionFactory.getReflectionFactory();
    
    private static Map<Class, Constructor> class2constuctor = new HashMap<Class, Constructor>();
    
    /**
     * Little trick to create new instance without using constructor.
     * Taken from http://www.javaspecialists.eu/archive/Issue175.html
     */
    private static <T> T createInstance(Class<T> clazz, Class<? super T> parent) {

        try {
        	Constructor intConstr = class2constuctor.get(clazz);

			if (intConstr == null) {
				Constructor objDef = parent.getDeclaredConstructor();
				intConstr = rf.newConstructorForSerialization(
						clazz, objDef);
				class2constuctor.put(clazz, intConstr);
			}
			
            return clazz.cast(intConstr.newInstance());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot create object", e);
        }
    }

    protected abstract Object deserialize(DataInput in, FastArrayList objectStack) throws IOException, ClassNotFoundException;

    protected abstract void serialize(DataOutput out, Object fieldValue, FastArrayList objectStack) throws IOException;
//


}
