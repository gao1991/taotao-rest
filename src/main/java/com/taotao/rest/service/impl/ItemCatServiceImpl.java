package com.taotao.rest.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.druid.util.StringUtils;
import com.taotao.common.utils.JsonUtils;
import com.taotao.mapper.TbItemCatMapper;
import com.taotao.pojo.TbItemCat;
import com.taotao.pojo.TbItemCatExample;
import com.taotao.pojo.TbItemCatExample.Criteria;
import com.taotao.rest.dao.JedisClient;
import com.taotao.rest.pojo.CatNode;
import com.taotao.rest.pojo.CatResult;
import com.taotao.rest.service.ItemCatService;
@Service
public class ItemCatServiceImpl implements ItemCatService{

	@Autowired
	private TbItemCatMapper itemCatMapper;
	
	@Autowired
	private JedisClient jedisClient;
	
	@Value("INDEX_ITEMCAT_REDIS_KEY")
	private String INDEX_ITEMCAT_REDIS_KEY;
	
	
	
	@Override
	public CatResult getItemCatList() {
		CatResult catResult = new CatResult();
		//向缓存中取内容
		try {
			String result = jedisClient.get(INDEX_ITEMCAT_REDIS_KEY);
			if (!StringUtils.isEmpty(result)) {
				//把字符串转换成list
				catResult = JsonUtils.jsonToPojo(result, CatResult.class);
				return catResult;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		catResult.setData(getCatList(0));
		
		//向缓存中添加内容
		try {
			//把list转换成字符串
			String catchString = JsonUtils.objectToJson(catResult);
			jedisClient.set(INDEX_ITEMCAT_REDIS_KEY, catchString);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return catResult;
	}
	
	private List<?> getCatList(long parentId){
		
		//创建查询条件
		TbItemCatExample example = new TbItemCatExample();
		Criteria criteria = example.createCriteria();
		criteria.andParentIdEqualTo(parentId);
		//执行查询
		List<TbItemCat> list = itemCatMapper.selectByExample(example);
		//返回值list
		List resultList = new ArrayList();
		int count = 0;
		//向list中添加节点
		for (TbItemCat tbItemCat : list) {
			//判断是否为父节点
			if (tbItemCat.getIsParent()) {
				CatNode catNode = new CatNode();
				if(parentId == 0){
					catNode.setName("<a href='/products/"+tbItemCat.getId()+".html'>"+tbItemCat.getName()+"</a>");
				}else{
					catNode.setName(tbItemCat.getName());
				}
				catNode.setUrl("/products/"+tbItemCat.getId()+".html");
				catNode.setItem(getCatList(tbItemCat.getId()));
				
				resultList.add(catNode);
				count++;
				if(parentId == 0 && count >= 14){
					break;
				}
			} else {
				resultList.add("/products/"+tbItemCat.getId()+".html|"+tbItemCat.getName());
			}
			
		}
		
		return resultList;
	}

}
