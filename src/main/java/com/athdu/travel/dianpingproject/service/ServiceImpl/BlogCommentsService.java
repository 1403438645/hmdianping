package com.athdu.travel.dianpingproject.service.ServiceImpl;


import com.athdu.travel.dianpingproject.entity.BlogComments;
import com.athdu.travel.dianpingproject.mapper.BlogCommentsMapper;
import com.athdu.travel.dianpingproject.service.IBlogCommentsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * @author baizhejun
 * @create 2022 -10 -25 - 11:59
 */
@Service
public class BlogCommentsService extends ServiceImpl<BlogCommentsMapper, BlogComments> implements IBlogCommentsService {
}
