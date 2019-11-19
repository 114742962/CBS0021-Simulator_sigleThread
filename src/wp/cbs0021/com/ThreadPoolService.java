package wp.cbs0021.com;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**  
 * @ProjectName:  [TankWar] 
 * @Package:      [com.guiyajun.tank.FreshSinglePool.java]  
 * @ClassName:    [FreshSinglePool]   
 * @Description:  [单例线程池]   
 * @Author:       [Guiyajun]   
 * @CreateDate:   [2019年10月22日 下午4:28:13]   
 * @UpdateUser:   [Guiyajun]   
 * @UpdateDate:   [2019年10月22日 下午4:28:13]   
 * @UpdateRemark: [说明本次修改内容]  
 * @Version:      [v1.0]
 */

public class ThreadPoolService {
    private static final int DEFAULT_CORE_SIZE=100;
    private static final int MAX_QUEUE_SIZE=500;
    public volatile static ThreadPoolExecutor executor;
    
    /**
    * @Title: getInstance
    * @Description:  获取单例的线程池对象
    * @return ThreadPoolExecutor    返回类型
    * @throws
     */
    public static ThreadPoolExecutor getInstance() {
        if (executor == null) {
            synchronized (ThreadPoolService.class) {
                if (executor == null) {
                    // 核心线程数
                    executor = new ThreadPoolExecutor(DEFAULT_CORE_SIZE,
                    // 最大线程数
                    MAX_QUEUE_SIZE,
                    // 闲置线程存活时间
                    Integer.MAX_VALUE,
                    // 时间单位
                    TimeUnit.MILLISECONDS,
                    // 线程队列
                    new LinkedBlockingDeque<Runnable>(Integer.MAX_VALUE),
                    // 线程工厂
                    Executors.defaultThreadFactory()
                    );
                }
            }
        }
        return executor;
    }
    
    /**
    * @Title: execute
    * @Description: 开启线程池中的任务
    * @param @param runnable    实现了runnable接口的类的对象
    * @return void    返回类型
    * @throws
     */
    public void execute(Runnable runnable) {
        if (runnable == null) {
            return;
        }
        executor.execute(runnable);
    }

    /**
    * @Title: cancel
    * @Description: 从线程队列中移除对象
    * @param @param runnable    实现了runnable接口的类的对象 
    * @return void    返回类型
    * @throws
     */
    public void cancel(Runnable runnable) {
        if (executor != null) {
            executor.getQueue().remove(runnable);
        }
    }
}