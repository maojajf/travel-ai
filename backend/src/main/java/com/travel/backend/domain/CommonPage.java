package com.travel.backend.domain;

import com.github.pagehelper.PageInfo;
import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.List;

/**
 * 分页数据封装类
 * Created by macro on 2019/4/19.
 */
public class CommonPage<T> {

    public static class Names {
        public static final String PAGE = "page";
        public static final String PAGE_SIZE = "pageSize";
        public static final String TOTAL_ROWS = "totalRows";
        public static final String TOTAL_PAGES = "totalPages";
    }

    private static final long serialVersionUID = 1L;
    /**
     * 当前页
     */
    private int page = 1;
    /**
     * 每页数据条数
     */
    private int pageSize = 10;
    /**
     * 总数据数
     */
    private long totalRows = 0;
    /**
     * 总页数
     */
    private int totalPages = 0;
    /**
     * 数据记录集
     */
    private List<T> list = new ArrayList<T>();

    /**
     * 前端提交的查询参数（JSON格式字符串）, 可以为空看业务需要
     */
    private String query;

    /**
     *  后端返回的字段 可以为空看业务需要
     */
    private Object result;

    /* 是否有下一页,true是,false否 */
    private boolean hasNextPage;

    public CommonPage(){}

    public CommonPage(int page , int pageSize) {
        this.page = page;
        this.pageSize = pageSize;
    }

    /**
     * 将PageHelper分页后的list转为分页信息
     */
    public static <T> CommonPage<T> restPage(List<T> list) {
        PageInfo<T> pageInfo = new PageInfo<T>(list);
        return restPage(pageInfo, list);
    }

    public static <T> CommonPage<T> restPage(PageInfo pageInfo, List<T> list) {
        CommonPage<T> result = new CommonPage<T>();
        result.setTotalPages(pageInfo.getPages());
        result.setPage(pageInfo.getPageNum());
        result.setPageSize(pageInfo.getPageSize());
        result.setTotalRows(pageInfo.getTotal());
        result.setHasNextPage(pageInfo.isHasNextPage());
        result.setList(list);
        return result;
    }
    
    /**
     * 将SpringData分页后的list转为分页信息
     */
    public static <T> CommonPage<T> restPage(Page<T> pageInfo) {
        CommonPage<T> result = new CommonPage<T>();
        result.setTotalPages(pageInfo.getTotalPages());
        result.setPage(pageInfo.getNumber());
        result.setPageSize(pageInfo.getSize());
        result.setTotalRows(pageInfo.getTotalElements());
        result.setList(pageInfo.getContent());
        result.setHasNextPage(pageInfo.hasNext());
        return result;
    }

    /**
     * 将SpringData分页后的list转为分页信息
     */
    public static <T> CommonPage<T> restPage(CommonPage pageInfo,List<T> list) {
        CommonPage<T> result = new CommonPage<T>();
        result.setTotalPages(pageInfo.getTotalPages());
        result.setPage(pageInfo.getPage());
        result.setPageSize(pageInfo.getPageSize());
        result.setTotalRows(pageInfo.getTotalRows());
        result.setHasNextPage(pageInfo.isHasNextPage());
        result.setList(list);
        return result;
    }

    /**
     * 将myBatisPlus分页后的list转为分页信息
     */
    public static <T,R> CommonPage<T> restPage(com.baomidou.mybatisplus.extension.plugins.pagination.Page<R> page,List<T> list) {
        CommonPage<T> result = new CommonPage<T>();
        result.setTotalPages((int) page.getPages());
        result.setPage((int) page.getCurrent());
        result.setPageSize((int) page.getSize());
        result.setTotalRows(page.getTotal());
        result.setHasNextPage(page.hasNext());
        result.setList(list);
        return result;
    }

