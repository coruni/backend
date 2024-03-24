package com.Fanbbs.common;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public  class RedisHelp {

    //
    public  void setKey(String key, Map<String, Object> map,Integer time,RedisTemplate redisTemplate) {
        redisTemplate.opsForHash().putAll(key, map);
        redisTemplate.expire(key, time, TimeUnit.SECONDS);
        //redisTemplate.opsForValue().set(key, map,time);
    }

    public void setRedis(String key, String value,Integer time ,RedisTemplate redisTemplate) {
        //关于TimeUnit下面有部分源码截图
        redisTemplate.opsForValue().set(key,value,time, TimeUnit.SECONDS);

    }
    public String getRedis(String key,RedisTemplate redisTemplate){
        if(redisTemplate.opsForValue().get(key)==null){
            return null;
        }
        return redisTemplate.opsForValue().get(key).toString();
    }

    //获取一个redis的map
    public  Map<Object, Object> getMapValue(String key,RedisTemplate redisTemplate) {
        return  redisTemplate.opsForHash().entries(key);
    }

    public void saveMapToRedis(String key, Map<Integer, Double> map,RedisTemplate redisTemplate,  int expireSeconds) {
        redisTemplate.opsForHash().putAll(key, map);
        redisTemplate.expire(key, expireSeconds, TimeUnit.SECONDS);
    }
    public Map<Integer, Double> getMapFromRedis(String key ,RedisTemplate redisTemplate) {
        return redisTemplate.opsForHash().entries(key);
    }

    public void updateMapInRedis(String key, Map<Integer, Double> map, RedisTemplate redisTemplate) {
        redisTemplate.opsForHash().putAll(key, map);
    }
    public  Object getValue(String key, String hashKey,RedisTemplate redisTemplate) {
        return  redisTemplate.opsForHash().get(key, hashKey);
    }

    public  void deleteData(List<String> keys,RedisTemplate redisTemplate) {
        // 通过key执行批量删除操作时先序列化template
        redisTemplate.setKeySerializer(new JdkSerializationRedisSerializer());
        redisTemplate.delete(keys);
    }
    public  void delete(String key,RedisTemplate redisTemplate) {
        // 通过key执行批量删除操作时先序列化template
        redisTemplate.delete(key);
    }

    public Set<Integer> getSetFromRedis(String key, RedisTemplate<String, String> redisTemplate) {
        Set<String> stringSet = redisTemplate.opsForSet().members(key);
        return stringSet.stream().map(Integer::valueOf).collect(Collectors.toSet());
    }

    public void saveSetToRedis(String key, Set<Integer> set, RedisTemplate<String, String> redisTemplate, long expireSeconds) {
        Set<String> stringSet = set.stream().map(String::valueOf).collect(Collectors.toSet());
        redisTemplate.opsForSet().add(key, stringSet.toArray(new String[0]));
        redisTemplate.expire(key, expireSeconds, TimeUnit.SECONDS);
    }

    public void updateSetInRedis(String key, Set<String> set, RedisTemplate<String, Object> redisTemplate) {
        redisTemplate.opsForSet().remove(key);
        redisTemplate.opsForSet().add(key, set.toArray(new String[0]));
    }
    //数据列表的操作，优化文章性能
    /**
     * 缓存List数据
     *
     * @param key      缓存的键值
     * @param dataList 待缓存的List数据
     * @return 缓存的对象
     */
    public <T> long setList(final String key, final List<T> dataList,final Integer time,RedisTemplate redisTemplate) {
        Long count = redisTemplate.opsForList().rightPushAll(key, dataList);
        redisTemplate.expire(key, time, TimeUnit.SECONDS);
        return count == null ? 0 : count;
    }

    /**
     * 获得缓存的list对象
     *
     * @param key 缓存的键值
     * @return 缓存键值对应的数据
     */
    public <T> List<T> getList(final String key,RedisTemplate redisTemplate) {
        return redisTemplate.opsForList().range(key, 0, -1);
    }

    /**
     * 泛匹配删除
     *
     * deleteKeysWithPattern("*example*", redisTemplate);
     */
    public void deleteKeysWithPattern(String pattern, RedisTemplate<String, String> redisTemplate) {
        Set<String> keysToDelete = redisTemplate.keys(pattern);

        // 遍历匹配的键，并删除它们
        for (String key : keysToDelete) {
            redisTemplate.delete(key);
        }
    }
}
