package com.mmall.service.impl;

import com.mmall.common.Const;
import com.mmall.common.ServerResponse;
import com.mmall.common.TokenCache;
import com.mmall.dao.UserMapper;
import com.mmall.pojo.User;
import com.mmall.service.IUserService;
import com.mmall.util.MD5Util;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("iUserService")
public class IUserServiceImpl implements IUserService {
    @Autowired
    private UserMapper userMapper;

    // 登陆
    @Override
    public ServerResponse<User> login(String username, String password) {
        // 判断用户名是否存在
        int resultCount = userMapper.checkUsername(username);
        if (resultCount == 0) {
            return ServerResponse.createByErrorMessage("用户名不存在");
        }

        String md5Password = MD5Util.MD5EncodeUtf8(password);
        // 这里返回一个User对象，通过resultMap="BaseResultMap"实现，而resultMap id="BaseResultMap" type="com.mmall.pojo.User"
        User user = userMapper.selectLogin(username, md5Password);
        if (user == null) {
            // 这里能肯定是密码错误，因为逻辑到了这里，肯定是username存在，否则在前面验证username的时候就return了
            return ServerResponse.createByErrorMessage("密码错误");
        }
        /*
        这里将password置为空，是因为我们返回给前端的return里面包含了user对象，让前端获得user对象的password存在安全隐患，而使用
        StringUtils.EMPTY替代""是为了让代码更优雅，至于为什么要将user返回给前端，因为登陆成功了，肯定要将对象返回给前端啊
         */
        user.setPassword(StringUtils.EMPTY);
        return ServerResponse.createBySuccess("登陆成功", user);
    }

    // 注册
    public ServerResponse<String> register(User user) {
        int resultCount = userMapper.checkUsername(user.getUsername());
        if (resultCount > 0) {
            return ServerResponse.createByErrorMessage("用户名已存在");
        }

        // 这里检查注册邮箱是否已存在，继续用resultCount没问题，因为逻辑到了这里resultCount还是0
        resultCount = userMapper.checkEmail(user.getEmail());
        if (resultCount > 0) {
            return ServerResponse.createByErrorMessage("Email已存在");
        }

        // 这里我自己添加一个检查手机号码的判断
        resultCount = userMapper.checkPhone(user.getPhone());
        if (resultCount > 0) {
            return ServerResponse.createByErrorMessage("该手机已注册，请直接登陆");
        }

        // 网站只能注册普通用户，不能注册admin用户，这里一定要设定好该user的role属性，因为数据库里设定了role字段非空
        user.setRole(Const.Role.ROLE_CUSTOMER);
        // user.setRole(Const.Role.ROLE_ADMIN);
        // MD5加密，这里我暂时没有使用salt值
        user.setPassword(MD5Util.MD5EncodeUtf8(user.getPassword()));
        resultCount = userMapper.insert(user);
        if (resultCount == 0) {
            return ServerResponse.createByErrorMessage("注册失败");
        }
        return ServerResponse.createBySuccessMessage("注册成功");
    }

    // 检查用户名或邮箱是否已经存在；这个功能有点不理解？？？？？？？？？？？？？？？？
    @Override
    public ServerResponse<String> checkValid(String input, String type) {
        if (StringUtils.isNotBlank(type)) {
            // 开始校验:这里只检查username和email，所以这里进行判断
            if ((Const.USERNAME.equals(type))) {
                int resultCount = userMapper.checkUsername(input);
                if (resultCount > 0) {
                    return ServerResponse.createByErrorMessage("用户名已存在");
                }
            }
            if ((Const.EMAIL.equals(type))) {
                int resultCount = userMapper.checkEmail(input);
                if (resultCount > 0) {
                    return ServerResponse.createByErrorMessage("Email已存在");
                }
            }

        }else {
            return ServerResponse.createByErrorMessage("参数错误");
        }
        return ServerResponse.createBySuccessMessage("校验成功");
    }

    // 忘记密码时通过安全问题找回，校验安全问题是否存在
    public ServerResponse<String> selectQuestion(String username) {
        ServerResponse validResponse = this.checkValid(username, Const.USERNAME);
        if (validResponse.isSuccess()) {
            // 用户不存在
            return ServerResponse.createByErrorMessage("用户不存在");
        }
        String question = userMapper.selectQuestionByUsername(username);
        if (StringUtils.isNotBlank(question)) {
            return ServerResponse.createBySuccess(question);
        }
        return ServerResponse.createByErrorMessage("没有设置密码的问题找回");
    }

