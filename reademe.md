+ **EventBus框架（Google Guava）：**

  + 组成：

    + `EventBus`类：【封装Guava EventBus对外暴露的所有可调用接口、实现了同步阻塞的观察者模式】

      > 主体（被观察者）类中需要注入EventBus类。

    + `AsyncEventBus`类（继承自EventBus类）：【通过多态形式创建，提供了异步非阻塞的观察者模式】

    + `EvenBus.register()`函数：【用来注册观察者，形成Observer注册表】

      > 可以接受任何类型（Object）的观察者，使得观察者类无需实现传统的Observer接口。

    + Observer注册表：【形成`消息类型`与`观察者事件处理函数`映射表】

      > 实例：
      >
      > `EventMsg`    --->   ` Android.actionOnEvent`
      >
      > `EventMsg_a`  -->  ` Android.actionOnEvent`、`Iphone.handleOnEvent`
      >
      > `EventMsg_b`  -->  ` Android.actionOnEvent`、`BlackBerry.actionOnEvent`
      >
      > `EventMsg_x`  -->  `BlackBarrey.actionOnEvent2`

    + `EvenBus.unregister()`函数：【从Observer注册表中移除某个观察者】

    + `EvenBus.post()`函数：【给Observer注册表内所有匹配的观察者发送事件消息】

      > 可匹配：能接收的消息类型是发送消息（post 函数定义中的 event）类型的父类。

    + `@Subscribe`注解（作用于观察者类中）：【用来标明观察者中某个函数能接收哪种类型的消息】

  + 工作流程：

    1. 主体调用`register()`函数注册观察者；

    2. EventBus解析所有观察者类中的`@Subscribe`注解；

    3. 生成Observer注册表；

    4. 生成消息事件

    5. 主体调用`post()`函数转发消息事件给观察者对应的事件处理函数：

       5.1  根据Observer注册表调取对应的`观察者事件处理函数`；

       5.2  将消息转发给可匹配的观察者（并非把消息发送给**所有的**观察者）；

       5.3  利用java反射执行对应的`观察者事件处理函数`。

  + 应用：

    ```java
    //（1）EventMsg事件消息
    public class EventMsg {
    }
    public class EventMsg_a extends EventMsg {
    }
    public class EventMsg_b extends EventMsg {
    }
    public class EventMsg_x {
    }
    
    //（2）被观察者
    /*
    	无需定义Subject接口，但是需要引入EventBus依赖：
    		register()函数：注册观察者群
    		post()函数：给观察者群发送事件消息
    */
    public class MojiApp {
        private EventBus eventBus = new AsyncEventBus(Executors.newFixedThreadPool(20));	// 异步阻塞模式
        
        public void setObservers(List<Observer> obs) {
            for (Observer ob : obs) {
                eventBus.register(ob);
            }
        }
        public void doSth(EventMsg em) {
            eventBus.post(em);
        }
    }
    
    //（3）观察者
    /*
    	无需定义Observer接口，任意类型的对象都可以注册进EventBus中，
    	通过 @Subscribe 注解来标明类中哪个函数可以接收被观察者发送的消息。
    */
    public class IPhone {
        private String seid;							// 依赖注入：iPhone手机的序列号
    
        @Subscribe
        public void handleOnEvent(EventMsg_a em) {		// 消息事件的类型可以不同，函数名可以不同。
            //TODO: 获取消息通知，执行自己的逻辑...
        }
    }
    
    public class Android {
        private String seid;							// 依赖注入：安卓手机的序列号
    
        @Subscribe
        public void actionOnEvent(EventMsg em) {
            //TODO: 获取消息通知，执行自己的逻辑...
        }
    }
    
    public class BlackBerry {
        private String seid;							// 依赖注入：黑莓手机的序列号
    
        @Subscribe
        public void actionOnEvent(EventMsg_b em) {
            //TODO: 获取消息通知，执行自己的逻辑...
        }
        @Subscribe
        public void actionOnEvent2(EventMsg_x em) {
            //TODO: 获取消息通知，执行自己的逻辑...
        }
    }
    
    //（4）运行
    public class Test {
        public static void main(String[] args) {
            //（4.1）生成观察者群（可以交给spring框架管理）
            List<Observer> obs = new ArrayList<>();
            obs.add(new Iphone("4352"));
            obs.add(new Iphone("6938"));
            obs.add(new Android("4451"));
            obs.add(new BlackBerry("1249"));
            //（4.2）创建被观察者（可以交给spring框架管理）
            MojiApp mj = new MojiApp();
            //（4.3）利用EventBus将观察者群注册进主体（被观察者）
            mj.setObservers(obs);
            //（4.4）创建消息事件对象（注意继承关系）（可以交给spring框架管理）
            EventMsg em = new EventMsg();
            EventMsg_a em_a = new EventMsg_a();
            EventMsg_b em_b = new EventMsg_b();
            EventMsg_x em_x = new EventMsg_x();
             //（4.5）调用主体的“业务”方法（EventBus会根据消息事件的类型来匹配不同观察者内的不同方法去发送）
            mj.doSth(em);							// 能接收到消息：安卓
            mj.doSth(em_a);							// 能接收到消息：安卓、IPhone
            mj.doSth(em_b);							// 能接收到消息：安卓、黑莓(actionOnEvent()方法执行)
            mj.doSth(em_x);							// 能接收到消息：黑莓(actionOnEvent2()方法执行)
        }
    }
    ```

  + 实现EventBus框架：

    ```java
    // 1. @Subscribe注解
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @Beta
    public @interface Subscribe {}
    ```

    ```java
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
    ```

    ```java
    // 3. ObserverRegistry：Observer注册表类
    public class ObserverRegistry {
        private ConcurrentMap<Class<?>, CopyOnWriteArraySet<ObserverAction>> registry = new ConcurrentHashMap<>();
        
        /**
         * 目的：将findAllObserverAction(Object ob)函数的结果（一个观察者的【事件消息类型 -> 观察者事件处理函数】映射表）注册进EventBus的注册表中
         * @param ob：观察者类的对象
         * @return void
         */
        public void register(Object ob) {
            Map<Class<?>, Collection<ObserverAction>> observerActions = findAllObserverActions(ob);
            for (Map.Entry<Class<?>, Collection<ObserverAction>> entry : observerActions.entrySet()) {
                Class<?> eventType = entry.getKey();
                Collection<ObserverAction> eventActions = entry.getValue();
                CopyOnWriteArraySet<ObserverAction> registeredEventActions = registry.get(eventType);
                if (registeredEventActions == null) {
                    registry.putIfAbset(eventType, new CopyOnWriteArraySet<>());
                    registeredEventActions = registry.get(eventType);
                }
                registeredEventActions.addAll(eventActions);
            }
        }
        
        /**
         * 目的：
         * @param event：事件消息类型的对象
         * @return matchedObservers：EventBus注册表中与事件消息类型匹配的所有ObserverAction列表
         */
        public List<ObserverAction> getMatchedObserverActions(Object event) {
            List<ObserverAction> matchedObservers = new ArrayList<>();
            Class<?> postedEventType = event.getClass();
            for (Map.Entry<Class<?>, CopyOnWriteArraySet<ObserverAction>> entry : registry.entrySet()) {
                Class<?> eventType = entry.getKey();
                Collection<ObserverAction> eventActions = entry.getValue();
                if (postedEventType.isAssignableFrom(eventType)) {
                    matchedObservers.addAll(eventActions);
                }
            }
            return matchedObservers;
        }
    
    
        /**
         * 目的：整理并导出一个观察者的【事件消息类型 -> 观察者事件处理函数】映射表
         * @param ob：观察者类的对象
         * @return observerActions：Map<事件消息参数类型.class, List<该观察者对象+方法1, 该观察者对象+方法2.....>>
         */
        private Map<Class<?>, Collection<ObserverAction>> findAllObserverAction(Object ob) {
            Map<Class<?>, Collection<ObserverAction>> observerActions = new HashMap<>();
            Class<?> clazz = ob.getClass();
            for (Method method : getAnnotatedMethods(clazz)) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                Class<?> eventType = parameterTypes[0];
                if (!observerActions.containsKey(eventType)) {
                    observerActions.put(eventType, new ArrayList<>());
                }
                observerActions.get(eventType).add(new ObserverAction(ob, method));
            }
            return observerActions;
        }
        
        /**
         * 目的：导出一个观察者类对象下所有被@Subscribe注解修饰的方法
         * @param clazz：观察者类的对象的字节码对象clazz = ob.getClass()
     * @return annotatedMethods：List<Method>
         */
        private List<Method> getAnnotatedMethods(Class<?> clazz) {
            List<Method> annotatedMethods = new ArrayList<>();
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Subscribe.class)) {
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    Proconditions.checkArgument(parameterTypes.length == 1,
                                               "method %s has @Subscribe annotation but has %s parameters."
                                                + "Subscriber methods must have exactly 1 parameter.", 
                                               method, parameterTypes.length);
                    annotatedMethods.add(method);
                }
            }
            return annotatedMethods;
        }
    }
    ```

    ```java
    // 4. EventBus
    public class EventBus {
        private Executor executor;
        private ObserverRegistry registry = new ObserverRegistry();
        
        public EventBus() {
            // MoreExecutors.directExecutor() 是 Google Guava 提供的工具类，看似是多线程，实际上是单线程。
            // 之所以要这么实现，主要还是为了跟 AsyncEventBus 统一代码逻辑，做到代码复用。
            this(MoreExecutors.directExecutor());
        }
        protected EventBus(Executor executor) {
            this.executor = executor;
    }
        
        public void register(Object ob) {
            registry.register(ob);
        }
        public void post(Object event) {
            List<ObserverAction> observerActions = registry.getMatchedObserverActions(event);
            for (ObserverAction observerAction : observerActions) {
                executor.execute(new Runnable() {
               @Override
                    public void run() {
                        observerAction.execute(event);
                    }
                });
            }
        }
    }
    ```

    ```java
    // 5. AsyncEventBus
    public class AsyncEventBus extends EventBus {
        public AsyncEventBus(Executor executor) {
            super(executor);
        }
    }
    ```

    

