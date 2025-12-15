package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parkingmanage.entity.Book;
import com.parkingmanage.mapper.BookMapper;
import com.parkingmanage.service.IBookService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;

/**
 <p>
  服务实现类
 </p>

 @author lzx
 @apiNote BookService
 @since 2023-07-09
*/
@Service
public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements IBookService {

    @Resource
    private IBookService bookService;

    @Override
    public Book getBookByOpenId(String openid) {
        return baseMapper.getBookByOpenId(openid);
    }

    @Override
    public int duplicate(Book book) {
        return baseMapper.duplicate(book);
    }

    @Override
    public List<Book> queryListBook(String name) {
        LambdaQueryWrapper<Book> queryWrapper = new LambdaQueryWrapper();
        if (StringUtils.hasLength(name)) {
            queryWrapper.like(Book::getName, name);
        }
        List<Book> books = bookService.list(queryWrapper);

        return books;
    }
}
