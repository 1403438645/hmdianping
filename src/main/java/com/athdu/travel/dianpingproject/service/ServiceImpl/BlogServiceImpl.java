package com.athdu.travel.dianpingproject.service.ServiceImpl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.athdu.travel.dianpingproject.dto.Result;
import com.athdu.travel.dianpingproject.dto.ScrollResult;
import com.athdu.travel.dianpingproject.dto.UserDTO;
import com.athdu.travel.dianpingproject.entity.Blog;
import com.athdu.travel.dianpingproject.entity.Follow;
import com.athdu.travel.dianpingproject.entity.User;
import com.athdu.travel.dianpingproject.mapper.BlogMapper;
import com.athdu.travel.dianpingproject.service.IBlogService;
import com.athdu.travel.dianpingproject.service.IFollowService;
import com.athdu.travel.dianpingproject.service.IUserService;
import com.athdu.travel.dianpingproject.utils.SystemConstants;
import com.athdu.travel.dianpingproject.utils.UserHolder;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.athdu.travel.dianpingproject.utils.RedisConstants.BLOG_LIKED_KEY;
import static com.athdu.travel.dianpingproject.utils.RedisConstants.FEED_KEY;

/**
 * @author baizhejun
 * @create 2022 -10 -24 - 20:18
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
    @Resource
    private IUserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IFollowService followService;




    @Override
    public Result queryHotBlog(Integer current) {
//        根据用户查询
        Page<Blog> page = query().orderByDesc("liked").page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));

//        获取当前页数据
        List<Blog> records = page.getRecords();
//        查询用户
        records.forEach(blog -> {
            this.queryBlogUser(blog);
            this.isBlogLiked(blog);
        });
        return Result.ok(records);




    }

    private void isBlogLiked(Blog blog) {
//        获取登录用户
        UserDTO user = UserHolder.getUser();
        if (user == null){
//            用户未登录
            return;
        }
        Long userId = user.getId();
//        判断当前登录用户是否已经点赞
        String key = "{blog:like}" + blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key,userId.toString());
        blog.setIsLike(score != null);
    }

    @Override
    public Result queryBlogById(Long id) {
//        查询blog
        Blog byId = getById(id);
        if (byId == null){
            return Result.fail("笔记不存在");

        }
//        查询blog有关的用户
        queryBlogUser(byId);
//        查询blog是否被点赞
        isBlogLiked(byId);

        return Result.ok(byId);
    }

    @Override
    public Result likeBlog(Long id) {
//        获取登录用户
        Long userId = UserHolder.getUser().getId();
//        判断当前用户是否已经点赞
        String key = BLOG_LIKED_KEY + id;
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if (score == null){
//            如果没有点赞，就可以点赞
//            点赞之后数据库+1
            boolean isSuceess = update().setSql("liked = liked + 1").eq("id", id).update();
            if (isSuceess){
                stringRedisTemplate.opsForZSet().add(key,userId.toString(),System.currentTimeMillis());
            }
        }else{
//            如果已经点赞，取消点赞
            boolean isSuccess = update().setSql("liked = liked -1 ").eq("id", id).update();
            if (isSuccess){
                stringRedisTemplate.opsForZSet().remove(key,userId.toString());
            }
        }

        return Result.ok();
    }

    @Override
    public Result queryBlogLikes(Long id) {
        String key = BLOG_LIKED_KEY + id;
//        查询top5的点赞用户
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 4);
//        防止空指针
        if (top5 == null || top5.isEmpty()){
            return Result.ok(Collections.emptyList());

        }
//        解析出其中用户id
        List<Long> ids = top5.stream().map(n -> Long.valueOf(n)).collect(Collectors.toList());
        String idStr = StrUtil.join(",", ids);
//        根据用户id查询用户 WHERE id IN ( 5 , 1 ) ORDER BY FIELD(id, 5, 1)
        List<UserDTO> userDTOS = userService.query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list()
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());

        return Result.ok(userDTOS);
    }

    @Override
    public Result saveBlog(Blog blog) {
//        获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
//        保存探店笔记
        boolean isSuccess = save(blog);
        if (!isSuccess){
            return Result.fail("保存新增笔记失败");
        }
//        查询笔记作者所有粉丝 select * from tb_follow where follow_user_id = ?
        List<Follow> follows = followService.query().eq("follow_user_id", user.getId()).list();
//        推送笔记id给所有粉丝
        for (Follow follow : follows) {
//            获取粉丝id
            Long userId = follow.getUserId();
//            推送
            String key = FEED_KEY + userId;
            stringRedisTemplate.opsForZSet().add(key,blog.getId().toString(),System.currentTimeMillis());
        }

        return Result.ok(blog.getId());
    }

    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
//       1. 获取当前的用户
        Long userid = UserHolder.getUser().getId();
//       2. 查询收件箱 ZREVRANGEBYSCORE key Max Min LIMIT offset count
        String key = FEED_KEY +userid;
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet().reverseRangeByScoreWithScores(key, 0, max, offset, 2);
//        非空判断
        if (typedTuples == null || typedTuples.isEmpty()){
            return Result.ok();
        }
//       3. 解析数据： blogId、miniTime（时间戳）、offset
        List<Long> ids = new ArrayList<>(typedTuples.size());
        long minTime = 0;
        int os = 1;
        for (ZSetOperations.TypedTuple<String> tuple : typedTuples){
//            获取id
            ids.add(Long.valueOf(tuple.getValue()));
//            获取分数（时间戳）
            long time = tuple.getScore().longValue();
            if (time == minTime){
                os ++;
            }else {
                minTime = time;
                os =1;
            }
        }

//        4.根据id查询blog
        String idStr = StrUtil.join(",", ids);
        List<Blog> blogs = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();

        for (Blog blog : blogs) {
            // 5.1.查询blog有关的用户
            queryBlogUser(blog);
            // 5.2.查询blog是否被点赞
            isBlogLiked(blog);
        }

        // 5 .封装并返回
        ScrollResult r = new ScrollResult();
        r.setList(blogs);
        r.setOffset(os);
        r.setMinTime(minTime);

        return Result.ok(r);
    }

    private void queryBlogUser(Blog blog){
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }
}
