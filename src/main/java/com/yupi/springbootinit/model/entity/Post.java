package com.yupi.springbootinit.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.yupi.springbootinit.config.mybatis.ListToStringHandler;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;

import static com.baomidou.mybatisplus.annotation.FieldStrategy.NOT_NULL;

/**
 * 帖子
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@TableName(value = "post", autoResultMap = true)
@Data
public class Post implements Serializable {

    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 标签列表 json
     */
    private String tags;

    /**
     * 点赞数
     */
    private Integer thumbNum;

    /**
     * 收藏数
     */
    private Integer favourNum;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 用户名称
     */
    private String userName;

    private Long postId;

    private String cover;

    private String description;

    @TableField(jdbcType = JdbcType.VARCHAR, insertStrategy = NOT_NULL, typeHandler = ListToStringHandler.class)
    private List<String> fileList;

    private boolean hasFavour;

    private boolean hasThumb;

    private String language;

    private String needVip;

    @TableField(jdbcType = JdbcType.VARCHAR, insertStrategy = NOT_NULL, typeHandler = ListToStringHandler.class)
    private List<String> pictureList;

    /**
     * 优先级
     */
    private Long priority;

    private String reviewMessage;

    private Integer reviewStatus;

    private String reviewTime;

    private Long reviewerId;

    private Long commentNum;

    @TableField(jdbcType = JdbcType.VARCHAR, insertStrategy = NOT_NULL, typeHandler = ListToStringHandler.class)
    private List<String> videoList;

    private Long viewNum;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}