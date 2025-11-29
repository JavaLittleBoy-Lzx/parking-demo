package com.parkingmanage.mapper;

import com.parkingmanage.entity.Book;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 <p>
  Mapper 接口
 </p>

 @author 李子雄
 @since 2023-07-09
*/
public interface BookMapper extends BaseMapper<Book> {
    Book getBookByOpenId(String openid);

    int duplicate(Book book);
}
