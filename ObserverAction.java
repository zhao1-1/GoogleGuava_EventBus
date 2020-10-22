// 2. ObserverAction类：用在ObserverRegistry观察者注册表类中
public class ObserverAction {
    private Object target;		// 观察者类
    private Method method;		// 方法
    
    public ObserverAction(Object target, Method method) {
        this.target = Preconditions.checkNotNull(target);
        this.method = method;
        this.method.setAccessible(true);
    }
    
    public void execute(Object event) {		// event是method方法的参数
        try {
            method.invoke(target, event);
        } catch (InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
