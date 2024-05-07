package com.kneelawk.codextra.impl;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class FieldNameHelper {
    private static final int API = Opcodes.ASM9;

    public static String getCurrentlyInitializingFieldName(Class<?> callerClass, Class<?> fieldClass) {
        try (InputStream is = callerClass.getClassLoader()
            .getResourceAsStream(callerClass.getName().replace('.', '/') + ".class")) {
            if (is == null) return "<unknown>";
            ClassReader reader = new ClassReader(is);
            FieldNameClassWalker walker = new FieldNameClassWalker(callerClass, fieldClass);
            reader.accept(walker, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            Field initializingField = walker.initializingField;
            if (initializingField == null) return "<unknown>";
            return initializingField.getName();
        } catch (IOException e) {
            CodextraLog.LOGGER.error("Error loading caller class {}", callerClass, e);
            return "<unknown>";
        }
    }

    private static class FieldNameClassWalker extends ClassVisitor {
        private final Class<?> toSearch;
        private final Type toSearchType;
        private final Type fieldType;

        private Field initializingField;

        protected FieldNameClassWalker(Class<?> toSearch, Class<?> fieldClass) {
            super(API);
            this.toSearch = toSearch;
            toSearchType = Type.getType(toSearch);
            fieldType = Type.getType(fieldClass);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                         String[] exceptions) {
            if ("<clinit>".equals(name)) {
                return new FieldNameMethodWalker();
            }
            return null;
        }

        private class FieldNameMethodWalker extends MethodVisitor {
            protected FieldNameMethodWalker() {
                super(API);
            }

            @Override
            public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                if (initializingField == null && opcode == Opcodes.PUTSTATIC &&
                    Type.getObjectType(owner).equals(toSearchType) && Type.getType(descriptor).equals(fieldType)) {
                    try {
                        Field field = toSearch.getDeclaredField(name);
                        field.setAccessible(true);
                        if (field.get(null) == null) {
                            initializingField = field;
                        }
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        CodextraLog.LOGGER.error("Error getting field {}.{}", toSearch.getName(), name, e);
                    }
                }
            }
        }
    }
}
