package com.taotao.rest.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.druid.util.StringUtils;
import com.taotao.common.pojo.TaotaoResult;
import com.taotao.common.utils.JsonUtils;
import com.taotao.mapper.TbItemDescMapper;
import com.taotao.mapper.TbItemMapper;
import com.taotao.mapper.TbItemParamItemMapper;
import com.taotao.pojo.TbItem;
import com.taotao.pojo.TbItemDesc;
import com.taotao.pojo.TbItemParamItem;
import com.taotao.pojo.TbItemParamItemExample;
import com.taotao.pojo.TbItemParamItemExample.Criteria;
import com.taotao.rest.dao.JedisClient;
import com.taotao.rest.service.ItemService;

@Service
public class ItemServiceImpl implements ItemService {

	@Autowired
	private TbItemMapper itemMapper;
	
	@Autowired
	private TbItemDescMapper itemDescMapper;
	
	@Autowired
	private TbItemParamItemMapper paramItemMapper;
	
	@Value("${REDIS_ITEM_KEY}")
	private String REDIS_ITEM_KEY;
	
	@Value("${REDIS_ITEM_EXPIRE}")
	private Integer REDIS_ITEM_EXPIRE;
	
	@Autowired
	private JedisClient jedisClient;
	
	@Override
	public TaotaoResult getItemBaseInfo(long itemId) {
		String key = REDIS_ITEM_KEY + ":" + itemId + ":base";
		try{
			//添加缓存逻辑
			//从缓存中取商品信息，商品ID对应的信息
			String json = jedisClient.get(key);
			//判断是否有值
			if (!StringUtils.isEmpty(json)) {
				TbItem item = JsonUtils.jsonToPojo(json, TbItem.class);
				return TaotaoResult.ok(item);
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
		// 根据商品ID查询商品信息
		TbItem item = itemMapper.selectByPrimaryKey(itemId);
		try {
			//把商品信息写入缓存
			jedisClient.set(key, JsonUtils.objectToJson(item));
			//设置key的有效期
			jedisClient.expire(key, REDIS_ITEM_EXPIRE);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return TaotaoResult.ok(item);
	}

	/**
	 * 取商品描述信息
	 */
	@Override
	public TaotaoResult getItemDesc(long itemId) {
		String key = REDIS_ITEM_KEY + ":" + itemId + ":desc";
		try{
			//添加缓存逻辑
			//从缓存中取商品信息，商品ID对应的信息
			String json = jedisClient.get(key);
			//判断是否有值
			if (!StringUtils.isEmpty(json)) {
				TbItemDesc itemDesc = JsonUtils.jsonToPojo(json, TbItemDesc.class);
				return TaotaoResult.ok(itemDesc);
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
		// 根据商品ID查询商品描述信息
		TbItemDesc itemDesc = itemDescMapper.selectByPrimaryKey(itemId);
		try {
			//把商品描述信息写入缓存
			jedisClient.set(key, JsonUtils.objectToJson(itemDesc));
			//设置key的有效期
			jedisClient.expire(key, REDIS_ITEM_EXPIRE);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return TaotaoResult.ok(itemDesc);
	}

	@Override
	public TaotaoResult getItemParam(long itemId) {
		String key = REDIS_ITEM_KEY + ":" + itemId + ":param";
		try{
			//添加缓存逻辑
			//从缓存中取商品信息，商品ID对应的信息
			String json = jedisClient.get(key);
			//判断是否有值
			if (!StringUtils.isEmpty(json)) {
				TbItemParamItem paramItem = JsonUtils.jsonToPojo(json, TbItemParamItem.class);
				return TaotaoResult.ok(paramItem);
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
		// 根据商品ID查询商品规格参数信息，按条件查询
		TbItemParamItemExample example = new TbItemParamItemExample();
		Criteria criteria = example.createCriteria();
		criteria.andItemIdEqualTo(itemId);
		List<TbItemParamItem> list = paramItemMapper.selectByExampleWithBLOBs(example);
		if (list != null && list.size() > 0) {
			TbItemParamItem paramItem = list.get(0);
			try {
				//把商品信息写入缓存
				jedisClient.set(key, JsonUtils.objectToJson(paramItem));
				//设置key的有效期
				jedisClient.expire(key, REDIS_ITEM_EXPIRE);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return TaotaoResult.ok(paramItem);
		}
		
		return TaotaoResult.build(400, "无此商品规格");
	}

}
