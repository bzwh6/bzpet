package com.bzpet.petuser.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.bzpet.common.constants.RedisConstants;
import com.bzpet.common.domain.R;
import com.bzpet.common.exception.BadException;
import com.bzpet.common.utils.UserContext;
import com.bzpet.petjwt.utils.JwtUtil;
import com.bzpet.petuser.domain.dto.LoginFormDTO;
import com.bzpet.petuser.domain.dto.RegisterDTO;
import com.bzpet.petuser.domain.dto.UserInfoDTO;
import com.bzpet.petuser.domain.po.User;
import com.bzpet.petuser.domain.vo.UserInfoVO;
import com.bzpet.petuser.domain.vo.UserVO;
import com.bzpet.petuser.mapper.UserMapper;
import com.bzpet.petuser.service.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author bzwh
 * @since 2026-08-20
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public R<String> logIn(LoginFormDTO loginFormDTO) {
        //从数据库里根据用户名查询用户
        User user = lambdaQuery()
                .eq(User::getUsername, loginFormDTO.getUsername())
                .one();
        if (user == null) {
            throw new BadException("用户名错误！");
        }
        if (!BCrypt.checkpw(loginFormDTO.getPassword(), user.getPassword())) {
            throw new BadException("密码错误！");
        }
        //生成jwt
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("userId", user.getId());
        String token = JwtUtil.genToken(userMap);
        //将token存入redis
        String key = RedisConstants.KEY.USER_TOKEN_KEY;
        stringRedisTemplate.opsForValue()
                .set(key + token, user.getId().toString()
                        , RedisConstants.EXPIRE.USER_TOKEN_EXPIRE, TimeUnit.SECONDS);
        return R.ok(token);
    }

    @Override
    public R<String> register(RegisterDTO registerDTO) {
        //直接插入
        User user = new User();
        BeanUtil.copyProperties(registerDTO, user);
        String password = registerDTO.getPassword();
        //对密码加密
        String hashpw = BCrypt.hashpw(password);
        user.setPassword(hashpw);
        //如果有一样的用户名会报异常
        try {
            save(user);
        } catch (Exception e) {
            throw new BadException("用户名已存在！");
        }
        return R.ok("注册成功。");
    }

    @Override
    public R<UserInfoVO> userInfo(Long id) {
        User user = getById(id);
        if (user == null) {
            throw new BadException("用户不存在！");
        }
        UserInfoVO userInfoVO = new UserInfoVO();
        BeanUtil.copyProperties(user, userInfoVO);
        return R.ok(userInfoVO);
    }

    @Override
    public R<List<UserVO>> userInfoList(List<Long> ids) {
        List<User> list = lambdaQuery()
                .in(User::getId, ids)
                .list();
        List<UserVO> vo = new ArrayList<>(list.size());
        for (User user : list) {
            UserVO userVO = new UserVO();
            Long id = user.getId();
            String nickname = user.getNickname();
            String avatar = user.getAvatar();
            userVO.setId(id);
            userVO.setNickname(nickname);
            userVO.setAvatar(avatar);
            vo.add(userVO);
        }
        return R.ok(vo);
    }

    @Override
    public R<String> logOut(String token) {
        //从redis里删除token
        stringRedisTemplate.delete(RedisConstants.KEY.USER_TOKEN_KEY + token);
        return R.ok("退出登录成功。");
    }

    /**
     * 更新用户基础信息
     *
     * @param userInfoDTO
     * @return
     */
    @Override
    public R<String> updateUserInfo(UserInfoDTO userInfoDTO) {
        //获取用户id
        Long userId = UserContext.getUserId();
        //根据用户id查询用户
        User user = getById(userId);
        if (user == null) {
            throw new BadException("用户不存在！");
        }
        //更新用户信息
        lambdaUpdate()
                .eq(User::getId, userId)
                .set(userInfoDTO.getNickname() != null, User::getNickname, userInfoDTO.getNickname())
                .set(userInfoDTO.getAvatar() != null, User::getAvatar, userInfoDTO.getAvatar())
                .update();
        return R.ok("更新成功。");
    }

    /**
     * 更新用户密码
     *
     * @param password
     * @return
     */
    @Override
    public R<String> updatePassword(String password) {
        //获取用户id
        Long userId = UserContext.getUserId();
        //根据用户id查询用户
        User user = getById(userId);
        if (user == null) {
            throw new BadException("用户不存在！");
        }
        //校验密码是否正确
        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new BadException("密码错误！");
        }
        //更新用户密码
        String hashpw = BCrypt.hashpw(password);
        user.setPassword(hashpw);
        updateById(user);
        return R.ok("密码更新成功。");
    }
}