    /**
     * 创建一个包含指定列表数据的分页对象，根据page和pageSize进行分页
     *
     * @param list 数据记录集
     * @param page 当前页（从1开始）
     * @param pageSize 每页数据条数
     * @param totalRows 总数据数（用于计算总页数）
     * @return CommonPage对象，包含当前页的数据
     */
    public static <T> CommonPage<T> create(List<T> list, int page, int pageSize, long totalRows) {
        // 计算总页数
        int totalPages = (int) Math.ceil((double) totalRows / pageSize);

        // 计算当前页的起始和结束索引
        int startIndex = (page - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, list.size());

        // 创建CommonPage对象并设置属性
        CommonPage<T> commonPage = new CommonPage<>();

        if (startIndex >= endIndex) {
            commonPage.setPage(page);
            commonPage.setPageSize(pageSize);
            commonPage.setTotalRows(totalRows);
            commonPage.setTotalPages(totalPages);
            commonPage.setHasNextPage(false);
            commonPage.setList(new ArrayList<>());
            return commonPage;
        }

        // 获取当前页的数据
        List<T> pageList = list.subList(startIndex, endIndex);

        if (totalPages == 0) {
            commonPage.setPage(page);
            commonPage.setPageSize(pageSize);
            commonPage.setTotalRows(totalRows);
            commonPage.setTotalPages(totalPages);
            commonPage.setHasNextPage(false);
            commonPage.setList(new ArrayList<>());
            return commonPage;
        }

        // 检查当前页是否有效
        if (page < 1 || page > totalPages) {
            throw new IllegalArgumentException("Invalid page number: " + page);
        }

        commonPage.setPage(page);
        commonPage.setPageSize(pageSize);
        commonPage.setTotalRows(totalRows);
        commonPage.setTotalPages(totalPages);
        commonPage.setHasNextPage(page < totalPages);
        commonPage.setList(pageList);

        return commonPage;
    }

    public static <T> CommonPage<T> create2(List<T> list, int page, int pageSize, long totalRows) {
        // 计算总页数
        int totalPages = (int) Math.ceil((double) totalRows / pageSize);

        // 创建CommonPage对象并设置属性
        CommonPage<T> commonPage = new CommonPage<>();

        if (totalPages == 0) {
            commonPage.setPage(page);
            commonPage.setPageSize(pageSize);
            commonPage.setTotalRows(totalRows);
            commonPage.setTotalPages(totalPages);
            commonPage.setHasNextPage(false);
            commonPage.setList(new ArrayList<>());
            return commonPage;
        }

        // 检查当前页是否有效
        if (page < 1 || page > totalPages) {
            throw new IllegalArgumentException("Invalid page number: " + page);
        }

        commonPage.setPage(page);
        commonPage.setPageSize(pageSize);
        commonPage.setTotalRows(totalRows);
        commonPage.setTotalPages(totalPages);
        commonPage.setHasNextPage(page < totalPages);
        commonPage.setList(list);

        return commonPage;
    }
    // 为解决不做count，但是需要知道是否有下一页的问题，查询时list为pageSize+1 ，如果大于，说明有下一页
    // 适用前端不展示总页数的场景，大数据下count 比较耗时，前端只做上一页，下一页即可
    public static <T> CommonPage<T> create3(List<T> list, int page, int pageSize) {

        // 创建CommonPage对象并设置属性
        CommonPage<T> commonPage = new CommonPage<>();
        // 检查当前页是否有效
        if (page < 1 ) {
            throw new IllegalArgumentException("Invalid page number: " + page);
        }

        commonPage.setPage(page);
        commonPage.setPageSize(pageSize);
        commonPage.setTotalRows(0L);
        commonPage.setHasNextPage(list.size() > pageSize);
        commonPage.setTotalPages(commonPage.hasNextPage? page+1 : page);// 兼容前端判断totalPage，有下一页则page+1
        commonPage.setList(commonPage.hasNextPage ? list.subList(0, pageSize) : list);

        return commonPage;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public long getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(long totalRows) {
        this.totalRows = totalRows;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public boolean isHasNextPage() {
        return hasNextPage;
    }

    public void setHasNextPage(boolean hasNextPage) {
        this.hasNextPage = hasNextPage;
    }
}