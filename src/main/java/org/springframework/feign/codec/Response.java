package org.springframework.feign.codec;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 *
 * @author shanhuiming
 *
 */
@Slf4j
@Data
public class Response<T> {

	/** 响应数据 */
	private T data;

	/** 响应码 */
	private String code;

	/** 响应描述 */
	private String msg;

	/** 错误堆栈信息 */
	private List<String> cause;

	public Response(){

	}

	public Response(String code, String msg, T data){
		this.code = code;
		this.msg = msg;
		this.data = data;
	}

	@Override
	public String toString() {
		return "{code=" + code + ", msg=" + msg + ", data=" + data + "}";
	}

	/**
	 * status=200, code=#{HttpCode.code}, msg=#{HttpCode.msg}, data=null
	 */
	public static <V> Response<V> code(HttpCode httpCode){
		return new Response<>(httpCode.getCode(), httpCode.getMsg(), null);
	}

	/**
	 * status=200, code=#{HttpCode.code}, msg=#{HttpCode.msg}, data=#{data}
	 */
	public static <V> Response<V> data(HttpCode httpCode, V data){
		return new Response<>(httpCode.getCode(), httpCode.getMsg(), data);
	}

	/**
	 * status=200, code=#{HttpCode.code}, msg=#{msg}, data=null
	 */
	public static <V> Response<V> msg(HttpCode httpCode, String msg){
		return new Response<>(httpCode.getCode(), msg, null);
	}

	/**
	 * status=200, code=200, msg="success", data=null
	 */
	public static <V> Response<V> success(){
        return new Response<>(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
    }

	/**
	 * status=200, code=200, msg="success", data=#{data}
	 */
	public static <V> Response<V> success(V data){
        return new Response<>(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), data);
    }

	/**
	 * status=200, code=200, msg=#{msg}, data=#{data}
	 */
	public static <V> Response<V> success(V data, String msg){
		return new Response<>(ResponseCode.SUCCESS.getCode(), msg, data);
	}

	/**
	 * status=200, code=500, msg="Internal Server Error", data=null
	 */
	public static <V> Response<V> error(){
		return new Response<>(ResponseCode.INTERNAL_SERVER_ERROR.getCode(), ResponseCode.INTERNAL_SERVER_ERROR.getMsg(), null);
	}

	/**
	 * status=200, code=500, msg=#{msg}, data=null
	 */
	public static <V> Response<V> error(String msg){
		return new Response<>(ResponseCode.INTERNAL_SERVER_ERROR.getCode(), msg, null);
	}

	/**
	 * status=200, code=200, msg="success", data=#{page}
	 */
	public static <E> Response<Page<E>> page(List<E> list){
		Response<Page<E>> response = new Response<>(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
		if(list == null){
			list = new ArrayList<>();
		}

		if(list instanceof com.github.pagehelper.Page<E> page){
			response.setData(new Page<>(page, page.getTotal()));
		}else {
			response.setData(new Page<>(list, list.size()));
		}
		return response;
	}

	/**
	 * status=200, code=200, msg="success", data=#{page}
	 */
	public static <E> Response<Page<E>> page(com.baomidou.mybatisplus.extension.plugins.pagination.Page<E> page){
		Response<Page<E>> response = new Response<>(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
		response.setData(new Page<>(page.getRecords(), page.getTotal()));
		return response;
	}

	/**
	 * status=200, code=200, msg="success", data=#{page}
	 */
	public static <T, E> Response<Page<E>> page(com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> page, Class<E> clazz){
		Response<Page<E>> response = new Response<>(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
		response.setData(new Page<>(copyList(page.getRecords(), clazz), page.getTotal()));
		return response;
	}

	/**
	 * status=200, code=200, msg="success", data=#{page}
	 */
	public static <T, E> Response<Page<E>> page(com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> page, Function<T, E> mapper) {
		Response<Page<E>> response = new Response<>(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
		response.setData(new Page<>(page.getRecords().stream().map(mapper).toList(), page.getTotal()));
		return response;
	}

	/**
	 * status=200, code=200, msg="success", data=#{page}
	 */
	public static <T, E> Response<Page<E>> page(com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> page, Function<T, E> mapper, Predicate<T> filter) {
		Response<Page<E>> response = new Response<>(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
		response.setData(new Page<>(page.getRecords().stream().filter(filter).map(mapper).toList(), page.getTotal()));
		return response;
	}

	static <E, T> E copyBean(T src, Class<E> clazz) {
		E target = null;
		try {
			target = clazz.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		BeanUtils.copyProperties(src, target);
		return target;
	}

	static <E, T> List<E> copyList(List<T> srcList, Class<E> clazz) {
		if (CollectionUtils.isEmpty(srcList)) {
			return Collections.emptyList();
		}
		return srcList.stream().map(src -> copyBean(src, clazz)).toList();
	}

	public static class Page<E> {

		/** 总数 */
		private int total;

		/** 列表数据 */
		private Collection<E> list;

		/** 页码 */
		private int page;

		/** 每页行数 */
		private int pageSize;

		/** 总页数 */
		private int totalPage;

		public Page(){

		}

		public Page(Collection<E> list, int total){
			this.list = list;
			this.total = total;
		}

		public Page(Collection<E> list, long total){
			this.list = list;
			this.total = (int)total;
		}

		public int getTotal() {
			return total;
		}

		@Deprecated
		public void setTotalRows(int total) {
			this.total = total;
		}

		@Deprecated
		public void setTotalRows(long total) {
			this.total = (int)total;
		}

		public void setTotal(int total) {
			this.total = total;
		}

		public void setTotal(long total) {
			this.total = (int)total;
		}

		public int getTotalPage() {
			return totalPage;
		}

		public void setTotalPage(int totalPage) {
			this.totalPage = totalPage;
		}

		public void setTotalPage(long totalPage) {
			this.totalPage = (int)totalPage;
		}

		public Collection<E> getList() {
			return list;
		}

		public void setList(Collection<E> list) {
			this.list = list;
		}

		public int getPage() {
			return page;
		}

		public int getPageSize() {
			return pageSize;
		}

		public void setPage(int page) {
			this.page = page;
		}

		public void setPageSize(int pageSize) {
			this.pageSize = pageSize;
		}

		public void setPage(long page) {
			this.page = (int)page;
		}

		public void setPageSize(long pageSize) {
			this.pageSize = (int)pageSize;
		}

		@Override
		public String toString() {
			return "{total=" + total + ", list=" + list + "}";
		}
	}
}
