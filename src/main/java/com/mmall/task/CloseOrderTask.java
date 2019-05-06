package com.mmall.task;

import com.mmall.common.Const;
import com.mmall.common.RedissonManager;
import com.mmall.service.IOrderService;
import com.mmall.util.PropertiesUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class CloseOrderTask {

    @Autowired
    private IOrderService iOrderService;

    @Autowired
    private RedissonManager redissonManager;

    //每1分钟（每个1分钟的整数倍）
    @Scheduled(cron = "0 */1 * * * ?")
    public void closeOrderTaskV4() {
        //获取锁的实例对象（看getLock的源码就知道了）
        RLock lock = redissonManager.getRedisson().getLock(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
        //先默认没有获取到锁
        boolean getLock = false;
        try {
             /* 尝试获取锁的等待时间为0秒的原因：
                如果执行closeOrderTaskV4的时间很短，比如100毫秒，小于wait_time设置的1秒，这时tomcat1执行完退出了，锁就释放了，
                tomcat2就会获得到这个锁了，也就会把closeOrderTaskV4再执行一次，多个tomcat获得锁，会导致相同任务多次执行*/
             
            //尝试获取锁的等待时间为0秒，50秒后释放锁，时间单位是秒，释放锁的原因，假设A买东西，建立行锁，B就无法买进同一商品，所以要释放锁，B才能买进
            if (getLock = lock.tryLock(0, 50, TimeUnit.SECONDS)) {
                log.info("Redisson获取到分布式锁:{},ThreadName:{}", Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK, Thread.currentThread().getName());
                //关闭订单任务时间
                int hour = Integer.parseInt(PropertiesUtil.getProperty("close.order.task.time.hour"));
                //关闭订单
                iOrderService.closeOrder(hour);
            } else {
                log.info("Redisson没有获取到分布式锁:{},ThreadName:{}", Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK, Thread.currentThread().getName());
            }
        } catch (InterruptedException e) {
            log.error("Redisson分布式锁获取异常", e);
        } finally {
            //这行代码的作用：假如tomcat1获取到锁，tomcat2没有获取到锁，tomcat2直接返回，没有进行释放锁的操作
            if (!getLock) {
                return;
            }
            //释放锁
            lock.unlock();
            log.info("Redisson分布式锁释放锁");
        }
    }
}
