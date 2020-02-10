package groovy.runtime.metaclass.org.flowable.task.service.impl.persistence.entity


import org.flowable.task.service.impl.persistence.entity.TaskEntityImpl

/**
 * @author martin.grofcik
 */
@SuppressWarnings("unused")
class TaskEntityImplMetaClass extends MetaClassImpl {
    TaskEntityImplMetaClass(Class theClass, MetaMethod[] add) {
        super(theClass, add)
    }

    TaskEntityImplMetaClass(Class theClass) {
        super(theClass)
    }

    TaskEntityImplMetaClass(MetaClassRegistry registry, Class theClass, MetaMethod[] add) {
        super(registry, theClass, add)
    }

    TaskEntityImplMetaClass(MetaClassRegistry registry, Class theClass) {
        super(registry, theClass)
    }

    Object invokeMissingProperty(Object instance, String propertyName, Object optionalValue, boolean isGetter) {
        if (instance instanceof TaskEntityImpl) {
            if (isGetter) {
                return instance.getVariable(propertyName)
            } else {
                instance.setVariable(propertyName, optionalValue)
                return null
            }
        }
        super.invokeMissingProperty(instance, propertyName, optionalValue, isGetter)
    }

}
