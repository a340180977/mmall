package com.mmall.common;

import com.mmall.util.PropertiesUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisPool {
    //jedis连接池，通过jedis连接池获取jedis实例，通过jedis实例操作redis服务
    private static JedisPool pool;
    //最大连接数，即jedis连接池中jedis实例个数
    private static Integer maxTotal = Integer.parseInt(PropertiesUtil.getProperty("redis.max.total"));
    //在jedis连接池中最大的idle状态(空闲的)的jedis实例的个数
    private static Integer maxIdle = Integer.parseInt(PropertiesUtil.getProperty("redis.max.idle"));
    //在jedis连接池中最小的idle状态(空闲的)的jedis实例的个数
    private static Integer minIdle = Integer.parseInt(PropertiesUtil.getProperty("redis.min.idle"));
    //在borrow一个jedis实例的时候，是否要进行验证操作，如果赋值true，则得到的jedis实例肯定是可以用的
    private static Boolean testOnBorrow = Boolean.parseBoolean(PropertiesUtil.getProperty("redis.test.borrow"));
    //在return一个jedis实例的时候，是否要进行验证操作，如果赋值true，则放回jedispool的jedis实例肯定是可以用的
    private static Boolean testOnReturn = Boolean.parseBoolean(PropertiesUtil.getProperty("redis.test.return"));

    private static String redisIp = PropertiesUtil.getProperty("redis1.ip");
    private static Integer redisPort = Integer.parseInt(PropertiesUtil.getProperty("redis1.port"));


    private static void initPool() {

        JedisPoolConfig config = new JedisPoolConfig();

        config.setMaxTotal(maxTotal);
        config.setMaxIdle(maxIdle);
        config.setMinIdle(minIdle);

        config.setTestOnBorrow(testOnBorrow);
        config.setTestOnReturn(testOnReturn);
        //初始化jedis连接池
        pool = new JedisPool(config, redisIp, redisPort);
    }

    static {
        initPool();
    }

    //通过jedis连接池获取jedis实例，通过jedis实例操作redis服务
    public static Jedis getJedis() {
        return pool.getResource();
    }

    //把用完的jedis实例放回jedis连接池
    public static void returnResource(Jedis jedis) {
        pool.returnResource(jedis);
    }

    public static void main(String[] args) {
        //通过jedis连接池获取jedis实例
        Jedis jedis = pool.getResource();
        //通过jedis实例操作redis服务
        jedis.set("geelykey", "geelyvalue");
        //把用完的jedis实例放回jedis连接池
        returnResource(jedis);
        //临时调用，销毁连接池中的所有jedis实例
        pool.destroy();
        System.out.println("program is end");
    }
}
