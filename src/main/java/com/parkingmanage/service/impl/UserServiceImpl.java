package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parkingmanage.common.exception.ServiceException;
import com.parkingmanage.entity.User;
import com.parkingmanage.mapper.UserMapper;
import com.parkingmanage.service.UserService;
import com.parkingmanage.utils.AesUtil;
import com.parkingmanage.utils.TokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 <p>
 服务实现类
 </p>

 @author yuli
 @since 2022-02-27
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Resource
    private HttpServletRequest request;
    @Autowired UserMapper userMapper;
    @Override
    public User login(String loginName, String password) {
        User user = userMapper.selectByUserName(loginName);
        if (user == null) {
            throw new ServiceException("用户名不存在或用户名输入错误");
        }
        
        // 检查账户是否被锁定
        if (user.getLockTime() != null) {
            LocalDateTime now = LocalDateTime.now();
            long minutesLocked = ChronoUnit.MINUTES.between(user.getLockTime(), now);
            
            if (minutesLocked < 10) {
                // 还在锁定期内
                long remainingMinutes = 10 - minutesLocked;
                throw new ServiceException("账户已被锁定，请" + remainingMinutes + "分钟后再试");
            } else {
                // 锁定时间已过，解除锁定
                user.setLockTime(null);
                user.setFailedLoginCount(0);
                this.updateById(user);
            }
        }
        
        // 验证密码
        if (!AesUtil.encrypt(password).equals(user.getPassword())) {
            // 密码错误，增加失败次数
            int failedCount = (user.getFailedLoginCount() != null ? user.getFailedLoginCount() : 0) + 1;
            user.setFailedLoginCount(failedCount);
            
            // 如果失败次数达到5次，锁定账户10分钟
            if (failedCount >= 5) {
                user.setLockTime(LocalDateTime.now());
                this.updateById(user);
                throw new ServiceException("密码错误次数过多，账户已被锁定10分钟，请稍后再试");
            } else {
                // 更新失败次数
                this.updateById(user);
                int remainingAttempts = 5 - failedCount;
                throw new ServiceException("用户名或密码输入错误，还可尝试" + remainingAttempts + "次");
            }
        }
        
        // 登录成功，重置失败次数和锁定时间
        if (user.getFailedLoginCount() != null && user.getFailedLoginCount() > 0) {
            user.setFailedLoginCount(0);
            user.setLockTime(null);
            this.updateById(user);
        }
        
        String token = TokenUtils.genToken(user.getUserId().toString(), user.getPassword());
        user.setToken(token);
        
        return user;
    }

    @Override
    public boolean verifyPassword(User user, String password) {
        if (user == null || password == null) {
            return false;
        }
        return AesUtil.encrypt(password).equals(user.getPassword());
    }
}
