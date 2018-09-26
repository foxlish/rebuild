/*
Copyright 2018 DEVEZHAO(zhaofang123@gmail.com)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.rebuild.web.admin.entitymanage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.entitymanage.DisplayType;
import com.rebuild.server.service.entitymanage.EasyMeta;
import com.rebuild.server.service.entitymanage.Field2Schema;
import com.rebuild.web.BaseControll;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.dialect.Type;
import cn.devezhao.persist4j.engine.ID;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 08/19/2018
 */
@Controller
@RequestMapping("/admin/entity/")
public class MetaFieldControll extends BaseControll  {
	
	@RequestMapping("{entity}/fields")
	public ModelAndView pageEntityFields(@PathVariable String entity, HttpServletRequest request) throws IOException {
		ModelAndView mv = createModelAndView("/admin/entity/fields.jsp");
		MetaEntityControll.setEntityBase(mv, entity);
		return mv;
	}
	
	@RequestMapping("list-field")
	public void listField(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String entityName = getParameter(request, "entity");
		Entity entity = MetadataHelper.getEntity(entityName);
		if (entity == null) {
			writeFailure(response, "无效实体");
			return;
		}
		
		List<Map<String, Object>> ret = new ArrayList<>();
		for (Field field : entity.getFields()) {
			EasyMeta easyMeta = new EasyMeta(field);
			if (field.getType() == FieldType.PRIMARY) {
				continue;
			}
			
			Map<String, Object> map = new HashMap<>();
			if (easyMeta.getMetaId() != null) {
				map.put("fieldId", easyMeta.getMetaId().toLiteral());
			}
			map.put("fieldName", easyMeta.getName());
			map.put("fieldLabel", easyMeta.getLabel());
			map.put("comments", easyMeta.getComments());
			map.put("displayType", easyMeta.getDisplayType(true));
			map.put("nullable", field.isNullable());
			map.put("builtin", easyMeta.isBuiltin());
			ret.add(map);
		}
		writeSuccess(response, ret);
	}

	@RequestMapping("{entity}/field/{field}")
	public ModelAndView pageEntityFields(@PathVariable String entity, @PathVariable String field, HttpServletRequest request) throws IOException {
		ModelAndView mv = createModelAndView("/admin/entity/field-edit.jsp");
		EasyMeta easyMeta = MetaEntityControll.setEntityBase(mv, entity);
		
		Field fieldMeta = ((Entity) easyMeta.getBaseMeta()).getField(field);
		EasyMeta fieldEasyMeta = new EasyMeta(fieldMeta);
		
		mv.getModel().put("fieldMetaId", fieldEasyMeta.isBuiltin() ? null : fieldEasyMeta.getMetaId());
		mv.getModel().put("fieldName", fieldEasyMeta.getName());
		mv.getModel().put("fieldLabel", fieldEasyMeta.getLabel());
		mv.getModel().put("fieldComments", fieldEasyMeta.getComments());
		mv.getModel().put("fieldType", fieldEasyMeta.getDisplayType(true));
		mv.getModel().put("fieldNullable", fieldMeta.isNullable());
		mv.getModel().put("fieldUpdatable", fieldMeta.isUpdatable());
		
		// 字段类型相关
		Type ft = fieldMeta.getType();
		if (ft == FieldType.REFERENCE) {
			Entity refentity = fieldMeta.getReferenceEntities()[0];
			mv.getModel().put("fieldRefentity", refentity.getName());
			mv.getModel().put("fieldRefentityLabel", new EasyMeta(refentity).getLabel());
		} else {
			mv.getModel().put("fieldExtConfig", fieldEasyMeta.getFieldExtConfig());
		}
		
		return mv;
	}
	
	@RequestMapping("field-new")
	public void fieldNew(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		String entityName = getParameterNotNull(request, "entity");
		String label = getParameterNotNull(request, "label");
		String type = getParameterNotNull(request, "type");
		String comments = getParameter(request, "comments");
		String refEntity = getParameter(request, "refEntity");
		
		Entity entity = MetadataHelper.getEntity(entityName);
		DisplayType dt = DisplayType.valueOf(type);
		
		String fieldName = null;
		try {
			fieldName = new Field2Schema(user).create(entity, label, dt, comments, refEntity);
			writeSuccess(response, fieldName);
		} catch (Exception ex) {
			writeFailure(response, ex.getLocalizedMessage());
			return;
		}
	}
	
	@RequestMapping("field-update")
	public void fieldUpdate(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		JSON formJson = ServletUtils.getRequestJson(request);
		Record record = EntityHelper.parse((JSONObject) formJson, user);
		Application.getCommonService().update(record);
		
		MetadataHelper.refreshMetadata();
		writeSuccess(response);
	}
}