package threadLocals.name;

import threadLocals.utils.User;

public class ThreadLocalDemo2 {

    // 第一种方法
    private static final ThreadLocal<User> USER_CONTEXT1 = new ThreadLocal(){
        @Override
        protected Object initialValue() {
            return new User(1, "user1");
        }
    };

    private static final ThreadLocal<User> USER_CONTEXT2 = ThreadLocal.withInitial(() -> new User(1, "user1"));

    public static void main(String[] args) {
        System.out.println(ThreadLocalDemo2.USER_CONTEXT1.get());
        System.out.println(ThreadLocalDemo2.USER_CONTEXT2.get());
    }
}
