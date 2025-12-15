package com.parkingmanage.service;

import com.parkingmanage.entity.Book;
import com.baomidou.mybatisplus.extension.service.IService;
import com.parkingmanage.entity.Patrol;

import java.util.List;

/**
 <p>
  服务类
 </p>

 @author 李子雄
 @since 2023-07-09
*/
public interface IBookService extends IService<Book> {
    Book getBookByOpenId(String openid);
    int duplicate(Book book);
    List<Book> queryListBook(String name);
}