    // 忘记密码时通过安全问题找回，校验安全问题的答案
    @Override
    public ServerResponse<String> checkAnswer(String username, String question, String answer) {
        // 这里应该增加一个用户名是否存在的判断
        int resultCount = userMapper.checkUsername(username);
        if (resultCount == 0) {
            return ServerResponse.createByErrorMessage("用户名不存在");
        }

        //  其实还应该增加一个password的入参，毕竟校验安全问题和答案需要密码

        // 增加一个question的判断，不能直接去判断answer
        resultCount = userMapper.checkQuestion(question);
        if (resultCount == 0) {
            return ServerResponse.createByErrorMessage("安全提示问题不正确");
        }

        resultCount = userMapper.checkAnswer(username, question, answer);
        if (resultCount > 0) {
            String forgetToken = UUID.randomUUID().toString();
            // 这里简单理解为将token放进TokenCache，等要需要的时候再get出来
            TokenCache.setKey(TokenCache.TOKEN_PREFIX + username, forgetToken);
            return ServerResponse.createBySuccess(forgetToken); // 要将token放到ServerResponse返回给前端
        }
        return ServerResponse.createByErrorMessage("安全提示问题的答案不正确");
    }

    // 非登陆状态重置密码
    @Override
    public ServerResponse<String> forgetResetPassword(String username, String passwordNew, String forgetToken) {
        if (StringUtils.isBlank(forgetToken)) { // 这里forgetToken的值是从前端传来的，也是我们之前put进去的
            return ServerResponse.createByErrorMessage("参数错误，token需要传递");
        }
        ServerResponse validResponse = this.checkValid(username, Const.USERNAME);
        if (validResponse.isSuccess()) {
            // 用户不存在
            return ServerResponse.createByErrorMessage("用户不存在");
        }
        // 这里新生成一个token，采用同样的方法，所以肯定跟上个方法里的forgetToken值一样
        String token = TokenCache.getKey(TokenCache.TOKEN_PREFIX + username);
        if (StringUtils.isBlank(token)) {
            return ServerResponse.createByErrorMessage("token无效或已过期");
        }
        if (StringUtils.equals(forgetToken, token)) {
            String md5Password = MD5Util.MD5EncodeUtf8(passwordNew);
            int rowCount = userMapper.updatePasswordByUsername(username, md5Password);
            if (rowCount > 0) {
                return ServerResponse.createBySuccessMessage("修改密码成功");
            }
        }else {
            return ServerResponse.createByErrorMessage("token错误，请重新获取token");
        }
        return ServerResponse.createByErrorMessage("密码修改失败");
    }

    // 登陆状态重置密码
    @Override
    public ServerResponse<String> resetPassword(String passwordOld, String passwordNew, User user) {
        // 防止横向越权，要校验一下这个用户的旧密码，一定要指定是这个用户，因为我们会查询一个count(1)，如果不指定id，那么结果就是true
        int resultCount = userMapper.checkPassword(MD5Util.MD5EncodeUtf8(passwordOld), user.getId());
        if (resultCount == 0) {
            return ServerResponse.createByErrorMessage("旧密码错误");
        }
        user.setPassword(MD5Util.MD5EncodeUtf8(passwordNew));
        int updateCount = userMapper.updateByPrimaryKeySelective(user);
        if (updateCount > 0) {
            return ServerResponse.createBySuccessMessage("密码更新成功");
        }
        return ServerResponse.createByErrorMessage("密码更新失败");
    }

    // 更新用户个人信息
    public ServerResponse<User> updateInformation(User user) {
        // username不能被更新
        // email也要校验，更新后的email是不是已存在，并且存在的email如果相同，不能是我们当前这个用户
        int resultCount = userMapper.checkEmailByUserId(user.getEmail(), user.getId());
        if (resultCount > 0) {
            return ServerResponse.createByErrorMessage("email已存在，请更换email再尝试更新");
        }
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setEmail(user.getEmail());
        updateUser.setPhone(user.getPhone());
        updateUser.setQuestion(user.getQuestion());
        updateUser.setAnswer(user.getAnswer());
        int updateCount = userMapper.updateByPrimaryKeySelective(updateUser);
        if (updateCount > 0) {
            return ServerResponse.createBySuccess("更新个人信息成功", updateUser);
        }
        return ServerResponse.createBySuccessMessage("更新个人信息失败");
    }

    // 获取用户个人信息
    public ServerResponse<User> getInformation(Integer userId) {
        User user = userMapper.selectByPrimaryKey(userId);
        if (user == null) {
            return ServerResponse.createByErrorMessage("找不到当前用户");
        }
        user.setPassword(StringUtils.EMPTY);
        return ServerResponse.createBySuccess(user);
    }

    // 校验是否是管理员身份
    public ServerResponse checkAdminRole(User user) {
        if (user != null && user.getRole() == Const.Role.ROLE_ADMIN) {
            return ServerResponse.createBySuccess();
        }
        return ServerResponse.createByError();
    }
}






