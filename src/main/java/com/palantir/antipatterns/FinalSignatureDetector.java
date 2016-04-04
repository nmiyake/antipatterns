package com.palantir.antipatterns;

import static org.apache.bcel.Constants.CONSTRUCTOR_NAME;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.bcel.BCELUtil;

public class FinalSignatureDetector implements Detector {

    private final BugReporter bugReporter;

    public FinalSignatureDetector(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        JavaClass obj = classContext.getJavaClass();
        if (BCELUtil.isSynthetic(obj)) {
            return;
        }
        for (Method method : obj.getMethods()) {
            if (method.isPublic() || method.isProtected()) {
                if (isIllegalFinalType(method.getReturnType(), classContext)) {
                    bugReporter.reportBug(new BugInstance(this, "PT_FINAL_TYPE_RETURN", NORMAL_PRIORITY)
                            .addClassAndMethod(obj, method)
                            .addType(method.getReturnType()));
                }
                if (obj.isFinal() && isConstructor(method)) {
                    bugReporter.reportBug(new BugInstance(this, "PT_FINAL_TYPE_CONSTRUCTOR", NORMAL_PRIORITY)
                            .addClassAndMethod(obj, method));
                }
                int param = 0;
                for (Type type : method.getArgumentTypes()) {
                    if (isIllegalFinalType(type, classContext)) {
                        bugReporter.reportBug(
                                new BugInstance(this, "PT_FINAL_TYPE_PARAM", NORMAL_PRIORITY)
                                        .addInt(param)
                                        .addClassAndMethod(obj, method)
                                        .addType(type));
                    }
                    param++;
                }
            }
        }
    }

    private static boolean isConstructor(Method method) {
        return method.getName().equals(CONSTRUCTOR_NAME);
    }

    private static boolean isIllegalFinalType(Type type, ClassContext classContext) {
        if (type instanceof ObjectType) {
            try {
                String className = ((ObjectType) type).getClassName();
                if (className.startsWith("java.")) {
                    // Types in java.lang are final for security reasons.
                    return false;
                }
                JavaClass cls = classContext.getAnalysisContext().lookupClass(className);
                return cls.isFinal() && !cls.isEnum();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    @Override
    public void report() {}
}
