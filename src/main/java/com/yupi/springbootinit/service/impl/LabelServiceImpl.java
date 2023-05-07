package com.yupi.springbootinit.service.impl;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.constant.RedisKeyConstant;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.exception.label.NullLabelDataException;
import com.yupi.springbootinit.mapper.LabelMapper;
import com.yupi.springbootinit.model.entity.Label;
import com.yupi.springbootinit.model.vo.LabelVo;
import com.yupi.springbootinit.service.LabelService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author: tangchongjie
 * @creattime: 2023--05--05 14:52
 * @description
 */
@Service
@Slf4j
public class LabelServiceImpl extends ServiceImpl<LabelMapper, Label>
        implements LabelService {

    /**
     * 根标签的父类id
     */
    private static final Long PAREN_ID = 0l;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private ThreadPoolExecutor threadPool;

    @Override
    public List<LabelVo> getTreeLabelData() {

        //先查redis
        String redisTreeLabelDate = stringRedisTemplate.opsForValue().get(RedisKeyConstant.LABEL_DATA_KEY);

        //redis中有，直接返回
        if(!StrUtil.isBlank(redisTreeLabelDate)){
            log.info("从redis中查询label的树状数据成功");
            List<LabelVo> treeLabelData = JSONUtil.toBean(redisTreeLabelDate, new TypeReference<List<LabelVo>>() {
            }, false);
            return treeLabelData;
        }

        log.info("从redis中查询label的树状数据失败，开始从数据库中查询进行重建缓存");
        //防止缓存击穿问题,锁一下
        RLock lock = redissonClient.getLock(RedisKeyConstant.LABEL_LOCK_KEY);
        //60秒过期的分布式锁
        lock.lock(RedisKeyConstant.EXPIRE_TIME, TimeUnit.SECONDS);


        //开始构建数据==========================================================》：
        //获取到所有的数据
        List<Label> allLabelData = baseMapper.selectList(null);
        //查询数据库数据为空，抛异常
        if(ObjectUtil.isEmpty(allLabelData)){
            throw new NullLabelDataException(ErrorCode.NO_LABEL_DATA);
        }

        //先遍历找到根标签,其父类id为 0l
        List<LabelVo> targetLabelDataList = allLabelData.stream().filter(item -> {
            return item.getParentId().equals(PAREN_ID);
        }).map(label -> {
            LabelVo labelVo = new LabelVo();
            labelVo.setLabelName(label.getLabelName());
            labelVo.setParentId(label.getParentId());
            labelVo.setId(label.getId());
            labelVo.setChildrenLabel(selectChildren(labelVo, allLabelData));
            return labelVo;
        }).collect(Collectors.toList());

        log.info("label数据构造成功");

        //构造一下以labelName为key, 存储labelId的list为value的map，这是因为可能一个标签存在于多个父标签下，方便按名获取到id
        Map<String, List<Long>> labelNameToIdMap = getLabelNameToIdMap(targetLabelDataList);

        //讲二者都存入redis
        String labelJSONStr = JSONUtil.toJsonStr(targetLabelDataList);
        String labelNameToMapMapJSONStr = JSONUtil.toJsonStr(labelNameToIdMap);
        stringRedisTemplate.opsForValue().set(RedisKeyConstant.LABEL_DATA_KEY, labelJSONStr);
        stringRedisTemplate.opsForValue().set(RedisKeyConstant.LABEL_MAP_KEY, labelNameToMapMapJSONStr);
        log.info("数据存入redis成功");

        //解锁
        lock.unlock();

        return targetLabelDataList;
    }

    /**
     * 该方法用于构造以labelName为key, labelId为value的map，方便按名获取到id
     * @return 以labelName为key, labelId为value的map
     */
    public Map<String, List<Long>> getLabelNameToIdMap(List<LabelVo> list) {

        if(ObjectUtil.isEmpty(list)){
            return null;
        }

        Map<String, List<Long>> map = new HashMap<>();
        addItem(list, map);

        return map;
    }

    /**
     * 递归遍历list的元素与其子元素
     * @param map
     */
    private void addItem(List<LabelVo> list, Map<String, List<Long>> map) {

        for (LabelVo labelVo : list) {
            //如果已经存在了，那么向list中添加即可，不然就是put进行一个新的
            if(map.containsKey(labelVo.getLabelName())){
                List<Long> idList = map.get(labelVo.getLabelName());
                idList.add(labelVo.getId());
            }else {
                ArrayList<Long> arrayList = new ArrayList<>();
                arrayList.add(labelVo.getId());
                map.put(labelVo.getLabelName(), arrayList);
            }

            //有子集则开始向map中添加子集
            if(!ObjectUtil.isEmpty(labelVo.getChildrenLabel())){
                addItem(labelVo.getChildrenLabel(), map);
            }
        }
    }

    /**
     * 上一个方法的重载
     * @param list
     * @return
     */
    @Override
    public Map<String, List<Long>> getLabelNameToIdMap(String list) {
        List<LabelVo> labelVos = JSONUtil.toBean(list, new TypeReference<List<LabelVo>>() {
        }, false);
        return getLabelNameToIdMap(labelVos);
    }

    /**
     * 为传入的标签设置子标签
     * @param now 当前标签
     * @param all 所有的数据
     * @return now的子标签集合
     */
    public List<LabelVo> selectChildren(LabelVo now, List<Label> all){

        List<LabelVo> collect = all.stream().filter(item -> {
                    return item.getParentId().equals(now.getId());
        }).map(item -> {
            LabelVo labelVo = new LabelVo();
            labelVo.setLabelName(item.getLabelName());
            labelVo.setParentId(item.getParentId());
            labelVo.setId(item.getId());
            //递归设置自己的子标签
            labelVo.setChildrenLabel(selectChildren(labelVo, all));
            return labelVo;
        }).collect(Collectors.toList());
        return collect;
    }



}
